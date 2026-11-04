package com.faceguard.service

import android.util.Log
import com.faceguard.data.database.Profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Singleton manager to share the active profile between
 * Recognition Screen and AppEnforcementService
 * 
 * The active profile automatically clears after a timeout when no face is detected.
 */
object ActiveProfileManager {
    private const val TAG = "ActiveProfileManager"
    
    // Timeout in milliseconds before clearing inactive profile
    private const val PROFILE_TIMEOUT_MS = 5000L // 5 seconds

    private var _activeProfile: Profile? = null
    private var clearJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    val activeProfile: Profile?
        get() = _activeProfile

    fun setActiveProfile(profile: Profile?) {
        Log.d(TAG, "========== ACTIVE PROFILE CHANGED ==========")
        Log.d(TAG, "Previous profile: ${_activeProfile?.name ?: "NONE"}")
        Log.d(TAG, "Previous profile ID: ${_activeProfile?.id ?: "NONE"}")
        Log.d(TAG, "New profile: ${profile?.name ?: "NONE"}")
        Log.d(TAG, "New profile ID: ${profile?.id ?: "NONE"}")
        
        // Cancel any pending clear job
        clearJob?.cancel()
        clearJob = null
        
        _activeProfile = profile
        Log.d(TAG, "Active profile updated successfully")
    }

    fun clearActiveProfile() {
        Log.d(TAG, "========== ACTIVE PROFILE CLEARED ==========")
        Log.d(TAG, "Cleared profile: ${_activeProfile?.name ?: "NONE"}")
        Log.d(TAG, "Cleared profile ID: ${_activeProfile?.id ?: "NONE"}")
        
        // Cancel any pending clear job
        clearJob?.cancel()
        clearJob = null
        
        _activeProfile = null
        Log.d(TAG, "Active profile is now null")
    }
    
    /**
     * Schedules the active profile to be cleared after a timeout.
     * This is called when NoFace or UnknownPerson is detected.
     * If a new face is recognized before the timeout, the clear is cancelled.
     */
    fun scheduleClearAfterTimeout() {
        Log.d(TAG, "========== SCHEDULING PROFILE CLEAR ==========")
        Log.d(TAG, "Current profile: ${_activeProfile?.name ?: "NONE"}")
        Log.d(TAG, "Timeout: ${PROFILE_TIMEOUT_MS}ms")
        
        // Cancel any existing clear job
        clearJob?.cancel()
        
        clearJob = scope.launch {
            delay(PROFILE_TIMEOUT_MS)
            if (_activeProfile != null) {
                Log.d(TAG, "========== TIMEOUT: CLEARING PROFILE ==========")
                Log.d(TAG, "Profile cleared due to timeout: ${_activeProfile?.name}")
                _activeProfile = null
                clearJob = null
            }
        }
        
        Log.d(TAG, "Profile clear scheduled in ${PROFILE_TIMEOUT_MS}ms")
    }
}
