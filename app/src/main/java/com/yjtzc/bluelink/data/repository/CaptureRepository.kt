package com.yjtzc.bluelink.data.repository

import com.yjtzc.bluelink.data.local.crypto.SecurePrefs
import com.yjtzc.bluelink.data.local.db.CardType
import com.yjtzc.bluelink.data.local.db.InspirationCardEntity
import com.yjtzc.bluelink.data.local.db.InspirationDao
import com.yjtzc.bluelink.data.local.db.PrivacyLevel
import com.yjtzc.bluelink.data.remote.BlueLinkApi
import com.yjtzc.bluelink.data.remote.dto.CreateCardRequest
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import java.util.UUID

/**
 * 灵感捕获仓库（V2.0 §8.5 / V2.1 §1.6）
 */
class CaptureRepository(
    private val api: BlueLinkApi,
    private val inspirationDao: InspirationDao,
    private val securePrefs: SecurePrefs
) {

    fun observeAllCards(): Flow<List<InspirationCardEntity>> = inspirationDao.observeAll()

    /**
     * 保存灵感卡片 → 加密原文入 SecurePrefs，Room 仅存 ref + snippet
     */
    suspend fun saveInspiration(
        content: String,
        type: CardType = CardType.TEXT,
        privacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY,
        tags: List<String> = emptyList()
    ): InspirationCardEntity {
        val id = UUID.randomUUID().toString()
        val ref = SecurePrefs.Keys.card(id)
        val ciphertext = content.encodeToByteArray()
        securePrefs.putCipherText(ref, ciphertext)

        val card = InspirationCardEntity(
            id = id,
            contentRef = ref,
            contentSnippet = content.take(30),
            type = type,
            privacyLevel = privacyLevel,
            tags = tags.joinToString(","),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        inspirationDao.upsert(card)

        // 非私密卡片 → 异步上传云端（脱敏）
        if (privacyLevel != PrivacyLevel.LOCAL_ONLY) {
            try {
                api.createInspiration(
                    CreateCardRequest(
                        content = content,  // TODO: 接入 PrivacyManager.sanitize()
                        type = type.name,
                        privacyLevel = privacyLevel.name,
                        tags = tags
                    )
                )
            } catch (_: Exception) {
                // 上传失败不阻塞本地保存；待 SyncCoordinator 重试
            }
        }

        return card
    }

    suspend fun readCardContent(card: InspirationCardEntity): String {
        val ciphertext = securePrefs.getCipherText(card.contentRef)
            ?: error("密文丢失: ${card.contentRef}")
        return ciphertext.decodeToString()
    }

    /**
     * 更新灵感卡片内容（保留 ID 和原有属性）
     */
    suspend fun updateCardContent(card: InspirationCardEntity, newContent: String): InspirationCardEntity {
        val ref = card.contentRef
        val ciphertext = newContent.encodeToByteArray()
        securePrefs.putCipherText(ref, ciphertext)

        // 解析多块 JSON 内容，拼接所有文字块前30字作为摘要（而非原始 JSON 字符串）
        val snippet = try {
            val arr = JSONArray(newContent)
            val textParts = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("type") == "text") {
                    textParts.add(obj.getString("data"))
                }
            }
            val combined = textParts.joinToString("").take(30)
            if (combined.isBlank()) newContent.take(30) else combined
        } catch (_: Exception) {
            newContent.take(30)
        }

        val updated = card.copy(
            contentSnippet = snippet,
            updatedAt = System.currentTimeMillis()
        )
        inspirationDao.upsert(updated)
        return updated
    }

    /**
     * 硬删除：移除 Room 记录 + 清除 SecurePrefs 密文（永久删除用）
     */
    suspend fun deleteCard(card: InspirationCardEntity) {
        securePrefs.removeCipherText(card.contentRef)
        inspirationDao.delete(card)
    }

    /**
     * 软删除：仅移除 Room 记录，保留 SecurePrefs 密文（回收站恢复用）
     */
    suspend fun softDeleteCard(card: InspirationCardEntity) {
        inspirationDao.delete(card)
    }

    /**
     * 从回收站恢复灵感卡片（原始 ID + 密文保留，恢复到无文件夹状态）
     */
    suspend fun restoreCardFromTrash(
        originalId: String,
        contentRef: String,
        contentSnippet: String,
        type: CardType,
        tags: String,
        privacyLevel: PrivacyLevel,
        folderId: String? = null
    ): InspirationCardEntity {
        val card = InspirationCardEntity(
            id = originalId,
            contentRef = contentRef,
            contentSnippet = contentSnippet,
            type = type,
            tags = tags,
            folderId = folderId,       // 恢复到原来的文件夹
            privacyLevel = privacyLevel,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        inspirationDao.upsert(card)
        return card
    }

    /**
     * 将灵感卡片移入/移出文件夹
     */
    suspend fun moveCardToFolder(cardId: String, folderId: String?) {
        inspirationDao.updateFolder(cardId, folderId)
    }

    /**
     * 将文件夹内所有卡片移出文件夹（删除文件夹时调用）
     */
    suspend fun moveCardToFolderByFolderId(folderId: String) {
        inspirationDao.clearFolder(folderId)
    }
}
