package com.example.qrcodescanner

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.qrcodescanner.databinding.ActivitySettingBinding
import com.example.qrcodescanner.utils.Utils
import androidx.core.content.edit

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
        
        binding.backBtn.setOnClickListener {
           onBackPressedDispatcher.onBackPressed()
        }
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
