package space.o4bit.projectasteria.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.AsteriaApplication
import space.o4bit.projectasteria.data.local.LaunchEntity
import space.o4bit.projectasteria.data.model.LaunchSortBy
import space.o4bit.projectasteria.data.repository.LaunchRepository
import space.o4bit.projectasteria.data.worker.LaunchReminderScheduler
import space.o4bit.projectasteria.ui.viewmodels.LaunchUiState
import space.o4bit.projectasteria.ui.viewmodels.LaunchViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaunchScreen(
    onSettingsClick: () -> Unit = {},
    onLaunchClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as AsteriaApplication
    val reminderScheduler = remember { LaunchReminderScheduler(WorkManager.getInstance(context)) }
    val repository = remember {
        LaunchRepository(
            launchDao = application.database.launchDao(),
            sortingPreferences = application.sortingPreferences,
            reminderScheduler = reminderScheduler,
            reminderPreferences = application.reminderPreferences,
            backgroundPreferences = application.backgroundPreferences
        )
    }
    val viewModel: LaunchViewModel = viewModel(
        factory = LaunchViewModel.Factory(repository, application.backgroundPreferences)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()
    val sortDirection by viewModel.sortDirection.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val refreshError by viewModel.refreshError.collectAsStateWithLifecycle()

    val remindedIds by application.reminderPreferences.remindedLaunchIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val pinnedIds by application.backgroundPreferences.pinnedLaunchIds.collectAsStateWithLifecycle(initialValue = emptySet())

    var showSortSheet by remember { mutableStateOf(false) }
    var showPinnedSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val listState = rememberSaveable(sortBy, sortDirection, saver = LazyListState.Saver) {
        LazyListState(0, 0)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(refreshError) {
        refreshError?.let {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        modifier = Modifier.padding(start = 12.dp),
                        text = "Upcoming Launches",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showPinnedSheet = true }) {
                        Icon(Icons.Default.PushPin, contentDescription = "Pinned Widget Launches")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            SortFilterHeader(
                sortLabel = sortBy.label,
                sortDirection = sortDirection,
                onOpenSortSheet = { showSortSheet = true },
                onToggleDirection = { viewModel.toggleDirection() }
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is LaunchUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is LaunchUiState.Error -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = state.message)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("Retry")
                            }
                        }
                    }
                    is LaunchUiState.Success -> {
                        if (state.launches.isEmpty()) {
                            Text(
                                text = "No upcoming launches found.",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = { viewModel.refresh() },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (isTablet) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 360.dp),
                                        contentPadding = PaddingValues(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clipToBounds()
                                    ) {
                                        items(
                                            items = state.launches,
                                            key = { "${sortBy.name}_${sortDirection.name}_${it.id}" }
                                        ) { launch ->
                                            val isReminded = remindedIds.contains(launch.id)
                                            val isPinned = pinnedIds.contains(launch.id)
                                            LaunchCard(
                                                launch = launch,
                                                isReminded = isReminded,
                                                isPinned = isPinned,
                                                onClick = { onLaunchClick(launch.id) },
                                                onTogglePin = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    scope.launch {
                                                        application.backgroundPreferences.togglePinLaunch(launch.id)
                                                        space.o4bit.projectasteria.widget.LaunchAppWidgetProvider.updateAllWidgets(context)
                                                        snackbarHostState.currentSnackbarData?.dismiss()
                                                        snackbarHostState.showSnackbar(
                                                            if (isPinned) "Unpinned from Widget" else "Pinned ${launch.name} to Widget"
                                                        )
                                                    }
                                                },
                                                onToggleReminder = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    scope.launch {
                                                        if (isReminded) {
                                                            application.reminderPreferences.removeReminder(launch.id)
                                                            reminderScheduler.cancel(launch.id)
                                                            snackbarHostState.currentSnackbarData?.dismiss()
                                                            snackbarHostState.showSnackbar("Reminder cancelled for ${launch.name}")
                                                        } else {
                                                            val scheduled = reminderScheduler.schedule(launch)
                                                            if (scheduled) {
                                                                application.reminderPreferences.setReminder(launch.id)
                                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                                snackbarHostState.showSnackbar("Reminder set 15m before ${launch.name}")
                                                            } else {
                                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                                snackbarHostState.showSnackbar("Cannot set reminder for past or unknown launch time")
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        state = listState,
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clipToBounds()
                                    ) {
                                        items(
                                            items = state.launches,
                                            key = { "${sortBy.name}_${sortDirection.name}_${it.id}" }
                                        ) { launch ->
                                            val isReminded = remindedIds.contains(launch.id)
                                            val isPinned = pinnedIds.contains(launch.id)
                                            LaunchCard(
                                                launch = launch,
                                                isReminded = isReminded,
                                                isPinned = isPinned,
                                                onClick = { onLaunchClick(launch.id) },
                                                onTogglePin = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    scope.launch {
                                                        application.backgroundPreferences.togglePinLaunch(launch.id)
                                                        space.o4bit.projectasteria.widget.LaunchAppWidgetProvider.updateAllWidgets(context)
                                                        snackbarHostState.currentSnackbarData?.dismiss()
                                                        snackbarHostState.showSnackbar(
                                                            if (isPinned) "Unpinned from Widget" else "Pinned ${launch.name} to Widget"
                                                        )
                                                    }
                                                },
                                                onToggleReminder = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    scope.launch {
                                                        if (isReminded) {
                                                            application.reminderPreferences.removeReminder(launch.id)
                                                            reminderScheduler.cancel(launch.id)
                                                            snackbarHostState.currentSnackbarData?.dismiss()
                                                            snackbarHostState.showSnackbar("Reminder cancelled for ${launch.name}")
                                                        } else {
                                                            val scheduled = reminderScheduler.schedule(launch)
                                                            if (scheduled) {
                                                                application.reminderPreferences.setReminder(launch.id)
                                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                                snackbarHostState.showSnackbar("Reminder set 15m before ${launch.name}")
                                                            } else {
                                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                                snackbarHostState.showSnackbar("Cannot set reminder for past or unknown launch time")
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showSortSheet) {
            SortOptionBottomSheet(
                options = LaunchSortBy.entries,
                selectedOption = sortBy,
                getOptionLabel = { it.label },
                onOptionSelected = { newSort ->
                    viewModel.setSortBy(newSort)
                    showSortSheet = false
                },
                onDismissRequest = { showSortSheet = false }
            )
        }
        if (showPinnedSheet) {
            PinnedLaunchesSheet(
                onDismissRequest = { showPinnedSheet = false },
                onLaunchClick = onLaunchClick
            )
        }
    }
}

@Composable
fun LaunchCard(
    launch: LaunchEntity,
    isReminded: Boolean,
    isPinned: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onTogglePin: () -> Unit = {},
    onToggleReminder: () -> Unit = {}
) {
    val formattedDate = remember(launch.net) {
        val parsed = runCatching { Instant.parse(launch.net) }.getOrNull()
        if (parsed != null) {
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withZone(ZoneId.systemDefault())
            formatter.format(parsed)
        } else {
            launch.net
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "scale")

    space.o4bit.projectasteria.ui.components.settings.AsteriaCard(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = launch.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (isPinned) "Unpin from Home" else "Pin to Home",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = onToggleReminder) {
                        Icon(
                            imageVector = if (isReminded) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = if (isReminded) "Cancel reminder" else "Set reminder",
                            tint = if (isReminded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Launch Time: $formattedDate",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!launch.providerName.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Provider: ${launch.providerName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!launch.locationName.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Location: ${launch.locationName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!launch.statusName.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Status: ${launch.statusName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}