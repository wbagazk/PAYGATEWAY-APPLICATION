package com.wbk.notificationforwarder.api

import com.wbk.notificationforwarder.model.LoginRequest
import com.wbk.notificationforwarder.model.NotificationRequest
import com.wbk.notificationforwarder.model.ProfileResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    // Login & Ambil Data Profile
    @POST("api/profile")
    fun getProfile(
        @Header("Authorization") signature: String, // Server butuh header ini
        @Body request: LoginRequest
    ): Call<ProfileResponse>

    // Kirim Notifikasi
    @POST("api/incoming-notification/{category}")
    fun sendNotification(
        @Path("category") category: String, // bank, ewallet, atau qris
        @Header("Authorization") signature: String,
        @Body request: NotificationRequest
    ): Call<Any> // Pakai Any karena kita cuma butuh status 200 OK
}