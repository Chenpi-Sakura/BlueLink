package com.yjtzc.bluelink.domain.usecase

import com.yjtzc.bluelink.data.repository.AnchorRepository
import com.yjtzc.bluelink.data.remote.dto.AskResponse

/**
 * 溯源提问用例 — 封装"提问 → 锚点生成"业务逻辑（V2.0 §8.1）
 */
class AskQuestionUseCase(
    private val anchorRepository: AnchorRepository
) {
    suspend operator fun invoke(
        query: String,
        granularity: String = "SENTENCE",
        scopeDocIds: List<String>? = null
    ): Result<AskResponse> {
        if (query.isBlank()) {
            return Result.failure(IllegalArgumentException("问题不能为空"))
        }
        return anchorRepository.askQuestion(query, granularity, scopeDocIds)
    }
}
