package com.antutu.ABenchMark

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast

/**
 * Minimal launcher matching the inspected app's flow:
 * permission checks -> foreground service -> delayed activity close.
 *
 * The visible activity is intentionally minimal; the original APK also
 * uses a transparent/immersive launcher surface rather than a conventional UI.
 */
class MainActivity : Activity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var closeScheduled = false

    companion object {
        // Requested behaviour: 5 seconds instead of the observed short close.
        const val AUTO_CLOSE_DELAY_MS = 5_000L
        // Replaces the original 500 ms startup callback with 10 seconds.
        const val SERVICE_START_DELAY_MS = 10_000L
        private const val REQUEST_OVERLAY = 4101
        private const val REQUEST_BATTERY = 4102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.decorView.alpha = 0f
        window.decorView.post { window.decorView.alpha = 0f }
        enableImmersiveMode()
        checkPermissionsAndStart()
    }

    override fun onResume() {
        super.onResume()
        enableImmersiveMode()
        if (!closeScheduled) checkPermissionsAndStart()
    }

    private fun enableImmersiveMode() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    private fun checkPermissionsAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Please grant 'Display over other apps' permission.",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_OVERLAY)
            return
        }

        val power = getSystemService(POWER_SERVICE) as PowerManager
        if (!power.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
                @Suppress("DEPRECATION")
                startActivityForResult(intent, REQUEST_BATTERY)
                return
            } catch (_: Exception) {
                // Some Android builds do not expose the direct exemption screen.
            }
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                4103
            )
            return
        }

        // Delayed service startup: original callback was 500 ms; requested value is 10,000 ms.
        mainHandler.removeCallbacks(startServiceRunnable)
        mainHandler.postDelayed(startServiceRunnable, SERVICE_START_DELAY_MS)
    }

    private val startServiceRunnable = Runnable {
        startOverlayService()
    }

    private fun startOverlayService() {
        if (closeScheduled) return
        closeScheduled = true

        val startIntent = Intent(this, PerformanceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(startIntent)
        } else {
            startService(startIntent)
        }

        Toast.makeText(
            this,
            "Monster Mode active! Open BGMI now.",
            Toast.LENGTH_LONG
        ).show()

        // Close only the launcher activity after the requested 5-second window.
        // The foreground service remains alive independently.
        mainHandler.postDelayed({
            if (!isFinishing) finish()
        }, AUTO_CLOSE_DELAY_MS)
    }

    @Deprecated("Deprecated by Android; retained for compatibility with the permission flow.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY || requestCode == REQUEST_BATTERY) {
            mainHandler.post { checkPermissionsAndStart() }
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
