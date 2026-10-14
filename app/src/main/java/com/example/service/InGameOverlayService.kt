package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.data.HyperPulseDatabase
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

class InGameOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var telemetryJob: Job? = null
    private var isExpanded = false

    companion object {
        const val OVERLAY_CHANNEL_ID = "hyperpulse_overlay_channel"
        const val NOTIFICATION_ID = 2002

        fun start(context: Context) {
            val intent = Intent(context, InGameOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, InGameOverlayService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            val notif = buildForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notif, 0)
            } else {
                startForeground(NOTIFICATION_ID, notif)
            }
        } catch (e: Exception) {
            android.util.Log.w("InGameOverlayService", "Failed startForeground: ${e.message}")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        setupMinimalPillView()
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun setupMinimalPillView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 48
            y = 100
        }

        // Tiny, pill-shaped, semi-transparent container
        val pill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 16, 32, 16)
            background = GradientDrawable().apply {
                setColor(0xD91E1F23.toInt()) // Neutral dark 85% opacity
                cornerRadius = 100f // Pill shape
            }
        }

        // 3 max metrics: FPS • CPU% • Temp
        val statsTv = TextView(this).apply {
            text = "60 FPS  •  18%  •  31°C"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT
        }
        pill.addView(statsTv)

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
            setPadding(16, 0, 0, 0)
        }

        val db = HyperPulseDatabase.getDatabase(this)
        val shizukuManager = ShizukuManager(this)
        val boostEngine = BoostEngine(this, db.boostDao(), shizukuManager)

        val boostAction = TextView(this).apply {
            text = "  Boost"
            setTextColor(0xFF8AB4F8.toInt())
            textSize = 12f
            setOnClickListener {
                scope.launch {
                    val res = boostEngine.executeBoost("In-Game Overlay")
                    statsTv.text = "Freed ${res.ramFreedMb}MB"
                }
            }
        }
        actionRow.addView(boostAction)

        val closeAction = TextView(this).apply {
            text = "  ✕"
            setTextColor(Color.GRAY)
            textSize = 12f
            setOnClickListener { stopSelf() }
        }
        actionRow.addView(closeAction)

        pill.addView(actionRow)

        // Expand/collapse on tap
        var isDragging = false
        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f

        pill.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    initialX = params.x
                    initialY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - touchStartX).toInt()
                    val deltaY = (event.rawY - touchStartY).toInt()
                    if (deltaX * deltaX + deltaY * deltaY > 25) {
                        isDragging = true
                        params.x = initialX + deltaX
                        params.y = initialY + deltaY
                        windowManager?.updateViewLayout(pill, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        isExpanded = !isExpanded
                        actionRow.visibility = if (isExpanded) View.VISIBLE else View.GONE
                        windowManager?.updateViewLayout(pill, params)
                    }
                    true
                }
                else -> false
            }
        }

        overlayView = pill
        windowManager?.addView(pill, params)

        val telemetryReader = HardwareTelemetryReader(this)
        telemetryJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val t = telemetryReader.readCurrentTelemetry()
                val text = "60 FPS  •  ${t.cpuPercent.roundToInt()}%  •  ${t.batteryTempC.roundToInt()}°C"
                scope.launch(Dispatchers.Main) {
                    if (!isExpanded) {
                        statsTv.text = text
                    }
                }
                delay(2000)
            }
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, OVERLAY_CHANNEL_ID)
            .setContentTitle("HyperPulse HUD")
            .setContentText("Telemetry pill active")
            .setSmallIcon(com.example.R.drawable.ic_stat_boost)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                OVERLAY_CHANNEL_ID,
                "HyperPulse Overlay",
                NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        telemetryJob?.cancel()
        overlayView?.let { windowManager?.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
