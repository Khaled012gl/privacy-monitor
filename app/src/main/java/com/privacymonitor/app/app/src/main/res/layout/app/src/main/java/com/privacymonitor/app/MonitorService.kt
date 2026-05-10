package com.privacymonitor.app

import android.app.*
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

class MonitorService : Service() {

    private lateinit var appOpsManager: AppOpsManager
    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private val handler = Handler(Looper.getMainLooper())
    private var wasMicActive = false
    private var wasCameraActive = false
    private val CHANNEL_ID = "privacy_monitor_channel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                ttsReady = true
            }
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Monitoring mic & camera..."))
        handler.post(monitorRunnable)
        return START_STICKY
    }

    private val monitorRunnable = object : Runnable {
        override fun run() {
            val micActive = isMicrophoneActive()
            val cameraActive = isCameraActive()

            if (micActive && !wasMicActive) {
                speak("Warning! The microphone has been activated.")
                updateNotification("⚠️ Microphone is active!")
            }
            if (cameraActive && !wasCameraActive) {
                speak("Warning! The camera has been activated.")
                updateNotification("⚠️ Camera is active!")
            }
            if (!micActive && !cameraActive && (wasMicActive || wasCameraActive)) {
                updateNotification("Monitoring mic & camera...")
            }

            val broadcast = Intent("com.privacymonitor.STATUS_UPDATE")
            broadcast.putExtra("mic_active", micActive)
            broadcast.putExtra("camera_active", cameraActive)
            sendBroadcast(broadcast)

            wasMicActive = micActive
            wasCameraActive = cameraActive
            handler.postDelayed(this, 1000L)
        }
    }

    private fun isMicrophoneActive(): Boolean {
        return try {
            val packages = packageManager.getInstalledPackages(0)
            packages.any { pkg ->
                try {
                    val uid = packageManager.getApplicationInfo(pkg.packageName, 0).uid
                    appOpsManager.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_RECORD_AUDIO, uid, pkg.packageName
                    ) == AppOpsManager.MODE_ALLOWED
                } catch (e: Exception) { false }
            }
        } catch (e: Exception) { false }
    }

    private fun isCameraActive(): Boolean {
        return try {
            val packages = packageManager.getInstalledPackages(0)
            packages.any { pkg ->
                try {
                    val uid = packageManager.getApplicationInfo(pkg.packageName, 0).uid
                    appOpsManager.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_CAMERA, uid, pkg.packageName
                    ) == AppOpsManager.MODE_ALLOWED
                } catch (e: Exception) { false }
            }
        } catch (e: Exception) { false }
    }

    private fun speak(text: String) {
        if (ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "alert")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Privacy Monitor", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String): Notification {
        val intent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔒 Privacy Monitor")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(intent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(message))
    }

    override fun onDestroy() {
        handler.removeCallbacks(monitorRunnable)
        if (ttsReady) tts.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
