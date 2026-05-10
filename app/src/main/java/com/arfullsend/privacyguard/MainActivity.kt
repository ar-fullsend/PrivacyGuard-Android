package com.arfullsend.privacyguard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.arfullsend.privacyguard.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var faceDetector: FaceDetector
    private var isMonitoring = false
    private lateinit var cameraExecutor: ExecutorService

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                viewBinding.statusText.text = "Camera permission denied. Cannot monitor."
                viewBinding.statusText.setTextColor(0xFFFF0000.toInt())
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize ML Kit Face Detector for fast on-device performance
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.1f)
            .build()
        faceDetector = FaceDetection.getClient(options)

        viewBinding.toggleButton.setOnClickListener {
            if (isMonitoring) {
                stopMonitoring()
            } else {
                startMonitoring()
            }
        }

        viewBinding.dismissShieldButton.setOnClickListener {
            viewBinding.shieldOverlay.visibility = View.GONE
            if (isMonitoring) {
                viewBinding.statusText.text = "Monitoring resumed"
            }
        }

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startMonitoring() {
        isMonitoring = true
        viewBinding.toggleButton.text = getString(R.string.stop_monitoring)
        viewBinding.statusText.text = "Monitoring active..."
        viewBinding.statusText.setTextColor(0xFF00FF00.toInt())
        // Camera already started in onCreate, analysis runs when bound
    }

    private fun stopMonitoring() {
        isMonitoring = false
        viewBinding.toggleButton.text = getString(R.string.start_monitoring)
        viewBinding.shieldOverlay.visibility = View.GONE
        viewBinding.statusText.text = "Monitoring stopped"
        viewBinding.statusText.setTextColor(0xFFFFFFFF.toInt())
        // Keep camera bound for quick resume, or unbind if needed
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, FaceAnalyzer(faceDetector) { faceCount ->
                        runOnUiThread {
                            updateUI(faceCount)
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
                viewBinding.statusText.text = "Camera ready. Tap Start Monitoring"
            } catch (exc: Exception) {
                Log.e("PrivacyGuard", "Camera binding failed", exc)
                viewBinding.statusText.text = "Failed to start camera"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateUI(faceCount: Int) {
        if (!isMonitoring) return

        if (faceCount > 1) {
            viewBinding.shieldOverlay.visibility = View.VISIBLE
            viewBinding.statusText.text = "THREAT: $faceCount faces detected!"
            viewBinding.statusText.setTextColor(0xFFFF0000.toInt())
        } else {
            viewBinding.shieldOverlay.visibility = View.GONE
            val status = if (faceCount == 0) "No face detected" else "Single face - Secure"
            viewBinding.statusText.text = status
            viewBinding.statusText.setTextColor(0xFF00FF00.toInt())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        faceDetector.close()
    }

    // Inner class for real-time face analysis
    private class FaceAnalyzer(
        private val faceDetector: FaceDetector,
        private val onFacesDetected: (Int) -> Unit
    ) : ImageAnalysis.Analyzer {

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                faceDetector.process(image)
                    .addOnSuccessListener { faces ->
                        onFacesDetected(faces.size)
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        Log.e("PrivacyGuard", "Face detection failed", e)
                        imageProxy.close()
                    }
                    .addOnCompleteListener {
                        // Ensure proxy is closed even on unexpected paths
                        if (!imageProxy.isClosed) imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }
}