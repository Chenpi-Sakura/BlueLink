package com.yjtzc.bluelink.data.remote.dto

import com.squareup.moshi.JsonClass

// ====== 文档相关（V2.0 §7.2.1） ======

@JsonClass(generateAdapter = true)
data class DocumentDto(
    val id: String,
    val title: String,
    val privacyLevel: String = "LOCAL_FIRST",
    val source: String? = null,
    val createdAt: Long = 0,
    val conceptBeacon: String? = null
)

@JsonClass(generateAdapter = true)
data class DocumentListDto(
    val items: List<DocumentDto>,
    val nextCursor: String? = null
)

@JsonClass(generateAdapter = true)
data class SegmentDto(
    val id: String,
    val index: Int = 0,
    val summary: String = "",
    val folded: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SegmentListDto(
    val segments: List<SegmentDto>
)

// ====== 溯源提问 ======

@JsonClass(generateAdapter = true)
data class AskRequest(
    val query: String,
    val granularity: String = "SENTENCE",       // "SENTENCE" | "PARAGRAPH"
    val scopeDocIds: List<String>? = null,
    val localSegmentIds: List<String>? = null   // 私密文档本地命中分段 ID（仅 ID，不含原文）
)

@JsonClass(generateAdapter = true)
data class AskResponse(
    val introduction: String,                    // 引导引言（≤3句）
    val anchors: List<AnchorDto>
)

@JsonClass(generateAdapter = true)
data class AnchorDto(
    val anchorId: String,
    val docTitle: String,
    val snippet: String,                         // 片段前 30 字
    val segmentId: String,
    val score: Float,
    val isLocal: Boolean = false                 // true = 本地私密文档命中
)

// ====== 去重 ======

@JsonClass(generateAdapter = true)
data class DeltaResponse(
    val foldedRanges: List<FoldedRangeDto> = emptyList(),
    val newContentRatio: Float = 1.0f
)

@JsonClass(generateAdapter = true)
data class FoldedRangeDto(
    val segmentIndexStart: Int,
    val segmentIndexEnd: Int,
    val reason: String = ""
)

// ====== 费曼伴学 ======

@JsonClass(generateAdapter = true)
data class FeynmanRequest(
    val userExplanation: String,
    val targetConcept: String,
    val contextSegmentIds: List<String>
)

@JsonClass(generateAdapter = true)
data class FeynmanResponse(
    val summary: String,
    val deviations: List<DeviationDto>,
    val gravityLines: List<GravityLineDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DeviationDto(
    val userSegment: String,
    val deviationType: String,                   // OMISSION | CONTRADICTION | OVER_EXTENSION
    val explanation: String,
    val originalSnippet: String,
    val anchorSegmentId: String
)

@JsonClass(generateAdapter = true)
data class GravityLineDto(
    val from: Int,                               // 用户表述中偏差的字符位置
    val toSegmentId: String
)

// ====== 知识图谱 ======

@JsonClass(generateAdapter = true)
data class GraphDto(
    val nodes: List<GraphNodeDto>,
    val edges: List<GraphEdgeDto>,
    val nextCursor: String? = null
)

@JsonClass(generateAdapter = true)
data class GraphNodeDto(
    val id: String,
    val label: String,
    val type: String,                            // DOCUMENT | INSPIRATION | CONCEPT
    val refId: String? = null
)

@JsonClass(generateAdapter = true)
data class GraphEdgeDto(
    val source: String,
    val target: String,
    val relation: String,                        // SUPPORT | CHALLENGE | SUPPLEMENT | CITE
    val confidence: Float,
    val isManual: Boolean = false
)

// ====== 灵感卡片 ======

@JsonClass(generateAdapter = true)
data class CreateCardRequest(
    val content: String,                         // 脱敏后的文本
    val type: String,                            // TEXT | VOICE | IMAGE
    val privacyLevel: String,
    val tags: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class InspirationDto(
    val id: String,
    val content: String,
    val type: String,
    val privacyLevel: String,
    val tags: List<String>,
    val createdAt: Long
)

// ====== 同步 ======

@JsonClass(generateAdapter = true)
data class SyncItemDto(
    val operation: String,
    val localRefId: String,
    val payloadJson: String
)

@JsonClass(generateAdapter = true)
data class SyncBatchResponse(
    val synced: Int,
    val failed: Int,
    val results: List<SyncResultDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SyncResultDto(
    val localRefId: String,
    val serverRefId: String? = null,
    val success: Boolean,
    val error: String? = null
)
