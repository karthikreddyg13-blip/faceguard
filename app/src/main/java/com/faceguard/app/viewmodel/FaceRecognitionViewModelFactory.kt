package com.faceguard.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for creating FaceRecognitionViewModel instances.
 * 
 * This factory follows the existing pattern in the project (similar to ProfileViewModelFactory)
 * and provides the Context required by FaceRecognitionManager.
 */
class FaceRecognitionViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FaceRecognitionViewModel::class.java)) {
            return FaceRecognitionViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
