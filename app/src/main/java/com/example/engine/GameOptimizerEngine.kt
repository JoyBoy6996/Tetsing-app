package com.example.engine

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.data.dao.GameDao
import com.example.data.entity.GameProfileEntity
import com.example.data.entity.GameSessionEntity
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledGame(
    val packageName: String,
    val appName: String,
    val isSystem: Boolean
)

class GameOptimizerEngine(
    private val context: Context,
    private val gameDao: GameDao,
    private val boostEngine: BoostEngine,
    private val shizukuManager: ShizukuManager
) {
    private val tag = "GameOptimizerEngine"
    private var wifiLock: WifiManager.WifiLock? = null

    suspend fun getInstalledGames(): List<InstalledGame> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val games = mutableListOf<InstalledGame>()

        for (app in packages) {
            val isGame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                app.category == ApplicationInfo.CATEGORY_GAME || (app.flags and ApplicationInfo.FLAG_IS_GAME) != 0
            } else {
                (app.flags and ApplicationInfo.FLAG_IS_GAME) != 0
            }

            if (isGame || isCommonGame(app.packageName)) {
                val label = pm.getApplicationLabel(app).toString()
                games.add(
                    InstalledGame(
                        packageName = app.packageName,
                        appName = label,
                        isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                )
            }
        }
        games
    }

    private fun isCommonGame(pkg: String): Boolean {
        val p = pkg.lowercase()
        return p.contains("game") || p.contains("pubg") || p.contains("genshin") ||
                p.contains("asphalt") || p.contains("codm") || p.contains("roblox") ||
                p.contains("minecraft") || p.contains("unity") || p.contains("supercell")
    }

    suspend fun applyGameMode(game: GameProfileEntity, currentTempC: Float): String {
        var activeMode = game.mode

        // Thermal safety fallback: if device is severely hot (>45°C) and mode is Turbo++, auto-fallback to Turbo/Balanced
        if (activeMode == "Turbo++" && currentTempC > 45f) {
            activeMode = "Turbo"
            Log.w(tag, "Thermal limit reached (45°C). Automatically downgrading mode to Turbo.")
        }

        when (activeMode) {
            "Balanced" -> {
                releaseLocks()
            }
            "Performance" -> {
                boostEngine.executeBoost("GameStart: Performance")
            }
            "Turbo" -> {
                boostEngine.executeBoost("GameStart: Turbo")
                acquireWifiHighPerfLock()
            }
            "Turbo++" -> {
                boostEngine.executeBoost("GameStart: Turbo++")
                acquireWifiHighPerfLock()
                applyShizukuTurboTweaks(game.packageName)
            }
            "Battery Gaming" -> {
                releaseLocks()
            }
            "Custom" -> {
                if (game.wifiHighPerf) acquireWifiHighPerfLock()
                boostEngine.executeBoost("GameStart: Custom")
            }
        }
        return activeMode
    }

    private suspend fun applyShizukuTurboTweaks(packageName: String) {
        val state = shizukuManager.shizukuState.value
        if (state == ShizukuState.READY_SHELL || state == ShizukuState.READY_ROOT) {
            try {
                // Set Android Game Mode Performance for game
                shizukuManager.setGameMode(packageName, "performance")
                // Trim caches across system
                shizukuManager.trimCaches()
            } catch (e: Exception) {
                Log.w(tag, "Shizuku tweaks failed: ${e.message}")
            }
        }
    }

    private fun acquireWifiHighPerfLock() {
        try {
            if (wifiLock == null) {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                wifiLock = wm?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "HyperPulse:GameWifiLock")
            }
            wifiLock?.acquire()
        } catch (e: Exception) {
            Log.w(tag, "Wifi lock acquire error: ${e.message}")
        }
    }

    fun releaseLocks() {
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (_: Exception) { }
    }

    suspend fun recordGameSession(session: GameSessionEntity) {
        gameDao.insertSession(session)
    }

    fun launchGame(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Could not launch game $packageName", e)
            false
        }
    }
}
