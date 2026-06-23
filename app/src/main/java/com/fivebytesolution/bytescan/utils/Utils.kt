package com.fivebytesolution.bytescan.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.fivebytesolution.bytescan.R
import com.google.mlkit.vision.barcode.common.Barcode

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

    fun connectToWifi(activity: Activity, ssid: String, password: String?, encryptionType: Int) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 200)
            return
        }

        val wifiManager = activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Check if WiFi is enabled
        if (!wifiManager.isWifiEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                activity.startActivity(Intent(Settings.Panel.ACTION_WIFI))
                toast("Please enable WiFi first", activity)
                return
            } else {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = true
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: Use Intent to add network permanently to system
            val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS)
            val bundle = Bundle()
            val suggestionList = arrayListOf(
                WifiNetworkSuggestion.Builder()
                    .setSsid(ssid)
                    .apply {
                        if (password != null) {
                            when (encryptionType) {
                                Barcode.WiFi.TYPE_WPA -> setWpa2Passphrase(password)
                                Barcode.WiFi.TYPE_WEP -> setWpa2Passphrase(password)
                            }
                        }
                    }
                    .build()
            )
            bundle.putParcelableArrayList(Settings.EXTRA_WIFI_NETWORK_LIST, suggestionList)
            intent.putExtras(bundle)
            try {
                activity.startActivity(intent)
                toast("Tap 'Save' to add $ssid to your device", activity)
            } catch (_: Exception) {
                activity.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10: Suggestions
            val suggestion = WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .apply {
                    if (password != null) {
                        when (encryptionType) {
                            Barcode.WiFi.TYPE_WPA -> setWpa2Passphrase(password)
                            Barcode.WiFi.TYPE_WEP -> setWpa2Passphrase(password)
                        }
                    }
                }
                .build()
            
            val status = wifiManager.addNetworkSuggestions(listOf(suggestion))
            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                toast("WiFi $ssid saved! Connecting via system...", activity)
                activity.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            } else {
                toast("Failed to save WiFi $ssid", activity)
            }
        } else {
            @Suppress("DEPRECATION")
            val wifiConfig = android.net.wifi.WifiConfiguration().apply {
                SSID = "\"$ssid\""
                when (encryptionType) {
                    Barcode.WiFi.TYPE_OPEN -> {
                        allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.NONE)
                    }
                    Barcode.WiFi.TYPE_WPA -> {
                        preSharedKey = "\"$password\""
                    }
                    Barcode.WiFi.TYPE_WEP -> {
                        wepKeys[0] = "\"$password\""
                        wepTxKeyIndex = 0
                        allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.NONE)
                        allowedGroupCiphers.set(android.net.wifi.WifiConfiguration.GroupCipher.WEP40)
                    }
                }
            }

            @Suppress("DEPRECATION")
            val netId = wifiManager.addNetwork(wifiConfig)
            if (netId != -1) {
                @Suppress("DEPRECATION")
                wifiManager.disconnect()
                @Suppress("DEPRECATION")
                wifiManager.enableNetwork(netId, true)
                @Suppress("DEPRECATION")
                wifiManager.reconnect()
                toast("Connecting to $ssid...", activity)
            } else {
                toast("Failed to add network $ssid", activity)
            }
        }
    }
}
