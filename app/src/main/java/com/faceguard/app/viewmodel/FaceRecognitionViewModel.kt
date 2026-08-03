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
 * 
 * Note: This ViewModel does NOT implement app locking functionality.
 * It only provides the recognition data flow for UI components to consume.
 */
class FaceRecognitionViewModel(
    private val context: Context
) : ViewModel() {

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

    /**
     * Performs face recognition on a bitmap image.
     * 
     * This method:
     * 1. Sets processing state to true
     * 2. Delegates to FaceRecognitionManager to process the image
     * 3. Updates the recognition result state
     * 4. Handles errors gracefully
     * 
     * @param bitmap The bitmap image to recognize
     */
    fun recognizeFromBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _errorMessage.value = null

                val result = faceRecognitionManager.recognizeFace(bitmap)
                _recognitionResult.value = result

                // TODO: Handle recognition result based on type
                // When actual matching is implemented, you can:
                // - Log activity for MatchFound results
                // - Trigger enrollment flow for UnknownPerson results
                // - Show appropriate UI feedback for NoFace results

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
                _recognitionResult.value = result

                // TODO: Handle recognition result based on type
                // When actual matching is implemented, you can:
                // - Log activity for MatchFound results
                // - Trigger enrollment flow for UnknownPerson results
                // - Show appropriate UI feedback for NoFace results

            } catch (e: Exception) {
                _errorMessage.value = "Recognition failed: ${e.message}"
                _recognitionResult.value = RecognitionResult.NoFace(e.message ?: "Unknown error")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Clears the current recognition result.
     * Call this to reset the state before a new recognition operation.
     */
    fun clearRecognitionResult() {
        _recognitionResult.value = null
    }

    /**
     * Clears any error messages.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Cleans up resources when the ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        faceRecognitionManager.release()
    }
}
