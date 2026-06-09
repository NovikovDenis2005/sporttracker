package com.example.sporttracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Версия увеличена до 2, добавлена таблица DailyMetricEntity
@Database(entities = [RecordEntity::class, DailyMetricEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sport_tracker_database"
                )
                    .fallbackToDestructiveMigration() // Защита от вылетов при обновлении БД
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}