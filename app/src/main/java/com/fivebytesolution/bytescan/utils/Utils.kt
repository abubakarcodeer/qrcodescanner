package com.fivebytesolution.bytescan.utils

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.fivebytesolution.bytescan.R

class Utils {
   fun bottomNavigationTransparent(activity: Activity){
        // Let content go behind navigation bar
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        // Control navigation icon color
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.isAppearanceLightNavigationBars = false  // false → light icons on transparent background
        controller.isAppearanceLightStatusBars = false
   }
    fun bottomNavigationColor(activity: Activity){
        // Get color from theme
        val typedValue = TypedValue()
        activity.theme.resolveAttribute(R.attr.splashScreenColor, typedValue, true)
        val themeColor = typedValue.data

        // Set navigation bar color
        activity.window.navigationBarColor = themeColor
    }
    fun toast(message: String,context: Context) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}