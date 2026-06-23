package com.fivebytesolution.bytescan

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fivebytesolution.bytescan.adapter.HistoryAdapter
import com.fivebytesolution.bytescan.database.AppDatabase
import com.fivebytesolution.bytescan.databinding.ActivityHistoryBinding
import com.fivebytesolution.bytescan.databinding.DialogCustomBinding
import com.fivebytesolution.bytescan.databinding.LayoutBottomSheetResultBinding
import com.fivebytesolution.bytescan.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private val utils = Utils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        utils.bottomNavigationTransparent(this)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupButtons()
        loadHistory()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(emptyList()) { history ->
            showResultBottomSheet(history.result)
        }
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.historyRecyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.deleteBtn.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showDeleteConfirmationDialog() {
        val dialog = Dialog(this)
        val dialogBinding = DialogCustomBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialogBinding.dialogTitle.text = getString(R.string.clear_history)
        dialogBinding.dialogMessage.text = "Are you sure you want to clear all scan history?"
        dialogBinding.btnPositive.text = getString(R.string.ok)
        dialogBinding.btnNegative.text = getString(R.string.cancel)

        dialogBinding.btnNegative.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnPositive.setOnClickListener {
            lifecycleScope.launch {
                AppDatabase.getDatabase(this@HistoryActivity).historyDao().deleteAll()
                loadHistory()
                dialog.dismiss()
                utils.toast("History cleared successfully", this@HistoryActivity)
            }
        }

        dialog.show()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val historyList = AppDatabase.getDatabase(this@HistoryActivity).historyDao().getAllHistory()
            if (historyList.isEmpty()) {
                binding.emptyView.visibility = View.VISIBLE
                binding.historyRecyclerView.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.GONE
                binding.historyRecyclerView.visibility = View.VISIBLE
                adapter.updateData(historyList)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showResultBottomSheet(result: String) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetBinding = LayoutBottomSheetResultBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        sheetBinding.resultText.text = result

        val wifiData = parseWifi(result)

        if (wifiData != null) {
            sheetBinding.actionBtn.text = "Connect to WiFi"
            sheetBinding.actionBtn.setOnClickListener {
                utils.connectToWifi(this, wifiData.first, wifiData.second, wifiData.third)
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

        bottomSheetDialog.show()
    }

    private fun parseWifi(result: String): Triple<String, String?, Int>? {
        if (!result.startsWith("WIFI:", ignoreCase = true)) return null

        val ssid = result.substringAfter("S:", "").substringBefore(";")
        val password = if (result.contains("P:")) result.substringAfter("P:").substringBefore(";") else null
        val typeStr = if (result.contains("T:")) result.substringAfter("T:").substringBefore(";").uppercase() else "NOPASS"

        val encryptionType = when (typeStr) {
            "WPA", "WPA2" -> Barcode.WiFi.TYPE_WPA
            "WEP" -> Barcode.WiFi.TYPE_WEP
            else -> Barcode.WiFi.TYPE_OPEN
        }

        return if (ssid.isNotEmpty()) Triple(ssid, password, encryptionType) else null
    }
}
