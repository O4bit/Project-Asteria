package space.o4bit.projectasteria.utils

import android.content.Context
import android.os.Build
import com.google.firebase.crashlytics.FirebaseCrashlytics
import space.o4bit.projectasteria.BuildConfig

/**
 * Utility object for handling crash reporting and error tracking
 */
object CrashReportingUtils {
    
    /**
     * Initialize Firebase Crashlytics with app metadata
     */
    fun initialize(context: Context) {
        val crashlytics = FirebaseCrashlytics.getInstance()

        crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
        crashlytics.setCustomKey("app_version_code", BuildConfig.VERSION_CODE)
        crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
        crashlytics.setCustomKey("device_model", Build.MODEL)
        crashlytics.setCustomKey("device_manufacturer", Build.MANUFACTURER)
        crashlytics.setCustomKey("android_version", Build.VERSION.RELEASE)
        crashlytics.setCustomKey("sdk_int", Build.VERSION.SDK_INT)

        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }
    
    /**
     * Report a non-fatal error with context
     */
    fun reportError(
        throwable: Throwable,
        message: String? = null,
        additionalData: Map<String, Any> = emptyMap()
    ) {
        val crashlytics = FirebaseCrashlytics.getInstance()

        additionalData.forEach { (key, value) ->
            when (value) {
                is String -> crashlytics.setCustomKey(key, value)
                is Int -> crashlytics.setCustomKey(key, value)
                is Long -> crashlytics.setCustomKey(key, value)
                is Float -> crashlytics.setCustomKey(key, value)
                is Double -> crashlytics.setCustomKey(key, value)
                is Boolean -> crashlytics.setCustomKey(key, value)
                else -> crashlytics.setCustomKey(key, value.toString())
            }
        }

        message?.let { crashlytics.log(it) }

        crashlytics.recordException(throwable)
    }
    
    /**
     * Get device information for manual reporting
     */
    fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "device_model" to Build.MODEL,
            "device_manufacturer" to Build.MANUFACTURER,
            "android_version" to Build.VERSION.RELEASE,
            "sdk_int" to Build.VERSION.SDK_INT.toString(),
            "app_version" to BuildConfig.VERSION_NAME,
            "app_version_code" to BuildConfig.VERSION_CODE.toString(),
            "build_type" to BuildConfig.BUILD_TYPE
        )
    }
    
    /**
     * Format device info and crash data for GitHub issue reporting
     */
    fun formatCrashDataForGitHub(
        error: String,
        stackTrace: String? = null,
        additionalContext: String? = null
    ): String {
        val deviceInfo = getDeviceInfo()
        
        return buildString {
            appendLine("## Bug Report")
            appendLine()
            appendLine("**Error:** $error")
            appendLine()
            
            if (stackTrace != null) {
                appendLine("**Stack Trace:**")
                appendLine("```")
                appendLine(stackTrace)
                appendLine("```")
                appendLine()
            }
            
            if (additionalContext != null) {
                appendLine("**Additional Context:**")
                appendLine(additionalContext)
                appendLine()
            }
            
            appendLine("**Device Information:**")
            deviceInfo.forEach { (key, value) ->
                appendLine("- $key: $value")
            }
            
            appendLine()
            appendLine("**Steps to Reproduce:**")
            appendLine("1. ")
            appendLine("2. ")
            appendLine("3. ")
            appendLine()
            appendLine("**Expected Behavior:**")
            appendLine()
            appendLine("**Actual Behavior:**")
        }
    }
    
    /**
     * Set user identifier for crash tracking
     */
    fun setUserId(userId: String) {
        FirebaseCrashlytics.getInstance().setUserId(userId)
    }
    
    /**
     * Add breadcrumb for debugging
     */
    fun addBreadcrumb(message: String) {
        FirebaseCrashlytics.getInstance().log(message)
    }
}
