# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ── Global attributes (Retrofit requires InnerClasses for Signature resolution,
#    EnclosingMethod for InnerClasses) ──
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# ── Retrofit ──
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# R8 full mode strips generic signatures; keep them for Call, Response
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Keep generic signature of Kotlin Continuation so Retrofit can extract
# Response<T> from suspend fun parameters (THIS fixes ParameterizedType crash)
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# R8 full mode: keep Retrofit service interfaces (created via Proxy)
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep inherited service interfaces
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# Keep return type classes referenced by service methods
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# Retain service method parameters when optimizing
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ── OkHttp ──
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ── Gson ──
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Preserve TypeToken generic info (needed for List<T> deserialization)
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ── Keep ALL data models with fields + generic signatures ──
-keep class pl.vestmedia.tennisreferee.data.model.** { *; }
-keepclassmembers class pl.vestmedia.tennisreferee.data.model.** { *; }

# ── Keep API service interface with full type info ──
-keep interface pl.vestmedia.tennisreferee.data.api.** { *; }

# ── Kotlin ──
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
