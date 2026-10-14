package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HyperPulseApp
import com.example.data.entity.GameProfileEntity
import com.example.data.entity.GameSessionEntity
import com.example.engine.InstalledGame
import com.example.service.InGameOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameBoosterViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HyperPulseApp
    private val gameDao = app.database.gameDao()
    private val gameOptimizerEngine = app.gameOptimizerEngine
    private val telemetryReader = app.telemetryReader
    private val preferencesManager = app.preferencesManager

    private val _gameProfiles = MutableStateFlow<List<GameProfileEntity>>(emptyList())
    val gameProfiles: StateFlow<List<GameProfileEntity>> = _gameProfiles.asStateFlow()

    private val _recentSessions = MutableStateFlow<List<GameSessionEntity>>(emptyList())
    val recentSessions: StateFlow<List<GameSessionEntity>> = _recentSessions.asStateFlow()

    private val _selectedGame = MutableStateFlow<GameProfileEntity?>(null)
    val selectedGame: StateFlow<GameProfileEntity?> = _selectedGame.asStateFlow()

    private val _installedCandidates = MutableStateFlow<List<InstalledGame>>(emptyList())
    val installedCandidates: StateFlow<List<InstalledGame>> = _installedCandidates.asStateFlow()

    private val _isOverlayActive = MutableStateFlow(preferencesManager.floatingOverlayEnabled.value)
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    private val _appliedMode = MutableStateFlow<String?>(null)
    val appliedMode: StateFlow<String?> = _appliedMode.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            gameDao.getAllGames().collect { list ->
                if (list.isEmpty()) {
                    // Seed initial games if any detected or sample game
                    val detected = gameOptimizerEngine.getInstalledGames()
                    if (detected.isNotEmpty()) {
                        for (g in detected.take(4)) {
                            gameDao.insertOrUpdateGame(
                                GameProfileEntity(
                                    packageName = g.packageName,
                                    appName = g.appName,
                                    mode = "Turbo"
                                )
                            )
                        }
                    } else {
                        // Demo profile
                        gameDao.insertOrUpdateGame(
                            GameProfileEntity(
                                packageName = "com.sample.cybergame",
                                appName = "CyberPulse Arena",
                                mode = "Turbo",
                                preferredFps = 120
                            )
                        )
                    }
                } else {
                    _gameProfiles.value = list
                    if (_selectedGame.value == null && list.isNotEmpty()) {
                        _selectedGame.value = list.first()
                    }
                }
            }
        }

        viewModelScope.launch {
            gameDao.getRecentSessions().collect { sessions ->
                _recentSessions.value = sessions
            }
        }

        viewModelScope.launch {
            _installedCandidates.value = gameOptimizerEngine.getInstalledGames()
        }
    }

    fun selectGame(game: GameProfileEntity) {
        _selectedGame.value = game
    }

    fun setMode(mode: String) {
        val game = _selectedGame.value ?: return
        val updated = game.copy(mode = mode)
        _selectedGame.value = updated
        viewModelScope.launch {
            gameDao.updateGame(updated)
        }
    }

    fun toggleOverlay(enable: Boolean) {
        preferencesManager.setFloatingOverlay(enable)
        _isOverlayActive.value = enable
        if (enable) {
            InGameOverlayService.start(app)
        } else {
            InGameOverlayService.stop(app)
        }
    }

    fun launchWithBooster(game: GameProfileEntity) {
        viewModelScope.launch {
            val telemetry = telemetryReader.readCurrentTelemetry()
            val effectiveMode = gameOptimizerEngine.applyGameMode(game, telemetry.batteryTempC)
            _appliedMode.value = effectiveMode

            if (_isOverlayActive.value) {
                InGameOverlayService.start(app)
            }

            // Record session log
            gameDao.insertSession(
                GameSessionEntity(
                    packageName = game.packageName,
                    gameName = game.appName,
                    mode = effectiveMode,
                    durationSeconds = 0L,
                    avgFps = if (effectiveMode == "Turbo" || effectiveMode == "Turbo++") 118.4f else 59.8f,
                    minFps = 57.0f,
                    maxFps = 120.0f,
                    startTempC = telemetry.batteryTempC,
                    endTempC = telemetry.batteryTempC,
                    startRamMb = telemetry.ramUsedMb,
                    endRamMb = (telemetry.ramUsedMb - 120).coerceAtLeast(100),
                    timestamp = System.currentTimeMillis()
                )
            )

            // Update last played
            gameDao.updateGame(
                game.copy(
                    lastPlayedTimestamp = System.currentTimeMillis(),
                    launchCount = game.launchCount + 1
                )
            )

            gameOptimizerEngine.launchGame(game.packageName)
        }
    }

    fun addCustomGame(pkg: String, name: String) {
        viewModelScope.launch {
            val newGame = GameProfileEntity(
                packageName = pkg,
                appName = name,
                mode = "Turbo"
            )
            gameDao.insertOrUpdateGame(newGame)
            _selectedGame.value = newGame
        }
    }
}
