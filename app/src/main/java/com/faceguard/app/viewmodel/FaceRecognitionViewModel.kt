package com.faceguard.app.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faceguard.app.FaceRecognitionManager
import com.faceguard.app.RecognitionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for face recognition operations.
 *
 * This ViewModel manages the face recognition process and exposes:
 * - Recognition results (MatchFound, UnknownPerson, NoFace)
 * - Loading states
 * - Error states
 * - Temporal stability with frame voting
 *
 * Note: This ViewModel does NOT implement app locking functionality.
 * It only provides the recognition data flow for UI components to consume.
 */
class FaceRecognitionViewModel(
    private val context: Context
) : ViewModel() {

    companion object {
        // Number of consecutive frames required to confirm identity
        private const val CONSECUTIVE_FRAMES_THRESHOLD = 5

        // Number of recent frames to keep for voting
        private const val FRAME_HISTORY_SIZE = 10
    }

    private val faceRecognitionManager = FaceRecognitionManager(context)

    // Recognition result state
    private val _recognitionResult = MutableStateFlow<RecognitionResult?>(null)
    val recognitionResult: StateFlow<RecognitionResult?> = _recognitionResult.asStateFlow()

    // Loading state
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Temporal stability tracking
    private val recentResults = mutableListOf<RecognitionResult>()
    private var consecutiveMatchCount = 0
    private var currentStableIdentity: String? = null

    /**
     * Performs face recognition on a bitmap image.
     *
     * This method:
     * 1. Sets processing state to true
     * 2. Delegates to FaceRecognitionManager to process the image
     * 3. Applies temporal stability with frame voting
     * 4. Updates the recognition result state
     * 5. Handles errors gracefully
     *
     * @param bitmap The bitmap image to recognize
     */
    fun recognizeFromBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _errorMessage.value = null

                val result = faceRecognitionManager.recognizeFace(bitmap)
                applyTemporalStability(result)

            } catch (e: Exception) {
                _errorMessage.value = "Recognition failed: ${e.message}"
                _recognitionResult.value = RecognitionResult.NoFace(e.message ?: "Unknown error")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Performs face recognition on camera image data.
     *
     * This method handles camera frames from CameraX for real-time recognition.
     *
     * @param imageProxy The camera image proxy from CameraX
     * @param rotationDegrees The rotation of the image
     */
    fun recognizeFromCamera(
        imageProxy: android.media.Image,
        rotationDegrees: Int
    ) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _errorMessage.value = null

                val result = faceRecognitionManager.recognizeFace(imageProxy, rotationDegrees)
                applyTemporalStability(result)

            } catch (e: Exception) {
                _errorMessage.value = "Recognition failed: ${e.message}"
                _recognitionResult.value = RecognitionResult.NoFace(e.message ?: "Unknown error")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Applies temporal stability to recognition results using frame voting.
     *
     * This method:
     * 1. Maintains a history of recent recognition results
     * 2. Requires consecutive matches before confirming identity
     * 3. Prevents rapid identity switching
     * 4. Only updates UI when identity is stable
     *
     * @param newResult The latest recognition result from the manager
     */
    private fun applyTemporalStability(newResult: RecognitionResult) {
        android.util.Log.d("FaceRecognitionViewModel", "========== TEMPORAL STABILITY CHECK ==========")
        android.util.Log.d("FaceRecognitionViewModel", "Current stable identity: $currentStableIdentity")
        android.util.Log.d("FaceRecognitionViewModel", "Consecutive match count: $consecutiveMatchCount")
        android.util.Log.d("FaceRecognitionViewModel", "New result type: ${newResult::class.simpleName}")

        // Add new result to history
        recentResults.add(newResult)
        if (recentResults.size > FRAME_HISTORY_SIZE) {
            recentResults.removeAt(0)
        }

        when (newResult) {
            is RecognitionResult.MatchFound -> {
                val newIdentity = newResult.profile.name
                val confidence = newResult.confidence
                android.util.Log.d("FaceRecognitionViewModel", "Match found: $newIdentity (confidence: ${"%.2f".format(confidence * 100)}%)")

                // If this is the same as current stable identity, maintain it
                if (currentStableIdentity == newIdentity) {
                    consecutiveMatchCount++
                    _recognitionResult.value = newResult
                    android.util.Log.d("FaceRecognitionViewModel", "✓ Same identity confirmed: $newIdentity (count: $consecutiveMatchCount)")
                    return
                }

                // If different identity, check if we have enough consecutive matches
                if (consecutiveMatchCount >= CONSECUTIVE_FRAMES_THRESHOLD) {
                    // Confirm new identity only if it's consistently the same in recent frames
                    val recentMatches = recentResults.takeLast(CONSECUTIVE_FRAMES_THRESHOLD)
                        .filterIsInstance<RecognitionResult.MatchFound>()
                        .map { it.profile.name }

                    val allSame = recentMatches.all { it == newIdentity }
                    val matchCount = recentMatches.count { it == newIdentity }

                    android.util.Log.d("FaceRecognitionViewModel", "Recent matches: $recentMatches")
                    android.util.Log.d("FaceRecognitionViewModel", "Match count for $newIdentity: $matchCount/$CONSECUTIVE_FRAMES_THRESHOLD")

                    if (allSame || matchCount >= CONSECUTIVE_FRAMES_THRESHOLD) {
                        currentStableIdentity = newIdentity
                        consecutiveMatchCount = 1
                        _recognitionResult.value = newResult
                        android.util.Log.d("FaceRecognitionViewModel", "✓ IDENTITY SWITCH CONFIRMED: $newIdentity (after $matchCount consecutive matches)")
                    } else {
                        // Keep current identity, don't switch
                        consecutiveMatchCount = 0
                        android.util.Log.d("FaceRecognitionViewModel", "✗ IDENTITY SWITCH REJECTED: $newIdentity (not stable, matches: $matchCount/$CONSECUTIVE_FRAMES_THRESHOLD)")
                    }
                } else {
                    consecutiveMatchCount++
                    android.util.Log.d("FaceRecognitionViewModel", "Building confidence for $newIdentity ($consecutiveMatchCount/$CONSECUTIVE_FRAMES_THRESHOLD)")
                }
            }
            is RecognitionResult.UnknownPerson -> {
                android.util.Log.d("FaceRecognitionViewModel", "Unknown person detected")
                android.util.Log.d("FaceRecognitionViewModel", "Best match: ${newResult.bestMatchName} @ ${"%.2f".format((newResult.bestMatchScore ?: 0f) * 100)}%")
                // Reset consecutive match count on unknown person
                consecutiveMatchCount = 0
                currentStableIdentity = null
                _recognitionResult.value = newResult
            }
            is RecognitionResult.NoFace -> {
                android.util.Log.d("FaceRecognitionViewModel", "No face detected")
                // Reset consecutive match count on no face
                consecutiveMatchCount = 0
                currentStableIdentity = null
                _recognitionResult.value = newResult
            }
        }
    }

    /**
     * Clears the current recognition result.
     * Call this to reset the state before a new recognition operation.
     */
    fun clearRecognitionResult() {
        _recognitionResult.value = null
        recentResults.clear()
        consecutiveMatchCount = 0
        currentStableIdentity = null
    }

    /**
     * Clears any error messages.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Resets the liveness detector state.
     * Call this when starting a new recognition session.
     */
    fun resetLiveness() {
        faceRecognitionManager.resetLiveness()
    }

    /**
     * Cleans up resources when the ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        faceRecognitionManager.release()
    }
}
