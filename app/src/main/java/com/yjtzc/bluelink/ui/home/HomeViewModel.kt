package com.yjtzc.bluelink.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yjtzc.bluelink.data.local.db.InspirationCardEntity
import com.yjtzc.bluelink.data.repository.CaptureRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val captureRepository: CaptureRepository
) : ViewModel() {

    private val _allCards = MutableStateFlow<List<InspirationCardEntity>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val cards: StateFlow<List<InspirationCardEntity>> = combine(
        _allCards, _searchQuery
    ) { all, query ->
        if (query.isBlank()) all
        else all.filter { card ->
            card.contentSnippet.contains(query, ignoreCase = true) ||
            card.tags.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeCards()
    }

    private fun observeCards() {
        viewModelScope.launch {
            captureRepository.observeAllCards().collect { entities ->
                _allCards.value = entities
            }
        }
    }

    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteCard(card: InspirationCardEntity) {
        viewModelScope.launch {
            captureRepository.deleteCard(card)
        }
    }
}
