package com.yjtzc.bluelink.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yjtzc.bluelink.data.remote.dto.AnchorDto
import com.yjtzc.bluelink.data.remote.dto.AskResponse
import com.yjtzc.bluelink.data.repository.AnchorRepository
import com.yjtzc.bluelink.data.repository.FeynmanRepository
import com.yjtzc.bluelink.domain.model.Deviation
import com.yjtzc.bluelink.domain.model.FeynmanReport
import com.yjtzc.bluelink.domain.model.GravityLine
import com.yjtzc.bluelink.ui.common.AppError
import com.yjtzc.bluelink.ui.common.UiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val content: String,                       // 用户文本 / AI 引言
    val anchors: List<AnchorDto> = emptyList(),
    val feynmanReport: FeynmanReport? = null
)

class ChatViewModel(
    private val anchorRepository: AnchorRepository,
    private val feynmanRepository: FeynmanRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _askState = MutableStateFlow<UiState<AskResponse>>(UiState.Idle)
    val askState: StateFlow<UiState<AskResponse>> = _askState.asStateFlow()

    private val _granularity = MutableStateFlow("SENTENCE")  // SENTENCE / PARAGRAPH
    val granularity: StateFlow<String> = _granularity.asStateFlow()

    private val _isFeynmanMode = MutableStateFlow(false)
    val isFeynmanMode: StateFlow<Boolean> = _isFeynmanMode.asStateFlow()

    fun toggleGranularity() {
        _granularity.value = if (_granularity.value == "SENTENCE") "PARAGRAPH" else "SENTENCE"
    }

    fun toggleFeynman() {
        _isFeynmanMode.value = !_isFeynmanMode.value
    }

    fun askQuestion(query: String) {
        if (query.isBlank()) return

        val userMsg = ChatMessage(
            id = "user-${System.currentTimeMillis()}",
            isUser = true,
            content = query
        )
        _messages.update { it + userMsg }
        _askState.value = UiState.Loading

        viewModelScope.launch {
            anchorRepository.askQuestion(query, _granularity.value)
                .onSuccess { response ->
                    val aiMsg = ChatMessage(
                        id = "ai-${System.currentTimeMillis()}",
                        isUser = false,
                        content = response.introduction,
                        anchors = response.anchors
                    )
                    _messages.update { it + aiMsg }
                    _askState.value = UiState.Success(response)
                }
                .onFailure { e ->
                    _askState.value = UiState.Error(
                        AppError.AIServiceError(e.message ?: "")
                    )
                }
        }
    }

    fun runFeynmanEvaluation(explanation: String, concept: String, segmentIds: List<String>) {
        viewModelScope.launch {
            feynmanRepository.evaluate(explanation, concept, segmentIds)
                .onSuccess { response ->
                    // 生成费曼报告消息
                    val report = FeynmanReport(
                        summary = response.summary,
                        deviations = response.deviations.mapNotNull { dto ->
                            val type = com.yjtzc.bluelink.domain.model.DeviationType.safeValueOf(dto.deviationType) ?: return@mapNotNull null
                            Deviation(
                                userSegment = dto.userSegment,
                                deviationType = type,
                                explanation = dto.explanation,
                                originalSnippet = dto.originalSnippet,
                                anchorSegmentId = dto.anchorSegmentId
                            )
                        },
                        gravityLines = response.gravityLines.map { gl ->
                            GravityLine(from = gl.from, toSegmentId = gl.toSegmentId)
                        }
                    )
                    val msg = ChatMessage(
                        id = "feynman-${System.currentTimeMillis()}",
                        isUser = false,
                        content = response.summary,
                        feynmanReport = report
                    )
                    _messages.update { it + msg }
                }
        }
    }
}
