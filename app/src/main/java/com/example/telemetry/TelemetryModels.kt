package com.example.telemetry

enum class HealthCategory {
    OPTIMAL,
    GOOD,
    ATTENTION,
    CRITICAL
}

data class SystemTelemetry(
    val cpuPercent: Float = 0f,
    val cpuFrequenciesMhz: List<Int> = emptyList(),
    val cpuGovernor: String = "schedutil",
    val cpuCores: Int = Runtime.getRuntime().availableProcessors(),
    val ramTotalMb: Long = 0L,
    val ramUsedMb: Long = 0L,
    val ramFreeMb: Long = 0L,
    val ramPercent: Float = 0f,
    val storageTotalGb: Float = 0f,
    val storageUsedGb: Float = 0f,
    val storageFreeGb: Float = 0f,
    val storagePercent: Float = 0f,
    val batteryPercent: Int = 100,
    val batteryTempC: Float = 28f,
    val batteryVoltageMv: Int = 4000,
    val batteryCurrentMa: Int = 0,
    val batteryHealth: String = "Good",
    val batteryStatus: String = "Discharging",
    val networkUpKbps: Float = 0f,
    val networkDownKbps: Float = 0f,
    val thermalHeadroom: Float = 1.0f,
    val thermalStatus: String = "Normal",
    val healthScore: Int = 95,
    val healthCategory: HealthCategory = HealthCategory.OPTIMAL,
    val fps: Float = 60f
)
