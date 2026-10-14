package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_profiles")
data class GameProfileEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val mode: String = "Turbo", // Balanced, Performance, Turbo, Turbo++, Battery, Custom
    val preferredFps: Int = 120,
    val dndEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val wifiHighPerf: Boolean = true,
    val touchProtection: Boolean = true,
    val lastPlayedTimestamp: Long = 0L,
    val totalPlaytimeSeconds: Long = 0L,
    val launchCount: Int = 0
)

@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val gameName: String,
    val mode: String,
    val durationSeconds: Long,
    val avgFps: Float,
    val minFps: Float,
    val maxFps: Float,
    val startTempC: Float,
    val endTempC: Float,
    val startRamMb: Long,
    val endRamMb: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "boost_logs")
data class BoostLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val ramFreedMb: Long,
    val storageCleanedMb: Long,
    val tempBeforeC: Float,
    val tempAfterC: Float,
    val boostType: String // "Manual", "Auto", "GameStart", "QuickTile"
)

@Entity(tableName = "shizuku_logs")
data class ShizukuLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val privilegeLevel: String, // "Shell (ADB)" or "Root (UID 0)"
    val result: String,
    val reverted: Boolean = false
)
