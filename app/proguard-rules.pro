# Project specific ProGuard rules

# Room rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.fivebytesolution.bytescan.model.** { *; }
-keep class com.fivebytesolution.bytescan.database.** { *; }

# ML Kit rules
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# CameraX rules
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# General optimizations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
