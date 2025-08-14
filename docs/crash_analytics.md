# Crash Analytics Implementation Documentation

This document outlines the crash analytics implementation in Project Asteria using Firebase Crashlytics.

## Overview

Project Asteria utilizes Firebase Crashlytics to automatically capture and report crashes and exceptions. This provides insights into app stability issues, helping identify and fix problems quickly.

## Implementation Components

### 1. Dependencies and Configuration

Firebase Crashlytics is integrated through the following Gradle configurations:

- Crashlytics Plugin in app/build.gradle.kts:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    // ...other plugins
}
```

- Crashlytics Dependencies in libs.versions.toml:
```toml
[libraries]
# Firebase
firebase-analytics = { module = "com.google.firebase:firebase-analytics-ktx" }
firebase-crashlytics = { module = "com.google.firebase:firebase-crashlytics-ktx" }
firebase-messaging = { module = "com.google.firebase:firebase-messaging-ktx" }
```

### 2. CrashReporter Utility Class

The `CrashReporter` class (in `utils/CrashReporter.kt`) provides a centralized interface for logging events, custom keys, and exceptions to Crashlytics:

```kotlin
object CrashReporter {
    // Initialize with app version and device info
    fun init(context: Context) {
        // Set custom keys for better crash context
        setCustomKey("app_version_name", BuildConfig.VERSION_NAME)
        setCustomKey("app_version_code", BuildConfig.VERSION_CODE)
        setCustomKey("device_manufacturer", Build.MANUFACTURER)
        setCustomKey("device_model", Build.MODEL)
        setCustomKey("android_version", Build.VERSION.SDK_INT)
        
        // Force enable Crashlytics collection in debug builds
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }
    
    // Log non-fatal exceptions with additional context
    fun logException(throwable: Throwable, message: String? = null) {
        message?.let {
            FirebaseCrashlytics.getInstance().log("Error context: $it")
        }
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }
    
    // Add user or event context to crash reports
    fun setCustomKey(key: String, value: String) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }
    
    fun setCustomKey(key: String, value: Boolean) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }
    
    fun setCustomKey(key: String, value: Int) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }
    
    fun setCustomKey(key: String, value: Long) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }
    
    fun setCustomKey(key: String, value: Float) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }
    
    // Log user actions and events as breadcrumbs
    fun logBreadcrumb(message: String) {
        FirebaseCrashlytics.getInstance().log(message)
    }
}
```

### 3. Initialization in Application Class

Crashlytics is initialized in the `AsteriaApplication` class:

```kotlin
class AsteriaApplication : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize crash reporting
        CrashReporter.init(this)
        
        // Other initializations...
    }
    // ...
}
```

### 4. Integration Points

Crash reporting is integrated at key points throughout the application:

#### Network Layer
- In `NasaApodService.kt`, network errors are logged with context about the request
- OkHttp interceptors capture API call details for better diagnostics

#### Repository Layer
- In `SpaceRepository.kt`, data fetching errors are caught and logged with appropriate context

#### UI Layer
- In `MainActivity.kt`, UI errors and exceptions are captured with screen context
- All composable functions using external data implement error handling

### 5. Testing Tools

A `TestCrashActivity` provides tools to manually test different types of crashes:

- Non-fatal exceptions
- Fatal crashes
- Memory errors
- ANR-like behaviors

This activity can be accessed from the Settings screen (in debug builds only) to verify crash reporting is working correctly.

## Captured Information

Each crash report includes:

1. **Stack Traces**: Full stack traces showing the error path
2. **Device Information**: 
   - Manufacturer and model
   - Android OS version
   - Screen size and density
3. **App Information**:
   - App version name and code
   - Build type (debug/release)
4. **Custom Context**:
   - Last API endpoints accessed
   - User actions (as breadcrumbs)
   - App state at crash time

## Development & Testing Guidelines

1. **Adding Custom Keys**: When implementing new features, add relevant custom keys to provide context
2. **Error Handling**: Catch exceptions in critical code paths and log them with `CrashReporter.logException()`
3. **Breadcrumbs**: Add breadcrumbs for important user actions with `CrashReporter.logBreadcrumb()`
4. **Testing**: Use the TestCrashActivity to verify crash reporting for different error types

## Dashboard Access

Crash reports can be viewed in the Firebase Console under the Crashlytics section. The dashboard provides:

- Real-time crash reporting
- Crash trends and statistics
- User impact metrics
- Detailed stack traces and device information

## Privacy Considerations

- No personally identifiable information (PII) is collected
- Only technical data about the crash context is included
- Users can opt out of analytics collection through device settings

## Future Enhancements

Planned improvements to the crash analytics system:

1. User opt-in for enhanced diagnostics
2. Session recording for better reproduction steps
3. Integration with CI/CD pipeline for automated regression testing
4. Custom crash grouping rules for better organization
