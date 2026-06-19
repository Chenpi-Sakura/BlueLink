package com.yjtzc.bluelink.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room 数据库（V2.0 §3.1 / V2.1 §1.4）
 *
 * 单文件 bluelink.db，客户端本地数据持久化。
 */
@Database(
    entities = [
        DocumentEntity::class,
        SegmentEntity::class,
        InspirationCardEntity::class,
        AnchorEntity::class,
        GraphNodeEntity::class,
        GraphEdgeEntity::class,
        PendingSyncEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
    abstract fun segmentDao(): SegmentDao
    abstract fun anchorDao(): AnchorDao
    abstract fun inspirationDao(): InspirationDao
    abstract fun graphNodeDao(): GraphNodeDao
    abstract fun graphEdgeDao(): GraphEdgeDao
    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "bluelink.db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
