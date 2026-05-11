# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Samsung Knox SDK rules (uncomment when using full knoxsdk.jar)
#-keep class com.samsung.android.knox.** { *; }
#-keep class com.samsung.android.knox.container.** { *; }
#-keep class com.samsung.android.knox.license.** { *; }
#-keep class com.samsung.android.knox.restriction.** { *; }
#-keep class com.samsung.android.knox.application.** { *; }
#-keep class com.samsung.android.knox.deviceinfo.** { *; }
#-keep class com.samsung.android.knox.security.password.** { *; }

# Keep PrivacyGuard classes
-keep class com.arfullsend.privacyguard.** { *; }