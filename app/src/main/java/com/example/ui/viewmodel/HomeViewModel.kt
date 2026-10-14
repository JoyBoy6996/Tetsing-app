package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HyperPulseApp
import com.example.engine.BoostResult
import com.example.telemetry.SystemTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HyperPulseApp
    private val telemetryReader = app.telemetryReader
    private val boostEngine = app.boostEngine

    private val _telemetry = MutableStateFlow(SystemTelemetry())
    val telemetry: StateFlow<SystemTelemetry> = _telemetry.asStateFlow()

    private val _cpuHistory = MutableStateFlow<List<Float>>(listOf(15f, 20f, 18f, 25f, 22f, 19f))
    val cpuHistory: StateFlow<List<Float>> = _cpuHistory.asStateFlow()

    private val _ramHistory = MutableStateFlow<List<Float>>(listOf(55f, 56f, 55f, 57f, 58f, 57f))
    val ramHistory: StateFlow<List<Float>> = _ramHistory.asStateFlow()

    private val _isBoosting = MutableStateFlow(false)
    val isBoosting: StateFlow<Boolean> = _isBoosting.asStateFlow()

    private val _lastBoostResult = MutableStateFlow<BoostResult?>(null)
    val lastBoostResult: StateFlow<BoostResult?> = _lastBoostResult.asStateFlow()

    init {
        startTelemetryLoop()
    }

    private fun startTelemetryLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val t = telemetryReader.readCurrentTelemetry()
                _telemetry.value = t

                val currentCpuList = _cpuHistory.value.toMutableList()
                if (currentCpuList.size >= 25) currentCpuList.removeAt(0)
                currentCpuList.add(t.cpuPercent)
                _cpuHistory.value = currentCpuList

                val currentRamList = _ramHistory.value.toMutableList()
                if (currentRamList.size >= 25) currentRamList.removeAt(0)
                currentRamList.add(t.ramPercent)
                _ramHistory.value = currentRamList

                delay(2000)
            }
        }
    }

    fun triggerOneTapBoost() {
        if (_isBoosting.value) return
        viewModelScope.launch {
            _isBoosting.value = true
            val result = boostEngine.executeBoost("Home Dashboard")
            _lastBoostResult.value = result
            delay(800) // Brief animation dwell
            _isBoosting.value = false

            // Update telemetry immediately
            _telemetry.value = telemetryReader.readCurrentTelemetry()
        }
    }

    fun dismissBoostResult() {
        _lastBoostResult.value = null
    }
}
