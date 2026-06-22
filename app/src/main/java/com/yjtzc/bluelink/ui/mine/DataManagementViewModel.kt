package com.yjtzc.bluelink.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        get() = exportDocuments || exportInspirationCards || exportGraph || exportSettings

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
    private val userPreferences: UserPreferences
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

    fun toggleExportContent(document: Boolean? = null, inspiration: Boolean? = null, graph: Boolean? = null, settings: Boolean? = null) {
        _exportState.update {
            it.copy(
                exportDocuments = document ?: it.exportDocuments,
                exportInspirationCards = inspiration ?: it.exportInspirationCards,
                exportGraph = graph ?: it.exportGraph,
                exportSettings = settings ?: it.exportSettings
            )
        }
    }

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
                // TODO: actual delete logic
                kotlinx.coroutines.delay(500)
                _deleteState.update { it.copy(isDeleting = false, deleteDone = true) }
            } catch (e: Exception) {
                _deleteState.update { it.copy(isDeleting = false, errorMessage = e.message) }
            }
        }
    }
}
