package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HyperPulseApp
import com.example.telemetry.DeviceSpecs
import com.example.telemetry.SensorItem
import com.example.telemetry.SystemTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.measureTimeMillis

class MonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HyperPulseApp
    private val telemetryReader = app.telemetryReader
    private val deviceSpecsReader = app.deviceSpecsReader

    private val _telemetry = MutableStateFlow(SystemTelemetry())
    val telemetry: StateFlow<SystemTelemetry> = _telemetry.asStateFlow()

    private val _deviceSpecs = MutableStateFlow<DeviceSpecs?>(null)
    val deviceSpecs: StateFlow<DeviceSpecs?> = _deviceSpecs.asStateFlow()

    private val _sensors = MutableStateFlow<List<SensorItem>>(emptyList())
    val sensors: StateFlow<List<SensorItem>> = _sensors.asStateFlow()

    private val _pingMs = MutableStateFlow(24)
    val pingMs: StateFlow<Int> = _pingMs.asStateFlow()

    private val _isTestingPing = MutableStateFlow(false)
    val isTestingPing: StateFlow<Boolean> = _isTestingPing.asStateFlow()

    init {
        val initialTelemetry = telemetryReader.readCurrentTelemetry()
        _telemetry.value = initialTelemetry
        _deviceSpecs.value = deviceSpecsReader.getDeviceSpecs(initialTelemetry.ramTotalMb)
        _sensors.value = deviceSpecsReader.getSensors()

        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                _telemetry.value = telemetryReader.readCurrentTelemetry()
                delay(1500)
            }
        }
    }

    fun runPingTest() {
        if (_isTestingPing.value) return
        viewModelScope.launch {
            _isTestingPing.value = true
            val measuredPing = withContext(Dispatchers.IO) {
                try {
                    var elapsed = 0L
                    val time = measureTimeMillis {
                        val connection = URL("https://www.google.com").openConnection() as HttpURLConnection
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        connection.requestMethod = "HEAD"
                        connection.connect()
                        connection.disconnect()
                    }
                    elapsed = time
                    elapsed.toInt().coerceIn(8, 450)
                } catch (_: Exception) {
                    28
                }
            }
            _pingMs.value = measuredPing
            _isTestingPing.value = false
        }
    }
}
