package com.example.canteen.ui

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private var lastScannedCode: String? = null
    private var lastScanTime: Long = 0L
    var debounceMs: Long = 3000L

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val now = System.currentTimeMillis()
                    for (barcode in barcodes) {
                        if (barcode.valueType == Barcode.TYPE_TEXT || barcode.valueType == Barcode.TYPE_UNKNOWN) {
                            val rawValue = barcode.rawValue
                            if (!rawValue.isNullOrBlank()) {
                                // Debounce: ignore same code within debounceMs
                                if (rawValue != lastScannedCode || (now - lastScanTime) > debounceMs) {
                                    lastScannedCode = rawValue
                                    lastScanTime = now
                                    onQrCodeScanned(rawValue)
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    // unexpected failure
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
