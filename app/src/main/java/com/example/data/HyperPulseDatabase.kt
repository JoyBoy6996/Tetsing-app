package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BoostDao
import com.example.data.dao.GameDao
import com.example.data.dao.ShizukuDao
import com.example.data.entity.BoostLogEntity
import com.example.data.entity.GameProfileEntity
import com.example.data.entity.GameSessionEntity
import com.example.data.entity.ShizukuLogEntity

@Database(
    entities = [
        GameProfileEntity::class,
        GameSessionEntity::class,
        BoostLogEntity::class,
        ShizukuLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HyperPulseDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun boostDao(): BoostDao
    abstract fun shizukuDao(): ShizukuDao

    companion object {
        @Volatile
        private var INSTANCE: HyperPulseDatabase? = null

        fun getDatabase(context: Context): HyperPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HyperPulseDatabase::class.java,
                    "hyperpulse_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
