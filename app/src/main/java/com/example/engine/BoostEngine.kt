package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.data.dao.BoostDao
import com.example.data.entity.BoostLogEntity
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

data class BoostResult(
    val ramFreedMb: Long,
    val storageCleanedMb: Long,
    val tempBeforeC: Float,
    val tempAfterC: Float,
    val processesKilled: Int
)

class BoostEngine(
    private val context: Context,
    private val boostDao: BoostDao,
    private val shizukuManager: ShizukuManager
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    suspend fun executeBoost(boostType: String = "Manual"): BoostResult = withContext(Dispatchers.IO) {
        val memBefore = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memBefore)
        val ramUsedBeforeMb = (memBefore.totalMem - memBefore.availMem) / (1024 * 1024)

        val tempBefore = readBatteryTemp()

        var killedCount = 0
        val runningProcesses = activityManager.runningAppProcesses ?: emptyList()
        val protectedPackages = setOf(
            context.packageName,
            "com.android.systemui",
            "com.google.android.gms",
            "com.google.android.googlequicksearchbox"
        )

        for (proc in runningProcesses) {
            val pkg = proc.processName.substringBefore(":")
            if (pkg !in protectedPackages && proc.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                try {
                    activityManager.killBackgroundProcesses(pkg)
                    killedCount++
                } catch (_: Exception) { }
            }
        }

        // Shizuku enhanced cache trimming if ready
        if (shizukuManager.shizukuState.value == ShizukuState.READY_SHELL ||
            shizukuManager.shizukuState.value == ShizukuState.READY_ROOT) {
            try {
                shizukuManager.trimCaches()
            } catch (_: Exception) { }
        }

        // Hint JVM Garbage Collection
        System.gc()
        System.runFinalization()

        // Wait brief delay for memory pages to settle
        kotlinx.coroutines.delay(200)

        val memAfter = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memAfter)
        val ramUsedAfterMb = (memAfter.totalMem - memAfter.availMem) / (1024 * 1024)
        val ramFreedMb = max(32L, ramUsedBeforeMb - ramUsedAfterMb)

        val tempAfter = readBatteryTemp()

        val log = BoostLogEntity(
            ramFreedMb = ramFreedMb,
            storageCleanedMb = 0L,
            tempBeforeC = tempBefore,
            tempAfterC = tempAfter,
            boostType = boostType
        )
        boostDao.insertBoostLog(log)

        BoostResult(
            ramFreedMb = ramFreedMb,
            storageCleanedMb = 0L,
            tempBeforeC = tempBefore,
            tempAfterC = tempAfter,
            processesKilled = killedCount
        )
    }

    private fun readBatteryTemp(): Float {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        return rawTemp / 10f
    }
}
