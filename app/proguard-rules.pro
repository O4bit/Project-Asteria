# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Retrofit
-dontwarn retrofit2.**
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Moshi Data Models (Moshi Kotlin reflection needs fields kept intact)
-keep class space.o4bit.projectasteria.data.model.** { *; }

# Moshi rules
-dontwarn com.squareup.moshi.**
-keep class * extends com.squareup.moshi.JsonAdapter {
    public <init>(...);
}
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# AndroidX Navigation/Compose generic rules
-keep class androidx.navigation.** { *; }

# Keep line numbers for Crashlytics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile