package com.wbk.notificationforwarder

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wbk.notificationforwarder.adapter.AppAdapter
import com.wbk.notificationforwarder.adapter.LogAdapter
import com.wbk.notificationforwarder.api.ApiClient
import com.wbk.notificationforwarder.model.LoginRequest
import com.wbk.notificationforwarder.model.PaymentItem
import com.wbk.notificationforwarder.model.ProfileResponse
import com.wbk.notificationforwarder.utils.HashUtils
import com.wbk.notificationforwarder.utils.HistoryManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var historyManager: HistoryManager
    private lateinit var tvUserName: TextView
    private lateinit var tvSaldo: TextView
    private lateinit var btnMasterSwitch: Button
    private lateinit var rvAppList: RecyclerView

    // Variabel untuk Log
    private lateinit var rvLogList: RecyclerView
    private lateinit var btnClearLog: TextView
    private lateinit var logAdapter: LogAdapter

    // Receiver untuk update log realtime
    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadLogs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isNotificationServiceEnabled()) {
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
            return
        }

        session = SessionManager(this)
        historyManager = HistoryManager(this)

        if (!session.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        setupDashboard()
        setupLogSection() // Setup bagian bawah (log)
        fetchProfileData()
    }

    override fun onResume() {
        super.onResume()
        // Register receiver saat aplikasi dibuka
        val filter = IntentFilter("com.wbk.notificationforwarder.UPDATE_LOGS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(logReceiver, filter)
        }
        loadLogs() // Refresh manual saat resume
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(logReceiver)
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

    private fun setupLogSection() {
        rvLogList = findViewById(R.id.rvLogList)
        btnClearLog = findViewById(R.id.btnClearLog)

        logAdapter = LogAdapter(emptyList())
        rvLogList.layoutManager = LinearLayoutManager(this)
        rvLogList.adapter = logAdapter

        loadLogs()

        btnClearLog.setOnClickListener {
            historyManager.clearLogs()
            loadLogs()
            Toast.makeText(this, "Log dihapus", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadLogs() {
        val logs = historyManager.getLogs()
        runOnUiThread {
            logAdapter.updateData(logs)
            if (logs.isNotEmpty()) {
                rvLogList.smoothScrollToPosition(0)
            }
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

                        tvUserName.text = "${member?.nama} (${member?.role})"
                        tvSaldo.text = "Rp ${member?.saldo}"

                        val allApps = mutableListOf<PaymentItem>()
                        val targetMap = mutableMapOf<String, String>()

                        settings?.bank?.forEach { if(it.isActive) { allApps.add(it); targetMap[it.targetApp] = "bank" } }
                        settings?.ewallet?.forEach { if(it.isActive) { allApps.add(it); targetMap[it.targetApp] = "ewallet" } }
                        settings?.qris?.forEach { if(it.isActive) { allApps.add(it); targetMap[it.targetApp] = "qris" } }

                        rvAppList.adapter = AppAdapter(allApps)
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