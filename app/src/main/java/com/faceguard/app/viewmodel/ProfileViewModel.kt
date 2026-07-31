package com.faceguard.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faceguard.data.database.ActivityLog
import com.faceguard.data.database.Profile
import com.faceguard.data.repository.ActivityLogRepository
import com.faceguard.data.repository.ProfileRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val activityLogRepository: ActivityLogRepository
) : ViewModel() {

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _faceValidationError = MutableStateFlow<String?>(null)
    val faceValidationError: StateFlow<String?> = _faceValidationError.asStateFlow()

    private val _developerMode = MutableStateFlow(false)
    val developerMode: StateFlow<Boolean> = _developerMode.asStateFlow()

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

    fun addProfile(name: String, relation: String, isOwner: Boolean, faceVector: ByteArray?, imagePath: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = Profile(
                    name = name,
                    relation = relation,
                    isOwner = isOwner,
                    faceVector = faceVector,
                    imagePath = imagePath,
                    createdAt = System.currentTimeMillis()
                )
                val profileId = repository.insertProfile(profile)
                
                val activityLog = ActivityLog(
                    profileId = profileId.toInt(),
                    timestamp = System.currentTimeMillis(),
                    result = "Profile Added",
                    intruderPhotoPath = null
                )
                activityLogRepository.insertLog(activityLog)
                
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
                
                val activityLog = ActivityLog(
                    profileId = profile.id,
                    timestamp = System.currentTimeMillis(),
                    result = "Profile Deleted",
                    intruderPhotoPath = null
                )
                activityLogRepository.insertLog(activityLog)
                
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateProfile(profile)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearFaceValidationError() {
        _faceValidationError.value = null
    }

    fun toggleDeveloperMode() {
        _developerMode.value = !_developerMode.value
    }

    fun validateAndEnrollFace(
        context: Context,
        uri: Uri,
        imagePath: String,
        profile: Profile
    ) {
        viewModelScope.launch {
            try {
                if (_developerMode.value) {
                    val updatedProfile = profile.copy(imagePath = imagePath)
                    updateProfile(updatedProfile)
                    _faceValidationError.value = null
                    
                    val activityLog = ActivityLog(
                        profileId = profile.id,
                        timestamp = System.currentTimeMillis(),
                        result = "Face Enrolled",
                        intruderPhotoPath = null
                    )
                    activityLogRepository.insertLog(activityLog)
                    
                    return@launch
                }

                val image = InputImage.fromFilePath(context, uri)
                val options = FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .build()
                val detector = FaceDetection.getClient(options)

                detector.process(image)
                    .addOnSuccessListener { faces ->
                        viewModelScope.launch {
                            when (faces.size) {
                                0 -> {
                                    _faceValidationError.value = "No face detected in the image"
                                    File(imagePath).delete()
                                }
                                1 -> {
                                    val updatedProfile = profile.copy(imagePath = imagePath)
                                    updateProfile(updatedProfile)
                                    _faceValidationError.value = null
                                    
                                    val activityLog = ActivityLog(
                                        profileId = profile.id,
                                        timestamp = System.currentTimeMillis(),
                                        result = "Face Enrolled",
                                        intruderPhotoPath = null
                                    )
                                    activityLogRepository.insertLog(activityLog)
                                }
                                else -> {
                                    _faceValidationError.value = "Multiple faces detected. Please use an image with exactly one face."
                                    File(imagePath).delete()
                                }
                            }
                            detector.close()
                        }
                    }
                    .addOnFailureListener { e ->
                        _faceValidationError.value = "Failed to detect faces: ${e.message}"
                        File(imagePath).delete()
                        detector.close()
                    }
            } catch (e: Exception) {
                _faceValidationError.value = "Face validation error: ${e.message}"
                File(imagePath).delete()
            }
        }
    }
}
