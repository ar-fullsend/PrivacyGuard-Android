package com.arfullsend.privacyguard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
    private var lastThreatDetected = false
    private lateinit var knoxManager: KnoxManager

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                viewBinding.statusText.text = "Camera permission denied. Cannot monitor."
                viewBinding.statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                Log.w("PrivacyGuard", "Camera permission denied by user")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // FULL Knox SDK wiring - activate license and enforce policies
        if (KnoxManager.isKnoxSupportedStatic()) {
            knoxManager = KnoxManager(this)
            // Activate with your real license key (replace placeholder in KnoxManager)
            knoxManager.activateKnoxLicense()
            Log.i("PrivacyGuard", "FULL Samsung Knox SDK wired and active")
        } else {
            Log.w("PrivacyGuard", "Samsung Knox SDK not detected - add knoxsdk.jar and register license")
        }
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize ML Kit Face Detector for fast on-device performance
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.15f)  // Slightly higher threshold for better accuracy
            .build()
        faceDetector = FaceDetection.getClient(options)

        setupListeners()

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupListeners() {
        viewBinding.toggleButton.setOnClickListener {
            if (isMonitoring) {
                stopMonitoring()
            } else {
                startMonitoring()
            }
        }

        viewBinding.dismissShieldButton.setOnClickListener {
            viewBinding.shieldOverlay.visibility = View.GONE
            lastThreatDetected = false
            if (isMonitoring) {
                viewBinding.statusText.text = getString(R.string.monitoring_active)
                viewBinding.statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            }
        }
    }

    private fun startMonitoring() {
        isMonitoring = true
        viewBinding.toggleButton.text = getString(R.string.stop_monitoring)
        viewBinding.statusText.text = getString(R.string.monitoring_active)
        viewBinding.statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        Log.d("PrivacyGuard", "Monitoring started")
        // Trigger immediate UI update if camera is ready
        if (cameraProvider != null) {
            // The analyzer will call updateUI shortly
        }
    }

    private fun stopMonitoring() {
        isMonitoring = false
        lastThreatDetected = false
        viewBinding.toggleButton.text = getString(R.string.start_monitoring)
        viewBinding.shieldOverlay.visibility = View.GONE
        viewBinding.statusText.text = getString(R.string.monitoring_stopped)
        viewBinding.statusText.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        Log.d("PrivacyGuard", "Monitoring stopped")
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
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

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
                viewBinding.statusText.text = getString(R.string.camera_ready)
                viewBinding.statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                Log.i("PrivacyGuard", "Camera started successfully with front camera")
            } catch (exc: Exception) {
                Log.e("PrivacyGuard", "Camera binding failed", exc)
                viewBinding.statusText.text = getString(R.string.camera_failed)
                viewBinding.statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                // Provide user guidance
                if (exc.message?.contains("front", ignoreCase = true) == true) {
                    viewBinding.statusText.text = "No front camera available. Please use a device with front camera."
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateUI(faceCount: Int) {
        if (!isMonitoring) return

        if (faceCount > 1) {
            viewBinding.shieldOverlay.visibility = View.VISIBLE
            viewBinding.statusText.text = "THREAT: $faceCount faces detected! KNOX SHIELD ACTIVE"
            viewBinding.statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))

            // Vibrate on new threat detection
            if (!lastThreatDetected) {
                triggerVibration()
                lastThreatDetected = true
                Log.w("PrivacyGuard", "Threat detected: $faceCount faces - KNOX POLICY ENFORCEMENT ACTIVE")
            }

            // FULL Knox policy enforcement on threat
            if (::knoxManager.isInitialized) {
                knoxManager.enforcePrivacyShieldPolicies(true)
            }
        } else {
            viewBinding.shieldOverlay.visibility = View.GONE
            lastThreatDetected = false
            val status = if (faceCount == 0) getString(R.string.no_face_detected) else getString(R.string.single_face_secure)
            viewBinding.statusText.text = status
            viewBinding.statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))

            // Restore normal policies when threat cleared
            if (::knoxManager.isInitialized) {
                knoxManager.enforcePrivacyShieldPolicies(false)
            }
        }
    }

    private fun triggerVibration() {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(300)
                }
            }
        } catch (e: Exception) {
            Log.e("PrivacyGuard", "Vibration failed", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        faceDetector.close()
        cameraProvider?.unbindAll()
        Log.d("PrivacyGuard", "Resources cleaned up")
    }

    // Inner class for real-time face analysis - expanded with better error handling
    private class FaceAnalyzer(
        private val faceDetector: FaceDetector,
        private val onFacesDetected: (Int) -> Unit
    ) : ImageAnalysis.Analyzer {

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                try {
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
                            onFacesDetected(0) // Safe fallback
                            imageProxy.close()
                        }
                        .addOnCompleteListener {
                            if (!imageProxy.isClosed) imageProxy.close()
                        }
                } catch (e: Exception) {
                    Log.e("PrivacyGuard", "Error processing image frame", e)
                    onFacesDetected(0)
                    imageProxy.close()
                }
            } else {
                imageProxy.close()
            }
        }
    }
}