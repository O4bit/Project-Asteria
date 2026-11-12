package space.o4bit.projectasteria.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.AsteriaApplication
import space.o4bit.projectasteria.data.preferences.SortingPreferencesRepository
import space.o4bit.projectasteria.data.repository.LaunchRepository
import space.o4bit.projectasteria.ui.components.settings.ActionGlassButton
import space.o4bit.projectasteria.ui.components.settings.AsteriaIcon
import space.o4bit.projectasteria.ui.components.settings.AsteriaSettingsDivider
import space.o4bit.projectasteria.ui.components.settings.GlassButton
import space.o4bit.projectasteria.ui.components.settings.SectionCard
import space.o4bit.projectasteria.ui.components.settings.SectionTitle
import space.o4bit.projectasteria.ui.components.settings.SettingsBottomPillDock
import space.o4bit.projectasteria.ui.theme.ThemedApp

class LaunchWidgetConfigActivity : ComponentActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)

        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(
            RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val app = applicationContext as AsteriaApplication
        setContent {
            ThemedApp {
                LaunchWidgetConfigScreen(
                    app = app,
                    appWidgetId = appWidgetId,
                    onSaveComplete = {
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        )
                        finish()
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LaunchWidgetConfigScreen(
    app: AsteriaApplication,
    appWidgetId: Int,
    onSaveComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val launchDao = remember { app.database.launchDao() }
    val backgroundPrefs = remember { app.backgroundPreferences }
    val launchRepository = remember {
        LaunchRepository(launchDao, SortingPreferencesRepository(app))
    }
    val allLaunches by launchDao.getOldestLaunches().collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedIds = remember { mutableStateListOf<String>() }
    var displayMode by remember {
        mutableStateOf(LaunchAppWidgetProvider.getWidgetDisplayMode(app, appWidgetId))
    }
    var searchQuery by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }

    val launchMap = remember(allLaunches) { allLaunches.associateBy { it.id } }

    LaunchedEffect(Unit) {
        val widgetIds = LaunchAppWidgetProvider.getWidgetLaunchIds(app, appWidgetId)
        val initialIds = widgetIds.ifEmpty {
            backgroundPrefs.orderedPinnedLaunchIds.first()
        }
        selectedIds.clear()
        selectedIds.addAll(initialIds)
        if (launchDao.getOldestLaunches().first().isEmpty()) {
            isRefreshing = true
            try {
                launchRepository.refreshUpcomingLaunches(20)
            } catch (_: Exception) {
            } finally {
                isRefreshing = false
            }
        }
    }

    val selectedLaunches = remember(selectedIds.toList(), launchMap) {
        selectedIds.mapNotNull { launchMap[it] }
    }
    val filteredLaunches = remember(allLaunches, searchQuery) {
        val query = searchQuery.trim().lowercase(Locale.ROOT)
        if (query.isEmpty()) {
            allLaunches
        } else {
            allLaunches.filter { launch ->
                launch.name.lowercase(Locale.ROOT).contains(query) ||
                    (launch.providerName?.lowercase(Locale.ROOT)?.contains(query) == true) ||
                    (launch.padName?.lowercase(Locale.ROOT)?.contains(query) == true) ||
                    (launch.locationName?.lowercase(Locale.ROOT)?.contains(query) == true)
            }
        }
    }
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    navigationIcon = {
                        IconButton(onClick = onCancel) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsteriaIcon(
                                icon = Icons.Default.RocketLaunch,
                                modifier = Modifier.size(36.dp)
                            )
                            Column {
                                Text(
                                    text = "Configure Launch Widget",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (selectedIds.isEmpty()) {
                                        "Auto-tracking upcoming launches"
                                    } else {
                                        "${selectedIds.size} specific launches selected"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (!isRefreshing) {
                                    scope.launch {
                                        isRefreshing = true
                                        try {
                                            launchRepository.refreshUpcomingLaunches(20)
                                        } catch (_: Exception) {
                                        } finally {
                                            isRefreshing = false
                                        }
                                    }
                                }
                            }
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh launches"
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 128.dp + navBarHeight
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle(
                                title = "Widget Layout Mode",
                                icon = Icons.Default.ViewAgenda
                            )
                            SectionCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    GlassButton(
                                        onClick = { displayMode = LaunchAppWidgetProvider.DISPLAY_MODE_MULTI },
                                        modifier = Modifier.weight(1f),
                                        isSelected = displayMode == LaunchAppWidgetProvider.DISPLAY_MODE_MULTI
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ViewAgenda,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Multi-Stack",
                                                fontWeight = if (displayMode == LaunchAppWidgetProvider.DISPLAY_MODE_MULTI) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Medium
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    GlassButton(
                                        onClick = { displayMode = LaunchAppWidgetProvider.DISPLAY_MODE_SINGLE },
                                        modifier = Modifier.weight(1f),
                                        isSelected = displayMode == LaunchAppWidgetProvider.DISPLAY_MODE_SINGLE
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ViewDay,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Single Focus",
                                                fontWeight = if (displayMode == LaunchAppWidgetProvider.DISPLAY_MODE_SINGLE) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Medium
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedLaunches.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SectionTitle(
                                    title = "Selected Order (${selectedLaunches.size})",
                                    icon = Icons.Default.RocketLaunch,
                                    action = {
                                        TextButton(
                                            onClick = { selectedIds.clear() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Clear all")
                                        }
                                    }
                                )
                                SectionCard {
                                    selectedLaunches.forEachIndexed { index, launch ->
                                        if (index > 0) {
                                            AsteriaSettingsDivider()
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = launch.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = listOfNotNull(
                                                        launch.providerName,
                                                        launch.locationName
                                                    ).joinToString(" \u2022 "),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (index > 0) {
                                                        val item = selectedIds.removeAt(index)
                                                        selectedIds.add(index - 1, item)
                                                    }
                                                },
                                                enabled = index > 0,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Move up",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (index < selectedIds.lastIndex) {
                                                        val item = selectedIds.removeAt(index)
                                                        selectedIds.add(index + 1, item)
                                                    }
                                                },
                                                enabled = index < selectedLaunches.lastIndex,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Move down",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { selectedIds.remove(launch.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            placeholder = { Text("Search upcoming launches...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                            }
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle(
                                title = "Available Launches (${filteredLaunches.size})",
                                icon = Icons.Default.Search
                            )
                            SectionCard {
                                if (filteredLaunches.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No matching upcoming launches found",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    filteredLaunches.forEachIndexed { index, launch ->
                                        if (index > 0) {
                                            AsteriaSettingsDivider()
                                        }
                                        val isSelected = selectedIds.contains(launch.id)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isSelected) {
                                                        selectedIds.remove(launch.id)
                                                    } else {
                                                        selectedIds.add(launch.id)
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        if (!selectedIds.contains(launch.id)) {
                                                            selectedIds.add(launch.id)
                                                        }
                                                    } else {
                                                        selectedIds.remove(launch.id)
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = launch.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = listOfNotNull(
                                                        launch.providerName,
                                                        launch.locationName
                                                    ).joinToString(" \u2022 "),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
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
        }

        SettingsBottomPillDock(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            if (selectedIds.isNotEmpty()) {
                ActionGlassButton(
                    onClick = { selectedIds.clear() },
                    icon = Icons.Default.Close,
                    contentDescription = "Clear selection",
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                )
            }
            GlassButton(
                onClick = {
                    val finalIds = selectedIds.toList()
                    scope.launch {
                        LaunchAppWidgetProvider.setWidgetLaunchIds(app, appWidgetId, finalIds)
                        LaunchAppWidgetProvider.setWidgetDisplayMode(app, appWidgetId, displayMode)
                        backgroundPrefs.setPinnedLaunches(finalIds)
                        LaunchAppWidgetProvider.updateSingleWidget(app, appWidgetId)
                        LaunchAppWidgetProvider.updateAllWidgets(app)
                        onSaveComplete()
                    }
                },
                modifier = Modifier.weight(1f),
                isSelected = true
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (selectedIds.isEmpty()) {
                            "Use Automatic Upcoming"
                        } else {
                            "Apply ${selectedIds.size} Launches"
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
