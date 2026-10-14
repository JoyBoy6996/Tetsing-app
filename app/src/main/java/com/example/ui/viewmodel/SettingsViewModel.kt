package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HyperPulseApp
import com.example.service.RoutineCleanJobService
import com.example.service.SystemMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HyperPulseApp
    private val prefs = app.preferencesManager

    val isDarkTheme: StateFlow<Boolean> = prefs.isDarkTheme
    val isAmoled: StateFlow<Boolean> = prefs.isAmoled
    val persistentNotification: StateFlow<Boolean> = prefs.persistentNotificationEnabled
    val floatingOverlay: StateFlow<Boolean> = prefs.floatingOverlayEnabled
    val autoBoost: StateFlow<Boolean> = prefs.autoBoostEnabled
    val autoBoostThreshold: StateFlow<Int> = prefs.autoBoostThresholdRam
    val thermalAlertThreshold: StateFlow<Int> = prefs.thermalAlertThresholdC

    val routineCleanEnabled: StateFlow<Boolean> = prefs.routineCleanEnabled
    val routineCleanIdleOnly: StateFlow<Boolean> = prefs.routineCleanIdleOnly
    val routineCleanChargingOnly: StateFlow<Boolean> = prefs.routineCleanChargingOnly
    val routineCleanIntervalDays: StateFlow<Int> = prefs.routineCleanIntervalDays
    val lastRoutineCleanSummary: StateFlow<String> = prefs.lastRoutineCleanSummary

    fun toggleDarkTheme(enable: Boolean) {
        prefs.setDarkTheme(enable)
    }

    fun toggleAmoled(enable: Boolean) {
        prefs.setAmoled(enable)
    }

    fun togglePersistentNotification(enable: Boolean) {
        prefs.setPersistentNotification(enable)
        if (enable) {
            SystemMonitorService.start(app)
        } else {
            SystemMonitorService.stop(app)
        }
    }

    fun toggleAutoBoost(enable: Boolean) {
        prefs.setAutoBoost(enable)
    }

    fun setAutoBoostThreshold(ramPct: Int) {
        prefs.setAutoBoostThreshold(ramPct)
    }

    fun setThermalAlertThreshold(tempC: Int) {
        prefs.setThermalAlertThreshold(tempC)
    }

    fun toggleRoutineClean(enable: Boolean) {
        prefs.setRoutineCleanEnabled(enable)
        if (enable) {
            RoutineCleanJobService.schedule(app)
        } else {
            RoutineCleanJobService.cancel(app)
        }
    }

    fun toggleRoutineCleanIdle(enable: Boolean) {
        prefs.setRoutineCleanIdleOnly(enable)
        if (routineCleanEnabled.value) {
            RoutineCleanJobService.schedule(app)
        }
    }

    fun toggleRoutineCleanCharging(enable: Boolean) {
        prefs.setRoutineCleanChargingOnly(enable)
        if (routineCleanEnabled.value) {
            RoutineCleanJobService.schedule(app)
        }
    }

    fun setRoutineCleanIntervalDays(days: Int) {
        prefs.setRoutineCleanIntervalDays(days)
        if (routineCleanEnabled.value) {
            RoutineCleanJobService.schedule(app)
        }
    }
}
