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
# Response<T> from suspend fun parameters (THIS fixes ParameterizedType crash
# right after language selection, on the first TennisApiService create()).
-keep class kotlin.coroutines.Continuation { *; }

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

# API DTOs are kotlinx.serialization (@SerialName). The library rules keep the
# generated serializers.
#
# The match model is written to the tablet's own disk: the active match kept for a
# restart (ActiveMatchStore) and the set history stored in Room. Since 1.0.0-dev.52
# both are kotlinx, whose generated serializers carry the names themselves — but the
# reader that opens data written by an older build is still Gson, reflecting over these
# fields. Obfuscated names change between builds, so dropping these rules would make a
# referee's unsynced match unreadable after an update.
-keep class pl.vestmedia.tennisreferee.domain.match.model.** { *; }
-keepclassmembers class pl.vestmedia.tennisreferee.domain.match.model.** { *; }

# ── Room database/entities ──
-keep class pl.vestmedia.tennisreferee.data.database.** { *; }
-keepnames class * extends androidx.room.RoomDatabase

# ── Keep API service interface with full type info ──
-keep interface pl.vestmedia.tennisreferee.data.api.** { *; }

# ── Kotlin ──
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

