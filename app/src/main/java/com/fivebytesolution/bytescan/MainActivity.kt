package com.fivebytesolution.bytescan

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
import androidx.lifecycle.lifecycleScope
import com.fivebytesolution.bytescan.database.AppDatabase
import com.fivebytesolution.bytescan.databinding.ActivityMainBinding
import com.fivebytesolution.bytescan.databinding.LayoutBottomSheetResultBinding
import com.fivebytesolution.bytescan.model.ScanHistory
import com.fivebytesolution.bytescan.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val utils = Utils()
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var camera: Camera? = null
    private val CAMERA_REQUEST = 100
    private var isFlashOn = false
    private var isBottomSheetShowing = false

    private var pendingWifi: WifiData? = null
    data class WifiData(val ssid: String, val password: String?, val encryptionType: Int)

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { scanImageFromUri(it) }
        }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Restore pending WiFi data if process was killed
        savedInstanceState?.let {
            val ssid = it.getString("pending_ssid")
            if (ssid != null) {
                pendingWifi = WifiData(
                    ssid,
                    it.getString("pending_password"),
                    it.getInt("pending_encryption")
                )
            }
        }

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

        binding.historyFab.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
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
        } else if (requestCode == 200) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingWifi?.let {
                    utils.connectToWifi(this, it.ssid, it.password, it.encryptionType)
                    pendingWifi = null
                }
            } else {
                utils.toast("Location permission is required to connect to WiFi", this)
                pendingWifi = null
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
                        val barcode = barcodes[0]
                        runOnUiThread {
                            handleScanFeedback()
                            showResultBottomSheet(barcode)
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
                        val barcode = barcodes[0]
                        handleScanFeedback()
                        showResultBottomSheet(barcode)
                    } else {
                        utils.toast("No QR code found in image", this)
                    }
                }
                .addOnFailureListener {
                    utils.toast("Failed to scan image", this)
                }
        } catch (_: Exception) {
            utils.toast("Error processing image", this)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showResultBottomSheet(barcode: Barcode) {
        val result = barcode.rawValue ?: ""
        if (isBottomSheetShowing || result.isEmpty()) return
        isBottomSheetShowing = true

        lifecycleScope.launch {
            AppDatabase.getDatabase(this@MainActivity).historyDao().insert(ScanHistory(result = result))
        }

        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetBinding = LayoutBottomSheetResultBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        sheetBinding.resultText.text = result

        if (barcode.valueType == Barcode.TYPE_WIFI) {
            sheetBinding.actionBtn.text = "Connect to WiFi"
            sheetBinding.actionBtn.setOnClickListener {
                barcode.wifi?.let { wifi ->
                    val ssid = wifi.ssid ?: ""
                    val password = wifi.password
                    val encryptionType = wifi.encryptionType
                    
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        pendingWifi = WifiData(ssid, password, encryptionType)
                        utils.connectToWifi(this, ssid, password, encryptionType)
                    } else {
                        utils.connectToWifi(this, ssid, password, encryptionType)
                    }
                }
                bottomSheetDialog.dismiss()
            }
        } else {
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
        }

        bottomSheetDialog.setOnDismissListener {
            isBottomSheetShowing = false
        }

        bottomSheetDialog.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingWifi?.let {
            outState.putString("pending_ssid", it.ssid)
            outState.putString("pending_password", it.password)
            outState.putInt("pending_encryption", it.encryptionType)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
