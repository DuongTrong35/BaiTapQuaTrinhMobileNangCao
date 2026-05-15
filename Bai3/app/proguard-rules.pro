# ProGuard rules for LG Remote Pro

# ── OkHttp ────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ── Gson ──────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ── Room ──────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ── WebOS Models ──────────────────────────────────────────
-keep class com.example.bai3.model.** { *; }
-keep class com.example.bai3.network.** { *; }

# ── AndroidX ─────────────────────────────────────────────
-keep class androidx.** { *; }
-dontwarn androidx.**

# ── Keep R classes ────────────────────────────────────────
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ── General ───────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile