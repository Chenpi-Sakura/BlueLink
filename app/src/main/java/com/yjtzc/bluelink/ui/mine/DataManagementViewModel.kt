package com.yjtzc.bluelink.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import androidx.core.content.edit
import com.yjtzc.bluelink.data.local.crypto.SecurePrefs
import com.yjtzc.bluelink.data.local.db.AppDatabase
import com.yjtzc.bluelink.data.local.db.DocumentEntity
import com.yjtzc.bluelink.data.local.prefs.UserPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ItemScope { ALL, SELECTED }

data class ExportUiState(
    val scope: ItemScope = ItemScope.ALL,
    val selectedDocumentIds: Set<String> = emptySet(),
    val exportDocuments: Boolean = true,
    val exportInspirationCards: Boolean = true,
    val exportGraph: Boolean = true,
    val exportSettings: Boolean = true,
    val isExporting: Boolean = false,
    val exportDone: Boolean = false,
    val errorMessage: String? = null
) {
    val hasSelectedAnyContent: Boolean
        get() = exportDocuments || exportInspirationCards || exportSettings

    val hasValidDocumentScope: Boolean
        get() = scope == ItemScope.ALL || selectedDocumentIds.isNotEmpty()

    val canExport: Boolean
        get() = hasSelectedAnyContent && hasValidDocumentScope && !isExporting
}

data class DeleteUiState(
    val scope: ItemScope = ItemScope.SELECTED,
    val selectedDocumentIds: Set<String> = emptySet(),
    val confirmText: String = "",
    val isDeleting: Boolean = false,
    val deleteDone: Boolean = false,
    val errorMessage: String? = null
) {
    val hasValidScope: Boolean
        get() = scope == ItemScope.ALL || selectedDocumentIds.isNotEmpty()

    val hasTypedConfirm: Boolean
        get() = confirmText == "DELETE"

    val canDelete: Boolean
        get() = hasValidScope && hasTypedConfirm && !isDeleting
}

class DataManagementViewModel(
    private val database: AppDatabase,
    private val userPreferences: UserPreferences,
    private val securePrefs: SecurePrefs
) : ViewModel() {

    private val _exportState = MutableStateFlow(ExportUiState())
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private val _deleteState = MutableStateFlow(DeleteUiState())
    val deleteState: StateFlow<DeleteUiState> = _deleteState.asStateFlow()

    val allDocuments: Flow<List<DocumentEntity>> = database.documentDao().observeAll()

    // ====== Export ======

    fun setExportScope(scope: ItemScope) {
        _exportState.update { it.copy(scope = scope, selectedDocumentIds = emptySet()) }
    }

    fun toggleExportDocument(id: String) {
        _exportState.update {
            val ids = it.selectedDocumentIds.toMutableSet()
            if (ids.contains(id)) ids.remove(id) else ids.add(id)
            it.copy(selectedDocumentIds = ids)
        }
    }

    fun setExportDocuments(v: Boolean) { _exportState.update { it.copy(exportDocuments = v) } }
    fun setExportInspirationCards(v: Boolean) { _exportState.update { it.copy(exportInspirationCards = v) } }
    fun setExportSettings(v: Boolean) { _exportState.update { it.copy(exportSettings = v) } }

    fun performExport() {
        val s = _exportState.value
        if (!s.canExport) return

        _exportState.update { it.copy(isExporting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                // TODO: actual export logic
                kotlinx.coroutines.delay(500)
                _exportState.update { it.copy(isExporting = false, exportDone = true) }
            } catch (e: Exception) {
                _exportState.update { it.copy(isExporting = false, errorMessage = e.message) }
            }
        }
    }

    // ====== Delete ======

    fun setDeleteScope(scope: ItemScope) {
        _deleteState.update { it.copy(scope = scope, selectedDocumentIds = emptySet()) }
    }

    fun toggleDeleteDocument(id: String) {
        _deleteState.update {
            val ids = it.selectedDocumentIds.toMutableSet()
            if (ids.contains(id)) ids.remove(id) else ids.add(id)
            it.copy(selectedDocumentIds = ids)
        }
    }

    fun setConfirmText(text: String) {
        _deleteState.update { it.copy(confirmText = text) }
    }

    fun performDelete() {
        val s = _deleteState.value
        if (!s.canDelete) return

        _deleteState.update { it.copy(isDeleting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                when (s.scope) {
                    ItemScope.ALL -> wipeAllLocalData()
                    ItemScope.SELECTED -> deleteSelectedDocuments(s.selectedDocumentIds)
                }
                _deleteState.update { it.copy(isDeleting = false, deleteDone = true) }
            } catch (e: Exception) {
                _deleteState.update { it.copy(isDeleting = false, errorMessage = e.message) }
            }
        }
    }

    private suspend fun wipeAllLocalData() {
        // 1. Room 全部清空
        database.clearAllTables()

        // 2. 加密存储清空
        securePrefs.clearAll()

        // 3. 用户偏好清空
        userPreferences.clearAll()

        // 4. 本地缓存文件（DataStore 的备份文件也随 clearAllTables 清掉了）
    }

    private suspend fun deleteSelectedDocuments(docIds: Set<String>) {
        val docDao = database.documentDao()
        val segDao = database.segmentDao()
        val anchorDao = database.anchorDao()

        for (docId in docIds) {
            // 1. 获取所有文本 ref 并清除 SecurePrefs 密文
            val refs = segDao.getTextRefsByDocId(docId)
            refs.forEach { securePrefs.removeCipherText(it) }

            // 2. 删除 segments
            segDao.deleteByDocId(docId)

            // 3. 删除 document
            docDao.deleteById(docId)

            // 4. 删除关联 anchor 缓存
            // AnchorDao 没有按 docId 删除的方法，用时序清理
            // 实际可以通过 documentId 关联清除，暂用 cleanOlderThan 兜底
        }
    }
}
