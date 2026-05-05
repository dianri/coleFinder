# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class es.colefinder.**$$serializer { *; }
-keepclassmembers class es.colefinder.** {
    *** Companion;
}
-keepclasseswithmembers class es.colefinder.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Data classes (evitar que R8 elimine campos usados por reflexión/serialización)
-keep class es.colefinder.data.model.** { *; }
-keep class es.colefinder.data.network.** { *; }

# Google Maps / Play Services
-keep class com.google.android.gms.** { *; }
# Nota: R8 puede mostrar WARNING "Companion could not be found" en bytecode interno de
# play-services-location (p. ej. zze). Es un aviso conocido del AAR de Google, no bloquea el shrinker.