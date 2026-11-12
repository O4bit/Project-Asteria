package space.o4bit.projectasteria.ui.components
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PushPin
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
import androidx.work.WorkManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.AsteriaApplication
import space.o4bit.projectasteria.data.local.LaunchEntity
import space.o4bit.projectasteria.data.worker.LaunchReminderScheduler
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    val bgPrefs = remember { application.backgroundPreferences }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val launch by launchDao.getLaunchByIdFlow(launchId).collectAsState(initial = null)
    val remindedIds by reminderPrefs.remindedLaunchIds.collectAsState(initial = emptySet())
    val isReminded = remindedIds.contains(launchId)
    val pinnedIds by bgPrefs.pinnedLaunchIds.collectAsState(initial = emptySet())
    val isPinned = pinnedIds.contains(launchId)
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
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                bgPrefs.togglePinLaunch(launchId)
                                space.o4bit.projectasteria.widget.LaunchAppWidgetProvider.updateAllWidgets(context)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (isPinned) "Unpin from Widget" else "Pin to Widget",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
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
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val configuration = LocalConfiguration.current
            val isTablet = configuration.smallestScreenWidthDp >= 600
            if (isTablet) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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
                                    .height(280.dp)
                                    .clip(RoundedCornerShape(20.dp))
                            )
                        }
                        Text(
                            text = currentLaunch.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        EfficientCountdown(launchNet = currentLaunch.net)
                        ReminderButton(
                            isReminded = isReminded,
                            onToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    val scheduler = LaunchReminderScheduler(WorkManager.getInstance(context))
                                    if (isReminded) {
                                        reminderPrefs.removeReminder(launchId)
                                        scheduler.cancel(launchId)
                                    } else {
                                        val scheduled = scheduler.schedule(currentLaunch)
                                        if (scheduled) {
                                            reminderPrefs.setReminder(launchId)
                                        }
                                    }
                                }
                            }
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        LaunchStatusAndDetails(currentLaunch = currentLaunch)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
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
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = currentLaunch.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        EfficientCountdown(launchNet = currentLaunch.net)
                        ReminderButton(
                            isReminded = isReminded,
                            onToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    val scheduler = LaunchReminderScheduler(WorkManager.getInstance(context))
                                    if (isReminded) {
                                        reminderPrefs.removeReminder(launchId)
                                        scheduler.cancel(launchId)
                                    } else {
                                        val scheduled = scheduler.schedule(currentLaunch)
                                        if (scheduled) {
                                            reminderPrefs.setReminder(launchId)
                                        }
                                    }
                                }
                            }
                        )
                        LaunchStatusAndDetails(currentLaunch = currentLaunch)
                    }
                }
            }
        }
    }
}

@Composable
private fun LaunchStatusAndDetails(currentLaunch: LaunchEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!currentLaunch.providerName.isNullOrEmpty()) {
                DetailRow(label = "Provider", value = currentLaunch.providerName)
            }
            val formattedDate = remember(currentLaunch.net) {
                val parsed = runCatching { Instant.parse(currentLaunch.net) }.getOrNull()
                if (parsed != null) {
                    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)
                        .withZone(ZoneId.systemDefault())
                    formatter.format(parsed)
                } else {
                    currentLaunch.net
                }
            }
            DetailRow(label = "Scheduled", value = formattedDate)
            if (!currentLaunch.locationName.isNullOrEmpty()) {
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
            if (!currentLaunch.padName.isNullOrEmpty()) {
                DetailRow(label = "Pad", value = currentLaunch.padName)
            }
        }
    }
    if (!currentLaunch.missionName.isNullOrEmpty() || !currentLaunch.missionDescription.isNullOrEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!currentLaunch.missionName.isNullOrEmpty()) {
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
@Composable
fun EfficientCountdown(launchNet: String) {
    val countdownText by produceState(initialValue = "Calculating...", key1 = launchNet) {
        val targetInstant = runCatching { Instant.parse(launchNet) }.getOrNull()
        if (targetInstant == null) {
            value = "Date unknown"
            return@produceState
        }
        while (isActive) {
            val now = Instant.now()
            val remaining = Duration.between(now, targetInstant)
            if (remaining.isNegative) {
                value = "Awaiting confirmed launch status"
                break
            }
            val days = remaining.toDays()
            val hours = remaining.toHours() % 24
            val minutes = remaining.toMinutes() % 60
            val seconds = remaining.seconds % 60
            value = when {
                days > 0 -> "T-${days}d ${hours}h ${minutes}m"
                else -> "T-${hours}h ${minutes}m ${seconds}s"
            }
            delay(if (days > 0) 60_000L else 1_000L)
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
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
        shape = RoundedCornerShape(16.dp)
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