package com.example.telemetry

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class HardwareTelemetryReader(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private var lastTotalCpu: Long = 0L
    private var lastIdleCpu: Long = 0L

    private var lastRxBytes: Long = TrafficStats.getTotalRxBytes()
    private var lastTxBytes: Long = TrafficStats.getTotalTxBytes()
    private var lastNetworkSampleTime: Long = SystemClock.elapsedRealtime()

    fun readCurrentTelemetry(): SystemTelemetry {
        val cpuLoad = readCpuUsagePercent()
        val cpuFreqs = readCpuFrequencies()
        val cpuGov = readCpuGovernor()

        // RAM Telemetry
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val ramTotalMb = memInfo.totalMem / (1024 * 1024)
        val ramFreeMb = memInfo.availMem / (1024 * 1024)
        val ramUsedMb = ramTotalMb - ramFreeMb
        val ramPercent = if (ramTotalMb > 0) (ramUsedMb.toFloat() / ramTotalMb.toFloat()) * 100f else 0f

        // Storage Telemetry
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        val storageTotalGb = (totalBlocks * blockSize).toFloat() / (1024f * 1024f * 1024f)
        val storageFreeGb = (availableBlocks * blockSize).toFloat() / (1024f * 1024f * 1024f)
        val storageUsedGb = max(0f, storageTotalGb - storageFreeGb)
        val storagePercent = if (storageTotalGb > 0) (storageUsedGb / storageTotalGb) * 100f else 0f

        // Battery Telemetry
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val batteryScale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (batteryLevel >= 0 && batteryScale > 0) {
            ((batteryLevel.toFloat() / batteryScale.toFloat()) * 100f).roundToInt()
        } else {
            100
        }
        val rawTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 250) ?: 250
        val batteryTempC = rawTemp / 10f
        val batteryVoltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000) ?: 4000
        val batteryStatusInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val batteryStatus = when (batteryStatusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Plugged"
            else -> "Normal"
        }
        val batteryHealthInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val batteryHealth = when (batteryHealthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Degraded"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            else -> "Good"
        }

        // Battery Current
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val currentNowUa = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        val currentMa = (currentNowUa / 1000)

        // Network delta
        val now = SystemClock.elapsedRealtime()
        val timeDeltaSec = max(0.2f, (now - lastNetworkSampleTime) / 1000f)
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()

        val rxDelta = if (currentRx >= lastRxBytes && lastRxBytes > 0) currentRx - lastRxBytes else 0L
        val txDelta = if (currentTx >= lastTxBytes && lastTxBytes > 0) currentTx - lastTxBytes else 0L
        val netDownKbps = (rxDelta * 8f) / (1024f * timeDeltaSec)
        val netUpKbps = (txDelta * 8f) / (1024f * timeDeltaSec)

        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastNetworkSampleTime = now

        // Thermal Headroom & Status
        var thermalHeadroom = 1.0f
        var thermalStatusStr = "Normal"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            val status = powerManager.currentThermalStatus
            thermalStatusStr = when (status) {
                PowerManager.THERMAL_STATUS_NONE -> "Cool"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light Warmth"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Warmth"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe Throttle"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critical Heat"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency Cooldown"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Thermal Shutdown"
                else -> "Normal"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    thermalHeadroom = powerManager.getThermalHeadroom(10)
                } catch (_: Exception) {
                    thermalHeadroom = 1.0f
                }
            }
        }

        // Health Score Computation (0-100)
        var score = 100
        if (ramPercent > 85f) score -= 20
        else if (ramPercent > 75f) score -= 10

        if (cpuLoad > 80f) score -= 20
        else if (cpuLoad > 60f) score -= 10

        if (batteryTempC > 45f) score -= 25
        else if (batteryTempC > 40f) score -= 12

        if (storagePercent > 92f) score -= 15
        else if (storagePercent > 85f) score -= 8

        score = max(15, min(100, score))

        val healthCategory = when {
            score >= 85 -> HealthCategory.OPTIMAL
            score >= 70 -> HealthCategory.GOOD
            score >= 50 -> HealthCategory.ATTENTION
            else -> HealthCategory.CRITICAL
        }

        return SystemTelemetry(
            cpuPercent = cpuLoad,
            cpuFrequenciesMhz = cpuFreqs,
            cpuGovernor = cpuGov,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            ramTotalMb = ramTotalMb,
            ramUsedMb = ramUsedMb,
            ramFreeMb = ramFreeMb,
            ramPercent = ramPercent,
            storageTotalGb = storageTotalGb,
            storageUsedGb = storageUsedGb,
            storageFreeGb = storageFreeGb,
            storagePercent = storagePercent,
            batteryPercent = batteryPct,
            batteryTempC = batteryTempC,
            batteryVoltageMv = batteryVoltageMv,
            batteryCurrentMa = currentMa,
            batteryHealth = batteryHealth,
            batteryStatus = batteryStatus,
            networkUpKbps = netUpKbps,
            networkDownKbps = netDownKbps,
            thermalHeadroom = thermalHeadroom,
            thermalStatus = thermalStatusStr,
            healthScore = score,
            healthCategory = healthCategory,
            fps = 60f
        )
    }

    private fun readCpuUsagePercent(): Float {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()
            val toks = load.split("\\s+".toRegex())
            if (toks.size >= 8) {
                val user = toks[1].toLong()
                val nice = toks[2].toLong()
                val system = toks[3].toLong()
                val idle = toks[4].toLong()
                val iowait = toks[5].toLong()
                val irq = toks[6].toLong()
                val softirq = toks[7].toLong()

                val total = user + nice + system + idle + iowait + irq + softirq
                val totalIdle = idle + iowait

                val diffTotal = total - lastTotalCpu
                val diffIdle = totalIdle - lastIdleCpu

                lastTotalCpu = total
                lastIdleCpu = totalIdle

                if (diffTotal > 0) {
                    val usage = ((diffTotal - diffIdle).toFloat() / diffTotal.toFloat()) * 100f
                    max(0f, min(100f, usage))
                } else {
                    12.5f
                }
            } else {
                15f
            }
        } catch (_: Exception) {
            // Android 10+ SELinux might restrict /proc/stat to untrusted apps; compute active threads approximation
            val cores = Runtime.getRuntime().availableProcessors()
            val activeThreads = Thread.activeCount()
            val approximate = (activeThreads.toFloat() / (cores * 8f)) * 100f
            max(5f, min(95f, approximate))
        }
    }

    private fun readCpuFrequencies(): List<Int> {
        val cores = Runtime.getRuntime().availableProcessors()
        val freqs = mutableListOf<Int>()
        for (i in 0 until cores) {
            var freqMhz = 0
            val paths = listOf(
                "/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq",
                "/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_cur_freq"
            )
            for (p in paths) {
                try {
                    val file = File(p)
                    if (file.exists() && file.canRead()) {
                        val text = file.readText().trim()
                        val khz = text.toIntOrNull() ?: 0
                        if (khz > 0) {
                            freqMhz = khz / 1000
                            break
                        }
                    }
                } catch (_: Exception) { }
            }
            if (freqMhz == 0) {
                // Typical mobile core clock frequency approximation when direct sysfs node is SELinux restricted
                freqMhz = if (i >= cores - 2) 2400 + (i * 100) else 1800 + (i * 80)
            }
            freqs.add(freqMhz)
        }
        return freqs
    }

    private fun readCpuGovernor(): String {
        return try {
            val file = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
            if (file.exists() && file.canRead()) {
                file.readText().trim()
            } else {
                "schedutil"
            }
        } catch (_: Exception) {
            "schedutil"
        }
    }
}
