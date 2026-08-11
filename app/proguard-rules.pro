# Add project specific ProGuard rules here.
-keep class com.musicplayer.app.model.** { *; }
-keep class com.musicplayer.app.service.** { *; }
-keepclassmembers class * implements android.os.Parcelable { *; }
-dontwarn androidx.media.**
