package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("hyperpulse_prefs", Context.MODE_PRIVATE)

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isAmoled = MutableStateFlow(prefs.getBoolean("is_amoled", false))
    val isAmoled: StateFlow<Boolean> = _isAmoled.asStateFlow()

    private val _persistentNotificationEnabled = MutableStateFlow(prefs.getBoolean("persistent_notif", false))
    val persistentNotificationEnabled: StateFlow<Boolean> = _persistentNotificationEnabled.asStateFlow()

    private val _floatingOverlayEnabled = MutableStateFlow(prefs.getBoolean("floating_overlay", false))
    val floatingOverlayEnabled: StateFlow<Boolean> = _floatingOverlayEnabled.asStateFlow()

    private val _autoBoostEnabled = MutableStateFlow(prefs.getBoolean("auto_boost", true))
    val autoBoostEnabled: StateFlow<Boolean> = _autoBoostEnabled.asStateFlow()

    private val _autoBoostThresholdRam = MutableStateFlow(prefs.getInt("auto_boost_ram_thresh", 85))
    val autoBoostThresholdRam: StateFlow<Int> = _autoBoostThresholdRam.asStateFlow()

    private val _thermalAlertThresholdC = MutableStateFlow(prefs.getInt("thermal_alert_thresh", 44))
    val thermalAlertThresholdC: StateFlow<Int> = _thermalAlertThresholdC.asStateFlow()

    private val _routineCleanEnabled = MutableStateFlow(prefs.getBoolean("routine_clean_enabled", true))
    val routineCleanEnabled: StateFlow<Boolean> = _routineCleanEnabled.asStateFlow()

    private val _routineCleanIdleOnly = MutableStateFlow(prefs.getBoolean("routine_clean_idle_only", true))
    val routineCleanIdleOnly: StateFlow<Boolean> = _routineCleanIdleOnly.asStateFlow()

    private val _routineCleanChargingOnly = MutableStateFlow(prefs.getBoolean("routine_clean_charging_only", true))
    val routineCleanChargingOnly: StateFlow<Boolean> = _routineCleanChargingOnly.asStateFlow()

    private val _routineCleanIntervalDays = MutableStateFlow(prefs.getInt("routine_clean_interval_days", 1))
    val routineCleanIntervalDays: StateFlow<Int> = _routineCleanIntervalDays.asStateFlow()

    private val _lastRoutineCleanSummary = MutableStateFlow(prefs.getString("last_routine_clean_summary", "Not run yet") ?: "Not run yet")
    val lastRoutineCleanSummary: StateFlow<String> = _lastRoutineCleanSummary.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean("is_dark_theme", enabled).apply()
        _isDarkTheme.value = enabled
    }

    fun setAmoled(enabled: Boolean) {
        prefs.edit().putBoolean("is_amoled", enabled).apply()
        _isAmoled.value = enabled
    }

    fun setPersistentNotification(enabled: Boolean) {
        prefs.edit().putBoolean("persistent_notif", enabled).apply()
        _persistentNotificationEnabled.value = enabled
    }

    fun setFloatingOverlay(enabled: Boolean) {
        prefs.edit().putBoolean("floating_overlay", enabled).apply()
        _floatingOverlayEnabled.value = enabled
    }

    fun setAutoBoost(enabled: Boolean) {
        prefs.edit().putBoolean("auto_boost", enabled).apply()
        _autoBoostEnabled.value = enabled
    }

    fun setAutoBoostThreshold(ramPct: Int) {
        prefs.edit().putInt("auto_boost_ram_thresh", ramPct).apply()
        _autoBoostThresholdRam.value = ramPct
    }

    fun setThermalAlertThreshold(tempC: Int) {
        prefs.edit().putInt("thermal_alert_thresh", tempC).apply()
        _thermalAlertThresholdC.value = tempC
    }

    fun setRoutineCleanEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("routine_clean_enabled", enabled).apply()
        _routineCleanEnabled.value = enabled
    }

    fun setRoutineCleanIdleOnly(enabled: Boolean) {
        prefs.edit().putBoolean("routine_clean_idle_only", enabled).apply()
        _routineCleanIdleOnly.value = enabled
    }

    fun setRoutineCleanChargingOnly(enabled: Boolean) {
        prefs.edit().putBoolean("routine_clean_charging_only", enabled).apply()
        _routineCleanChargingOnly.value = enabled
    }

    fun setRoutineCleanIntervalDays(days: Int) {
        prefs.edit().putInt("routine_clean_interval_days", days).apply()
        _routineCleanIntervalDays.value = days
    }

    fun setLastRoutineCleanSummary(summary: String) {
        prefs.edit().putString("last_routine_clean_summary", summary).apply()
        _lastRoutineCleanSummary.value = summary
    }
}
