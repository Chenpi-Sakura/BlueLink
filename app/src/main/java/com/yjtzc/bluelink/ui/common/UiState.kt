package com.yjtzc.bluelink.ui.common

/**
 * 通用 UI 状态封装（V2.0 §11.2.3）
 *
 * 用法示例：
 * ```
 * val state: UiState<List<Document>> by viewModel.state.collectAsStateWithLifecycle()
 * when (val s = state) {
 *     is UiState.Idle -> IdleHint()
 *     is UiState.Loading -> LoadingIndicator()
 *     is UiState.Success -> DocumentList(s.data)
 *     is UiState.Error -> ErrorView(s.error, onRetry)
 * }
 * ```
 */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: AppError) : UiState<Nothing>
}

sealed class AppError(val message: String) {
    data object NetworkUnavailable : AppError("网络不可用，请检查网络连接")
    data class AIServiceError(val detail: String = "") : AppError("AI 响应失败：$detail")
    data class ServerError(val code: Int) : AppError("服务异常（$code），请稍后重试")
    data class ValidationError(val field: String) : AppError("$field 输入有误")
    data class Unknown(val detail: String) : AppError(detail)
}
