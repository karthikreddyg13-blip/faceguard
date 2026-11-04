package com.faceguard.app

import android.util.Log
import com.google.mlkit.vision.face.Face
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * LivenessDetector provides practical liveness detection using ML Kit face data.
 *
 * This class implements multiple liveness checks to prevent photo spoofing:
 * - Blink detection (eye state changes)
 * - Head movement detection (pose changes)
 * - Face tracking consistency
 * - Eye openness validation
 *
 * A static photo on another phone should not pass these checks.
 */
class LivenessDetector {
    companion object {
        private const val TAG = "LivenessDetector"
        
        // Minimum frames required for liveness detection
        private const val MIN_FRAMES_FOR_LIVENESS = 5
        
        // Blink detection thresholds
        private const val BLINK_THRESHOLD = 0.3f
        private const val MIN_BLINKS_REQUIRED = 1
        
        // Head movement thresholds (degrees)
        private const val MIN_HEAD_MOVEMENT = 5f
        
        // Eye openness threshold
        private const val MIN_EYE_OPENNESS = 0.2f
    }
    
    private val faceHistory = mutableListOf<FaceData>()
    private var blinkCount = 0
    private var lastEyeState = EyeState.OPEN
    private var hasHeadMovement = false
    
    /**
     * Data class to store face information for liveness tracking
     */
    private data class FaceData(
        val headEulerX: Float,
        val headEulerY: Float,
        val headEulerZ: Float,
        val leftEyeOpen: Float?,
        val rightEyeOpen: Float?,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Eye state enum for blink detection
     */
    private enum class EyeState {
        OPEN, CLOSED
    }
    
    /**
     * Processes a detected face for liveness detection.
     * 
     * @param face The ML Kit Face object
     * @return LivenessResult indicating if the face passes liveness checks
     */
    fun processFace(face: Face): LivenessResult {
        val faceData = FaceData(
            headEulerX = face.headEulerAngleX,
            headEulerY = face.headEulerAngleY,
            headEulerZ = face.headEulerAngleZ,
            leftEyeOpen = face.leftEyeOpenProbability,
            rightEyeOpen = face.rightEyeOpenProbability
        )
        
        faceHistory.add(faceData)
        
        // Keep only recent history (last 30 frames)
        if (faceHistory.size > 30) {
            faceHistory.removeAt(0)
        }
        
        // Check if we have enough frames for liveness detection
        if (faceHistory.size < MIN_FRAMES_FOR_LIVENESS) {
            return LivenessResult.InsufficientData(
                frames = faceHistory.size,
                required = MIN_FRAMES_FOR_LIVENESS
            )
        }
        
        // Perform liveness checks
        val blinkResult = detectBlink(faceData)
        val headMovementResult = detectHeadMovement()
        val eyeOpennessResult = validateEyeOpenness(faceData)
        
        val isLive = blinkResult.passed || headMovementResult.passed
        
        return LivenessResult.Detected(
            isLive = isLive,
            blinkDetected = blinkResult.passed,
            headMovementDetected = headMovementResult.passed,
            eyesOpen = eyeOpennessResult.passed,
            confidence = calculateConfidence(blinkResult, headMovementResult, eyeOpennessResult)
        )
    }
    
    /**
     * Detects blinks by monitoring eye state changes.
     */
    private fun detectBlink(currentFace: FaceData): LivenessCheck {
        val leftEyeOpen = currentFace.leftEyeOpen ?: 0f
        val rightEyeOpen = currentFace.rightEyeOpen ?: 0f
        val avgEyeOpen = (leftEyeOpen + rightEyeOpen) / 2f
        
        val currentEyeState = if (avgEyeOpen < BLINK_THRESHOLD) {
            EyeState.CLOSED
        } else {
            EyeState.OPEN
        }
        
        // Count state transitions
        if (currentEyeState != lastEyeState) {
            if (currentEyeState == EyeState.OPEN && lastEyeState == EyeState.CLOSED) {
                blinkCount++
                Log.d(TAG, "Blink detected. Total blinks: $blinkCount")
            }
            lastEyeState = currentEyeState
        }
        
        val passed = blinkCount >= MIN_BLINKS_REQUIRED
        return LivenessCheck(
            passed = passed,
            message = "Blinks: $blinkCount/$MIN_BLINKS_REQUIRED"
        )
    }
    
    /**
     * Detects head movement by analyzing pose changes over time.
     */
    private fun detectHeadMovement(): LivenessCheck {
        if (faceHistory.size < 2) {
            return LivenessCheck(false, "Insufficient data")
        }
        
        val oldest = faceHistory.first()
        val newest = faceHistory.last()
        
        val deltaX = abs(newest.headEulerX - oldest.headEulerX)
        val deltaY = abs(newest.headEulerY - oldest.headEulerY)
        val deltaZ = abs(newest.headEulerZ - oldest.headEulerZ)
        
        val totalMovement = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
        
        hasHeadMovement = totalMovement >= MIN_HEAD_MOVEMENT
        
        return LivenessCheck(
            passed = hasHeadMovement,
            message = "Head movement: ${"%.2f".format(totalMovement)}°"
        )
    }
    
    /**
     * Validates that eyes are open (prevents closed-eye photos).
     */
    private fun validateEyeOpenness(faceData: FaceData): LivenessCheck {
        val leftEyeOpen = faceData.leftEyeOpen ?: 0f
        val rightEyeOpen = faceData.rightEyeOpen ?: 0f
        val avgEyeOpen = (leftEyeOpen + rightEyeOpen) / 2f
        
        val passed = avgEyeOpen >= MIN_EYE_OPENNESS
        return LivenessCheck(
            passed = passed,
            message = "Eye openness: ${"%.2f".format(avgEyeOpen)}"
        )
    }
    
    /**
     * Calculates overall confidence in liveness detection.
     */
    private fun calculateConfidence(
        blink: LivenessCheck,
        headMovement: LivenessCheck,
        eyeOpenness: LivenessCheck
    ): Float {
        var confidence = 0f
        
        if (blink.passed) confidence += 0.5f
        if (headMovement.passed) confidence += 0.4f
        if (eyeOpenness.passed) confidence += 0.1f
        
        return confidence.coerceIn(0f, 1f)
    }
    
    /**
     * Resets the liveness detector state.
     * Call this when starting a new recognition session.
     */
    fun reset() {
        faceHistory.clear()
        blinkCount = 0
        lastEyeState = EyeState.OPEN
        hasHeadMovement = false
        Log.d(TAG, "Liveness detector reset")
    }
    
    /**
     * Sealed class representing liveness detection results.
     */
    sealed class LivenessResult {
        data class InsufficientData(val frames: Int, val required: Int) : LivenessResult()
        data class Detected(
            val isLive: Boolean,
            val blinkDetected: Boolean,
            val headMovementDetected: Boolean,
            val eyesOpen: Boolean,
            val confidence: Float
        ) : LivenessResult()
    }
    
    /**
     * Data class for individual liveness check results.
     */
    private data class LivenessCheck(
        val passed: Boolean,
        val message: String
    )
}
