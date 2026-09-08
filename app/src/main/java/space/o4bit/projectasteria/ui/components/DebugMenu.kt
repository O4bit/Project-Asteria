package space.o4bit.projectasteria.ui.components

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.BuildConfig
import space.o4bit.projectasteria.data.model.AstronomyPicture
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture
import space.o4bit.projectasteria.utils.TestCrashActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebugMenuDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Developer Debug Menu",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Notification Testing",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                DebugDialogItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "Trigger APOD Notification",
                    subtitle = "Test daily space discovery alert with image & custom icon",
                    onClick = {
                        checkAndTriggerNotification(context) {
                            scope.launch {
                                triggerTestApodNotification(context)
                                Toast.makeText(context, "APOD notification sent!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                DebugDialogItem(
                    icon = Icons.Default.RocketLaunch,
                    title = "Trigger Launch Alert",
                    subtitle = "Test 15-minute countdown launch reminder alert",
                    onClick = {
                        checkAndTriggerNotification(context) {
                            triggerTestLaunchNotification(context)
                            Toast.makeText(context, "Launch notification sent!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                Text(
                    text = "Diagnostic Tools",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                DebugDialogItem(
                    icon = Icons.Default.BugReport,
                    title = "Crash Reporter Simulation",
                    subtitle = "Test exception handling, ANRs & breadcrumb logging",
                    onClick = {
                        launchTestCrashActivity(context)
                        onDismiss()
                    }
                )

                DebugDialogItem(
                    icon = Icons.Default.ContentCopy,
                    title = "Copy System Info",
                    subtitle = "Copy device, OS, and build specs to clipboard",
                    onClick = {
                        copySystemInfo(context)
                        Toast.makeText(context, "System info copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // Build specs footer
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Build: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Package: ${context.packageName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "API Level: ${Build.VERSION.SDK_INT} (${Build.MANUFACTURER} ${Build.MODEL})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DebugDialogItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun checkAndTriggerNotification(context: Context, onPermitted: () -> Unit) {
    val notificationManager = androidx.core.app.NotificationManagerCompat.from(context)

    if (!notificationManager.areNotificationsEnabled()) {
        Toast.makeText(context, "Notifications disabled for Asteria. Opening settings...", Toast.LENGTH_LONG).show()
        val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        val activity = context.findActivity()
        if (activity is space.o4bit.projectasteria.MainActivity) {
            activity.requestNotificationPermission()
            Toast.makeText(context, "Please allow notification permission and tap again", Toast.LENGTH_LONG).show()
        } else if (activity != null) {
            androidx.core.app.ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
            Toast.makeText(context, "Please allow notification permission and tap again", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Notification permission required. Enable in Android Settings.", Toast.LENGTH_LONG).show()
            val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
        return
    }
    onPermitted()
}

private suspend fun triggerTestApodNotification(context: Context) {
    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val sampleApod = AstronomyPicture(
        date = dateStr,
        explanation = "A debug space discovery notification from Project Asteria. Features the updated monochrome vector icon, picture expander, and deep link navigation.",
        hdUrl = "https://apod.nasa.gov/apod/image/2408/NGC6744_Selby_960.jpg",
        mediaType = "image",
        title = "Debug: Deep Cosmos Discovery",
        url = "https://apod.nasa.gov/apod/image/2408/NGC6744_Selby_960.jpg"
    )
    val enhanced = EnhancedAstronomyPicture(
        astronomyPicture = sampleApod,
        shortFact = "Light from this cosmic nebula traveled over millions of years to reach our sensors.",
        notificationTitle = "Today's Space Discovery: Deep Cosmos",
        notificationBody = "Test notification for verifying notification icon alignment and delivery."
    )
    val notifId = (System.currentTimeMillis() % 100000).toInt() + 100
    SpaceNotificationBuilder.showAstronomyNotification(context, enhanced, customNotificationId = notifId)
}

private fun triggerTestLaunchNotification(context: Context) {
    SpaceNotificationBuilder.showLaunchReminderNotification(
        context = context,
        launchName = "Falcon Heavy | Europa Clipper",
        launchId = "debug-launch-${System.currentTimeMillis()}"
    )
}

private fun copySystemInfo(context: Context) {
    val info = buildString {
        appendLine("Project Asteria Debug Info")
        appendLine("Version: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
        appendLine("Package: ${context.packageName}")
        appendLine("Android SDK: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
        appendLine("Board: ${Build.BOARD}")
        appendLine("Timestamp: ${Date()}")
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Asteria System Info", info))
}

private fun launchTestCrashActivity(context: Context) {
    val intent = Intent(context, TestCrashActivity::class.java)
    context.startActivity(intent)
}
