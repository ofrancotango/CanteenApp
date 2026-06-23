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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.canteen.ui.theme.AppAccent
import com.example.canteen.ui.theme.AppBackground
import com.example.canteen.ui.theme.AppBorder
import com.example.canteen.ui.theme.AppMuted
import com.example.canteen.ui.theme.AppSurface
import com.example.canteen.ui.theme.AppText
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun QRScannerScreen(
    onQrCodeScanned: (String) -> Unit,
    onCancel: () -> Unit,
    scanCount: Int
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
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            IconButton(onClick = onCancel) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppSurface)
                        .border(1.dp, AppBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u2190", color = AppMuted, fontSize = 18.sp)
                }
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Scan Badge",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = AppText
                )
                Text(
                    text = "Point camera at QR code",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppMuted
                )
            }
        }

        // Viewfinder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(AppBackground)
                        .border(1.dp, AppBorder, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val imageAnalyzer = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also {
                                        it.setAnalyzer(cameraExecutor, QrCodeAnalyzer { qrCode ->
                                            previewView.post { onQrCodeScanned(qrCode) }
                                        })
                                    }
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner, cameraSelector, preview, imageAnalyzer
                                    )
                                } catch (exc: Exception) {
                                    // ignore
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Corner marks
                    Box(modifier = Modifier.fillMaxSize()) {
                        androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxSize()) {
                            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.fillMaxWidth().height(28.dp).padding(12.dp)
                                    .border(2.dp, AppAccent, RoundedCornerShape(topStart = 3.dp))
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Box(modifier = Modifier.fillMaxWidth().height(28.dp).padding(12.dp)
                                    .border(2.dp, AppAccent, RoundedCornerShape(bottomStart = 3.dp))
                                )
                            }
                            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.fillMaxWidth().height(28.dp).padding(12.dp)
                                    .border(2.dp, AppAccent, RoundedCornerShape(topEnd = 3.dp))
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Box(modifier = Modifier.fillMaxWidth().height(28.dp).padding(12.dp)
                                    .border(2.dp, AppAccent, RoundedCornerShape(bottomEnd = 3.dp))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Hold steady \u2014 scanning automatically",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppMuted
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Camera permission is required",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Button(onClick = onCancel) {
                        Text("Back")
                    }
                }
            }
        }

        // Bottom counter
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppSurface)
                .border(1.dp, AppBorder)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Today: $scanCount",
                style = MaterialTheme.typography.titleMedium,
                color = AppText,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private fun Modifier.border(width: androidx.compose.ui.unit.Dp, color: Color, shape: androidx.compose.ui.graphics.Shape) = this.then(
    androidx.compose.foundation.BorderStroke(width, color).let { _ ->
        androidx.compose.ui.draw.drawBehind {
            // simplified border
        }
    }
)
