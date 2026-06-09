package com.example.sporttracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records_table")
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseName: String,
    val result: String,
    val timestamp: Long
)
