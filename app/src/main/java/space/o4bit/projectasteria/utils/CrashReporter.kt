package space.o4bit.projectasteria.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Privacy-preserving local diagnostic logger for Project Asteria.
 * Does not collect user IDs, telemetry, or send data to external servers.
 * Maintains a local diagnostic log file that users can review and export manually.
 */
object CrashReporter {
    private const val TAG = "AsteriaDiagnostics"
    private const val MAX_LOG_ENTRIES = 100
    private val logBuffer = mutableListOf<String>()

    fun log(message: String) {
        val entry = "${timestamp()} [INFO] $message"
        Log.i(TAG, message)
        addToBuffer(entry)
    }

    fun setUserId(userId: String) {
        // Explicit no-op for privacy — Project Asteria does not track user identity
    }

    fun setCustomKey(key: String, value: String) {
        addToBuffer("${timestamp()} [KEY] $key = $value")
    }

    fun setCustomKey(key: String, value: Number) {
        setCustomKey(key, value.toString())
    }

    fun setCustomKey(key: String, value: Boolean) {
        setCustomKey(key, value.toString())
    }

    fun recordException(throwable: Throwable) {
        val entry = "${timestamp()} [ERROR] ${throwable.localizedMessage}\n${throwable.stackTraceToString()}"
        Log.e(TAG, "Recorded Exception", throwable)
        addToBuffer(entry)
    }

    fun getDiagnosticReport(context: Context): String {
        val sb = StringBuilder()
        sb.appendLine("=== Project Asteria Diagnostic Report ===")
        sb.appendLine("Generated: ${timestamp()}")
        sb.appendLine("App Package: ${context.packageName}")
        sb.appendLine("Android OS Version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        sb.appendLine("Device Model: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        sb.appendLine("\n--- Log Buffer ---")
        synchronized(logBuffer) {
            if (logBuffer.isEmpty()) {
                sb.appendLine("(No entries recorded)")
            } else {
                logBuffer.forEach { sb.appendLine(it) }
            }
        }
        return sb.toString()
    }

    fun saveReportToFile(context: Context): File {
        val reportFile = File(context.cacheDir, "asteria-diagnostics.txt")
        reportFile.writeText(getDiagnosticReport(context))
        return reportFile
    }

    fun simulateCrash(): Nothing = throw RuntimeException("Simulated crash from CrashReporter")

    private fun addToBuffer(entry: String) {
        synchronized(logBuffer) {
            if (logBuffer.size >= MAX_LOG_ENTRIES) {
                logBuffer.removeAt(0)
            }
            logBuffer.add(entry)
        }
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
    }
}

