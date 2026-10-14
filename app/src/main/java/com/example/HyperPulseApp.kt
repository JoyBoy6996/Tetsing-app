package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.HyperPulseDatabase
import com.example.data.PreferencesManager
import com.example.engine.BoostEngine
import com.example.engine.CleanerEngine
import com.example.engine.GameOptimizerEngine
import com.example.service.SystemMonitorService
import com.example.shizuku.ShizukuManager
import com.example.telemetry.DeviceSpecsReader
import com.example.telemetry.HardwareTelemetryReader

class HyperPulseApp : Application() {

    lateinit var database: HyperPulseDatabase
        private set
    lateinit var preferencesManager: PreferencesManager
        private set
    lateinit var telemetryReader: HardwareTelemetryReader
        private set
    lateinit var deviceSpecsReader: DeviceSpecsReader
        private set
    lateinit var shizukuManager: ShizukuManager
        private set
    lateinit var boostEngine: BoostEngine
        private set
    lateinit var cleanerEngine: CleanerEngine
        private set
    lateinit var gameOptimizerEngine: GameOptimizerEngine
        private set

    companion object {
        lateinit var instance: HyperPulseApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = HyperPulseDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)
        telemetryReader = HardwareTelemetryReader(this)
        deviceSpecsReader = DeviceSpecsReader(this)
        shizukuManager = ShizukuManager(this)
        shizukuManager.init()

        boostEngine = BoostEngine(this, database.boostDao(), shizukuManager)
        cleanerEngine = CleanerEngine(this, database.boostDao())
        gameOptimizerEngine = GameOptimizerEngine(this, database.gameDao(), boostEngine, shizukuManager)

        createNotificationChannels()

        // Initialize low-usage scheduled clean maintenance
        try {
            com.example.service.RoutineCleanJobService.schedule(this)
        } catch (_: Exception) { }
        try {
            com.example.service.RoutineCleanJobService.schedule(this)
        } catch (_: Exception) { }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val monitorChannel = NotificationChannel(
                SystemMonitorService.CHANNEL_ID,
                "HyperPulse Real-Time Telemetry",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live hardware monitoring and quick boost actions"
            }
            nm.createNotificationChannel(monitorChannel)
        }
    }
}
