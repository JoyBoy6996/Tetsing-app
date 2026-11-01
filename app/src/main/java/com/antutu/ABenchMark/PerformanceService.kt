package com.antutu.ABenchMark

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * Foreground overlay service modeled on the inspected APK's service lifecycle.
 * This starter replica uses a transparent 1x1 hardware-accelerated overlay
 * because the supplied APK's overlay view is a plain View, not a visible HUD.
 */
class PerformanceService : Service() {

    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var running = false

    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            overlayView?.invalidate()
            heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
        }
    }

    companion object {
        private const val CHANNEL_ID = "performance_overlay_service_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.antutu.ABenchMark.ACTION_STOP"
        private const val HEARTBEAT_INTERVAL_MS = 3_000L

        fun stop(context: Context) {
            context.startService(
                Intent(context, PerformanceService::class.java).setAction(ACTION_STOP)
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundCompat()
        attachOverlay()
        running = true
        startHeartbeatPulse()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForegroundService()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Performance Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the performance overlay service running"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, PerformanceService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle("Performance overlay active")
                .setContentText("Service is running")
                .setContentIntent(openIntent)
                .setOngoing(true)
                .addAction(
                    Notification.Action.Builder(
                        null, "Stop", stopIntent
                    ).build()
                )
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle("Performance overlay active")
                .setContentText("Service is running")
                .setContentIntent(openIntent)
                .setOngoing(true)
                .build()
        }
    }

    private fun immutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0

    private fun attachOverlay() {
        if (!android.provider.Settings.canDrawOverlays(this)) return
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val view = View(this).apply {
                alpha = 0f
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                1,
                1,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }

            windowManager?.addView(view, params)
            overlayView = view
        } catch (_: Exception) {
            // Overlay attachment can fail if permission is revoked or OEM policy blocks it.
        }
    }

    private fun startHeartbeatPulse() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS)
    }

    private fun detachOverlay() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        val view = overlayView ?: return
        try {
            windowManager?.removeView(view)
        } catch (_: Exception) {
        } finally {
            overlayView = null
        }
    }

    private fun stopForegroundService() {
        running = false
        detachOverlay()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        running = false
        detachOverlay()
        heartbeatHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
