package com.example.sporttracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TrackerViewModelFactory(
    private val repository: RecordRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackerViewModel::class.java)) {
            return TrackerViewModel(repository) as T
        }
        throw IllegalArgumentException("Критическая ошибка: Неизвестный класс ViewModel")
    }
}