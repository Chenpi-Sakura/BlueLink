package com.yjtzc.bluelink.data.repository

import com.yjtzc.bluelink.data.local.db.TrashDao
import com.yjtzc.bluelink.data.local.db.TrashItemEntity
import com.yjtzc.bluelink.data.local.crypto.SecurePrefs
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * 回收站仓库（删除后保留 15 天）
 */
class TrashRepository(
    private val trashDao: TrashDao,
    private val securePrefs: SecurePrefs
) {

    fun observeAll(): Flow<List<TrashItemEntity>> = trashDao.observeAll()

    fun observeCount(): Flow<Int> = trashDao.observeCount()

    suspend fun getById(id: String): TrashItemEntity? = trashDao.getById(id)

    /**
     * 移入回收站
     */
    suspend fun moveToTrash(
        originalId: String,
        itemType: String,
        title: String,
        contentRef: String? = null,
        contentSnippet: String = "",
        metadataJson: String? = null
    ) {
        val now = System.currentTimeMillis()
        val item = TrashItemEntity(
            id = UUID.randomUUID().toString(),
            originalId = originalId,
            itemType = itemType,
            title = title,
            contentRef = contentRef,
            contentSnippet = contentSnippet,
            metadataJson = metadataJson,
            deletedAt = now,
            expiresAt = now + 15L * 24 * 60 * 60 * 1000
        )
        trashDao.upsert(item)
    }

    /**
     * 从回收站恢复（仅删除回收站记录，由调用方负责还原内容）
     */
    suspend fun restore(item: TrashItemEntity) {
        trashDao.delete(item)
    }

    /**
     * 删除回收站记录（不清理密文，由调用方按需处理）
     */
    suspend fun deleteFromTrash(item: TrashItemEntity) {
        trashDao.delete(item)
    }

    /**
     * 永久删除
     */
    suspend fun permanentlyDelete(item: TrashItemEntity) {
        // 清除 SecurePrefs 中的密文
        if (item.contentRef != null) {
            securePrefs.removeCipherText(item.contentRef)
        }
        trashDao.delete(item)
    }

    /**
     * 清理过期项目
     */
    suspend fun cleanExpired() {
        trashDao.deleteExpired()
    }
}
