package space.o4bit.projectasteria.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.AsteriaApplication
import space.o4bit.projectasteria.data.local.LaunchEntity
import space.o4bit.projectasteria.data.repository.LaunchRepository
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.TimeUnit
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import space.o4bit.projectasteria.data.worker.LaunchReminderWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaunchScreen(
    onSettingsClick: () -> Unit = {},
    onLaunchClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as AsteriaApplication
    val repository = remember { 
        LaunchRepository(
            launchDao = application.database.launchDao(),
            sortingPreferences = application.sortingPreferences
        ) 
    }
    
    val launches by repository.launches.collectAsState(initial = emptyList())
    var isLoading by remember { mutableStateOf(launches.isEmpty()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    val isAscending by application.sortingPreferences.isLaunchesAscending.collectAsState(initial = true)
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(Unit) {
        if (launches.isEmpty()) {
            scope.launch {
                isLoading = true
                errorMessage = null
                try {
                    repository.refreshUpcomingLaunches()
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Failed to fetch launch data."
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Upcoming Launches") },
                actions = {
                    // Sort chip: captures first visible key before toggling, then scrolls back to it
                    FilterChip(
                        selected = !isAscending,
                        onClick = {
                            scope.launch {
                                application.sortingPreferences.toggleLaunchSort()
                                // Snap to top so cards reorder in place
                                listState.scrollToItem(0)
                            }
                        },
                        label = { Text(if (isAscending) "Soonest" else "Latest") },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading && launches.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (!errorMessage.isNullOrEmpty() && launches.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = errorMessage!!)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                repository.refreshUpcomingLaunches()
                                errorMessage = null
                            } catch (e: Exception) {
                                errorMessage = e.message
                            } finally {
                                isLoading = false
                            }
                        }
                    }) {
                        Text("Retry")
                    }
                }
            } else if (launches.isEmpty()) {
                Text(
                    text = "No upcoming launches found.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(launches, key = { it.id }) { launch ->
                        LaunchCard(
                            launch = launch,
                            onClick = { onLaunchClick(launch.id) },
                            onShowSnackbar = { message ->
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LaunchCard(
    launch: LaunchEntity,
    onClick: () -> Unit = {},
    onShowSnackbar: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as AsteriaApplication
    val reminderPrefs = remember { application.reminderPreferences }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    // Collect reminder state
    val remindedIds by reminderPrefs.remindedLaunchIds.collectAsState(initial = emptySet())
    val isReminded = remindedIds.contains(launch.id)

    // Parse UTC Date
    val formattedDate = remember(launch.net) {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(launch.net)
            date?.let {
                val formatter = java.text.DateFormat.getDateTimeInstance(
                    java.text.DateFormat.MEDIUM, 
                    java.text.DateFormat.SHORT, 
                    java.util.Locale.getDefault()
                )
                formatter.format(it)
            } ?: launch.net
        } catch (e: Exception) {
            launch.net
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "scale")

    space.o4bit.projectasteria.ui.components.settings.AsteriaCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
    ) {
        Column {
            if (!launch.image.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(launch.image)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Photo of ${launch.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                )
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = launch.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Status",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Status: ${launch.statusName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Scheduled: $formattedDate",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (!launch.locationName.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Location",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = launch.locationName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!launch.missionDescription.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    var isExpanded by rememberSaveable { mutableStateOf(false) }
                    Text(
                        text = launch.missionDescription!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.animateContentSize()
                    )
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(if (isExpanded) "Less" else "More")
                    }
                }
                
                // Add Countdown Timer & Haptic Reminder Bell
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LaunchCountdownTimer(launchNet = launch.net)
                    
                    // Interactive haptic reminder bell
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                if (isReminded) {
                                    reminderPrefs.removeReminder(launch.id)
                                    WorkManager.getInstance(context)
                                        .cancelAllWorkByTag("launch_reminder_${launch.id}")
                                    onShowSnackbar("Reminder removed")
                                } else {
                                    reminderPrefs.setReminder(launch.id)
                                    // Schedule WorkManager reminder
                                    val data = Data.Builder()
                                        .putString("launch_id", launch.id)
                                        .putString("launch_name", launch.name)
                                        .build()

                                    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                                        timeZone = TimeZone.getTimeZone("UTC")
                                    }
                                    val targetDate = parser.parse(launch.net)?.time ?: return@launch
                                    val delayMs = targetDate - TimeUnit.MINUTES.toMillis(15) - System.currentTimeMillis()

                                    if (delayMs > 0) {
                                        val workRequest = OneTimeWorkRequestBuilder<LaunchReminderWorker>()
                                            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                                            .setInputData(data)
                                            .addTag("launch_reminder_${launch.id}")
                                            .build()
                                        WorkManager.getInstance(context).enqueue(workRequest)
                                        onShowSnackbar("Reminder set for ${launch.name}")
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isReminded) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = if (isReminded) "Cancel Reminder for ${launch.name}" else "Set Reminder for ${launch.name}",
                            tint = if (isReminded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun LaunchCountdownTimer(launchNet: String) {
    var countdownText by remember { mutableStateOf("Calculating...") }

    LaunchedEffect(launchNet) {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val targetDate = parser.parse(launchNet)?.time ?: return@LaunchedEffect

        while (isActive) {
            val now = System.currentTimeMillis()
            val diff = targetDate - now

            if (diff <= 0) {
                countdownText = "Launched!"
                break
            }

            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60

            countdownText = when {
                days > 0 -> "T-Minus: ${days}d ${hours}h" // coarse updates for days
                else -> "T-Minus: ${hours}h ${minutes}m ${seconds}s"
            }

            // Optimize delay: if more than a day away, update every hour. Else update every second.
            if (days > 0) {
                delay(TimeUnit.HOURS.toMillis(1))
            } else {
                delay(1000)
            }
        }
    }

    Text(
        text = countdownText,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.tertiary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    )
}
