package com.privacymonitor.app

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRequiredPermissions()

        setContent {
            PrivacyMonitorTheme {
                MainScreen(
                    onStartService = { startMonitorService() },
                    onStopService = { stopMonitorService() },
                    onOpenBatterySettings = { openBatterySettings() }
                )
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissions.launch(permissions.toTypedArray())
    }

    private fun startMonitorService() {
        val intent = Intent(this, MonitorService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopMonitorService() {
        val intent = Intent(this, MonitorService::class.java)
        stopService(intent)
    }

    private fun openBatterySettings() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            startActivity(intent)
        }
    }
}

@Composable
fun MainScreen(
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    var isMonitoring by remember { mutableStateOf(false) }
    var micActive by remember { mutableStateOf(false) }
    var cameraActive by remember { mutableStateOf(false) }

    // Pulse animation for active indicators
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Listen to service broadcasts
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                micActive = intent?.getBooleanExtra("mic_active", false) ?: false
                cameraActive = intent?.getBooleanExtra("camera_active", false) ?: false
            }
        }
        val filter = android.content.IntentFilter("com.privacymonitor.STATUS_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "🔒",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Privacy Monitor",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Watching your mic & camera",
                fontSize = 14.sp,
                color = Color(0xFF8B949E)
            )
        }

        // Status Cards
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Microphone Card
            StatusCard(
                emoji = "🎤",
                title = "Microphone",
                isActive = micActive,
                isMonitoring = isMonitoring,
                pulseScale = if (micActive) pulseScale else 1f,
                activeColor = Color(0xFFFF453A),
                inactiveColor = Color(0xFF30D158)
            )

            // Camera Card
            StatusCard(
                emoji = "📷",
                title = "Camera",
                isActive = cameraActive,
                isMonitoring = isMonitoring,
                pulseScale = if (cameraActive) pulseScale else 1f,
                activeColor = Color(0xFFFF453A),
                inactiveColor = Color(0xFF30D158)
            )
        }

        // Controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Main toggle button
            Button(
                onClick = {
                    if (isMonitoring) {
                        onStopService()
                        isMonitoring = false
                        micActive = false
                        cameraActive = false
                    } else {
                        onStartService()
                        isMonitoring = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMonitoring) Color(0xFFFF453A) else Color(0xFF30D158)
                )
            ) {
                Text(
                    text = if (isMonitoring) "⏹  Stop Monitoring" else "▶  Start Monitoring",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // Battery optimization button
            OutlinedButton(
                onClick = onOpenBatterySettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B949E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
            ) {
                Text(
                    text = "⚡ Disable Battery Optimization (Samsung)",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isMonitoring)
                    "✅ Running in background — you'll hear an alert if mic or camera activates"
                else
                    "Tap Start to begin monitoring in the background",
                fontSize = 12.sp,
                color = Color(0xFF8B949E),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatusCard(
    emoji: String,
    title: String,
    isActive: Boolean,
    isMonitoring: Boolean,
    pulseScale: Float,
    activeColor: Color,
    inactiveColor: Color
) {
    val cardColor = when {
        !isMonitoring -> Color(0xFF161B22)
        isActive -> Color(0xFF3D1A1A)
        else -> Color(0xFF1A2D1E)
    }

    val dotColor = when {
        !isMonitoring -> Color(0xFF484F58)
        isActive -> activeColor
        else -> inactiveColor
    }

    val statusText = when {
        !isMonitoring -> "Not monitoring"
        isActive -> "⚠️ ACTIVE — Alert sent!"
        else -> "Safe — Not in use"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = emoji, fontSize = 32.sp)
                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        color = if (isActive) Color(0xFFFF453A) else Color(0xFF8B949E)
                    )
                }
            }

            // Pulse dot
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .scale(if (isActive) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
fun PrivacyMonitorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF30D158),
            background = Color(0xFF0D1117),
            surface = Color(0xFF161B22)
        ),
        content = content
    )
}
