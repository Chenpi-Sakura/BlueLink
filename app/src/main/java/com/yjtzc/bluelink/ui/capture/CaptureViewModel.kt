package com.yjtzc.bluelink.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yjtzc.bluelink.data.local.db.CardType
import com.yjtzc.bluelink.data.local.db.PrivacyLevel
import com.yjtzc.bluelink.data.repository.CaptureRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CaptureState(
    val tabIndex: Int = 0,                        // 0=文字, 1=语音, 2=拍摄
    val textContent: String = "",
    val isPrivate: Boolean = true,
    val isSaving: Boolean = false,
    val showManualInputFallback: Boolean = false,
    val ocrStatus: String? = null
)

class CaptureViewModel(
    private val captureRepository: CaptureRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CaptureState())
    val state: StateFlow<CaptureState> = _state.asStateFlow()

    fun setTabIndex(index: Int) {
        _state.update { it.copy(tabIndex = index) }
    }

    fun setTextContent(text: String) {
        _state.update { it.copy(textContent = text) }
    }

    fun setPrivate(isPrivate: Boolean) {
        _state.update { it.copy(isPrivate = isPrivate) }
    }

    fun saveInspiration(onSuccess: () -> Unit) {
        val text = _state.value.textContent
        if (text.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            captureRepository.saveInspiration(
                content = text,
                type = CardType.TEXT,
                privacyLevel = if (_state.value.isPrivate) PrivacyLevel.LOCAL_ONLY else PrivacyLevel.LOCAL_FIRST
            )
            _state.update { it.copy(isSaving = false, textContent = "", tabIndex = 0) }
            onSuccess()
        }
    }

    fun onOcrFailed() {
        _state.update { it.copy(showManualInputFallback = true, ocrStatus = "未识别到文字，请手动输入") }
    }

    fun dismissOcrError() {
        _state.update { it.copy(showManualInputFallback = false, ocrStatus = null) }
    }
}
