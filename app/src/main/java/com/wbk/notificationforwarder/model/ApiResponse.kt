package com.wbk.notificationforwarder.model

import com.google.gson.annotations.SerializedName

// Wadah Utama Response
data class ProfileResponse(
    val success: Boolean,
    val message: String,
    val data: ProfileData?
)

data class ProfileData(
    val member: Member,
    @SerializedName("payment_settings")
    val paymentSettings: PaymentSettings?
)

data class Member(
    val memberId: String,
    val nama: String,
    val saldo: Double,
    val role: String
)

data class PaymentSettings(
    val bank: List<PaymentItem>?,
    val ewallet: List<PaymentItem>?,
    val qris: List<PaymentItem>?
)

data class PaymentItem(
    val appId: String,
    val appName: String,     // Nama di web, misal "BCA Mobile"
    val targetApp: String,   // Package name di HP, misal "com.bca"
    val isActive: Boolean,
    val notificationPattern: String
)

// Wadah untuk Request Login
data class LoginRequest(
    val apiId: String,
    val apiKey: String
)

// Wadah untuk Kirim Notifikasi
data class NotificationRequest(
    @SerializedName("app") val app: String,
    @SerializedName("app_name") val app_name: String,
    @SerializedName("notification") val notification: String,
    @SerializedName("device_id") val device_id: String,
    @SerializedName("device_name") val device_name: String,
    @SerializedName("timestamp") val timestamp: Long,

    @SerializedName("apiId") val apiId: String,

    // PENTING: Mengunci nama field jadi "apikey" agar tidak tertukar/typo
    @SerializedName("apiKey") val apiKey: String
)