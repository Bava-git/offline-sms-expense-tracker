# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Room entities and DAOs
-keep class com.example.smsexpense.data.** { *; }
-keepclassmembers class * extends androidx.room.Entity { *; }

# Keep Parcelable / Serializable if used
-keepnames class * implements android.os.Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep names for classes referenced in AndroidManifest
-keep class * extends android.app.Activity
-keep class * extends android.content.BroadcastReceiver

-keepclassmembers class **.R$* {
    static <fields>;
}