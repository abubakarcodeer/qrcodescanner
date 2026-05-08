package com.fivebytesolution.qrix

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fivebytesolution.qrix.databinding.ActivityOnBoardingScreenBinding
import com.fivebytesolution.qrix.utils.Utils
import androidx.core.content.edit


class OnBoardingScreen : AppCompatActivity() {
    private lateinit var binding:ActivityOnBoardingScreenBinding
    val utils = Utils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOnBoardingScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        utils.bottomNavigationColor(this)

        binding.button.setOnClickListener {
            val prefs = getSharedPreferences("QRCodePref", MODE_PRIVATE)
            prefs.edit {
                putBoolean("isOnBoard", true)
            }

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

}