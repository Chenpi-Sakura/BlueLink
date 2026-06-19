package com.yjtzc.bluelink.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ====== 枚举（V2.0 §6.1） ======

enum class PrivacyLevel { LOCAL_ONLY, LOCAL_FIRST, CLOUD_OK }
enum class CardType { TEXT, VOICE, IMAGE }
enum class NodeType { DOCUMENT, INSPIRATION, CONCEPT }
enum class RelationType { SUPPORT, CHALLENGE, SUPPLEMENT, CITE }
enum class SyncOp { CREATE_DOC, CREATE_CARD, UPDATE_PROFILE, FOLD_SEGMENT, DELETE_DOC, DELETE_CARD }
enum class SyncStatus { PENDING, IN_FLIGHT, SUCCESS, FAILED }

// ====== Room 实体 ======

/**
 * 文档（V2.1 §1.4 / §5.4）
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,                // UUID
    val title: String,
    val privacyLevel: PrivacyLevel,
    val source: String? = null,                // 来源路径
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val conceptBeacon: String? = null,         // 概念路标
    @ColumnInfo(name = "is_folded_global") val isFoldedGlobal: Boolean = false,
    val pageCount: Int? = null,                // V2.1: PDF 页数（预留）
    val hasOriginalImage: Boolean = false      // V2.1: 是否有 PDF 原图
)

/**
 * 文档切片（V2.1 §1.4）
 * V2.1 安全策略：原文不存 Room，存 EncryptedSharedPreferences；
 * Room 仅保留 textRef（指向密文的 key）+ textSnippet（前30字明文摘要）。
 */
@Entity(
    tableName = "segments",
    indices = [
        Index("docId"),
        Index(value = ["docId", "indexInDoc"], unique = true)
    ]
)
data class SegmentEntity(
    @PrimaryKey val id: String,                // UUID
    val docId: String,
    val indexInDoc: Int,
    val textRef: String,                       // V2.1: 指向 SecurePrefs key（"seg:<id>"）
    val textSnippet: String = "",              // V2.1: 前 30 字摘要（明文，列表展示用）
    val vectorBlob: ByteArray? = null,         // 向量（非私密文档缓存）
    val isFolded: Boolean = false,
    val isSpotlightTarget: Boolean = false
)

/**
 * 灵感卡片（V2.1 §1.4）
 */
@Entity(tableName = "inspiration_cards")
data class InspirationCardEntity(
    @PrimaryKey val id: String,
    val contentRef: String,                    // V2.1: 指向 SecurePrefs key（"card:<id>"）
    val contentSnippet: String = "",           // V2.1: 前 30 字摘要
    val type: CardType,
    val privacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY,
    val tags: String = "",                     // 逗号分隔关键词
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 锚点缓存（V2.0 §6.1）
 */
@Entity(tableName = "anchors")
data class AnchorEntity(
    @PrimaryKey val id: String,
    val queryHash: String,                     // SHA-256(question + scope)
    val segmentId: String,
    val docTitle: String,
    val snippet: String,                       // 片段前 30 字（明文，用于列表展示）
    val score: Float,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 图谱节点（V2.0 §6.1）
 */
@Entity(tableName = "graph_nodes")
data class GraphNodeEntity(
    @PrimaryKey val id: String,
    val label: String,
    val type: NodeType,
    val refId: String? = null
)

/**
 * 图谱边（V2.0 §6.1）
 */
@Entity(
    tableName = "graph_edges",
    primaryKeys = ["sourceId", "targetId"],
    indices = [Index("sourceId"), Index("targetId")]
)
data class GraphEdgeEntity(
    val sourceId: String,
    val targetId: String,
    val relation: RelationType,
    val confidence: Float,
    val isManual: Boolean = false
)

/**
 * 离线同步队列（V2.1 §5.5）
 */
@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey val id: String,
    val operation: SyncOp,
    val localRefId: String,                    // V2.1: 客户端 UUID
    val serverRefId: String? = null,           // V2.1: 同步成功后回填
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String? = null,             // V2.1: 上次失败原因
    val status: SyncStatus = SyncStatus.PENDING
)
