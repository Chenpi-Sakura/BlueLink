package com.yjtzc.bluelink.domain.usecase

import com.yjtzc.bluelink.data.local.db.PrivacyLevel
import com.yjtzc.bluelink.data.repository.CaptureRepository

/**
 * 灵感捕获用例（V2.0 §8.5）
 */
class CaptureInspirationUseCase(
    private val captureRepository: CaptureRepository
) {
    suspend operator fun invoke(
        content: String,
        privacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY,
        tags: List<String> = emptyList()
    ): Result<String> {
        if (content.isBlank()) {
            return Result.failure(IllegalArgumentException("内容不能为空"))
        }
        return runCatching {
            val card = captureRepository.saveInspiration(
                content = content,
                privacyLevel = privacyLevel,
                tags = tags
            )
            card.id
        }
    }
}
