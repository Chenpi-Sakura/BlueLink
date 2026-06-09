package com.yjtzc.bluelink

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * Application 入口 — 全局初始化时机。
 * 创建 AppContainer（手写 DI 容器）、通知频道等。
 */
class BlueLinkApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
    }

    /**
     * V2.1 Vivo 适配：所有通知走统一 NotificationChannel（§6.4）
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SYNC, "同步与备份",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "灵感卡片与文档同步状态" }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_CAPTURE, "灵感捕获",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "OCR 识别与费曼评估结果" }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_RECORDING, "录音中",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "语音灵感录入前台服务" }
            )
        }
    }

    companion object {
        const val CHANNEL_SYNC = "bluelink_sync"
        const val CHANNEL_CAPTURE = "bluelink_capture"
        const val CHANNEL_RECORDING = "bluelink_recording"
    }
}
