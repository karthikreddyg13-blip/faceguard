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
 * - Using ML Kit Face Detection to extract face information
 * - Loading enrolled profiles from database
 * - Comparing face vectors using cosine similarity
 * - Returning match results based on threshold
 */
class FaceRecognitionManager(private val context: Context) {

    companion object {
        // Threshold for face matching (0.0 to 1.0)
        // Higher values require more similarity for a match
        private const val MATCH_THRESHOLD = 0.85f
    }

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

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
     * 3. Extracts face vectors from detected faces
     * 4. Compares face vectors using cosine similarity
     * 5. Returns MatchFound if similarity exceeds threshold, otherwise UnknownPerson
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
                    val faceVector = extractFaceVector(face)

                    if (profiles.isEmpty()) {
                        Log.d("FaceRecognitionManager", "No enrolled profiles found")
                        cont.resume(RecognitionResult.UnknownPerson(faceVector))
                        return@addOnSuccessListener
                    }

                    // Compare with all enrolled profiles
                    var bestMatch: Profile? = null
                    var bestSimilarity = 0f

                    for (profile in profiles) {
                        val storedVector = profile.faceVector
                        if (storedVector == null || storedVector.isEmpty()) {
                            continue
                        }

                        val storedFloatArray = byteArrayToFloatArray(storedVector)
                        val similarity = compareFaceVectors(faceVector, storedFloatArray)

                        Log.d("FaceRecognitionManager", "Profile: ${profile.name}, similarity: $similarity")

                        if (similarity > bestSimilarity) {
                            bestSimilarity = similarity
                            bestMatch = profile
                        }
                    }

                    // Check if best match exceeds threshold
                    if (bestMatch != null && bestSimilarity >= MATCH_THRESHOLD) {
                        Log.d("FaceRecognitionManager", "Match found: ${bestMatch.name} with similarity $bestSimilarity")
                        cont.resume(RecognitionResult.MatchFound(bestMatch, bestSimilarity))
                    } else {
                        Log.d("FaceRecognitionManager", "No match found. Best similarity: $bestSimilarity")
                        cont.resume(RecognitionResult.UnknownPerson(faceVector))
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FaceRecognitionManager", "Face detection failed", e)
                    cont.resume(RecognitionResult.NoFace("Detection failed: ${e.message}"))
                }
        }
    }

    /**
     * Extracts a face vector from ML Kit Face detection results.
     * 
     * The vector includes:
     * - Bounding box coordinates (center X, center Y, width, height)
     * - Head Euler angles (X, Y, Z rotation)
     * - Facial classification probabilities (smiling, left eye open, right eye open)
     * 
     * TODO: This is a basic feature vector. For production use, consider:
     * - Using ML Kit's FaceMesh for more detailed landmarks
     * - Implementing a deep learning embedding model for better recognition
     * - Adding more facial features for improved accuracy
     * 
     * @param face The ML Kit Face object
     * @return FloatArray representing the face feature vector
     */
    private fun extractFaceVector(face: Face): FloatArray {
        val box = face.boundingBox
        return floatArrayOf(
            // Bounding box features
            box.centerX().toFloat(),
            box.centerY().toFloat(),
            box.width().toFloat(),
            box.height().toFloat(),
            // Head pose angles
            face.headEulerAngleX,
            face.headEulerAngleY,
            face.headEulerAngleZ,
            // Classification probabilities
            face.smilingProbability ?: 0f,
            face.leftEyeOpenProbability ?: 0f,
            face.rightEyeOpenProbability ?: 0f
        )
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
    }
}
