package com.faceguard.app

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FaceScanner(private val context: Context) {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

    // Capture one frame and extract face vector
    suspend fun scanFace(lifecycleOwner: LifecycleOwner): FloatArray? =
        suspendCancellableCoroutine { cont ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, imageCapture
                    )

                    imageCapture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    detector.process(image)
                                        .addOnSuccessListener { faces ->
                                            if (faces.isNotEmpty()) {
                                                val face = faces[0]
                                                val vector = extractFaceVector(face)
                                                cont.resume(vector)
                                            } else {
                                                Log.d("FaceScanner", "No face detected")
                                                cont.resume(null)
                                            }
                                            imageProxy.close()
                                            cameraProvider.unbindAll()
                                        }
                                        .addOnFailureListener {
                                            Log.e("FaceScanner", "Detection failed", it)
                                            imageProxy.close()
                                            cameraProvider.unbindAll()
                                            cont.resume(null)
                                        }
                                } else {
                                    imageProxy.close()
                                    cont.resume(null)
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("FaceScanner", "Capture error", exception)
                                cont.resume(null)
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e("FaceScanner", "Camera bind error", e)
                    cont.resume(null)
                }
            }, ContextCompat.getMainExecutor(context))
        }

    // Convert face landmarks into a float vector
    private fun extractFaceVector(
        face: com.google.mlkit.vision.face.Face
    ): FloatArray {
        val box = face.boundingBox
        return floatArrayOf(
            box.centerX().toFloat(),
            box.centerY().toFloat(),
            box.width().toFloat(),
            box.height().toFloat(),
            face.headEulerAngleX,
            face.headEulerAngleY,
            face.headEulerAngleZ,
            face.smilingProbability ?: 0f,
            face.leftEyeOpenProbability ?: 0f,
            face.rightEyeOpenProbability ?: 0f,
        )
    }
}