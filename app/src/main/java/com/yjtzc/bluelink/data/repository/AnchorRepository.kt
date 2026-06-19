package com.yjtzc.bluelink.data.repository

import com.yjtzc.bluelink.data.local.db.AnchorDao
import com.yjtzc.bluelink.data.local.db.AnchorEntity
import com.yjtzc.bluelink.data.local.db.SegmentDao
import com.yjtzc.bluelink.data.remote.BlueLinkApi
import com.yjtzc.bluelink.data.remote.dto.AskRequest
import com.yjtzc.bluelink.data.remote.dto.AskResponse
import java.security.MessageDigest

/**
 * 锚点仓库 — 提问溯源的核心数据层（V2.0 §8.1）
 */
class AnchorRepository(
    private val api: BlueLinkApi,
    private val anchorDao: AnchorDao,
    private val segmentDao: SegmentDao
) {

    /**
     * 发送溯源提问，返回锚点列表。
     * 结果写入本地缓存（Room anchors 表）。
     */
    suspend fun askQuestion(
        query: String,
        granularity: String = "SENTENCE",
        scopeDocIds: List<String>? = null
    ): Result<AskResponse> {
        return runCatching {
            val request = AskRequest(
                query = query,
                granularity = granularity,
                scopeDocIds = scopeDocIds
            )
            val response = api.askQuestion(request)
            if (response.isSuccessful) {
                val body = response.body()!!
                // 缓存锚点到本地
                val queryHash = hashQuery(query, granularity)
                val entities = body.anchors.map { dto ->
                    AnchorEntity(
                        id = dto.anchorId,
                        queryHash = queryHash,
                        segmentId = dto.segmentId,
                        docTitle = dto.docTitle,
                        snippet = dto.snippet,
                        score = dto.score
                    )
                }
                anchorDao.upsertAll(entities)
                body
            } else {
                throw Exception("API 返回错误: ${response.code()}")
            }
        }
    }

    /**
     * 从本地缓存加载历史锚点（离线可用）
     */
    suspend fun getCachedAnchors(query: String, granularity: String = "SENTENCE"): List<AnchorEntity> {
        return anchorDao.getByQueryHash(hashQuery(query, granularity))
    }

    suspend fun markAnchorRead(anchorId: String) {
        anchorDao.markRead(anchorId)
    }

    suspend fun cleanOldAnchors(before: Long) {
        anchorDao.cleanOlderThan(before)
    }

    private fun hashQuery(query: String, granularity: String): String {
        val input = "$query|$granularity"
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.encodeToByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
