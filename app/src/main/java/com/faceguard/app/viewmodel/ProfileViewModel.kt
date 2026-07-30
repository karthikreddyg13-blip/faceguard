package com.faceguard.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faceguard.data.database.Profile
import com.faceguard.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                repository.getAllProfiles().collect { profileList ->
                    _profiles.value = profileList
                    _isLoading.value = false
                }

            } catch (e: Exception) {
                _errorMessage.value = "Failed to load profiles: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun addProfile(name: String, relation: String, isOwner: Boolean, faceVector: ByteArray?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = Profile(
                    name = name,
                    relation = relation,
                    isOwner = isOwner,
                    faceVector = faceVector,
                    createdAt = System.currentTimeMillis()
                )
                repository.insertProfile(profile)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteProfile(profile)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
