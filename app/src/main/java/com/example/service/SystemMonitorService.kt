package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.HyperPulseDatabase
import com.example.data.PreferencesManager
import com.example.engine.BoostEngine
import com.example.shizuku.ShizukuManager
import com.example.telemetry.HardwareTelemetryReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class SystemMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var monitorJob: Job? = null

    private lateinit var telemetryReader: HardwareTelemetryReader
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var boostEngine: BoostEngine
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "hyperpulse_monitor_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_QUICK_BOOST = "com.example.ACTION_QUICK_BOOST"

        fun start(context: Context) {
            val intent = Intent(context, SystemMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SystemMonitorService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        telemetryReader = HardwareTelemetryReader(this)
        preferencesManager = PreferencesManager(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val db = HyperPulseDatabase.getDatabase(this)
        val shizukuManager = ShizukuManager(this)
        boostEngine = BoostEngine(this, db.boostDao(), shizukuManager)

        createNotificationChannel()
        try {
            val notif = buildNotification("Initializing HyperPulse telemetry…")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notif, 0)
            } else {
                startForeground(NOTIFICATION_ID, notif)
            }
        } catch (e: Exception) {
            android.util.Log.w("SystemMonitorService", "Failed to startForeground: ${e.message}")
        }
        startMonitoringLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_QUICK_BOOST) {
            serviceScope.launch {
                val res = boostEngine.executeBoost("Persistent Notification")
                val notif = buildNotification("⚡ Boosted! Cleaned ${res.ramFreedMb}MB RAM (${res.tempAfterC}°C)")
                notificationManager.notify(NOTIFICATION_ID, notif)
            }
        }
        return START_STICKY
    }

    private fun startMonitoringLoop() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                if (preferencesManager.persistentNotificationEnabled.value) {
                    val telemetry = telemetryReader.readCurrentTelemetry()
                    val text = "CPU: ${telemetry.cpuPercent.roundToInt()}% | RAM: ${telemetry.ramPercent.roundToInt()}% (${telemetry.ramUsedMb}MB) | Temp: ${telemetry.batteryTempC}°C"
                    val notif = buildNotification(text)
                    try {
                        notificationManager.notify(NOTIFICATION_ID, notif)
                    } catch (_: Exception) { }
                }
                delay(3000)
            }
        }
    }

    private fun buildNotification(content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val boostIntent = Intent(this, SystemMonitorService::class.java).apply {
            action = ACTION_QUICK_BOOST
        }
        val boostPendingIntent = PendingIntent.getService(
            this,
            1,
            boostIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HyperPulse System Telemetry")
            .setContentText(content)
            .setSmallIcon(com.example.R.drawable.ic_stat_boost)
            .setContentIntent(openAppPendingIntent)
            .addAction(com.example.R.drawable.ic_stat_boost, "1-Tap Boost", boostPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "HyperPulse Real-Time Telemetry",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live CPU, RAM, and thermal metrics"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
