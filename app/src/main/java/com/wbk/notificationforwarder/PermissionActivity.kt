package com.wbk.notificationforwarder

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PermissionActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnOpenSettings: Button
    private lateinit var btnNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        tvStatus = findViewById(R.id.tvPermissionStatus)
        btnOpenSettings = findViewById(R.id.btnOpenSettings)
        btnNext = findViewById(R.id.btnNext)

        btnOpenSettings.setOnClickListener {
            // Membuka menu pengaturan Notifikasi Android
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnNext.setOnClickListener {
            // Lanjut ke Login atau Main
            val session = SessionManager(this)
            if (session.isLoggedIn()) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    private fun checkPermission() {
        val isGranted = isNotificationServiceEnabled()
        if (isGranted) {
            tvStatus.text = "Status: Diizinkan"
            tvStatus.setTextColor(Color.parseColor("#4CAF50")) // Hijau

            btnNext.isEnabled = true
            btnNext.alpha = 1.0f
            btnOpenSettings.text = "Izin Sudah Diberikan"
            btnOpenSettings.isEnabled = false
        } else {
            tvStatus.text = "Status: Tidak Diizinkan"
            tvStatus.setTextColor(Color.parseColor("#D32F2F")) // Merah

            btnNext.isEnabled = false
            btnNext.alpha = 0.5f
            btnOpenSettings.isEnabled = true
        }
    }

    // Logic ajaib untuk mengecek apakah user sudah mencentang aplikasi kita
    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                val component = ComponentName.unflattenFromString(name)
                if (component != null && component.packageName == pkgName) {
                    return true
                }
            }
        }
        return false
    }
}