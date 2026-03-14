package com.example.qrcodescanner

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.qrcodescanner.databinding.ActivityGenerateQrcodeBinding
import com.example.qrcodescanner.utils.Utils
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

class GenerateQRCodeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGenerateQrcodeBinding
    private val utils = Utils()
    private var qrBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGenerateQrcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        utils.bottomNavigationTransparent(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.generateBtn.setOnClickListener {
            val text = binding.qrInputEt.text.toString().trim()
            if (text.isNotEmpty()) {
                generateQRCode(text)
            } else {
                utils.toast("Please enter some text", this)
            }
        }

        binding.shareBtn.setOnClickListener {
            qrBitmap?.let { shareBitmap(it) }
        }
    }

    private fun generateQRCode(text: String) {
        val width = 500
        val height = 500
        val writer = MultiFormatWriter()
        val hints = mutableMapOf<EncodeHintType, Any>()
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        
        try {
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, hints)
            val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            qrBitmap = bitmap
            binding.qrCodeIv.setImageBitmap(bitmap)
            binding.qrResultCard.visibility = View.VISIBLE
            binding.shareBtn.visibility = View.VISIBLE
            binding.shareWithOtherText.visibility = View.VISIBLE
        } catch (e: WriterException) {
            e.printStackTrace()
            utils.toast("Failed to generate QR Code", this)
        }
    }

    private fun shareBitmap(bitmap: Bitmap) {
        try {
            val cachePath = File(externalCacheDir, "images")
            cachePath.mkdirs()
            val stream = FileOutputStream("$cachePath/image.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val imagePath = File(externalCacheDir, "images/image.png")
            val contentUri: Uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imagePath)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
        } catch (e: Exception) {
            e.printStackTrace()
            utils.toast("Error sharing image", this)
        }
    }
}
