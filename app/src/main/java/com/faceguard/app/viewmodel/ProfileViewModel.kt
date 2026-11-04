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
import com.google.mlkit.vision.face.Face
import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
                    profileName = name,
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
                // Store profile name before deletion for activity log
                val profileName = profile.name
                repository.deleteProfile(profile)
                
                val activityLog = ActivityLog(
                    profileId = profile.id,
                    profileName = profileName,
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
                        profileName = profile.name,
                        timestamp = System.currentTimeMillis(),
                        result = "Face Enrolled",
                        intruderPhotoPath = null
                    )
                    activityLogRepository.insertLog(activityLog)
                    
                    return@launch
                }

                val image = InputImage.fromFilePath(context, uri)
                // Use same detector options as FaceRecognitionManager for consistent face vector extraction
                val options = FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .enableTracking()
                    .build()
                val detector = FaceDetection.getClient(options)

                android.util.Log.d("ProfileViewModel", "========== FACE ENROLLMENT START ==========")
                android.util.Log.d("ProfileViewModel", "Profile: ${profile.name} (ID: ${profile.id})")

                detector.process(image)
                    .addOnSuccessListener { faces ->
                        viewModelScope.launch {
                            when (faces.size) {
                                0 -> {
                                    _faceValidationError.value = "No face detected in the image"
                                    File(imagePath).delete()
                                }
                                1 -> {
                                    // Validate face quality
                                    val face = faces[0]
                                    val validationResult = validateFaceQuality(face)
                                    
                                    if (!validationResult.isValid) {
                                        _faceValidationError.value = validationResult.reason
                                        File(imagePath).delete()
                                        detector.close()
                                        return@launch
                                    }
                                    
                                    // Extract face vector using the same structure as FaceRecognitionManager
                                    val faceVector = extractFaceVector(face)

                                    // Convert FloatArray to ByteArray for database storage
                                    val faceVectorBytes = floatArrayToByteArray(faceVector)

                                    // Update profile with both image path and face vector
                                    val updatedProfile = profile.copy(
                                        imagePath = imagePath,
                                        faceVector = faceVectorBytes
                                    )
                                    updateProfile(updatedProfile)
                                    _faceValidationError.value = null

                                    android.util.Log.d("ProfileViewModel", "Face enrolled: ${profile.name}")

                                    val activityLog = ActivityLog(
                                        profileId = profile.id,
                                        profileName = profile.name,
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

    /**
     * Extracts a face vector from ML Kit Face detection results using facial landmarks.
     *
     * This method uses the same structure as FaceRecognitionManager.extractFaceVector()
     * to ensure consistency between enrollment and recognition.
     *
     * The vector includes expression-invariant facial features:
     * - All facial landmarks (left eye, right eye, nose, mouth, ears, cheeks)
     * - Normalized landmark positions relative to bounding box
     * - Head Euler angles for pose information
     * - Eye openness probabilities (for liveness, not recognition)
     * - Bounding box aspect ratio (face shape)
     *
     * EXCLUDED: Smiling probability (expression-sensitive)
     * This provides expression-invariant discrimination between different people.
     *
     * @param face The ML Kit Face object
     * @return FloatArray representing the face feature vector
     */
    private fun extractFaceVector(face: Face): FloatArray {
        val box = face.boundingBox
        val boxWidth = box.width().toFloat()
        val boxHeight = box.height().toFloat()
        val boxCenterX = box.centerX().toFloat()
        val boxCenterY = box.centerY().toFloat()

        val landmarks = mutableListOf<Float>()

        // Add normalized landmark positions (structural features only)
        val landmarkTypes = listOf(
            FaceLandmark.LEFT_EYE,
            FaceLandmark.RIGHT_EYE,
            FaceLandmark.NOSE_BASE,
            FaceLandmark.LEFT_MOUTH,
            FaceLandmark.RIGHT_MOUTH,
            FaceLandmark.LEFT_EAR,
            FaceLandmark.RIGHT_EAR,
            FaceLandmark.LEFT_CHEEK,
            FaceLandmark.RIGHT_CHEEK
        )

        for (landmarkType in landmarkTypes) {
            val landmark = face.getLandmark(landmarkType)
            if (landmark != null) {
                // Normalize landmark position relative to bounding box center
                val normalizedX = (landmark.position.x - boxCenterX) / boxWidth
                val normalizedY = (landmark.position.y - boxCenterY) / boxHeight
                landmarks.add(normalizedX)
                landmarks.add(normalizedY)
            } else {
                // If landmark not detected, use placeholder values
                landmarks.add(0f)
                landmarks.add(0f)
            }
        }

        // Add head pose angles (important for pose normalization)
        landmarks.add(face.headEulerAngleX / 180f) // Normalize to -1 to 1 range
        landmarks.add(face.headEulerAngleY / 180f)
        landmarks.add(face.headEulerAngleZ / 180f)

        // Add eye openness probabilities (for liveness detection, not recognition)
        landmarks.add(face.leftEyeOpenProbability ?: 0f)
        landmarks.add(face.rightEyeOpenProbability ?: 0f)

        // Add bounding box aspect ratio (face shape)
        landmarks.add(boxWidth / boxHeight)

        return landmarks.toFloatArray()
    }

    /**
     * Face landmark type constants for ML Kit Face Detection
     */
    private object FaceLandmark {
        const val LEFT_EYE = 0
        const val RIGHT_EYE = 1
        const val NOSE_BASE = 2
        const val LEFT_MOUTH = 3
        const val RIGHT_MOUTH = 4
        const val LEFT_EAR = 5
        const val RIGHT_EAR = 6
        const val LEFT_CHEEK = 7
        const val RIGHT_CHEEK = 8
    }

    /**
     * Validates face quality for enrollment.
     *
     * Checks:
     * - Face size (not too small or too large)
     * - Head pose (not extreme angles)
     * - Landmark detection (key features visible)
     *
     * @param face The ML Kit Face object
     * @return ValidationResult indicating if face is suitable for enrollment
     */
    private fun validateFaceQuality(face: Face): ValidationResult {
        android.util.Log.d("ProfileViewModel", "========== FACE VALIDATION START ==========")
        
        val box = face.boundingBox
        val faceWidth = box.width().toFloat()
        val faceHeight = box.height().toFloat()

        android.util.Log.d("ProfileViewModel", "Face width: $faceWidth px")
        android.util.Log.d("ProfileViewModel", "Face height: $faceHeight px")
        android.util.Log.d("ProfileViewModel", "Head Euler X: ${face.headEulerAngleX}°")
        android.util.Log.d("ProfileViewModel", "Head Euler Y: ${face.headEulerAngleY}°")
        android.util.Log.d("ProfileViewModel", "Head Euler Z: ${face.headEulerAngleZ}°")
        android.util.Log.d("ProfileViewModel", "Left eye open: ${face.leftEyeOpenProbability}")
        android.util.Log.d("ProfileViewModel", "Right eye open: ${face.rightEyeOpenProbability}")

        // Check face size
        val minFaceSize = 80f  // Relaxed from 100f
        val maxFaceSize = 1200f  // Relaxed from 800f

        android.util.Log.d("ProfileViewModel", "Size check: min=$minFaceSize, max=$maxFaceSize")
        
        if (faceWidth < minFaceSize || faceHeight < minFaceSize) {
            android.util.Log.d("ProfileViewModel", "✗ FAILED: Face too small (width: $faceWidth, height: $faceHeight, min: $minFaceSize px)")
            return ValidationResult(false, "Face too small. Please move closer to the camera.")
        }

        if (faceWidth > maxFaceSize || faceHeight > maxFaceSize) {
            android.util.Log.d("ProfileViewModel", "✗ FAILED: Face too large (width: $faceWidth, height: $faceHeight, max: $maxFaceSize px)")
            return ValidationResult(false, "Face too large. Please move further from the camera.")
        }
        
        android.util.Log.d("ProfileViewModel", "✓ Size check passed")

        // Check head pose (relaxed from 30° to 45°)
        val maxHeadAngle = 45f
        android.util.Log.d("ProfileViewModel", "Head angle check: max=$maxHeadAngle°")
        
        if (kotlin.math.abs(face.headEulerAngleX) > maxHeadAngle ||
            kotlin.math.abs(face.headEulerAngleY) > maxHeadAngle ||
            kotlin.math.abs(face.headEulerAngleZ) > maxHeadAngle) {
            android.util.Log.d("ProfileViewModel", "✗ FAILED: Head angle too extreme (X: ${face.headEulerAngleX}°, Y: ${face.headEulerAngleY}°, Z: ${face.headEulerAngleZ}°, max: $maxHeadAngle°)")
            return ValidationResult(false, "Face angle too extreme. Please face the camera directly.")
        }
        
        android.util.Log.d("ProfileViewModel", "✓ Head angle check passed")

        // Check key landmarks are detected (relaxed - only require eyes)
        val requiredLandmarks = listOf(
            FaceLandmark.LEFT_EYE,
            FaceLandmark.RIGHT_EYE
        )

        android.util.Log.d("ProfileViewModel", "Landmark check: required=$requiredLandmarks")
        
        val missingLandmarks = requiredLandmarks.filter { face.getLandmark(it) == null }
        if (missingLandmarks.isNotEmpty()) {
            android.util.Log.d("ProfileViewModel", "✗ FAILED: Missing landmarks: $missingLandmarks")
            android.util.Log.d("ProfileViewModel", "Detected landmarks check:")
            requiredLandmarks.forEach { landmarkType ->
                val detected = face.getLandmark(landmarkType) != null
                android.util.Log.d("ProfileViewModel", "  - Landmark $landmarkType: ${if (detected) "✓" else "✗"}")
            }
            return ValidationResult(false, "Key facial features not detected. Please ensure your face is clearly visible.")
        }
        
        android.util.Log.d("ProfileViewModel", "✓ All required landmarks detected")

        android.util.Log.d("ProfileViewModel", "========== FACE VALIDATION PASSED ==========")
        return ValidationResult(true, "")
    }

    /**
     * Data class for face validation results
     */
    private data class ValidationResult(
        val isValid: Boolean,
        val reason: String
    )

    /**
     * Converts a FloatArray to ByteArray for database storage.
     * 
     * This method converts each float in the array to 4 bytes using ByteBuffer,
     * resulting in a ByteArray that can be stored in the Room database.
     * 
     * The conversion process:
     * 1. Allocate a ByteBuffer with capacity = floatArray.size * 4 bytes
     * 2. Write each float to the buffer (4 bytes per float)
     * 3. Convert buffer to ByteArray
     * 
     * @param floatArray The FloatArray to convert
     * @return ByteArray representation of the float array
     */
    private fun floatArrayToByteArray(floatArray: FloatArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(floatArray.size * 4)
        for (value in floatArray) {
            byteBuffer.putFloat(value)
        }
        return byteBuffer.array()
    }
}
