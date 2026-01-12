package com.wbk.notificationforwarder.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryModel(
    val appName: String,
    val message: String,
    val status: String, // "SUCCESS", "FAILED", "FILTERED"
    val timestamp: Long,
    val httpCode: Int = 0
)

class HistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("PG_HISTORY", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val MAX_LOGS = 50 // Batasi hanya 50 log terakhir agar tidak memberatkan HP

    fun saveLog(log: HistoryModel) {
        val currentList = getLogs().toMutableList()
        currentList.add(0, log) // Tambah di paling atas (terbaru)

        // Hapus log lama jika melebihi batas
        if (currentList.size > MAX_LOGS) {
            currentList.removeAt(currentList.size - 1)
        }

        val json = gson.toJson(currentList)
        prefs.edit().putString("history_logs", json).apply()
    }

    fun getLogs(): List<HistoryModel> {
        val json = prefs.getString("history_logs", null) ?: return emptyList()
        val type = object : TypeToken<List<HistoryModel>>() {}.type
        return gson.fromJson(json, type)
    }

    fun clearLogs() {
        prefs.edit().clear().apply()
    }

    companion object {
        fun formatTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}