package com.wbk.notificationforwarder

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.wbk.notificationforwarder.api.ApiClient
import com.wbk.notificationforwarder.model.LoginRequest
import com.wbk.notificationforwarder.model.ProfileResponse
import com.wbk.notificationforwarder.utils.HashUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etApiId = findViewById<TextInputEditText>(R.id.etApiId)
        val etApiKey = findViewById<TextInputEditText>(R.id.etApiKey)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val session = SessionManager(this)

        btnLogin.setOnClickListener {
            val apiId = etApiId.text.toString().trim()
            val apiKey = etApiKey.text.toString().trim()

            if (apiId.isEmpty() || apiKey.isEmpty()) {
                Toast.makeText(this, "API ID dan Key harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Tampilkan Loading
            val loading = ProgressDialog(this)
            loading.setMessage("Sedang memverifikasi...")
            loading.setCancelable(false)
            loading.show()

            // 1. Siapkan Request
            val signature = HashUtils.createSignature(apiId, apiKey)
            val request = LoginRequest(apiId, apiKey)

            // 2. Panggil Server
            ApiClient.instance.getProfile(signature, request).enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    loading.dismiss()

                    // Cek jika server merespon sukses (HTTP 200)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!

                        if (body.success) {
                            // 3. Simpan Data jika Login Sukses
                            session.saveAuth(apiId, apiKey)

                            // Kita simpan juga nama dan saldo sementara (opsional, bisa ambil lagi nanti)
                            // Untuk simplisitas, kita lanjut ke Main
                            Toast.makeText(applicationContext, "Login Berhasil: ${body.data?.member?.nama}", Toast.LENGTH_LONG).show()

                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(applicationContext, "Gagal: ${body.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Error 401/404/500
                        Toast.makeText(applicationContext, "Login Gagal. Cek API ID/Key.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    loading.dismiss()
                    Toast.makeText(applicationContext, "Error Koneksi: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}

