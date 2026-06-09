package com.yjtzc.bluelink.data.remote

import com.yjtzc.bluelink.data.remote.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API 接口 — 对应 FastAPI 后端所有端点（V2.0 §7.1 / V2.1 §4.2）
 *
 * 所有请求由 [AuthInterceptor] 自动注入 X-User-Id Header。
 */
interface BlueLinkApi {

    // ====== 文档 ======

    @Multipart
    @POST("api/v1/documents/upload")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part,
        @Part("privacy_level") privacyLevel: String
    ): Response<DocumentDto>

    @GET("api/v1/documents/{doc_id}/segments")
    suspend fun getDocumentSegments(
        @Path("doc_id") docId: String
    ): Response<SegmentListDto>

    @GET("api/v1/documents")
    suspend fun listDocuments(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<DocumentListDto>

    // ====== 溯源提问 ======

    @POST("api/v1/questions/ask")
    suspend fun askQuestion(
        @Body request: AskRequest
    ): Response<AskResponse>

    // ====== 去重 ======

    @POST("api/v1/documents/{doc_id}/compute_delta")
    suspend fun computeDelta(
        @Path("doc_id") docId: String
    ): Response<DeltaResponse>

    // ====== 费曼伴学 ======

    @POST("api/v1/feynman/evaluate")
    suspend fun evaluateFeynman(
        @Body request: FeynmanRequest
    ): Response<FeynmanResponse>

    // ====== 知识图谱 ======

    @GET("api/v1/graph")
    suspend fun fetchGraph(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 500
    ): Response<GraphDto>

    // ====== 灵感卡片 ======

    @POST("api/v1/cards")
    suspend fun createInspiration(
        @Body request: CreateCardRequest
    ): Response<InspirationDto>

    // ====== 同步 ======

    @POST("api/v1/sync/batch")
    suspend fun batchSync(
        @Body payload: List<SyncItemDto>
    ): Response<SyncBatchResponse>
}
