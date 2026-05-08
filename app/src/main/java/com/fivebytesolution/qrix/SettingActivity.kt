package com.fivebytesolution.qrix

import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fivebytesolution.qrix.databinding.ActivitySettingBinding
import com.fivebytesolution.qrix.utils.Utils
import androidx.core.content.edit
import androidx.core.net.toUri

class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding
    private val utils = Utils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        utils.bottomNavigationTransparent(this)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupSettings()
        setupSupportActions()
        
        binding.backBtn.setOnClickListener {
           onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupSupportActions() {
        binding.cardView3.setOnClickListener {
            showRateUsDialog()
        }

        binding.cardView4.setOnClickListener {
            shareApp()
        }

        binding.cardView5.setOnClickListener {
            showPrivacyPolicyDialog()
        }
    }

    private fun showRateUsDialog() {
        showCustomDialog(
            title = getString(R.string.rate_us),
            message = getString(R.string.your_best_reward_to_us),
            positiveBtnText = "Rate Now",
            negativeBtnText = "Later",
            onPositiveClick = {
                val uri = "market://details?id=$packageName".toUri()
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                try {
                    startActivity(goToMarket)
                } catch (e: Exception) {
                    startActivity(Intent(Intent.ACTION_VIEW,
                        "http://play.google.com/store/apps/details?id=$packageName".toUri()))
                }
            }
        )
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
        val shareMessage = "Check out this ${getString(R.string.app_name)} app: https://play.google.com/store/apps/details?id=$packageName"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun showPrivacyPolicyDialog() {
        showCustomDialog(
            title = getString(R.string.privacy_policy),
            message = getString(R.string.privacy_policy_content),
            positiveBtnText = "Close",
            negativeBtnText = null,
            onPositiveClick = { }
        )
    }

    private fun showCustomDialog(
        title: String,
        message: String,
        positiveBtnText: String,
        negativeBtnText: String?,
        onPositiveClick: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val btnPositive = dialogView.findViewById<AppCompatButton>(R.id.btnPositive)
        val btnNegative = dialogView.findViewById<AppCompatButton>(R.id.btnNegative)

        tvTitle.text = title
        tvMessage.text = message
        btnPositive.text = positiveBtnText
        
        if (negativeBtnText != null) {
            btnNegative.visibility = View.VISIBLE
            btnNegative.text = negativeBtnText
        } else {
            btnNegative.visibility = View.GONE
        }

        btnPositive.setOnClickListener {
            onPositiveClick()
            dialog.dismiss()
        }

        btnNegative.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupSettings() {
        val sharedPref = getSharedPreferences("settings", MODE_PRIVATE)
        
        // Load saved states, default vibrate to true, beep to false
        binding.vibrateSwitch.isChecked = sharedPref.getBoolean("vibrate", true)
        binding.beepSwitch.isChecked = sharedPref.getBoolean("beep", false)

        binding.vibrateSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit { putBoolean("vibrate", isChecked) }
        }

        binding.beepSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit { putBoolean("beep", isChecked) }
        }
    }
}
