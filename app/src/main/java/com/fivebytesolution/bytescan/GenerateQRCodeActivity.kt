package com.fivebytesolution.bytescan

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.fivebytesolution.bytescan.adapter.QRCategory
import com.fivebytesolution.bytescan.adapter.QRCategoryAdapter
import com.fivebytesolution.bytescan.adapter.QRType
import com.fivebytesolution.bytescan.databinding.ActivityGenerateQrcodeBinding
import com.fivebytesolution.bytescan.utils.Utils
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
    private var selectedType: QRType = QRType.TEXT

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

        setupCategories()
        setupButtons()
    }

    private fun setupCategories() {
        val categories = listOf(
            QRCategory("Text", R.drawable.ic_text, QRType.TEXT),
            QRCategory("Website", R.drawable.ic_website, QRType.WEBSITE),
            QRCategory("Wi-Fi", R.drawable.ic_wifi, QRType.WIFI),
            QRCategory("Event", R.drawable.ic_event, QRType.EVENT),
            QRCategory("Contact", R.drawable.ic_contact, QRType.CONTACT),
            QRCategory("Business", R.drawable.ic_business, QRType.BUSINESS),
            QRCategory("Location", R.drawable.ic_location, QRType.LOCATION),
            QRCategory("WhatsApp", R.drawable.ic_whatsapp, QRType.WHATSAPP),
            QRCategory("Email", R.drawable.ic_email, QRType.EMAIL),
            QRCategory("Twitter", R.drawable.ic_twitter, QRType.TWITTER),
            QRCategory("Instagram", R.drawable.ic_instagram, QRType.INSTAGRAM),
            QRCategory("Telephone", R.drawable.ic_telephone, QRType.TELEPHONE)
        )

        binding.categoriesRv.layoutManager = GridLayoutManager(this, 3)
        binding.categoriesRv.adapter = QRCategoryAdapter(categories) { category ->
            showInputFields(category)
        }
    }

    private fun showInputFields(category: QRCategory) {
        selectedType = category.type
        binding.inputContainer.visibility = View.VISIBLE
        binding.inputTypeTv.text = category.name
        binding.qrResultCard.visibility = View.GONE
        binding.shareBtn.visibility = View.GONE
        
        // Clear previous fields except generate button
        val count = binding.fieldsContainer.childCount
        if (count > 1) {
            binding.fieldsContainer.removeViews(0, count - 1)
        }
        
        // Re-add primary input if removed or for simple types
        when (selectedType) {
            QRType.TEXT, QRType.WEBSITE, QRType.WHATSAPP, QRType.TWITTER, QRType.INSTAGRAM, QRType.TELEPHONE -> {
                val hint = when (selectedType) {
                    QRType.WEBSITE -> "Enter Website URL"
                    QRType.WHATSAPP -> "Enter WhatsApp Number"
                    QRType.TWITTER -> "Enter Twitter Username"
                    QRType.INSTAGRAM -> "Enter Instagram Username"
                    QRType.TELEPHONE -> "Enter Telephone Number"
                    else -> "Enter Text"
                }
                addInputField("input", hint)
            }
            QRType.WIFI -> {
                addInputField("ssid", "Enter SSID (Network Name)")
                addInputField("password", "Enter Password")
            }
            QRType.EMAIL -> {
                addInputField("email", "Enter Email Address")
                addInputField("subject", "Enter Subject")
                addInputField("body", "Enter Message")
            }
            QRType.LOCATION -> {
                addInputField("lat", "Enter Latitude")
                addInputField("long", "Enter Longitude")
            }
            QRType.CONTACT -> {
                addInputField("name", "Enter Full Name")
                addInputField("phone", "Enter Phone Number")
                addInputField("email", "Enter Email")
            }
            QRType.EVENT -> {
                addInputField("title", "Enter Event Title")
                addInputField("location", "Enter Event Location")
            }
            QRType.BUSINESS -> {
                addInputField("bname", "Enter Business Name")
                addInputField("bphone", "Enter Business Phone")
                addInputField("bweb", "Enter Business Website")
            }
        }
    }

    private fun addInputField(tag: String, hint: String) {
        val editText = EditText(this)
        editText.tag = tag
        editText.hint = hint
        editText.setBackgroundResource(R.drawable.bg_input_field)
        editText.setPadding(40, 0, 40, 0)
        editText.setTextColor(Color.BLACK)
        editText.setHintTextColor(Color.GRAY)
        val params = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            150
        )
        params.setMargins(0, 10, 0, 10)
        editText.layoutParams = params
        binding.fieldsContainer.addView(editText, binding.fieldsContainer.childCount - 1)
    }

    private fun setupButtons() {
        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.inputBackBtn.setOnClickListener {
            binding.inputContainer.visibility = View.GONE
        }

        binding.generateBtn.setOnClickListener {
            val qrData = collectQRData()
            if (qrData.isNotEmpty()) {
                generateQRCode(qrData)
            } else {
                utils.toast("Please fill in the required fields", this)
            }
        }

        binding.shareBtn.setOnClickListener {
            qrBitmap?.let { shareBitmap(it) }
        }
    }

    private fun collectQRData(): String {
        return when (selectedType) {
            QRType.TEXT -> {
                getInputText("input")
            }
            QRType.WEBSITE -> {
                val url = getInputText("input")
                if (url.isNotEmpty()) {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                } else ""
            }
            QRType.TELEPHONE -> {
                val phone = getInputText("input")
                if (phone.isNotEmpty()) "tel:$phone" else ""
            }
            QRType.TWITTER -> {
                val user = getInputText("input")
                if (user.isNotEmpty()) "https://twitter.com/$user" else ""
            }
            QRType.INSTAGRAM -> {
                val user = getInputText("input")
                if (user.isNotEmpty()) "https://instagram.com/$user" else ""
            }
            QRType.WHATSAPP -> {
                val phone = getInputText("input")
                if (phone.isNotEmpty()) "https://wa.me/$phone" else ""
            }
            QRType.WIFI -> {
                val ssid = getInputText("ssid")
                val pass = getInputText("password")
                if (ssid.isNotEmpty() && pass.isNotEmpty()) "WIFI:S:$ssid;T:WPA;P:$pass;;" else ""
            }
            QRType.EMAIL -> {
                val email = getInputText("email")
                val subject = getInputText("subject")
                val body = getInputText("body")
                if (email.isNotEmpty() && subject.isNotEmpty() && body.isNotEmpty()) "MATMSG:TO:$email;SUB:$subject;BODY:$body;;" else ""
            }
            QRType.LOCATION -> {
                val lat = getInputText("lat")
                val lon = getInputText("long")
                if (lat.isNotEmpty() && lon.isNotEmpty()) "geo:$lat,$lon" else ""
            }
            QRType.CONTACT -> {
                val name = getInputText("name")
                val phone = getInputText("phone")
                val email = getInputText("email")
                if (name.isNotEmpty() && phone.isNotEmpty() && email.isNotEmpty()) "BEGIN:VCARD\nVERSION:3.0\nFN:$name\nTEL:$phone\nEMAIL:$email\nEND:VCARD" else ""
            }
            QRType.EVENT -> {
                val title = getInputText("title")
                val loc = getInputText("location")
                if (title.isNotEmpty() && loc.isNotEmpty()) "BEGIN:VEVENT\nSUMMARY:$title\nLOCATION:$loc\nEND:VEVENT" else ""
            }
            QRType.BUSINESS -> {
                val name = getInputText("bname")
                val phone = getInputText("bphone")
                val web = getInputText("bweb")
                if (name.isNotEmpty() && phone.isNotEmpty() && web.isNotEmpty()) "BIZCARD:N:$name;T:$phone;W:$web;;" else ""
            }
        }
    }

    private fun getInputText(tag: String): String {
        val view = binding.fieldsContainer.findViewWithTag<EditText>(tag)
        return view?.text?.toString()?.trim() ?: ""
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
