package com.diabeat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.diabeat.R
import com.diabeat.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log

class FCMService : FirebaseMessagingService() {
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // 将 token 发送到服务器
        sendTokenToServer(token)
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // 处理通知
        remoteMessage.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "DiabEat AI",
                body = notification.body ?: "",
                data = remoteMessage.data
            )
        }
    }
    
    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 创建通知渠道（Android 8.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DiabEat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "胰岛素和用餐提醒通知"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // 创建点击意图
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", data["type"])
            putExtra("notification_data", data.toString())
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 构建通知
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    private fun sendTokenToServer(token: String) {
        Log.d("FCMService", "FCM token received: $token")
    }
    
    companion object {
        private const val CHANNEL_ID = "diabeat_notifications"
    }
}

