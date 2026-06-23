# Project specific ProGuard rules

# Room rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# General optimizations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
