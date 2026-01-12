package com.wbk.notificationforwarder

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wbk.notificationforwarder.adapter.AppAdapter
import com.wbk.notificationforwarder.api.ApiClient
import com.wbk.notificationforwarder.model.LoginRequest
import com.wbk.notificationforwarder.model.PaymentItem
import com.wbk.notificationforwarder.model.ProfileResponse
import com.wbk.notificationforwarder.utils.HashUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var tvUserName: TextView
    private lateinit var tvSaldo: TextView
    private lateinit var btnMasterSwitch: Button
    private lateinit var rvAppList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isNotificationServiceEnabled()) {
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
            return
        }

        session = SessionManager(this)
        if (!session.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        setupDashboard()
        fetchProfileData() // Ambil data dari server
    }

    private fun setupDashboard() {
        tvUserName = findViewById(R.id.tvUserName)
        tvSaldo = findViewById(R.id.tvSaldo)
        btnMasterSwitch = findViewById(R.id.btnMasterSwitch)
        rvAppList = findViewById(R.id.rvAppList)
        rvAppList.layoutManager = LinearLayoutManager(this)

        tvUserName.text = "Memuat data..."
        updateSwitchUI()

        btnMasterSwitch.setOnClickListener {
            val currentStatus = session.isServiceActive()
            session.setServiceActive(!currentStatus)
            updateSwitchUI()
            Toast.makeText(this, "Status Layanan Diubah", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSwitchUI() {
        if (session.isServiceActive()) {
            btnMasterSwitch.text = "Layanan: AKTIF (Sedang Membaca)"
            btnMasterSwitch.setBackgroundColor(getColor(R.color.btn_blue))
        } else {
            btnMasterSwitch.text = "Layanan: NONAKTIF (Berhenti)"
            btnMasterSwitch.setBackgroundColor(getColor(R.color.outline_red))
        }
    }

    private fun fetchProfileData() {
        val apiId = session.getApiId() ?: ""
        val apiKey = session.getApiKey() ?: ""
        val signature = HashUtils.createSignature(apiId, apiKey)

        ApiClient.instance.getProfile(signature, LoginRequest(apiId, apiKey))
            .enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()!!.data
                        val member = data?.member
                        val settings = data?.paymentSettings

                        // 1. Update UI Header
                        tvUserName.text = "${member?.nama} (${member?.role})"
                        tvSaldo.text = "Rp ${member?.saldo}"

                        // 2. Gabungkan semua payment setting jadi satu list
                        val allApps = mutableListOf<PaymentItem>()
                        val targetMap = mutableMapOf<String, String>() // Map untuk Session

                        settings?.bank?.forEach {
                            if(it.isActive) { allApps.add(it); targetMap[it.targetApp] = "bank" }
                        }
                        settings?.ewallet?.forEach {
                            if(it.isActive) { allApps.add(it); targetMap[it.targetApp] = "ewallet" }
                        }
                        settings?.qris?.forEach {
                            if(it.isActive) { allApps.add(it); targetMap[it.targetApp] = "qris" }
                        }

                        // 3. Tampilkan di RecyclerView
                        rvAppList.adapter = AppAdapter(allApps)

                        // 4. Simpan Map ke Session untuk dipakai NotificationService
                        session.saveTargetMap(targetMap)

                    } else {
                        tvUserName.text = "Gagal memuat data"
                    }
                }

                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    tvUserName.text = "Error Koneksi"
                }
            })
    }

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