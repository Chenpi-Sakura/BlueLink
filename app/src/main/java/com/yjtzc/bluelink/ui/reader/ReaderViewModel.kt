package com.yjtzc.bluelink.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yjtzc.bluelink.data.local.db.SegmentEntity
import com.yjtzc.bluelink.data.repository.DocumentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _segments = MutableStateFlow<List<SegmentEntity>>(emptyList())
    val segments: StateFlow<List<SegmentEntity>> = _segments.asStateFlow()

    private val _spotlightTargetId = MutableStateFlow<String?>(null)
    val spotlightTargetId: StateFlow<String?> = _spotlightTargetId.asStateFlow()

    private val _foldedSegmentIds = MutableStateFlow<Set<String>>(emptySet())
    val foldedSegmentIds: StateFlow<Set<String>> = _foldedSegmentIds.asStateFlow()

    private var currentDocId: String = ""

    fun loadDocument(docId: String, spotlightSegmentId: String? = null) {
        currentDocId = docId
        viewModelScope.launch {
            documentRepository.observeSegments(docId).collect { segs ->
                _segments.value = segs
            }
        }
        if (spotlightSegmentId != null) {
            setSpotlight(spotlightSegmentId)
        }
    }

    fun setSpotlight(segmentId: String?) {
        _spotlightTargetId.value = segmentId
    }

    fun clearSpotlight() {
        _spotlightTargetId.value = null
    }

    fun toggleFold(segmentId: String) {
        _foldedSegmentIds.update { current ->
            if (segmentId in current) current - segmentId
            else current + segmentId
        }
    }

    /**
     * V2.1 MVP 纯文本阅读
     */
    suspend fun getSegmentFullText(segment: SegmentEntity): String {
        return documentRepository.readSegmentText(segment)
    }
}
