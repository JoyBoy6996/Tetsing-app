package com.example.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.data.HyperPulseDatabase
import com.example.engine.BoostEngine
import com.example.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class QuickBoostTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "1-Tap Boost"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = "Ready"
            }
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        tile.state = Tile.STATE_ACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "Boosting…"
        }
        tile.updateTile()

        triggerHaptic()

        scope.launch {
            val db = HyperPulseDatabase.getDatabase(applicationContext)
            val shizukuManager = ShizukuManager(applicationContext)
            val boostEngine = BoostEngine(applicationContext, db.boostDao(), shizukuManager)

            val res = boostEngine.executeBoost("Quick Settings Tile")

            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "+${res.ramFreedMb}MB Cleaned"
            }
            tile.updateTile()
        }
    }

    private fun triggerHaptic() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        } catch (_: Exception) { }
    }
}
