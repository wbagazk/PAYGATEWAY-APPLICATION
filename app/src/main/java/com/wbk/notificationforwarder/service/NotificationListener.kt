package com.wbk.notificationforwarder.service

import android.app.Notification
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.wbk.notificationforwarder.SessionManager
import com.wbk.notificationforwarder.api.ApiClient
import com.wbk.notificationforwarder.model.NotificationRequest
import com.wbk.notificationforwarder.utils.HashUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationListener : NotificationListenerService() {

    // Log saat service pertama kali jalan (terhubung)
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.e("PG_DEBUG", "=== SERVICE TERHUBUNG! SIAP MEMBACA NOTIFIKASI ===")
    }

    // Log saat service putus
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.e("PG_DEBUG", "=== SERVICE TERPUTUS! ===")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null) return

        val pkgName = sbn.packageName
        Log.e("PG_DEBUG", "1. Ada Notif masuk dari: $pkgName") // <--- LOG PENTING

        val session = SessionManager(applicationContext)

        // 1. Cek Master Switch
        if (!session.isServiceActive()) {
            Log.e("PG_DEBUG", "   -> Ditolak: Layanan Master Switch dimatikan user.")
            return
        }

        // 2. Cek Login
        val apiId = session.getApiId()
        val apiKey = session.getApiKey()
        if (apiId == null || apiKey == null) {
            Log.e("PG_DEBUG", "   -> Ditolak: User belum login.")
            return
        }

        // 3. Cek App Target
        val targetMap = session.getTargetMap()
        Log.e("PG_DEBUG", "   -> Daftar Target di HP: ${targetMap.keys}") // Cek apakah list target kosong?

        if (!targetMap.containsKey(pkgName)) {
            Log.e("PG_DEBUG", "   -> Ditolak: $pkgName tidak ada dalam daftar target.")
            return
        }

        val category = targetMap[pkgName] ?: return
        Log.e("PG_DEBUG", "2. LOLOS SELEKSI! Mengirim $pkgName ($category) ke server...")

        // 4. Ekstrak Isi
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val fullMessage = "$title $text $subText"

        // 5. Kirim ke Server
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"

        val request = NotificationRequest(
            app = pkgName,
            app_name = getAppName(pkgName),
            notification = fullMessage,
            device_id = deviceId,
            device_name = deviceName,
            timestamp = System.currentTimeMillis(),
            apiId = apiId,
            apiKey = apiKey
        )

        val signature = HashUtils.createSignature(apiId, apiKey)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.instance.sendNotification(category, signature, request)
                    .enqueue(object : Callback<Any> {
                        override fun onResponse(call: Call<Any>, response: Response<Any>) {
                            Log.e("PG_API", "SUKSES KIRIM! Server Response: ${response.code()}")
                        }
                        override fun onFailure(call: Call<Any>, t: Throwable) {
                            Log.e("PG_API", "GAGAL KIRIM: ${t.message}")
                        }
                    })
            } catch (e: Exception) {
                Log.e("PG_API", "ERROR: ${e.message}")
            }
        }
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