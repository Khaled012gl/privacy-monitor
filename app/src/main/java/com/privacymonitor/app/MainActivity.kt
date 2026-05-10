package com.privacymonitor.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusMic: TextView
    private lateinit var statusCamera: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnBattery: Button
    private var isMonitoring = false

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val micActive = intent?.getBooleanExtra("mic_active", false) ?: false
            val cameraActive = intent?.getBooleanExtra("camera_active", false) ?: false
            statusMic.text = if (micActive) "🎤 Microphone: ⚠️ ACTIVE!" else "🎤 Microphone: Safe"
            statusCamera.text = if (cameraActive) "📷 Camera: ⚠️ ACTIVE!" else "📷 Camera: Safe"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusMic = findViewById(R.id.statusMic)
        statusCamera = findViewById(R.id.statusCamera)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnBattery = findViewById(R.id.btnBattery)

        btnStart.setOnClickListener {
            val intent = Intent(this, MonitorService::class.java)
            ContextCompat.startForegroundService(this, intent)
            isMonitoring = true
            btnStart.isEnabled = false
            btnStop.isEnabled = true
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, MonitorService::class.java))
            isMonitoring = false
            btnStart.isEnabled = true
            btnStop.isEnabled = false
            statusMic.text = "🎤 Microphone: Not monitoring"
            statusCamera.text = "📷 Camera: Not monitoring"
        }

        btnBattery.setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        }

        btnStop.isEnabled = false
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("com.privacymonitor.STATUS_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statusReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(statusReceiver)
    }
}
