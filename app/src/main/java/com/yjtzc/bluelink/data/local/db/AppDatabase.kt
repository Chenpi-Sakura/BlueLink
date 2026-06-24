package com.yjtzc.bluelink.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        PendingSyncEntity::class,
        TrashItemEntity::class
    ],
    version = 5,                       // V4 → V5: 加 inspiration_cards.coverAspectRatio 列（存图片宽高比，用于 UI 渲染避免 Image layout 跳变）
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
    abstract fun trashDao(): TrashDao

    companion object {
        /**
         * V4 → V5: 给 inspiration_cards 表加 coverAspectRatio REAL DEFAULT NULL 列
         * - 旧数据保留为 NULL（fallback 4:3 渲染）
         * - 用户重新编辑保存时由 CaptureRepository.updateCardContent 计算真实 ratio 写入
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inspiration_cards ADD COLUMN coverAspectRatio REAL DEFAULT NULL")
            }
        }

        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "bluelink.db"
            )
                // 之前用 fallbackToDestructiveMigration() 升级时直接丢数据——已替换为 addMigrations(MIGRATION_4_5)
                // 保留旧数据，旧卡片 coverAspectRatio = NULL（fallback 4:3 渲染），用户重新编辑后自动写入真实 ratio
                .addMigrations(MIGRATION_4_5)
                .build()
        }
    }
}
