package com.yjtzc.bluelink.domain.usecase

import com.yjtzc.bluelink.data.repository.FeynmanRepository
import com.yjtzc.bluelink.data.remote.dto.FeynmanResponse
import com.yjtzc.bluelink.domain.model.Deviation
import com.yjtzc.bluelink.domain.model.DeviationType
import com.yjtzc.bluelink.domain.model.FeynmanReport
import com.yjtzc.bluelink.domain.model.GravityLine

/**
 * 费曼评估用例（V2.0 §8.3）
 */
class FeynmanEvaluateUseCase(
    private val feynmanRepository: FeynmanRepository
) {
    suspend operator fun invoke(
        userExplanation: String,
        targetConcept: String,
        contextSegmentIds: List<String>
    ): Result<FeynmanReport> {
        if (userExplanation.isBlank()) {
            return Result.failure(IllegalArgumentException("请先输入你的解释"))
        }

        return feynmanRepository.evaluate(userExplanation, targetConcept, contextSegmentIds)
            .map { response ->
                FeynmanReport(
                    summary = response.summary,
                    deviations = response.deviations.map { dto ->
                        Deviation(
                            userSegment = dto.userSegment,
                            deviationType = DeviationType.valueOf(dto.deviationType),
                            explanation = dto.explanation,
                            originalSnippet = dto.originalSnippet,
                            anchorSegmentId = dto.anchorSegmentId
                        )
                    },
                    gravityLines = response.gravityLines.map { gl ->
                        GravityLine(from = gl.from, toSegmentId = gl.toSegmentId)
                    }
                )
            }
    }
}
