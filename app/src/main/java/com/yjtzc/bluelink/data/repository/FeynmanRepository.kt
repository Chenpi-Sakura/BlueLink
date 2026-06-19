package com.yjtzc.bluelink.data.repository

import com.yjtzc.bluelink.data.remote.BlueLinkApi
import com.yjtzc.bluelink.data.remote.dto.FeynmanRequest
import com.yjtzc.bluelink.data.remote.dto.FeynmanResponse

/**
 * 费曼伴学仓库（V2.0 §8.3）
 */
class FeynmanRepository(
    private val api: BlueLinkApi
) {

    /**
     * 调用后端做大模型 NLI 比对，返回偏差报告
     */
    suspend fun evaluate(
        userExplanation: String,
        targetConcept: String,
        contextSegmentIds: List<String>
    ): Result<FeynmanResponse> {
        return runCatching {
            val request = FeynmanRequest(
                userExplanation = userExplanation,
                targetConcept = targetConcept,
                contextSegmentIds = contextSegmentIds
            )
            val response = api.evaluateFeynman(request)
            if (response.isSuccessful) {
                response.body()!!
            } else {
                throw Exception("费曼评估失败: ${response.code()}")
            }
        }
    }
}
