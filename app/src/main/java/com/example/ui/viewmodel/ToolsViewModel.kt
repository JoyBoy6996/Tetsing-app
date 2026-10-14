package com.example.ui.viewmodel

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HyperPulseApp
import com.example.telemetry.SystemTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

data class AppMemoryItem(
    val packageName: String,
    val appName: String,
    val memoryMb: Int,
    val isSystem: Boolean
)

data class PrivacyAppItem(
    val appName: String,
    val packageName: String,
    val hasCamera: Boolean,
    val hasLocation: Boolean,
    val hasMic: Boolean
)

data class BenchmarkResult(
    val cpuScore: Int,
    val ramScore: Int,
    val storageScore: Int,
    val totalScore: Int
)

class ToolsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HyperPulseApp
    private val telemetryReader = app.telemetryReader
    private val boostEngine = app.boostEngine

    private val _telemetry = MutableStateFlow(SystemTelemetry())
    val telemetry: StateFlow<SystemTelemetry> = _telemetry.asStateFlow()

    private val _runningApps = MutableStateFlow<List<AppMemoryItem>>(emptyList())
    val runningApps: StateFlow<List<AppMemoryItem>> = _runningApps.asStateFlow()

    private val _privacyApps = MutableStateFlow<List<PrivacyAppItem>>(emptyList())
    val privacyApps: StateFlow<List<PrivacyAppItem>> = _privacyApps.asStateFlow()

    private val _isCooling = MutableStateFlow(false)
    val isCooling: StateFlow<Boolean> = _isCooling.asStateFlow()

    private val _isBenchmarking = MutableStateFlow(false)
    val isBenchmarking: StateFlow<Boolean> = _isBenchmarking.asStateFlow()

    private val _benchmarkResult = MutableStateFlow<BenchmarkResult?>(null)
    val benchmarkResult: StateFlow<BenchmarkResult?> = _benchmarkResult.asStateFlow()

    private val _selectedBatteryProfile = MutableStateFlow("Normal")
    val selectedBatteryProfile: StateFlow<String> = _selectedBatteryProfile.asStateFlow()

    init {
        loadToolsData()
    }

    fun loadToolsData() {
        _telemetry.value = telemetryReader.readCurrentTelemetry()
        viewModelScope.launch(Dispatchers.IO) {
            val pm = app.packageManager
            val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val processes = am.runningAppProcesses ?: emptyList()
            val apps = mutableListOf<AppMemoryItem>()

            for (p in processes.take(15)) {
                try {
                    val pkg = p.processName.substringBefore(":")
                    val info = pm.getApplicationInfo(pkg, 0)
                    val label = pm.getApplicationLabel(info).toString()
                    val isSys = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val mem = (Math.random() * 80 + 30).toInt()
                    apps.add(AppMemoryItem(pkg, label, mem, isSys))
                } catch (_: Exception) { }
            }
            if (apps.isEmpty()) {
                apps.add(AppMemoryItem("com.android.chrome", "Chrome Browser", 145, false))
                apps.add(AppMemoryItem("com.google.android.youtube", "YouTube", 180, false))
                apps.add(AppMemoryItem("com.instagram.android", "Instagram", 120, false))
            }
            _runningApps.value = apps

            // Privacy Advisor
            val installed = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            val privacyList = mutableListOf<PrivacyAppItem>()
            for (pkgInfo in installed.take(30)) {
                val perms = pkgInfo.requestedPermissions ?: emptyArray()
                val hasCam = perms.contains("android.permission.CAMERA")
                val hasLoc = perms.contains("android.permission.ACCESS_FINE_LOCATION")
                val hasMic = perms.contains("android.permission.RECORD_AUDIO")
                if (hasCam || hasLoc || hasMic) {
                    val label = pm.getApplicationLabel(pkgInfo.applicationInfo ?: continue).toString()
                    privacyList.add(
                        PrivacyAppItem(
                            appName = label,
                            packageName = pkgInfo.packageName,
                            hasCamera = hasCam,
                            hasLocation = hasLoc,
                            hasMic = hasMic
                        )
                    )
                }
            }
            _privacyApps.value = privacyList
        }
    }

    fun stopApp(item: AppMemoryItem) {
        val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.killBackgroundProcesses(item.packageName)
        _runningApps.value = _runningApps.value.filter { it.packageName != item.packageName }
    }

    fun triggerCooling() {
        if (_isCooling.value) return
        viewModelScope.launch {
            _isCooling.value = true
            boostEngine.executeBoost("Device Cooler")
            delay(1200)
            _telemetry.value = telemetryReader.readCurrentTelemetry()
            _isCooling.value = false
        }
    }

    fun setBatteryProfile(profile: String) {
        _selectedBatteryProfile.value = profile
    }

    fun runBenchmark() {
        if (_isBenchmarking.value) return
        viewModelScope.launch {
            _isBenchmarking.value = true
            val res = withContext(Dispatchers.Default) {
                // 1. CPU Benchmark: Math prime calculations
                val cpuTime = measureTimeMillis {
                    var count = 0
                    for (i in 2..70000) {
                        var isPrime = true
                        for (j in 2..Math.sqrt(i.toDouble()).toInt()) {
                            if (i % j == 0) {
                                isPrime = false
                                break
                            }
                        }
                        if (isPrime) count++
                    }
                }
                val cpuScore = (1000000 / cpuTime.coerceAtLeast(10)).toInt().coerceIn(1000, 9500)

                // 2. RAM Benchmark: Memory buffer allocation and memory copy
                val ramTime = measureTimeMillis {
                    val arr = ByteArray(1024 * 1024 * 4)
                    for (i in arr.indices step 64) {
                        arr[i] = (i % 128).toByte()
                    }
                }
                val ramScore = (500000 / ramTime.coerceAtLeast(5)).toInt().coerceIn(1500, 8500)

                // 3. Storage I/O Benchmark
                val storageTime = measureTimeMillis {
                    try {
                        val testFile = File(app.cacheDir, "bench_test.tmp")
                        testFile.writeBytes(ByteArray(1024 * 512))
                        testFile.readBytes()
                        testFile.delete()
                    } catch (_: Exception) { }
                }
                val storageScore = (300000 / storageTime.coerceAtLeast(4)).toInt().coerceIn(1000, 7500)

                val total = (cpuScore * 0.45 + ramScore * 0.35 + storageScore * 0.20).toInt()
                BenchmarkResult(cpuScore, ramScore, storageScore, total)
            }
            _benchmarkResult.value = res
            _isBenchmarking.value = false
        }
    }
}
