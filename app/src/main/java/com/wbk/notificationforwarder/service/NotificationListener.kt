package com.wbk.notificationforwarder.service

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.wbk.notificationforwarder.SessionManager
import com.wbk.notificationforwarder.api.ApiClient
import com.wbk.notificationforwarder.model.NotificationRequest
import com.wbk.notificationforwarder.utils.HashUtils
import com.wbk.notificationforwarder.utils.HistoryManager
import com.wbk.notificationforwarder.utils.HistoryModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationListener : NotificationListenerService() {

    // === TAMBAHAN 1: JALANKAN FOREGROUND SERVICE SAAT LISTENER CONNECT ===
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.e("PG_DEBUG", "=== SERVICE LISTENER TERHUBUNG! ===")

        // Menyalakan KeepAliveService agar aplikasi tidak dibunuh OS
        val intent = Intent(this, KeepAliveService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    // (Opsional) Matikan KeepAlive jika listener putus/user mematikan izin
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.e("PG_DEBUG", "=== SERVICE LISTENER TERPUTUS! ===")
        // stopService(Intent(this, KeepAliveService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkgName = sbn.packageName
        val session = SessionManager(applicationContext)
        val historyManager = HistoryManager(applicationContext) // Pastikan file HistoryManager ada dari langkah sebelumnya

        // Ambil isi notifikasi
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // Potong pesan untuk log agar rapi
        val shortMessage = if (text.length > 50) text.substring(0, 50) + "..." else text

        // 1. Cek Master Switch
        if (!session.isServiceActive()) return

        // 2. Cek apakah masuk target
        val targetMap = session.getTargetMap()
        if (!targetMap.containsKey(pkgName)) return // Filter aplikasi non-target

        val category = targetMap[pkgName] ?: return
        val appName = getAppName(pkgName)

        // Persiapan Kirim
        val fullMessage = "$title $text"
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
        val apiId = session.getApiId() ?: ""
        val apiKey = session.getApiKey() ?: ""

        if (apiId.isEmpty() || apiKey.isEmpty()) return

        val request = NotificationRequest(
            app = pkgName,
            app_name = appName,
            notification = fullMessage,
            device_id = deviceId,
            device_name = deviceName,
            timestamp = System.currentTimeMillis(),
            apiId = apiId,
            apiKey = apiKey
        )

        val signature = HashUtils.createSignature(apiId, apiKey)

        // Kirim ke API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.instance.sendNotification(category, signature, request)
                    .enqueue(object : Callback<Any> {
                        override fun onResponse(call: Call<Any>, response: Response<Any>) {
                            val status = if (response.isSuccessful) "SUCCESS" else "FAILED"
                            val code = response.code()

                            // Simpan Log (Pastikan HistoryModel sudah ada)
                            val log = HistoryModel(
                                appName = appName,
                                message = shortMessage,
                                status = "$status ($code)",
                                timestamp = System.currentTimeMillis(),
                                httpCode = code
                            )
                            historyManager.saveLog(log)
                            broadcastUpdate()

                            Log.e("PG_API", "Result: $code")
                        }

                        override fun onFailure(call: Call<Any>, t: Throwable) {
                            // Simpan Log Gagal Koneksi
                            val log = HistoryModel(
                                appName = appName,
                                message = "Error: ${t.message}",
                                status = "FAILED (Network)",
                                timestamp = System.currentTimeMillis()
                            )
                            historyManager.saveLog(log)
                            broadcastUpdate()
                        }
                    })
            } catch (e: Exception) {
                Log.e("PG_API", "Exception: ${e.message}")
            }
        }
    }

    private fun broadcastUpdate() {
        // Kirim sinyal ke MainActivity untuk refresh list log
        val intent = Intent("com.wbk.notificationforwarder.UPDATE_LOGS")
        intent.setPackage(packageName) // Pastikan hanya diterima aplikasi sendiri
        sendBroadcast(intent)
    }

    private fun getAppName(pkg: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            pkg
        }
    }
}