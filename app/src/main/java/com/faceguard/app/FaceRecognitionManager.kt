package com.faceguard.app

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.faceguard.data.database.FaceGuardDatabase
import com.faceguard.data.database.Profile
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.sqrt

/**
 * Manager class responsible for face recognition operations.
 *
 * This class handles:
 * - Accepting camera images or bitmaps for face detection
 * - Using ML Kit Face Detection with FaceMesh to extract detailed facial landmarks
 * - Loading enrolled profiles from database
 * - Comparing face landmark vectors using cosine similarity
 * - Returning match results based on threshold
 */
class FaceRecognitionManager(private val context: Context) {

    companion object {
        // Threshold for face matching (0.0 to 1.0)
        // Higher values require more similarity for a match
        // Adjusted based on observed similarity scores from ML Kit face landmarks
        private const val MATCH_THRESHOLD = 0.85f

        // Minimum confidence gap between top and second-best match
        // Prevents ambiguous matches where profiles are too similar
        // Increased to 0.10f (10%) to ensure reliable discrimination between different people
        private const val MIN_CONFIDENCE_GAP = 0.10f

        // Minimum face size for enrollment (pixels)
        private const val MIN_FACE_SIZE = 100

        // Maximum face size for enrollment (pixels)
        private const val MAX_FACE_SIZE = 800
    }

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()
    )
    
    private val livenessDetector = LivenessDetector()

    /**
     * Processes a bitmap image to perform face recognition.
     * 
     * @param bitmap The bitmap image to process
     * @return RecognitionResult indicating match status
     */
    suspend fun recognizeFace(bitmap: Bitmap): RecognitionResult {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            processImageForRecognition(image)
        } catch (e: Exception) {
            Log.e("FaceRecognitionManager", "Error processing bitmap", e)
            RecognitionResult.NoFace("Failed to process image: ${e.message}")
        }
    }

    /**
     * Processes camera image data (YUV format) for face recognition.
     * 
     * @param imageProxy The camera image proxy from CameraX
     * @param rotationDegrees The rotation of the image
     * @return RecognitionResult indicating match status
     */
    suspend fun recognizeFace(
        imageProxy: android.media.Image,
        rotationDegrees: Int
    ): RecognitionResult {
        return try {
            val image = InputImage.fromMediaImage(imageProxy, rotationDegrees)
            processImageForRecognition(image)
        } catch (e: Exception) {
            Log.e("FaceRecognitionManager", "Error processing camera image", e)
            RecognitionResult.NoFace("Failed to process camera image: ${e.message}")
        }
    }

    /**
     * Internal method to process InputImage and perform face recognition.
     *
     * This method:
     * 1. Loads enrolled profiles from database
     * 2. Uses ML Kit to detect faces in the image
     * 3. Performs liveness detection
     * 4. Extracts face vectors from detected faces
     * 5. Compares face vectors using cosine similarity
     * 6. Returns MatchFound if similarity exceeds threshold, otherwise UnknownPerson
     *
     * @param image The ML Kit InputImage to process
     * @return RecognitionResult with the match outcome
     */
    private suspend fun processImageForRecognition(image: InputImage): RecognitionResult {
        // Load enrolled profiles from database first
        val database = FaceGuardDatabase.getDatabase(context)
        val profiles = try {
            database.profileDao().getAll().first()
        } catch (e: Exception) {
            Log.e("FaceRecognitionManager", "Failed to load profiles", e)
            emptyList<Profile>()
        }

        Log.d("FaceRecognitionManager", "Recognition: ${profiles.size} profiles, threshold=$MATCH_THRESHOLD, min_gap=$MIN_CONFIDENCE_GAP")

        return suspendCancellableCoroutine { cont ->
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isEmpty()) {
                        Log.d("FaceRecognitionManager", "No face detected")
                        cont.resume(RecognitionResult.NoFace())
                        return@addOnSuccessListener
                    }

                    // Process the first detected face
                    val face = faces[0]
                    
                    // Perform liveness detection
                    val livenessResult = livenessDetector.processFace(face)
                    
                    when (livenessResult) {
                        is LivenessDetector.LivenessResult.InsufficientData -> {
                            // Continue with recognition
                        }
                        is LivenessDetector.LivenessResult.Detected -> {
                            if (!livenessResult.isLive && livenessResult.confidence > 0.7f) {
                                Log.d("FaceRecognitionManager", "Spoof detected - liveness check failed")
                                cont.resume(RecognitionResult.NoFace("Liveness check failed - possible photo spoof"))
                                return@addOnSuccessListener
                            }
                        }
                    }
                    
                    val faceVector = extractFaceVector(face)

                    if (profiles.isEmpty()) {
                        cont.resume(RecognitionResult.UnknownPerson(faceVector))
                        return@addOnSuccessListener
                    }

                    // Compare with all enrolled profiles
                    val similarities = mutableMapOf<String, Float>()
                    var bestMatch: Profile? = null
                    var bestSimilarity = 0f
                    var secondBestMatch: Profile? = null
                    var secondBestSimilarity = 0f

                    for (profile in profiles) {
                        val storedVector = profile.faceVector
                        if (storedVector == null || storedVector.isEmpty()) {
                            continue
                        }

                        val storedFloatArray = byteArrayToFloatArray(storedVector)
                        val similarity = compareFaceVectors(faceVector, storedFloatArray)
                        similarities[profile.name] = similarity

                        // Update best and second-best matches
                        if (similarity > bestSimilarity) {
                            secondBestMatch = bestMatch
                            secondBestSimilarity = bestSimilarity
                            bestSimilarity = similarity
                            bestMatch = profile
                        } else if (similarity > secondBestSimilarity) {
                            secondBestMatch = profile
                            secondBestSimilarity = similarity
                        }
                    }

                    // Log all similarity scores for debugging and threshold tuning
                    Log.d("FaceRecognitionManager", "=== Similarity Scores (Threshold: $MATCH_THRESHOLD, Min Gap: $MIN_CONFIDENCE_GAP) ===")
                    similarities.forEach { (name, score) ->
                        val status = if (score >= MATCH_THRESHOLD) "✓" else "✗"
                        Log.d("FaceRecognitionManager", "  $status $name: ${"%.4f".format(score)} (${"%.1f".format(score * 100)}%)")
                    }
                    Log.d("FaceRecognitionManager", "Best: ${bestMatch?.name} @ ${"%.4f".format(bestSimilarity)} (${"%.1f".format(bestSimilarity * 100)}%)")
                    Log.d("FaceRecognitionManager", "Second: ${secondBestMatch?.name} @ ${"%.4f".format(secondBestSimilarity)} (${"%.1f".format(secondBestSimilarity * 100)}%)")
                    Log.d("FaceRecognitionManager", "Gap: ${"%.4f".format(bestSimilarity - secondBestSimilarity)} (${"%.1f".format((bestSimilarity - secondBestSimilarity) * 100)}%)")

                    // Check if best match exceeds threshold AND has sufficient confidence gap
                    val confidenceGap = bestSimilarity - secondBestSimilarity
                    val meetsThreshold = bestMatch != null && bestSimilarity >= MATCH_THRESHOLD
                    val hasConfidenceGap = confidenceGap >= MIN_CONFIDENCE_GAP || secondBestMatch == null

                    if (meetsThreshold && hasConfidenceGap) {
                        Log.d("FaceRecognitionManager", "✓ MATCH: ${bestMatch!!.name} (score: ${"%.4f".format(bestSimilarity)}, gap: ${"%.4f".format(confidenceGap)})")
                        cont.resume(RecognitionResult.MatchFound(
                            profile = bestMatch!!,
                            confidence = bestSimilarity,
                            allSimilarities = similarities.toMap(),
                            secondBestName = secondBestMatch?.name,
                            secondBestScore = secondBestSimilarity.takeIf { secondBestMatch != null }
                        ))
                    } else {
                        val reason = when {
                            !meetsThreshold -> "below threshold (${"%.4f".format(bestSimilarity)} < $MATCH_THRESHOLD)"
                            !hasConfidenceGap -> "insufficient confidence gap (${"%.4f".format(confidenceGap)} < $MIN_CONFIDENCE_GAP)"
                            else -> "unknown"
                        }
                        Log.d("FaceRecognitionManager", "✗ NO MATCH: $reason")
                        cont.resume(RecognitionResult.UnknownPerson(
                            faceVector = faceVector,
                            allSimilarities = similarities.toMap(),
                            bestMatchName = bestMatch?.name,
                            bestMatchScore = bestSimilarity.takeIf { bestMatch != null }
                        ))
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FaceRecognitionManager", "Face detection failed", e)
                    cont.resume(RecognitionResult.NoFace("Detection failed: ${e.message}"))
                }
        }
    }

    /**
     * Extracts a face vector from ML Kit Face detection results using facial landmarks.
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
     * Compares two face vectors using cosine similarity.
     *
     * Cosine similarity measures the cosine of the angle between two vectors,
     * ranging from -1 (opposite) to 1 (identical). For face recognition,
     * we normalize to 0-1 range where higher values indicate more similarity.
     *
     * @param vector1 First face vector
     * @param vector2 Second face vector
     * @return Similarity score between 0.0 and 1.0
     */
    private fun compareFaceVectors(vector1: FloatArray, vector2: FloatArray): Float {
        if (vector1.size != vector2.size) return 0f

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in vector1.indices) {
            dotProduct += vector1[i] * vector2[i]
            norm1 += vector1[i] * vector1[i]
            norm2 += vector2[i] * vector2[i]
        }

        if (norm1 == 0f || norm2 == 0f) return 0f

        val similarity = dotProduct / (sqrt(norm1) * sqrt(norm2))
        // Normalize from [-1, 1] to [0, 1] range
        return (similarity + 1f) / 2f
    }

    /**
     * Converts a ByteArray to FloatArray for face vector comparison.
     *
     * This method reverses the conversion done during enrollment,
     * converting each 4-byte sequence back to a float.
     *
     * The conversion process:
     * 1. Wrap ByteArray in ByteBuffer
     * 2. Read each float (4 bytes) from the buffer
     * 3. Return as FloatArray
     *
     * @param byteArray The ByteArray to convert
     * @return FloatArray representation of the byte array
     */
    private fun byteArrayToFloatArray(byteArray: ByteArray): FloatArray {
        val byteBuffer = ByteBuffer.wrap(byteArray)
        val floatArray = FloatArray(byteArray.size / 4)
        for (i in floatArray.indices) {
            floatArray[i] = byteBuffer.float
        }
        return floatArray
    }

    /**
     * Releases resources used by the detector.
     * Call this when the manager is no longer needed.
     */
    fun release() {
        detector.close()
        livenessDetector.reset()
    }
    
    /**
     * Resets the liveness detector state.
     * Call this when starting a new recognition session.
     */
    fun resetLiveness() {
        livenessDetector.reset()
    }
}
