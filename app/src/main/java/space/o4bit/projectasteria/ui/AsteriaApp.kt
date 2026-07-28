package space.o4bit.projectasteria.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Satellite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.AsteriaApplication
import space.o4bit.projectasteria.R
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture
import space.o4bit.projectasteria.data.preferences.BackgroundPreferencesRepository
import space.o4bit.projectasteria.data.preferences.UiHintsPreferencesRepository
import space.o4bit.projectasteria.data.repository.AsteroidRepository
import space.o4bit.projectasteria.data.repository.LaunchRepository
import space.o4bit.projectasteria.data.repository.SpaceRepository
import space.o4bit.projectasteria.ui.components.AnimatedBackground
import space.o4bit.projectasteria.ui.components.AsteroidScreen
import space.o4bit.projectasteria.ui.components.AstronomyExplanationCard
import space.o4bit.projectasteria.ui.components.AstronomyPictureCard
import space.o4bit.projectasteria.ui.components.BackgroundType
import space.o4bit.projectasteria.ui.components.IssScreen
import space.o4bit.projectasteria.ui.components.LaunchScreen
import space.o4bit.projectasteria.ui.components.SwipeHintOverlay
import space.o4bit.projectasteria.ui.components.settings.AsteriaBottomNavigation
import space.o4bit.projectasteria.ui.navigation.AsteriaNavGraph
import space.o4bit.projectasteria.ui.viewmodels.ApodUiState
import space.o4bit.projectasteria.ui.viewmodels.ApodViewModel
import space.o4bit.projectasteria.ui.viewmodels.LaunchUiState
import space.o4bit.projectasteria.ui.viewmodels.LaunchViewModel
import space.o4bit.projectasteria.util.NetworkConnectivityObserver

enum class MainTab(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    APOD("APOD", Icons.Filled.Image, Icons.Outlined.Image),
    ISS("ISS", Icons.Filled.Satellite, Icons.Outlined.Satellite),
    LAUNCHES("Launches", Icons.Filled.RocketLaunch, Icons.Outlined.RocketLaunch),
    ASTEROIDS("Asteroids", Icons.Filled.Public, Icons.Outlined.Public)
}

@Composable
fun AsteriaApp(
    openDirectlyFromNotification: Boolean = false,
    onRequestNotificationPermission: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as AsteriaApplication
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // ── Pager state persisted across config changes via rememberSaveable ──────
    val initialPage = rememberSaveable { 0 }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { MainTab.entries.size }
    )

    // ── Repositories & ViewModels ─────────────────────────────────────────────
    val spaceRepository = remember { SpaceRepository(application.database.apodDao()) }
    val launchRepository = remember {
        LaunchRepository(
            launchDao = application.database.launchDao(),
            sortingPreferences = application.sortingPreferences
        )
    }
    val asteroidRepository = remember {
        AsteroidRepository(
            asteroidDao = application.database.asteroidDao(),
            sortingPreferences = application.sortingPreferences
        )
    }
    val backgroundPrefs = remember { BackgroundPreferencesRepository(context) }
    val uiHintsPrefs = remember { UiHintsPreferencesRepository(context) }

    val apodViewModel: ApodViewModel =
        viewModel(factory = ApodViewModel.Factory(spaceRepository))
    val launchViewModel: LaunchViewModel =
        viewModel(factory = LaunchViewModel.Factory(launchRepository, backgroundPrefs))

    val apodState by apodViewModel.uiState.collectAsState()
    val launchState by launchViewModel.uiState.collectAsState()

    val backgroundTypeName by backgroundPrefs.backgroundType.collectAsState(initial = BackgroundType.SPACE.name)
    val backgroundType = BackgroundType.fromName(backgroundTypeName)
    val mainTabsHintShown by uiHintsPrefs.mainTabsHintShown.collectAsState(initial = false)

    val currentApodPicture = { (apodState as? ApodUiState.Success)?.picture }
    val launchSpeedMultiplier = (launchState as? LaunchUiState.Success)?.launchSpeedMultiplier ?: 1f

    // Track hint when user swipes tabs.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collectLatest { page ->
                if (page != 0 && !mainTabsHintShown) {
                    uiHintsPrefs.setMainTabsHintShown(true)
                }
            }
    }

    // Handle OPEN_FULLSCREEN notification extra — navigate to the fullscreen route.
    // Deep-link URI intents (asteria://image) are handled automatically by NavHost
    // because the intent-filter is declared in AndroidManifest.xml.
    LaunchedEffect(openDirectlyFromNotification) {
        if (openDirectlyFromNotification) {
            navController.navigate("fullscreen")
        }
    }

    AnimatedBackground(
        type = backgroundType,
        modifier = Modifier.fillMaxSize(),
        launchSpeedMultiplier = launchSpeedMultiplier
    ) {
        AsteriaNavGraph(
            navController = navController,
            spaceRepository = spaceRepository,
            currentApodPicture = currentApodPicture,
            onRequestNotificationPermission = onRequestNotificationPermission
        ) {
            // ── "main" destination: the tab pager ────────────────────────────
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    AsteriaBottomNavigation(
                        tabs = MainTab.entries.map { it.title },
                        selectedIcons = MainTab.entries.map { it.selectedIcon },
                        unselectedIcons = MainTab.entries.map { it.unselectedIcon },
                        selectedIndex = pagerState.currentPage,
                        onTabSelected = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = true,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (MainTab.entries[page]) {
                            MainTab.APOD -> ApodTab(
                                apodState = apodState,
                                onSettingsClick = { navController.navigate("settings") },
                                onHistoryClick  = { navController.navigate("history") },
                                onCardClick = {
                                    val date = currentApodPicture()
                                        ?.astronomyPicture?.date
                                    navController.navigate(
                                        if (date != null) "fullscreen?date=$date"
                                        else "fullscreen"
                                    )
                                },
                                onExplanationClick = {
                                    val date = currentApodPicture()
                                        ?.astronomyPicture?.date
                                    if (date != null) {
                                        navController.navigate("explanation/$date")
                                    }
                                },
                                onRetryClick = { apodViewModel.loadTodayApod() },
                                onToggleFavorite = { enhanced ->
                                    apodViewModel.toggleFavorite(enhanced.astronomyPicture.date, !enhanced.isFavorite)
                                }
                            )
                            MainTab.ASTEROIDS ->
                                AsteroidScreen(
                                    onSettingsClick = { navController.navigate("settings") }
                                )
                            MainTab.LAUNCHES ->
                                LaunchScreen(
                                    onSettingsClick = { navController.navigate("settings") },
                                    onLaunchClick = { launchId ->
                                        navController.navigate("launch_detail/$launchId")
                                    }
                                )
                            MainTab.ISS ->
                                IssScreen(
                                    onSettingsClick = { navController.navigate("settings") }
                                )
                        }
                    }

                    if (!mainTabsHintShown) {
                        SwipeHintOverlay(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 96.dp),
                            text = "Swipe to change tabs"
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// APOD tab composable
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApodTab(
    apodState: ApodUiState,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onCardClick: () -> Unit,
    onExplanationClick: () -> Unit,
    onRetryClick: () -> Unit,
    onToggleFavorite: (EnhancedAstronomyPicture) -> Unit = {}
) {
    val context = LocalContext.current
    val connectivityObserver = remember { NetworkConnectivityObserver(context) }
    val isOnline by connectivityObserver.observe().collectAsState(
        initial = connectivityObserver.isCurrentlyOnline()
    )
    var showOfflineDialog by remember { mutableStateOf(false) }

    if (showOfflineDialog) {
        AlertDialog(
            onDismissRequest = { showOfflineDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.outline_cloud_off_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("No Internet Connection") },
            text = {
                Text(
                    "You're currently offline. Project Asteria is displaying cached content " +
                            "from your last online session. Some features may be unavailable until you reconnect."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showOfflineDialog = false
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }) { Text("Open Wi-Fi Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showOfflineDialog = false }) { Text("Dismiss") }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("Project Asteria", style = MaterialTheme.typography.titleLarge)
                },
                actions = {
                    if (!isOnline) {
                        IconButton(
                            onClick = { showOfflineDialog = true },
                            modifier = Modifier.semantics {
                                contentDescription = "You are offline. Tap for details."
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.outline_cloud_off_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = onHistoryClick) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "View previous APODs"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isOnline) {
                    SmallFloatingActionButton(
                        onClick = { showOfflineDialog = true },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.outline_cloud_off_24),
                            contentDescription = "Offline — tap to see options"
                        )
                    }
                }
                FloatingActionButton(
                    onClick = onSettingsClick,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.semantics { contentDescription = "Open settings" }
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
    ) { paddingValues ->
    val isExpanded = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600

    if (isExpanded && apodState is ApodUiState.Success) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AstronomyPictureCard(
                    enhancedPicture = apodState.picture,
                    onCardClick = onCardClick,
                    onToggleFavorite = { onToggleFavorite(apodState.picture) }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                AstronomyExplanationCard(
                    explanation = apodState.picture.astronomyPicture.explanation,
                    onCardClick = onExplanationClick
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            when (apodState) {
                is ApodUiState.Loading -> item {
                    CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                }

                is ApodUiState.Error -> item {
                    // If we have cached content, show it with a slim error banner on top.
                    if (apodState.cachedPicture != null) {
                        Column {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = apodState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp, vertical = 8.dp
                                    )
                                )
                            }
                            AstronomyPictureCard(
                                enhancedPicture = apodState.cachedPicture,
                                onCardClick = onCardClick
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = apodState.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(16.dp))
                            FilledTonalButton(onClick = onRetryClick) { Text("Retry") }
                        }
                    }
                }

                is ApodUiState.Success -> {
                    item {
                        AstronomyPictureCard(
                            enhancedPicture = apodState.picture,
                            onCardClick = onCardClick,
                            onToggleFavorite = { onToggleFavorite(apodState.picture) }
                        )
                    }
                    item {
                        AstronomyExplanationCard(
                            explanation = apodState.picture.astronomyPicture.explanation,
                            onCardClick = onExplanationClick
                        )
                    }
                }
            }
        }
    }
    }
}
