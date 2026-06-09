package com.example.sporttracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metrics_table")
data class DailyMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weight: String,
    val bodyFat: String,
    val dateInMillis: Long
)