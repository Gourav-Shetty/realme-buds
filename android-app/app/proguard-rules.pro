# ProGuard/R8 rules for Realme Buds.
#
# `isMinifyEnabled` is deliberately left `false` for the release build (this
# app is shipped unminified until the rules below have been verified on a
# device), but this file is referenced by `app/build.gradle.kts` and kept
# up to date so minification can be turned on safely later.

# ---- Keep the app's entry points (only needed once minification is on) ----
-keep class com.example.realmebuds.MainActivity { *; }

# ---- Kotlin / JVM ----
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-keepattributes *Annotation*, InnerClasses, Signature, LineNumberTable, SourceFile, EnclosingMethod

# Kotlinx serialization (used by Navigation3 NavKey state save/restore)
-keepattributes RuntimeVisibleAnnotations
-keepclassmembers class kotlinx.serialization.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.realmebuds.**$$serializer { *; }
-keepclassmembers class com.example.realmebuds.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.realmebuds.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- AndroidX / Lifecycle ----
-keep class * extends androidx.lifecycle.ViewModel { <init>(); }
-keep class * implements androidx.lifecycle.LifecycleObserver { *; }

# ---- Compose ----
# Compose runtime handles its own rules; only suppress warnings from
# optional/debug classes that are stripped from release artifacts.
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# ---- Bluetooth / app model classes surfaced through reflection-free APIs ----
# Data classes referenced from saved state must keep their names/fields.
-keepclassmembers class com.example.realmebuds.bluetooth.** { *; }
-keepclassmembers class com.example.realmebuds.protocol.RealmeProtocol$* { *; }
