package com.example.qrcodescanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.*
import android.util.Patterns
import android.util.Size
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.qrcodescanner.databinding.ActivityMainBinding
import com.example.qrcodescanner.databinding.LayoutBottomSheetResultBinding
import com.example.qrcodescanner.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val utils = Utils()
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var camera: Camera? = null
    private val CAMERA_REQUEST = 100
    private var isFlashOn = false
    private var isBottomSheetShowing = false

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { scanImageFromUri(it) }
        }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        utils.bottomNavigationTransparent(this)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupButtons()
        checkAndRequestPermissions()
    }

    private fun setupButtons() {
        binding.flashBtn.setOnClickListener {
            if (hasCameraPermission()) {
                if (camera != null) {
                    toggleFlashLight()
                } else {
                    utils.toast("Initializing camera, please wait...", this)
                    startCamera()
                }
            } else {
                requestCameraPermission()
            }
        }

        binding.galleyBtn.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.qrCodeGenerateBtn.setOnClickListener {
            startActivity(Intent(this, GenerateQRCodeActivity::class.java))
        }

        binding.settingBtn.setOnClickListener {
           startActivity(Intent(this, SettingActivity::class.java))
        }
    }

    private fun hasCameraPermission(): Boolean = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                utils.toast("Permission Granted", this)
                startCamera()
            } else {
                utils.toast("Camera permission is required for this feature", this)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun toggleFlashLight() {
        val cameraControl = camera?.cameraControl
        val cameraInfo = camera?.cameraInfo

        if (cameraInfo?.hasFlashUnit() == true) {
            isFlashOn = !isFlashOn
            cameraControl?.enableTorch(isFlashOn)
            val iconRes = if (isFlashOn) R.drawable.flash_off_filled else R.drawable.baseline_flash_on_24
            binding.flashBtn.setImageResource(iconRes)
        } else {
            utils.toast("Flash Light not Supported or Camera not ready", this)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
                .also {
                    it.surfaceProvider = binding.PreviewView.surfaceProvider
                }

            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                ).build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                if (!isBottomSheetShowing) {
                    processImageProxy(imageProxy)
                } else {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                utils.toast("Use case binding failed: ${exc.message}", this)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        barcodes[0].rawValue?.let { value ->
                            runOnUiThread {
                                handleScanFeedback()
                                showResultBottomSheet(value)
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private fun handleScanFeedback() {
        val sharedPref = getSharedPreferences("settings", MODE_PRIVATE)
        val shouldVibrate = sharedPref.getBoolean("vibrate", true)
        val shouldBeep = sharedPref.getBoolean("beep", false)

        if (shouldVibrate) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        }

        if (shouldBeep) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun scanImageFromUri(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        barcodes[0].rawValue?.let { value ->
                            handleScanFeedback()
                            showResultBottomSheet(value)
                        }
                    } else {
                        utils.toast("No QR code found in image", this)
                    }
                }
                .addOnFailureListener {
                    utils.toast("Failed to scan image", this)
                }
        } catch (e: Exception) {
            utils.toast("Error processing image", this)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showResultBottomSheet(result: String) {
        if (isBottomSheetShowing) return
        isBottomSheetShowing = true

        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetBinding = LayoutBottomSheetResultBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        sheetBinding.resultText.text = result

        val isUrl = Patterns.WEB_URL.matcher(result).matches()

        if (isUrl) {
            sheetBinding.actionBtn.text = "Open Link"
            sheetBinding.actionBtn.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(if (result.startsWith("http")) result else "http://$result"))
                startActivity(intent)
                bottomSheetDialog.dismiss()
            }
        } else {
            sheetBinding.actionBtn.text = "Copy Text"
            sheetBinding.actionBtn.setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Scanned Text", result)
                clipboard.setPrimaryClip(clip)
                utils.toast("Text copied to clipboard", this)
                bottomSheetDialog.dismiss()
            }
        }

        bottomSheetDialog.setOnDismissListener {
            isBottomSheetShowing = false
        }

        bottomSheetDialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
