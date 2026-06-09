package com.yjtzc.bluelink.data.repository

import com.yjtzc.bluelink.data.local.crypto.SecurePrefs
import com.yjtzc.bluelink.data.local.db.CardType
import com.yjtzc.bluelink.data.local.db.InspirationCardEntity
import com.yjtzc.bluelink.data.local.db.InspirationDao
import com.yjtzc.bluelink.data.local.db.PrivacyLevel
import com.yjtzc.bluelink.data.remote.BlueLinkApi
import com.yjtzc.bluelink.data.remote.dto.CreateCardRequest
import kotlinx.coroutines.flow.Flow
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
            tags = tags.joinToString(",")
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

    suspend fun deleteCard(card: InspirationCardEntity) {
        securePrefs.removeCipherText(card.contentRef)
        inspirationDao.delete(card)
    }
}
