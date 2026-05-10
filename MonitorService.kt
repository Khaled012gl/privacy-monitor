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
    private var checkInterval = 1000L // Check every 1 second

    private var wasMicActive = false
    private var wasCameraActive = false

    private val CHANNEL_ID = "privacy_monitor_channel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        setupTTS()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Monitoring mic & camera..."))
        startMonitoring()
        return START_STICKY // Restart if killed
    }

    private fun setupTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                tts.setSpeechRate(0.9f)
                ttsReady = true
            }
        }
    }

    private fun startMonitoring() {
        handler.post(monitorRunnable)
    }

    private val monitorRunnable = object : Runnable {
        override fun run() {
            val micActive = isMicrophoneActive()
            val cameraActive = isCameraActive()

            // Mic just turned ON
            if (micActive && !wasMicActive) {
                speak("Warning! The microphone has been activated.")
                updateNotification("⚠️ Microphone is active!")
            }

            // Camera just turned ON
            if (cameraActive && !wasCameraActive) {
                speak("Warning! The camera has been activated.")
                updateNotification("⚠️ Camera is active!")
            }

            // Both turned off
            if (!micActive && !cameraActive && (wasMicActive || wasCameraActive)) {
                updateNotification("Monitoring mic & camera...")
            }

            // Broadcast status to UI
            val broadcastIntent = Intent("com.privacymonitor.STATUS_UPDATE")
            broadcastIntent.putExtra("mic_active", micActive)
            broadcastIntent.putExtra("camera_active", cameraActive)
            sendBroadcast(broadcastIntent)

            wasMicActive = micActive
            wasCameraActive = cameraActive

            handler.postDelayed(this, checkInterval)
        }
    }

    private fun isMicrophoneActive(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val mode = appOpsManager.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_RECORD_AUDIO,
                    android.os.Process.myUid(),
                    packageName
                )
                // Also check other apps
                val packages = packageManager.getInstalledPackages(0)
                packages.any { pkg ->
                    try {
                        val uid = packageManager.getApplicationInfo(pkg.packageName, 0).uid
                        appOpsManager.unsafeCheckOpNoThrow(
                            AppOpsManager.OPSTR_RECORD_AUDIO,
                            uid,
                            pkg.packageName
                        ) == AppOpsManager.MODE_ALLOWED
                    } catch (e: Exception) { false }
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun isCameraActive(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val packages = packageManager.getInstalledPackages(0)
                packages.any { pkg ->
                    try {
                        val uid = packageManager.getApplicationInfo(pkg.packageName, 0).uid
                        appOpsManager.unsafeCheckOpNoThrow(
                            AppOpsManager.OPSTR_CAMERA,
                            uid,
                            pkg.packageName
                        ) == AppOpsManager.MODE_ALLOWED
                    } catch (e: Exception) { false }
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun speak(text: String) {
        if (ttsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "alert_${System.currentTimeMillis()}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Privacy Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when Privacy Monitor is running"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔒 Privacy Monitor")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(message: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(message))
    }

    override fun onDestroy() {
        handler.removeCallbacks(monitorRunnable)
        if (ttsReady) tts.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
