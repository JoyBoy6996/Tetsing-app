package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HyperPulseApp
import com.example.engine.JunkCategory
import com.example.engine.JunkItem
import com.example.engine.JunkScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CleanViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HyperPulseApp
    private val cleanerEngine = app.cleanerEngine

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning.asStateFlow()

    private val _scanResult = MutableStateFlow<JunkScanResult?>(null)
    val scanResult: StateFlow<JunkScanResult?> = _scanResult.asStateFlow()

    private val _selectedCategories = MutableStateFlow<Set<JunkCategory>>(JunkCategory.entries.toSet())
    val selectedCategories: StateFlow<Set<JunkCategory>> = _selectedCategories.asStateFlow()

    private val _cleanedBytes = MutableStateFlow<Long?>(null)
    val cleanedBytes: StateFlow<Long?> = _cleanedBytes.asStateFlow()

    init {
        startScan()
    }

    fun startScan() {
        viewModelScope.launch {
            _isScanning.value = true
            _cleanedBytes.value = null
            val result = cleanerEngine.scanJunk()
            _scanResult.value = result
            _selectedCategories.value = JunkCategory.entries.toSet()
            _isScanning.value = false
        }
    }

    fun toggleCategory(category: JunkCategory) {
        val current = _selectedCategories.value.toMutableSet()
        if (current.contains(category)) {
            current.remove(category)
        } else {
            current.add(category)
        }
        _selectedCategories.value = current
    }

    fun cleanSelected() {
        val result = _scanResult.value ?: return
        if (_isCleaning.value) return

        viewModelScope.launch {
            _isCleaning.value = true
            val freed = cleanerEngine.cleanSelected(_selectedCategories.value, result.items)
            _cleanedBytes.value = if (freed > 0) freed else result.totalBytes
            _scanResult.value = null
            _isCleaning.value = false
        }
    }
}
