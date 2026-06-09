package com.example.sporttracker

import kotlinx.coroutines.flow.Flow

class RecordRepository(private val dao: RecordDao) {

    fun getRecords(): Flow<List<RecordEntity>> = dao.getAllRecords()

    suspend fun addRecord(exerciseName: String, result: String) {
        val record = RecordEntity(
            exerciseName = exerciseName,
            result = result,
            timestamp = System.currentTimeMillis()
        )
        dao.insertRecord(record)
    }

    suspend fun deleteRecord(record: RecordEntity) {
        dao.deleteRecord(record)
    }

    suspend fun clearAllRecords() {
        dao.clearAllRecords()
    }

    // --- НОВОЕ: Функции для дневника веса ---
    fun getMetrics(): Flow<List<DailyMetricEntity>> = dao.getAllMetrics()

    suspend fun addMetric(weight: String, bodyFat: String) {
        val metric = DailyMetricEntity(
            weight = weight,
            bodyFat = bodyFat,
            dateInMillis = System.currentTimeMillis()
        )
        dao.insertMetric(metric)
    }
}