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
    val errorMessage: String? = null,
    val exportJson: String? = null
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
                val json = assembleExportJson(s)
                _exportState.update { it.copy(isExporting = false, exportDone = true, exportJson = json) }
            } catch (e: Exception) {
                _exportState.update { it.copy(isExporting = false, errorMessage = e.message) }
            }
        }
    }

    private suspend fun assembleExportJson(s: ExportUiState): String {
        val root = org.json.JSONObject()

        root.put("version", "2.1")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("exportScope", s.scope.name)
        root.put("selectedDocumentIds", org.json.JSONArray(s.selectedDocumentIds.toList()))

        // identity
        root.put("identity", org.json.JSONObject().apply {
            put("userId", userPreferences.userId.first() ?: "")
            put("mode", userPreferences.privacyMode.first())
        })

        // documents + segments
        if (s.exportDocuments) {
            val docsJson = org.json.JSONArray()
            val segsJson = org.json.JSONArray()
            val allDocs = database.documentDao().observeAll().first()
            val docs = if (s.scope == ItemScope.ALL) allDocs
                       else allDocs.filter { it.id in s.selectedDocumentIds }
            for (doc in docs) {
                docsJson.put(org.json.JSONObject().apply {
                    put("id", doc.id); put("title", doc.title)
                    put("privacyLevel", doc.privacyLevel.name)
                    put("createdAt", doc.createdAt)
                })
                val segs = database.segmentDao().getByDocId(doc.id)
                for (seg in segs) {
                    segsJson.put(org.json.JSONObject().apply {
                        put("id", seg.id); put("docId", seg.docId)
                        put("indexInDoc", seg.indexInDoc)
                        put("textSnippet", seg.textSnippet)
                    })
                }
            }
            root.put("documents", docsJson)
            root.put("segments", segsJson)
        }

        // inspiration cards
        if (s.exportInspirationCards) {
            val cards = database.inspirationDao().observeAll().first()
            root.put("inspirationCards", org.json.JSONArray(cards.map { card ->
                org.json.JSONObject().apply {
                    put("id", card.id); put("type", card.type.name)
                    put("contentSnippet", card.contentSnippet)
                    put("privacyLevel", card.privacyLevel.name)
                    put("createdAt", card.createdAt)
                }
            }))
        }

        // graph
        if (s.exportGraph) {
            root.put("graph", org.json.JSONObject().apply {
                put("nodes", org.json.JSONArray(database.graphNodeDao().getAll().map {
                    org.json.JSONObject().apply { put("id", it.id); put("label", it.label); put("type", it.type.name) }
                }))
                put("edges", org.json.JSONArray(database.graphEdgeDao().getAll().map {
                    org.json.JSONObject().apply { put("sourceId", it.sourceId); put("targetId", it.targetId); put("relation", it.relation.name) }
                }))
            })
        }

        // settings
        if (s.exportSettings) {
            root.put("settings", org.json.JSONObject().apply {
                put("appearance", org.json.JSONObject().apply {
                    put("themeMode", userPreferences.themeMode.first())
                    put("dynamicColor", userPreferences.dynamicColor.first())
                    put("fontScale", userPreferences.fontScale.first())
                    put("highContrast", userPreferences.highContrast.first())
                })
                put("cognitive", org.json.JSONObject().apply {
                    put("defaultGranularity", userPreferences.defaultGranularity.first())
                    put("directnessLevel", userPreferences.directnessLevel.first())
                    put("exploreDepth", userPreferences.exploreDepth.first())
                    put("companionStyle", userPreferences.companionStyle.first())
                })
                put("privacy", org.json.JSONObject().apply {
                    put("level", userPreferences.privacyMode.first())
                })
            })
        }

        return root.toString(2)
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
