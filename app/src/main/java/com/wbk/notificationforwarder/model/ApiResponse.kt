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
    val app: String,          // Package name
    val app_name: String,     // Nama App
    val notification: String, // Isi pesan
    val device_id: String,
    val device_name: String,
    val timestamp: Long,
    val apiId: String,
    val apikey: String
)