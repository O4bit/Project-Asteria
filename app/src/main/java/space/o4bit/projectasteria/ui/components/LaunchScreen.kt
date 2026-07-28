package space.o4bit.projectasteria.ui.components

import android.content.Context
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.AsteriaApplication
import space.o4bit.projectasteria.data.local.LaunchEntity
import space.o4bit.projectasteria.data.model.LaunchSortBy
import space.o4bit.projectasteria.data.repository.LaunchRepository
import space.o4bit.projectasteria.data.worker.LaunchReminderWorker
import space.o4bit.projectasteria.ui.viewmodels.LaunchUiState
import space.o4bit.projectasteria.ui.viewmodels.LaunchViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

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
    val viewModel: LaunchViewModel = viewModel(
        factory = LaunchViewModel.Factory(repository, application.backgroundPreferences)
    )

    val uiState by viewModel.uiState.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()

    var showSortSheet by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val listState = rememberSaveable(sortBy, sortDirection, saver = LazyListState.Saver) {
        LazyListState(0, 0)
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Upcoming Launches") },
                actions = {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                                onRefresh = {
                                    scope.launch {
                                        isRefreshing = true
                                        viewModel.refresh()
                                        isRefreshing = false
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            ) {
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
            }
        }

        if (showSortSheet) {
            SortOptionBottomSheet(
                options = LaunchSortBy.entries,
                selectedOption = sortBy,
                getOptionLabel = { it.label },
                onOptionSelected = { viewModel.setSortBy(it) },
                onDismissRequest = { showSortSheet = false }
            )
        }
    }
}

@Composable
fun LaunchCard(
    launch: LaunchEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onShowSnackbar: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as AsteriaApplication
    val reminderPrefs = remember { application.reminderPreferences }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val remindedIds by reminderPrefs.remindedLaunchIds.collectAsState(initial = emptySet())
    val isReminded = remindedIds.contains(launch.id)

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
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .semantics(mergeDescendants = true) {}
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

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            if (isReminded) {
                                reminderPrefs.removeReminder(launch.id)
                                cancelLaunchReminder(context, launch.id)
                                onShowSnackbar("Reminder cancelled for ${launch.name}")
                            } else {
                                reminderPrefs.setReminder(launch.id)
                                scheduleLaunchReminder(context, launch)
                                onShowSnackbar("Reminder set 15m before ${launch.name}")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isReminded) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = if (isReminded) "Cancel reminder" else "Set reminder",
                        tint = if (isReminded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

private fun scheduleLaunchReminder(context: Context, launch: LaunchEntity) {
    val workManager = WorkManager.getInstance(context)
    val delayMillis = launch.netMillis - System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(15)

    if (delayMillis > 0) {
        val inputData = Data.Builder()
            .putString("LAUNCH_ID", launch.id)
            .putString("LAUNCH_NAME", launch.name)
            .build()

        val reminderWork = OneTimeWorkRequestBuilder<LaunchReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("reminder_${launch.id}")
            .build()

        workManager.enqueue(reminderWork)
    }
}

private fun cancelLaunchReminder(context: Context, launchId: String) {
    val workManager = WorkManager.getInstance(context)
    workManager.cancelAllWorkByTag("reminder_$launchId")
}
