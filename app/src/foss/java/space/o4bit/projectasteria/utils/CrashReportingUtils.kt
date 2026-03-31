package space.o4bit.projectasteria.utils

import android.content.Context
import android.os.Build
import space.o4bit.projectasteria.BuildConfig

/**
 * No-op utility for FOSS builds (F-Droid)
 */
object CrashReportingUtils {
    
    fun initialize(context: Context) {
        // No-op for FOSS
    }
    
    fun reportError(
        throwable: Throwable,
        message: String? = null,
        additionalData: Map<String, Any> = emptyMap()
    ) {
        // No-op for FOSS - but log to console in debug
        if (BuildConfig.DEBUG) {
            println("FOSS ERROR REPORT: $message")
            throwable.printStackTrace()
        }
    }
    
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
    
    fun formatCrashDataForGitHub(
        error: String,
        stackTrace: String? = null,
        additionalContext: String? = null
    ): String {
        val deviceInfo = getDeviceInfo()
        
        return buildString {
            appendLine("## Bug Report (FOSS)")
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
        }
    }
    
    fun setUserId(userId: String) {}
    fun addBreadcrumb(message: String) {}
}
