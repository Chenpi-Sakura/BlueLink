package com.yjtzc.bluelink.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yjtzc.bluelink.data.local.db.DocumentEntity
import com.yjtzc.bluelink.data.repository.DocumentRepository
import com.yjtzc.bluelink.domain.model.Document
import com.yjtzc.bluelink.domain.model.toDomain
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> = _documents.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeDocuments()
    }

    private fun observeDocuments() {
        viewModelScope.launch {
            documentRepository.observeAll().collect { entities ->
                _documents.value = entities.map { it.toDomain() }
            }
        }
    }

    fun onSearchChange(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                observeDocuments()  // 恢复全量
            } else {
                documentRepository.search(query).collect { entities ->
                    _documents.value = entities.map { it.toDomain() }
                }
            }
        }
    }

    fun refreshFromRemote() {
        viewModelScope.launch {
            _isLoading.value = true
            documentRepository.fetchFromRemote()
            _isLoading.value = false
        }
    }

    fun deleteDocument(doc: Document) {
        viewModelScope.launch {
            val entity = documentRepository.getById(doc.id)
            entity?.let { documentRepository.deleteDocument(it) }
        }
    }
}
