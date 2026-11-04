package com.faceguard.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner as ComposeLocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faceguard.app.RecognitionResult
import com.faceguard.app.viewmodel.FaceRecognitionViewModel
import com.faceguard.app.viewmodel.FaceRecognitionViewModelFactory
import com.faceguard.service.ActiveProfileManager
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

val RecognitionBg = Color(0xFF0B0B14)
val RecognitionCard = Color(0xFF131320)

@androidx.camera.core.ExperimentalGetImage
@Composable
fun RecognitionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = ComposeLocalLifecycleOwner.current
    
    val viewModel: FaceRecognitionViewModel = viewModel(
        factory = FaceRecognitionViewModelFactory(context)
    )
    
    val recognitionResult by viewModel.recognitionResult.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Update active profile when recognition succeeds
    LaunchedEffect(recognitionResult) {
        when (val result = recognitionResult) {
            is RecognitionResult.MatchFound -> {
                Log.d("RecognitionScreen", "========== FACE RECOGNIZED ==========")
                Log.d("RecognitionScreen", "Profile name: ${result.profile.name}")
                Log.d("RecognitionScreen", "Profile ID: ${result.profile.id}")
                Log.d("RecognitionScreen", "Confidence: ${(result.confidence * 100).toInt()}%")
                Log.d("RecognitionScreen", "Second best: ${result.secondBestName} @ ${"%.2f".format((result.secondBestScore ?: 0f) * 100)}%")
                Log.d("RecognitionScreen", "All similarities: ${result.allSimilarities.map { "${it.key}=${"%.2f".format(it.value * 100)}%" }}")
                Log.d("RecognitionScreen", "Setting active profile in ActiveProfileManager...")
                ActiveProfileManager.setActiveProfile(result.profile)
                Log.d("RecognitionScreen", "Active profile set successfully")
                // Reset liveness detector after successful recognition
                viewModel.resetLiveness()
            }
            is RecognitionResult.UnknownPerson -> {
                Log.d("RecognitionScreen", "========== UNKNOWN PERSON ==========")
                Log.d("RecognitionScreen", "Best match: ${result.bestMatchName} @ ${"%.2f".format((result.bestMatchScore ?: 0f) * 100)}%")
                Log.d("RecognitionScreen", "All similarities: ${result.allSimilarities.map { "${it.key}=${"%.2f".format(it.value * 100)}%" }}")
                Log.d("RecognitionScreen", "Scheduling active profile clear due to unknown person...")
                ActiveProfileManager.scheduleClearAfterTimeout()
                // Reset liveness detector on unknown person (new session)
                viewModel.resetLiveness()
            }
            is RecognitionResult.NoFace -> {
                Log.d("RecognitionScreen", "========== NO FACE DETECTED ==========")
                Log.d("RecognitionScreen", "Scheduling active profile clear due to no face...")
                ActiveProfileManager.scheduleClearAfterTimeout()
                // Reset liveness detector on no face (new session)
                viewModel.resetLiveness()
            }
            null -> {
                Log.d("RecognitionScreen", "========== NULL RESULT ==========")
                Log.d("RecognitionScreen", "Scheduling active profile clear due to null result...")
                ActiveProfileManager.scheduleClearAfterTimeout()
                // Reset liveness detector on null result (new session)
                viewModel.resetLiveness()
            }
        }
    }
    
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    var imageAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(Unit) {
        Log.d("RecognitionScreen", "Starting camera initialization...")
        try {
            val provider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider = provider
            Log.d("RecognitionScreen", "Camera provider initialized successfully")
        } catch (e: Exception) {
            Log.e("RecognitionScreen", "Failed to initialize camera provider", e)
        }
    }

    // Bind camera when both provider and previewView are ready
    LaunchedEffect(cameraProvider, previewView) {
        if (cameraProvider != null && previewView != null) {
            Log.d("RecognitionScreen", "Both provider and previewView ready, binding camera")
            try {
                preview?.let {
                    Log.d("RecognitionScreen", "Unbinding previous preview")
                    cameraProvider!!.unbindAll()
                }

                Log.d("RecognitionScreen", "Building Preview use case")
                preview = Preview.Builder()
                    .build()
                    .also {
                        Log.d("RecognitionScreen", "Setting surface provider")
                        it.setSurfaceProvider(previewView!!.surfaceProvider)
                    }

                Log.d("RecognitionScreen", "Building ImageAnalysis use case for recognition")
                // Use a background executor for image analysis to avoid blocking main thread
                val cameraExecutor = Executors.newSingleThreadExecutor()
                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            Log.d("RecognitionScreen", "Frame received for analysis")
                            
                            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                            val mediaImage = imageProxy.image
                            
                            if (mediaImage != null) {
                                // Convert ImageProxy to Bitmap for ML Kit processing
                                val bitmap = imageProxyToBitmap(imageProxy)
                                if (bitmap != null) {
                                    viewModel.recognizeFromBitmap(bitmap)
                                }
                            }
                            
                            imageProxy.close()
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                Log.d("RecognitionScreen", "Using front camera selector")

                Log.d("RecognitionScreen", "Unbinding all use cases before binding")
                cameraProvider!!.unbindAll()
                Log.d("RecognitionScreen", "Binding camera to lifecycle with preview and analysis")
                cameraProvider!!.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                Log.d("RecognitionScreen", "Camera bound successfully for recognition")
            } catch (e: Exception) {
                Log.e("RecognitionScreen", "Failed to bind camera to lifecycle", e)
            }
        }
    }

    if (!hasCameraPermission) {
        RecognitionPermissionRequestDialog(
            onRequestPermission = {
                hasCameraPermission = true
            },
            onDismiss = onBack
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RecognitionBg)
    ) {
        AndroidView(
            factory = { ctx ->
                Log.d("RecognitionScreen", "Creating PreviewView")
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also {
                    previewView = it
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Back button
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5577),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Text("←", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Recognition result card at bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RecognitionCard)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val result = recognitionResult) {
                        null -> {
                            if (isProcessing) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF00C878),
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        "Scanning for faces...",
                                        color = Color(0xFFE4E4F4),
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                Text(
                                    "Initializing camera...",
                                    color = Color(0xFF44446A),
                                    fontSize = 14.sp
                                )
                            }
                        }
                        is RecognitionResult.MatchFound -> {
                            Text(
                                "✓ Recognized",
                                color = Color(0xFF00C878),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                result.profile.name,
                                color = Color(0xFFE4E4F4),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Confidence: ${(result.confidence * 100).toInt()}%",
                                color = Color(0xFF44446A),
                                fontSize = 12.sp
                            )
                            if (result.secondBestName != null && result.secondBestScore != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "2nd best: ${result.secondBestName} (${(result.secondBestScore * 100).toInt()}%)",
                                    color = Color(0xFF666688),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        is RecognitionResult.UnknownPerson -> {
                            Text(
                                "⚠ Unknown Person",
                                color = Color(0xFFFF5577),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Face detected but no match found",
                                color = Color(0xFF44446A),
                                fontSize = 12.sp
                            )
                            if (result.bestMatchName != null && result.bestMatchScore != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Best match: ${result.bestMatchName} (${(result.bestMatchScore * 100).toInt()}%)",
                                    color = Color(0xFF666688),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        is RecognitionResult.NoFace -> {
                            Text(
                                "No Face Detected",
                                color = Color(0xFF44446A),
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            error,
                            color = Color(0xFFFF5577),
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RecognitionPermissionRequestDialog(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RecognitionCard,
        title = { Text("Camera Permission", color = Color(0xFFE4E4F4), fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Camera permission is required for face recognition.",
                color = Color(0xFF44446A),
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C878), contentColor = RecognitionBg)
            ) {
                Text("Grant", fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF44446A))
            }
        }
    )
}

/**
 * Converts an ImageProxy to a Bitmap for ML Kit processing.
 * This handles YUV to RGB conversion for camera frames.
 */
private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    return try {
        val nv21Buffer = yuvToNv21(imageProxy)
        val yuvImage = YuvImage(nv21Buffer, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val outputStream = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, outputStream)
        val jpegBytes = outputStream.toByteArray()
        android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    } catch (e: Exception) {
        Log.e("RecognitionScreen", "Failed to convert ImageProxy to Bitmap", e)
        null
    }
}

/**
 * Converts ImageProxy YUV data to NV21 format.
 */
private fun yuvToNv21(imageProxy: ImageProxy): ByteArray {
    val yBuffer = imageProxy.planes[0].buffer
    val uBuffer = imageProxy.planes[1].buffer
    val vBuffer = imageProxy.planes[2].buffer
    
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    
    val nv21 = ByteArray(ySize + uSize + vSize)
    
    yBuffer.get(nv21, 0, ySize)
    
    val uvPixelStride = imageProxy.planes[1].pixelStride
    val uvRowStride = imageProxy.planes[1].rowStride
    
    // U and V planes are interleaved in NV21 format
    var uvIndex = ySize
    for (row in 0 until imageProxy.height / 2) {
        for (col in 0 until imageProxy.width / 2) {
            val uIndex = row * uvRowStride + col * uvPixelStride
            val vIndex = row * uvRowStride + col * uvPixelStride
            if (uIndex < uSize && vIndex < vSize) {
                nv21[uvIndex++] = vBuffer.get(vIndex)
                nv21[uvIndex++] = uBuffer.get(uIndex)
            }
        }
    }
    
    return nv21
}
