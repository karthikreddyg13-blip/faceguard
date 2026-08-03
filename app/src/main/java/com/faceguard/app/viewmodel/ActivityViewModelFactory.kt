package com.faceguard.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.faceguard.data.repository.ActivityLogRepository

class ActivityViewModelFactory(
    private val activityLogRepository: ActivityLogRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityViewModel::class.java)) {
            return ActivityViewModel(activityLogRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
