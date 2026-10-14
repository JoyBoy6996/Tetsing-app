package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.BoostLogEntity
import com.example.data.entity.GameProfileEntity
import com.example.data.entity.GameSessionEntity
import com.example.data.entity.ShizukuLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM game_profiles ORDER BY lastPlayedTimestamp DESC")
    fun getAllGames(): Flow<List<GameProfileEntity>>

    @Query("SELECT * FROM game_profiles WHERE packageName = :pkg LIMIT 1")
    suspend fun getGame(pkg: String): GameProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGame(game: GameProfileEntity)

    @Update
    suspend fun updateGame(game: GameProfileEntity)

    @Query("DELETE FROM game_profiles WHERE packageName = :pkg")
    suspend fun deleteGame(pkg: String)

    @Query("SELECT * FROM game_sessions ORDER BY timestamp DESC LIMIT 30")
    fun getRecentSessions(): Flow<List<GameSessionEntity>>

    @Insert
    suspend fun insertSession(session: GameSessionEntity): Long
}

@Dao
interface BoostDao {
    @Query("SELECT * FROM boost_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentBoostLogs(): Flow<List<BoostLogEntity>>

    @Insert
    suspend fun insertBoostLog(log: BoostLogEntity): Long

    @Query("SELECT SUM(ramFreedMb) FROM boost_logs")
    fun getTotalRamFreedMb(): Flow<Long?>

    @Query("SELECT SUM(storageCleanedMb) FROM boost_logs")
    fun getTotalStorageCleanedMb(): Flow<Long?>
}

@Dao
interface ShizukuDao {
    @Query("SELECT * FROM shizuku_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAllLogs(): Flow<List<ShizukuLogEntity>>

    @Insert
    suspend fun insertLog(log: ShizukuLogEntity): Long

    @Query("UPDATE shizuku_logs SET reverted = 1 WHERE id = :id")
    suspend fun markReverted(id: Long)

    @Query("UPDATE shizuku_logs SET reverted = 1")
    suspend fun markAllReverted()
}
