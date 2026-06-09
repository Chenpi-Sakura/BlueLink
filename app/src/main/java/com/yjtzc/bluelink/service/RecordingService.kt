package com.yjtzc.bluelink.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.yjtzc.bluelink.BlueLinkApplication
import com.yjtzc.bluelink.MainActivity
import com.yjtzc.bluelink.R

/**
 * 前台录音服务 — Vivo 后台管控应对（V2.1 §6.2）
 *
 * 录音中使用 ForegroundService 保持进程存活，
 * 防止 OriginOS 后台冻结导致录音中断。
 */
class RecordingService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, BlueLinkApplication.CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_favorite)   // TODO: 替换为 ic_mic
            .setContentTitle("蓝链正在录音")
            .setContentText("灵感语音录入中...")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // TODO: 初始化 SpeechRecognizer 并开始监听
        // speechRecognizer.startListening(...)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // TODO: 停止 SpeechRecognizer
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
