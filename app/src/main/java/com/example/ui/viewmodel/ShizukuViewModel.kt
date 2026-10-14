package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HyperPulseApp
import com.example.data.entity.ShizukuLogEntity
import com.example.shizuku.ShizukuState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShizukuViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HyperPulseApp
    private val shizukuManager = app.shizukuManager
    private val shizukuDao = app.database.shizukuDao()

    val shizukuState: StateFlow<ShizukuState> = shizukuManager.shizukuState

    private val _logs = MutableStateFlow<List<ShizukuLogEntity>>(emptyList())
    val logs: StateFlow<List<ShizukuLogEntity>> = _logs.asStateFlow()

    private val _lastOutput = MutableStateFlow<String?>(null)
    val lastOutput: StateFlow<String?> = _lastOutput.asStateFlow()

    init {
        viewModelScope.launch {
            shizukuDao.getAllLogs().collect { list ->
                _logs.value = list
            }
        }
        shizukuManager.checkState()
    }

    fun requestPermission() {
        shizukuManager.requestPermission()
    }

    fun refreshState() {
        shizukuManager.checkState()
    }

    fun executeTrimCaches() {
        viewModelScope.launch {
            val res = shizukuManager.trimCaches()
            _lastOutput.value = if (res.success) "Caches trimmed successfully via Shell!" else "Failed: ${res.output}"
            val priv = if (shizukuState.value == ShizukuState.READY_ROOT) "Root (UID 0)" else "Shell (ADB)"
            shizukuDao.insertLog(
                ShizukuLogEntity(
                    action = "pm trim-caches 4096M",
                    privilegeLevel = priv,
                    result = if (res.success) "Success" else "Error: ${res.output}"
                )
            )
        }
    }

    fun revertAllTweaks() {
        viewModelScope.launch {
            shizukuDao.markAllReverted()
            _lastOutput.value = "All tweaks reverted to system default."
        }
    }
}
