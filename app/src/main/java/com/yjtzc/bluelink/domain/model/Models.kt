package com.yjtzc.bluelink.domain.model

import com.yjtzc.bluelink.data.local.db.*

/**
 * 领域模型 — 纯数据类，零 Android 依赖（V2.0 §4.1）
 *
 * 与 Room Entity 分离的原因：
 * - Entity 可能随数据库需求变化（加索引、加字段）
 * - Domain 模型是 UI 层的"合同"，不受存储细节影响
 * - 未来 KMP 迁移时，domain 层可直接共享到 iOS
 */

data class Document(
    val id: String,
    val title: String,
    val privacyLevel: PrivacyLevel,
    val source: String?,
    val createdAt: Long,
    val conceptBeacon: String?,
    val isFoldedGlobal: Boolean
)

data class Segment(
    val id: String,
    val docId: String,
    val indexInDoc: Int,
    val textSnippet: String,
    val isFolded: Boolean,
    val isSpotlightTarget: Boolean
) {
    /** 完整文本须通过 Repository 从 SecurePrefs 解密获取 */
    lateinit var fullText: String
}

data class Anchor(
    val id: String,
    val docTitle: String,
    val snippet: String,
    val segmentId: String,
    val score: Float,
    val isRead: Boolean
)

data class InspirationCard(
    val id: String,
    val contentSnippet: String,
    val type: CardType,
    val privacyLevel: PrivacyLevel,
    val tags: List<String>,
    val createdAt: Long
) {
    /** 完整内容须通过 Repository 从 SecurePrefs 解密获取 */
    lateinit var fullContent: String
}

data class Deviation(
    val userSegment: String,
    val deviationType: DeviationType,
    val explanation: String,
    val originalSnippet: String,
    val anchorSegmentId: String
)

enum class DeviationType { OMISSION, CONTRADICTION, OVER_EXTENSION }

data class FeynmanReport(
    val summary: String,
    val deviations: List<Deviation>,
    val gravityLines: List<GravityLine>
)

data class GravityLine(
    val from: Int,           // 用户表述中的字符位置
    val toSegmentId: String
)

data class GraphData(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>
)

data class GraphNode(
    val id: String,
    val label: String,
    val type: NodeType,
    val refId: String?
)

data class GraphEdge(
    val sourceId: String,
    val targetId: String,
    val relation: RelationType,
    val confidence: Float
)

// ====== Entity → Domain 映射扩展 ======

fun DocumentEntity.toDomain() = Document(
    id = id, title = title, privacyLevel = privacyLevel,
    source = source, createdAt = createdAt, conceptBeacon = conceptBeacon,
    isFoldedGlobal = isFoldedGlobal
)

fun AnchorEntity.toDomain() = Anchor(
    id = id, docTitle = docTitle, snippet = snippet,
    segmentId = segmentId, score = score, isRead = isRead
)

fun GraphNodeEntity.toDomain() = GraphNode(
    id = id, label = label, type = type, refId = refId
)

fun GraphEdgeEntity.toDomain() = GraphEdge(
    sourceId = sourceId, targetId = targetId, relation = relation, confidence = confidence
)
