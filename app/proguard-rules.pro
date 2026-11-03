# WebRTC native classes
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Firebase Realtime Database models
-keepclassmembers class com.directshare.app.data.firebase.** {
    *;
}

# Room entities
-keep class com.directshare.app.data.local.** { *; }
