package space.o4bit.projectasteria.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.AsteriaApplication
import space.o4bit.projectasteria.data.local.LaunchEntity
import space.o4bit.projectasteria.data.worker.LaunchReminderWorker
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Full-screen detail view for a specific launch.
 * Features an efficient countdown using produceState (single coroutine ticker)
 * and a haptic reminder toggle button backed by DataStore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaunchDetailScreen(
    launchId: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as AsteriaApplication
    val launchDao = remember { application.database.launchDao() }
    val reminderPrefs = remember { application.reminderPreferences }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Collect the launch from Room
    val launch by launchDao.getLaunchByIdFlow(launchId).collectAsState(initial = null)

    // Collect reminder state
    val remindedIds by reminderPrefs.remindedLaunchIds.collectAsState(initial = emptySet())
    val isReminded = remindedIds.contains(launchId)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Launch Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { innerPadding ->
        val currentLaunch = launch
        if (currentLaunch == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero image
                if (!currentLaunch.image.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(currentLaunch.image)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Launch Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    )
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    // Title
                    Text(
                        text = currentLaunch.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Efficient countdown using produceState — single coroutine ticker
                    EfficientCountdown(launchNet = currentLaunch.net)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Reminder toggle button with haptic feedback
                    ReminderButton(
                        isReminded = isReminded,
                        onToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                if (isReminded) {
                                    reminderPrefs.removeReminder(launchId)
                                    // Cancel the work if possible
                                    WorkManager.getInstance(context)
                                        .cancelAllWorkByTag("launch_reminder_$launchId")
                                } else {
                                    reminderPrefs.setReminder(launchId)
                                    scheduleLaunchReminder(context, currentLaunch)
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Status
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Status",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Status: ${currentLaunch.statusName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (currentLaunch.statusDescription.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentLaunch.statusDescription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Provider
                    if (!currentLaunch.providerName.isNullOrEmpty()) {
                        DetailRow(label = "Provider", value = currentLaunch.providerName)
                    }

                    // Scheduled time
                    val formattedDate = remember(currentLaunch.net) {
                        try {
                            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val date = parser.parse(currentLaunch.net)
                            date?.let {
                                val formatter = java.text.DateFormat.getDateTimeInstance(
                                    java.text.DateFormat.FULL,
                                    java.text.DateFormat.SHORT,
                                    java.util.Locale.getDefault()
                                )
                                formatter.format(it)
                            } ?: currentLaunch.net
                        } catch (e: Exception) {
                            currentLaunch.net
                        }
                    }
                    DetailRow(label = "Scheduled", value = formattedDate)

                    // Location
                    if (!currentLaunch.locationName.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "Location",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentLaunch.locationName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Pad
                    if (!currentLaunch.padName.isNullOrEmpty()) {
                        DetailRow(label = "Pad", value = currentLaunch.padName)
                    }

                    // Mission
                    if (!currentLaunch.missionName.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Mission: ${currentLaunch.missionName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!currentLaunch.missionDescription.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentLaunch.missionDescription,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.animateContentSize()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Efficient countdown timer using produceState — runs a single coroutine
 * that produces a countdown string. Coarse updates (hourly) for distant launches,
 * fine-grained (1s) for launches within 24h.
 */
@Composable
fun EfficientCountdown(launchNet: String) {
    val countdownText by produceState(initialValue = "Calculating...", key1 = launchNet) {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val targetTime = parser.parse(launchNet)?.time ?: run {
            value = "Date unknown"
            return@produceState
        }

        while (isActive) {
            val diff = targetTime - System.currentTimeMillis()

            if (diff <= 0) {
                value = "Launched!"
                break
            }

            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60

            value = when {
                days > 0 -> "T-${days}d ${hours}h ${minutes}m"
                else -> "T-${hours}h ${minutes}m ${seconds}s"
            }

            // Adaptive delay: if >1 day away, update every minute. Else every second.
            delay(if (days > 0) 60_000L else 1_000L)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = countdownText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/**
 * Animated reminder toggle button with haptic feedback.
 * Shows filled bell when active, outlined when inactive.
 */
@Composable
fun ReminderButton(
    isReminded: Boolean,
    onToggle: () -> Unit
) {
    val icon = if (isReminded) Icons.Filled.Notifications else Icons.Outlined.Notifications
    val label = if (isReminded) "Reminder Set" else "Set Reminder"
    val containerColor = if (isReminded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh

    Button(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = if (isReminded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Schedules a WorkManager reminder for 15 minutes before the launch.
 */
private fun scheduleLaunchReminder(context: android.content.Context, launch: LaunchEntity) {
    val data = Data.Builder()
        .putString("launch_id", launch.id)
        .putString("launch_name", launch.name)
        .build()

    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val targetDate = parser.parse(launch.net)?.time ?: return
    val delayMs = targetDate - TimeUnit.MINUTES.toMillis(15) - System.currentTimeMillis()

    if (delayMs > 0) {
        val workRequest = OneTimeWorkRequestBuilder<LaunchReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("launch_reminder_${launch.id}")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
