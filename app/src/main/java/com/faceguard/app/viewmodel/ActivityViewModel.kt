package com.faceguard.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faceguard.data.database.ActivityLog
import com.faceguard.data.database.Profile
import com.faceguard.data.repository.ActivityLogRepository
import com.faceguard.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActivityViewModel(
    private val activityLogRepository: ActivityLogRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _activityLogs = MutableStateFlow<List<ActivityLogWithProfile>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLogWithProfile>> = _activityLogs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadActivityLogs()
    }

    private fun loadActivityLogs() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                activityLogRepository.getAllLogs().collect { logs ->
                    val logsWithProfiles = logs.map { log ->
                        val profile = log.profileId?.let { profileRepository.getProfileById(it) }
                        ActivityLogWithProfile(
                            activityLog = log,
                            profileName = profile?.name ?: "Unknown"
                        )
                    }
                    _activityLogs.value = logsWithProfiles
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }
}

data class ActivityLogWithProfile(
    val activityLog: ActivityLog,
    val profileName: String
)
