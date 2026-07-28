package space.o4bit.projectasteria.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Local, privacy-preserving diagnostic logger.
 * Keeps an in-memory rotating buffer of the last 500 log events.
 * Logs stay on device and can be exported by the user from Settings -> About.
 */
object DiagnosticLogger {
    private const val MAX_LOGS = 500
    private val logBuffer = ConcurrentLinkedQueue<String>()

    init {
        log("DiagnosticLogger", "Diagnostic logger initialized")
    }

    fun log(tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = buildString {
            append("[$timestamp] [$tag] $message")
            if (throwable != null) {
                append("\nException: ${throwable.localizedMessage}")
                append("\nStacktrace: ${throwable.stackTraceToString().take(500)}")
            }
        }
        logBuffer.add(entry)
        while (logBuffer.size > MAX_LOGS) {
            logBuffer.poll()
        }
    }

    fun exportLogs(context: Context): File {
        val logsDir = File(context.cacheDir, "diagnostics")
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }
        val file = File(logsDir, "asteria_diagnostics_${System.currentTimeMillis()}.txt")
        val header = "Project Asteria Diagnostic Log\nExported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\nTotal Log Entries: ${logBuffer.size}\n========================================\n\n"
        file.writeText(header + logBuffer.joinToString("\n---\n"))
        return file
    }

    fun shareLogs(context: Context) {
        val file = exportLogs(context)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Project Asteria Diagnostic Log")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Diagnostic Log")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun getLogCount(): Int = logBuffer.size

    fun clearLogs() {
        logBuffer.clear()
        log("DiagnosticLogger", "Logs cleared by user")
    }
}
