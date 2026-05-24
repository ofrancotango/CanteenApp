package com.example.canteen.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun QRScannerScreen(
    onQrCodeScanned: (String) -> Unit,
    onCancel: () -> Unit,
    scanCount: Int // NEW Parameter
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
                    
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, QrCodeAnalyzer { qrCode ->
                                    // Switch to main thread to call callback
                                    previewView.post {
                                        onQrCodeScanned(qrCode)
                                    }
                                })
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalyzer
                            )
                        } catch (exc: Exception) {
                            // Handle exceptions
                        }

                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // LIVE COUNTER OVERLAY
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            ) {
                Text(
                    text = "Today: $scanCount",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            ) {
                Text("Cancel")
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Camera permission is required to scan QR codes.")
            Button(onClick = onCancel, modifier = Modifier.padding(top = 16.dp)) {
                Text("Back")
            }
        }
    }
}
