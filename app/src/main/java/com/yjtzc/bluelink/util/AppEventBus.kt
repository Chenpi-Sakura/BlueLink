package com.yjtzc.bluelink.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 全局事件总线 — 跨页面提示 SnackBar / Toast（V2.0 §11.2.2）
 */
sealed class AppEvent {
    data object NeedReLogin : AppEvent()
    data object AIServiceUnavailable : AppEvent()
    data class ServerError(val code: Int) : AppEvent()
    data class RequestFailed(val message: String) : AppEvent()
    data object DataParseError : AppEvent()
    data class DatabaseError(val detail: String) : AppEvent()
    data class UnknownError(val message: String) : AppEvent()
}

object AppEventBus {
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    fun emit(event: AppEvent) {
        _events.tryEmit(event)
    }
}
