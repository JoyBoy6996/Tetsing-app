package com.example.telemetry

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

data class DeviceSpecs(
    val deviceModel: String,
    val manufacturer: String,
    val board: String,
    val socModel: String,
    val cpuArchitecture: String,
    val cpuCores: Int,
    val totalRamMb: Long,
    val displayResolution: String,
    val refreshRateHz: Float,
    val displayDpi: Int,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val kernelVersion: String,
    val supportedAbis: List<String>
)

data class SensorItem(
    val name: String,
    val type: String,
    val vendor: String,
    val powerMa: Float,
    val resolution: Float,
    val maxRange: Float
)

class DeviceSpecsReader(private val context: Context) {

    fun getDeviceSpecs(ramTotalMb: Long): DeviceSpecs {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getRealMetrics(metrics)

        @Suppress("DEPRECATION")
        val refreshRate = windowManager?.defaultDisplay?.refreshRate ?: 60f

        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.ifBlank { Build.HARDWARE }
        } else {
            Build.HARDWARE
        }

        val kernel = System.getProperty("os.version") ?: "Linux"

        return DeviceSpecs(
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            board = Build.BOARD,
            socModel = soc,
            cpuArchitecture = System.getProperty("os.arch") ?: "aarch64",
            cpuCores = Runtime.getRuntime().availableProcessors(),
            totalRamMb = ramTotalMb,
            displayResolution = "${metrics.widthPixels} x ${metrics.heightPixels}",
            refreshRateHz = refreshRate,
            displayDpi = metrics.densityDpi,
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A",
            kernelVersion = kernel,
            supportedAbis = Build.SUPPORTED_ABIS.toList()
        )
    }

    fun getSensors(): List<SensorItem> {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensors = sensorManager?.getSensorList(Sensor.TYPE_ALL) ?: emptyList()
        return sensors.map {
            SensorItem(
                name = it.name,
                type = it.stringType.substringAfterLast("."),
                vendor = it.vendor,
                powerMa = it.power,
                resolution = it.resolution,
                maxRange = it.maximumRange
            )
        }
    }
}
