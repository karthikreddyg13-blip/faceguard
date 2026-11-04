package com.faceguard.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CameraScreen(
    onPhotoCaptured: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = ComposeLocalLifecycleOwner.current
    
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
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
        Log.d("CameraScreen", "Starting camera initialization...")
        try {
            val provider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider = provider
            Log.d("CameraScreen", "Camera provider initialized successfully")
        } catch (e: Exception) {
            Log.e("CameraScreen", "Failed to initialize camera provider", e)
        }
    }

    // Bind camera when both provider and previewView are ready
    LaunchedEffect(cameraProvider, previewView) {
        if (cameraProvider != null && previewView != null) {
            Log.d("CameraScreen", "Both provider and previewView ready, binding camera")
            try {
                preview?.let {
                    Log.d("CameraScreen", "Unbinding previous preview")
                    cameraProvider!!.unbindAll()
                }

                Log.d("CameraScreen", "Building Preview use case")
                preview = Preview.Builder()
                    .build()
                    .also {
                        Log.d("CameraScreen", "Setting surface provider")
                        it.setSurfaceProvider(previewView!!.surfaceProvider)
                    }

                Log.d("CameraScreen", "Building ImageCapture use case")
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                Log.d("CameraScreen", "Using front camera selector")

                Log.d("CameraScreen", "Unbinding all use cases before binding")
                cameraProvider!!.unbindAll()
                Log.d("CameraScreen", "Binding camera to lifecycle")
                cameraProvider!!.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                Log.d("CameraScreen", "Camera bound successfully")
            } catch (e: Exception) {
                Log.e("CameraScreen", "Failed to bind camera to lifecycle", e)
            }
        }
    }

    if (!hasCameraPermission) {
        PermissionRequestDialog(
            onRequestPermission = {
                hasCameraPermission = true
            },
            onDismiss = onDismiss
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B14))
    ) {
        AndroidView(
            factory = { ctx ->
                Log.d("CameraScreen", "Creating PreviewView")
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also {
                    previewView = it
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131320).copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            Log.d("CameraScreen", "Capture button pressed, imageCapture: ${imageCapture != null}")
                            imageCapture?.let { capture ->
                                Log.d("CameraScreen", "Starting photo capture")
                                capturePhoto(
                                    context = context,
                                    imageCapture = capture,
                                    onPhotoCaptured = { path ->
                                        Log.d("CameraScreen", "Photo captured successfully: $path")
                                        onPhotoCaptured(path)
                                    }
                                )
                            } ?: run {
                                Log.e("CameraScreen", "ImageCapture is null, cannot capture photo")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C878),
                            contentColor = Color(0xFF0B0B14)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Capture", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5577),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PermissionRequestDialog(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131320),
        title = { Text("Camera Permission", color = Color(0xFFE4E4F4), fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Camera permission is required to capture profile photos.",
                color = Color(0xFF44446A),
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C878), contentColor = Color(0xFF0B0B14))
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

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onPhotoCaptured: (String) -> Unit
) {
    Log.d("CameraScreen", "capturePhoto called")
    val photoFile = createImageFile(context)
    Log.d("CameraScreen", "Photo file created: ${photoFile.absolutePath}")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Log.d("CameraScreen", "Image saved successfully to: ${photoFile.absolutePath}")
                onPhotoCaptured(photoFile.absolutePath)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Failed to save image", exception)
            }
        }
    )
}

private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
    val storageDir = context.filesDir
    return File(storageDir, "profile_${timeStamp}.jpg")
}
