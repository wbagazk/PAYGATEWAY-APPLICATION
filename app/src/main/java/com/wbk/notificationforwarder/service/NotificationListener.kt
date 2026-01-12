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

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null) return

        val session = SessionManager(applicationContext)

        // 1. Cek Master Switch: Apakah layanan diaktifkan user?
        if (!session.isServiceActive()) return

        // 2. Cek Login: Apakah user sudah login?
        val apiId = session.getApiId()
        val apiKey = session.getApiKey()
        if (apiId == null || apiKey == null) return

        // 3. Cek App Target: Apakah aplikasi ini ada di daftar yang kita pantau?
        val packageName = sbn.packageName
        val targetMap = session.getTargetMap() // Ambil map dari session

        // Jika packageName tidak ada di map, berhenti (abaikan notifikasi)
        if (!targetMap.containsKey(packageName)) return

        // Ambil kategori (bank/ewallet/qris) dari map
        val category = targetMap[packageName] ?: return

        // 4. Ekstrak Isi Notifikasi
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        // Gabungkan teks agar Regex di server bisa membaca seluruh pola
        val fullMessage = "$title $text $subText"

        Log.d("PG_NOTIF", "Forwarding: $packageName -> $fullMessage")

        // 5. Kirim ke Server (Gunakan Coroutine agar tidak membebani Main Thread)
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"

        val request = NotificationRequest(
            app = packageName,
            app_name = getAppName(packageName),
            notification = fullMessage,
            device_id = deviceId,
            device_name = deviceName,
            timestamp = System.currentTimeMillis(),
            apiId = apiId,
            apikey = apiKey
        )

        val signature = HashUtils.createSignature(apiId, apiKey)

        // Kirim via Retrofit
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.instance.sendNotification(category, signature, request)
                    .enqueue(object : Callback<Any> {
                        override fun onResponse(call: Call<Any>, response: Response<Any>) {
                            Log.d("PG_API", "Success sent: ${response.code()}")
                        }
                        override fun onFailure(call: Call<Any>, t: Throwable) {
                            Log.e("PG_API", "Failed: ${t.message}")
                        }
                    })
            } catch (e: Exception) {
                Log.e("PG_API", "Error: ${e.message}")
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