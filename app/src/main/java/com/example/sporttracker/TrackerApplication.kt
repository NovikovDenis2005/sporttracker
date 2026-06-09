package com.example.sporttracker

import android.app.Application

class TrackerApplication : Application() {
    // Ленивая инициализация базы данных и репозитория
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { RecordRepository(database.recordDao()) }
}