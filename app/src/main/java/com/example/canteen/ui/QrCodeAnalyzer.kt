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

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        if (barcode.valueType == Barcode.TYPE_TEXT || barcode.valueType == Barcode.TYPE_UNKNOWN) {
                            // Raw value might be null, but usually displayValue or rawValue has the string
                            val rawValue = barcode.rawValue
                            if (!rawValue.isNullOrBlank()) {
                                onQrCodeScanned(rawValue)
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
