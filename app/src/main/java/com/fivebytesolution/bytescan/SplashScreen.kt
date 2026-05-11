package com.fivebytesolution.bytescan

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.fivebytesolution.bytescan.utils.Utils

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {
    val  utils = Utils()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

       utils.bottomNavigationColor(this)

        Handler(Looper.getMainLooper()).postDelayed({
            decideNextScreen()
        }, 1000)
    }

    private fun decideNextScreen() {
        val prefs = getSharedPreferences("QRCodePref", MODE_PRIVATE)
        val isOnboarded = prefs.getBoolean("isOnBoard", false)

        val nextActivity = if (isOnboarded) MainActivity::class.java else OnBoardingScreen::class.java
        startActivity(Intent(this, nextActivity))
        finish()
    }
}