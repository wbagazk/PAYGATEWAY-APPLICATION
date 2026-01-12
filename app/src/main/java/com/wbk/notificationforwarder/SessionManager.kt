package com.wbk.notificationforwarder

import android.content.Context
import android.content.SharedPreferences
import kotlin.apply

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("PG_PREFS", Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()

    companion object {
        const val KEY_API_ID = "api_id"
        const val KEY_API_KEY = "api_key"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_IS_SERVICE_ACTIVE = "is_service_active"
    }

    fun saveAuth(apiId: String, apiKey: String) {
        editor.putString(KEY_API_ID, apiId)
        editor.putString(KEY_API_KEY, apiKey)
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.apply()
    }

    fun getApiId(): String? = prefs.getString(KEY_API_ID, null)
    fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun setServiceActive(isActive: Boolean) {
        editor.putBoolean(KEY_IS_SERVICE_ACTIVE, isActive)
        editor.apply()
    }

    fun isServiceActive(): Boolean = prefs.getBoolean(KEY_IS_SERVICE_ACTIVE, true) // Default true

    fun logout() {
        editor.clear()
        editor.apply()
    }
    // Simpan daftar aplikasi yang dipantau dalam format: "com.bca:bank,com.dana:ewallet"
    fun saveTargetMap(map: Map<String, String>) {
        // Kita ubah Map menjadi String panjang dipisah koma
        val setString = map.entries.joinToString(",") { "${it.key}:${it.value}" }
        editor.putString("target_app_map", setString)
        editor.apply()
    }

    // Ambil daftar aplikasi saat Service berjalan
    fun getTargetMap(): Map<String, String> {
        val raw = prefs.getString("target_app_map", "") ?: ""
        if (raw.isEmpty()) return emptyMap()

        val map = mutableMapOf<String, String>()
        raw.split(",").forEach {
            val parts = it.split(":")
            if (parts.size == 2) {
                map[parts[0]] = parts[1] // Key: package, Value: category
            }
        }
        return map
    }
}