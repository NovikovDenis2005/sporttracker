package com.example.sporttracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Query("SELECT * FROM records_table ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<RecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RecordEntity)

    @Delete
    suspend fun deleteRecord(record: RecordEntity)

    @Query("DELETE FROM records_table")
    suspend fun clearAllRecords()

    // --- НОВОЕ: Для отслеживания прогресса тела ---
    @Query("SELECT * FROM metrics_table ORDER BY dateInMillis DESC")
    fun getAllMetrics(): Flow<List<DailyMetricEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: DailyMetricEntity)
}