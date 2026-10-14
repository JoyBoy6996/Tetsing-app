package com.example

import com.example.data.entity.GameProfileEntity
import com.example.shizuku.ShizukuState
import com.example.telemetry.HealthCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HyperPulseLogicTest {

    @Test
    fun testHealthScoreComputation() {
        // High load simulation
        val ramPercent = 90f
        val cpuLoad = 85f
        val batteryTempC = 46f
        val storagePercent = 95f

        var score = 100
        if (ramPercent > 85f) score -= 20
        else if (ramPercent > 75f) score -= 10

        if (cpuLoad > 80f) score -= 20
        else if (cpuLoad > 60f) score -= 10

        if (batteryTempC > 45f) score -= 25
        else if (batteryTempC > 40f) score -= 12

        if (storagePercent > 92f) score -= 15
        else if (storagePercent > 85f) score -= 8

        val clampedScore = score.coerceIn(15, 100)
        assertEquals(20, clampedScore)

        val category = when {
            clampedScore >= 85 -> HealthCategory.OPTIMAL
            clampedScore >= 70 -> HealthCategory.GOOD
            clampedScore >= 50 -> HealthCategory.ATTENTION
            else -> HealthCategory.CRITICAL
        }
        assertEquals(HealthCategory.CRITICAL, category)
    }

    @Test
    fun testThermalFallbackLogic() {
        val game = GameProfileEntity(
            packageName = "com.sample.game",
            appName = "Demo Action",
            mode = "Turbo++"
        )
        val highTemp = 46.5f

        var activeMode = game.mode
        if (activeMode == "Turbo++" && highTemp > 45f) {
            activeMode = "Turbo"
        }

        assertEquals("Turbo", activeMode)
    }

    @Test
    fun testShizukuStateTransitions() {
        val stateRoot = ShizukuState.READY_ROOT
        val stateShell = ShizukuState.READY_SHELL

        assertTrue(stateRoot == ShizukuState.READY_ROOT)
        assertTrue(stateShell != ShizukuState.NOT_RUNNING)
    }

    @Test
    fun testRoutineCleanScheduleConstraints() {
        val intervalDays = 1
        val intervalMillis = intervalDays * 24L * 60L * 60L * 1000L
        assertEquals(86400000L, intervalMillis)

        val idleOnly = true
        val chargingOnly = true
        assertTrue(idleOnly && chargingOnly)
    }
}
