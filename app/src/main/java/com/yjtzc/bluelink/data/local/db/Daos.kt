package com.yjtzc.bluelink.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ====== DocumentDao ======

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: String): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(doc: DocumentEntity)

    @Delete
    suspend fun delete(doc: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM documents WHERE title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun search(query: String): Flow<List<DocumentEntity>>
}

// ====== SegmentDao ======

@Dao
interface SegmentDao {
    @Query("SELECT * FROM segments WHERE docId = :docId ORDER BY indexInDoc ASC")
    suspend fun getByDocId(docId: String): List<SegmentEntity>

    @Query("SELECT * FROM segments WHERE docId = :docId ORDER BY indexInDoc ASC")
    fun observeByDocId(docId: String): Flow<List<SegmentEntity>>

    @Query("SELECT * FROM segments WHERE id = :id")
    suspend fun getById(id: String): SegmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(segments: List<SegmentEntity>)

    @Query("UPDATE segments SET isFolded = :folded WHERE id IN (:ids)")
    suspend fun updateFolded(ids: List<String>, folded: Boolean)

    @Query("UPDATE segments SET isSpotlightTarget = :target WHERE id = :id")
    suspend fun setSpotlight(id: String, target: Boolean)

    @Query("SELECT textRef FROM segments WHERE docId = :docId")
    suspend fun getTextRefsByDocId(docId: String): List<String>

    @Query("DELETE FROM segments WHERE docId = :docId")
    suspend fun deleteByDocId(docId: String)

    @Delete
    suspend fun delete(entity: SegmentEntity)
}

// ====== AnchorDao ======

@Dao
interface AnchorDao {
    @Query("SELECT * FROM anchors WHERE queryHash = :queryHash ORDER BY score DESC")
    suspend fun getByQueryHash(queryHash: String): List<AnchorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(anchors: List<AnchorEntity>)

    @Query("UPDATE anchors SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("DELETE FROM anchors WHERE createdAt < :before")
    suspend fun cleanOlderThan(before: Long)
}

// ====== InspirationDao ======

@Dao
interface InspirationDao {
    @Query("SELECT * FROM inspiration_cards ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InspirationCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(card: InspirationCardEntity)

    @Delete
    suspend fun delete(card: InspirationCardEntity)
}

// ====== GraphNodeDao ======

@Dao
interface GraphNodeDao {
    @Query("SELECT * FROM graph_nodes")
    suspend fun getAll(): List<GraphNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(nodes: List<GraphNodeEntity>)

    @Query("DELETE FROM graph_nodes")
    suspend fun clearAll()
}

// ====== GraphEdgeDao ======

@Dao
interface GraphEdgeDao {
    @Query("SELECT * FROM graph_edges")
    suspend fun getAll(): List<GraphEdgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(edges: List<GraphEdgeEntity>)

    @Query("DELETE FROM graph_edges")
    suspend fun clearAll()
}

// ====== PendingSyncDao ======

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun listPending(): List<PendingSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingSyncEntity)

    @Query("UPDATE pending_sync SET status = :status, retryCount = :retryCount, lastError = :lastError WHERE id = :id")
    suspend fun updateStatus(id: String, status: SyncStatus, retryCount: Int = 0, lastError: String? = null)

    @Query("UPDATE pending_sync SET serverRefId = :serverRefId, status = 'SUCCESS' WHERE id = :id")
    suspend fun markSuccess(id: String, serverRefId: String? = null)
}
