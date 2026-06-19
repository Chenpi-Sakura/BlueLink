package com.yjtzc.bluelink.data.repository

import com.yjtzc.bluelink.data.local.crypto.SecurePrefs
import com.yjtzc.bluelink.data.local.db.DocumentDao
import com.yjtzc.bluelink.data.local.db.DocumentEntity
import com.yjtzc.bluelink.data.local.db.SegmentDao
import com.yjtzc.bluelink.data.local.db.SegmentEntity
import com.yjtzc.bluelink.data.remote.BlueLinkApi
import com.yjtzc.bluelink.data.remote.dto.DocumentDto
import com.yjtzc.bluelink.data.remote.dto.SegmentDto
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * 文档仓库 — 合并 remote API + local Room + 加密存储（V2.0 §4.1 / V2.1 §1.6）
 */
class DocumentRepository(
    private val api: BlueLinkApi,
    private val documentDao: DocumentDao,
    private val segmentDao: SegmentDao,
    private val securePrefs: SecurePrefs
) {

    // ====== 查询 ======

    fun observeAll(): Flow<List<DocumentEntity>> = documentDao.observeAll()

    fun search(query: String): Flow<List<DocumentEntity>> = documentDao.search(query)

    suspend fun getById(id: String): DocumentEntity? = documentDao.getById(id)

    suspend fun getSegments(docId: String): List<SegmentEntity> = segmentDao.getByDocId(docId)

    fun observeSegments(docId: String): Flow<List<SegmentEntity>> = segmentDao.observeByDocId(docId)

    suspend fun getSegmentById(id: String): SegmentEntity? = segmentDao.getById(id)

    // ====== 保存（加密原文 → SecurePrefs，Room 仅存 ref） ======

    suspend fun saveDocument(
        title: String,
        source: String? = null,
        privacyLevel: com.yjtzc.bluelink.data.local.db.PrivacyLevel = com.yjtzc.bluelink.data.local.db.PrivacyLevel.LOCAL_FIRST,
        segments: List<String> = emptyList()
    ): DocumentEntity {
        val docId = UUID.randomUUID().toString()
        val doc = DocumentEntity(
            id = docId,
            title = title,
            source = source,
            privacyLevel = privacyLevel
        )
        documentDao.upsert(doc)

        // 加密每段文本存入 SecurePrefs
        segments.forEachIndexed { index, text ->
            saveSegment(text, docId, index)
        }

        return doc
    }

    // ====== 分段读写 ======

    suspend fun saveSegment(text: String, docId: String, indexInDoc: Int): SegmentEntity {
        val id = UUID.randomUUID().toString()
        val ref = SecurePrefs.Keys.segment(id)
        val ciphertext = text.encodeToByteArray()  // V2.1 MVP: 直接存原文到 SecurePrefs
        securePrefs.putCipherText(ref, ciphertext)
        val entity = SegmentEntity(
            id = id,
            docId = docId,
            indexInDoc = indexInDoc,
            textRef = ref,
            textSnippet = text.take(30)
        )
        segmentDao.upsertAll(listOf(entity))
        return entity
    }

    suspend fun readSegmentText(segment: SegmentEntity): String {
        val ciphertext = securePrefs.getCipherText(segment.textRef)
            ?: error("密文丢失: ${segment.textRef}")
        return ciphertext.decodeToString()
    }

    // ====== 远程同步 ======

    suspend fun fetchFromRemote(): List<DocumentDto> {
        val response = api.listDocuments()
        return if (response.isSuccessful) {
            val body = response.body()!!
            // 缓存到本地 Room
            body.items.forEach { dto ->
                documentDao.upsert(
                    DocumentEntity(
                        id = dto.id,
                        title = dto.title,
                        privacyLevel = com.yjtzc.bluelink.data.local.db.PrivacyLevel.valueOf(dto.privacyLevel),
                        source = dto.source,
                        conceptBeacon = dto.conceptBeacon
                    )
                )
            }
            body.items
        } else {
            emptyList()
        }
    }

    suspend fun fetchSegmentsFromRemote(docId: String): List<SegmentDto> {
        val response = api.getDocumentSegments(docId)
        return if (response.isSuccessful) {
            response.body()!!.segments
        } else {
            emptyList()
        }
    }

    // ====== 删除（同步清除密文，V2.1 §1.7） ======

    suspend fun deleteDocument(doc: DocumentEntity) {
        val refs = segmentDao.getTextRefsByDocId(doc.id)
        segmentDao.deleteByDocId(doc.id)
        documentDao.delete(doc)
        refs.forEach { securePrefs.removeCipherText(it) }
    }

    // ====== 折叠状态 ======

    suspend fun updateFolded(segmentIds: List<String>, folded: Boolean) {
        segmentDao.updateFolded(segmentIds, folded)
    }
}
