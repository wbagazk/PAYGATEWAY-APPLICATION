package com.wbk.notificationforwarder.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.wbk.notificationforwarder.R

class KeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, "CHANNEL_FOREGROUND")
            .setContentTitle("PG Forwarder Aktif")
            .setContentText("Sedang memantau notifikasi di latar belakang...")
            .setSmallIcon(R.mipmap.ic_launcher) // Pastikan icon ada
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Tidak bisa di-swipe user
            .build()

        // Kompatibilitas Android 14 (API 34)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }

        // START_STICKY: Jika dimatikan paksa oleh sistem karena low memory,
        // service akan hidup lagi otomatis saat memori tersedia.
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "CHANNEL_FOREGROUND",
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW // Low agar tidak bunyi/getar terus
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}