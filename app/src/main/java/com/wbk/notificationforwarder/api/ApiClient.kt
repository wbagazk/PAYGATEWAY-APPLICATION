package com.wbk.notificationforwarder.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // PENTING: Ganti URL ini sesuai servermu.
    // Jika pakai Emulator Android Studio: "http://10.0.2.2:3000/"
    // Jika pakai HP Fisik: "http://IP_LAPTOP_KAMU:3000/" (Pastikan satu WiFi)
    // Jika sudah online: "https://paygateway.up.app/"
    private const val BASE_URL = "https://paygateway.up.railway.app/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}