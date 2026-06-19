package com.yjtzc.bluelink.data.repository

import com.yjtzc.bluelink.data.local.db.*
import com.yjtzc.bluelink.data.remote.BlueLinkApi
import com.yjtzc.bluelink.data.remote.dto.GraphDto

/**
 * 知识图谱仓库（V2.0 §8.4）
 */
class GraphRepository(
    private val api: BlueLinkApi,
    private val nodeDao: GraphNodeDao,
    private val edgeDao: GraphEdgeDao
) {

    suspend fun getLocalNodes(): List<GraphNodeEntity> = nodeDao.getAll()

    suspend fun getLocalEdges(): List<GraphEdgeEntity> = edgeDao.getAll()

    /**
     * 从后端拉取全量图谱数据并缓存到 Room
     */
    suspend fun fetchFromRemote(): Result<GraphDto> {
        return runCatching {
            val response = api.fetchGraph()
            if (response.isSuccessful) {
                val body = response.body()!!
                // 全量替换本地图谱
                nodeDao.clearAll()
                edgeDao.clearAll()

                nodeDao.upsertAll(body.nodes.map { dto ->
                    GraphNodeEntity(
                        id = dto.id,
                        label = dto.label,
                        type = NodeType.valueOf(dto.type),
                        refId = dto.refId
                    )
                })
                edgeDao.upsertAll(body.edges.map { dto ->
                    GraphEdgeEntity(
                        sourceId = dto.source,
                        targetId = dto.target,
                        relation = RelationType.valueOf(dto.relation),
                        confidence = dto.confidence,
                        isManual = dto.isManual
                    )
                })
                body
            } else {
                throw Exception("图谱加载失败: ${response.code()}")
            }
        }
    }
}
