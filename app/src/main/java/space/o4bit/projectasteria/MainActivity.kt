package space.o4bit.projectasteria

import kotlinx.coroutines.Dispatchers
import android.Manifest
import androidx.lifecycle.lifecycleScope
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.Send
import space.o4bit.projectasteria.ui.components.AsteroidScreen
import space.o4bit.projectasteria.ui.components.LaunchScreen
import space.o4bit.projectasteria.ui.components.IssScreen
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.snapshotFlow
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture
import space.o4bit.projectasteria.data.repository.SpaceRepository
import space.o4bit.projectasteria.data.worker.DailySpaceWorker
import space.o4bit.projectasteria.ui.components.AstronomyExplanationCard
import space.o4bit.projectasteria.ui.components.AstronomyPictureCard
import space.o4bit.projectasteria.widget.WidgetClickReceiver
import space.o4bit.projectasteria.ui.components.ExplanationDetailScreen
import space.o4bit.projectasteria.ui.components.FullscreenImageViewer
import space.o4bit.projectasteria.ui.components.SettingsScreen
import space.o4bit.projectasteria.ui.components.AnimatedBackground
import space.o4bit.projectasteria.ui.components.BackgroundType
import space.o4bit.projectasteria.ui.components.OssLicensesScreen
import space.o4bit.projectasteria.ui.components.HistoryScreen
import space.o4bit.projectasteria.ui.components.LaunchDetailScreen
import space.o4bit.projectasteria.ui.components.ApodDetailScreen
import space.o4bit.projectasteria.ui.components.SwipeHintOverlay
import space.o4bit.projectasteria.ui.theme.ThemedApp
import space.o4bit.projectasteria.data.preferences.BackgroundPreferencesRepository
import space.o4bit.projectasteria.data.preferences.UiHintsPreferencesRepository
import space.o4bit.projectasteria.data.repository.LaunchRepository

import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Satellite
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Public
import space.o4bit.projectasteria.ui.components.settings.AsteriaBottomNavigation

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Defer heavy initializers off the Main thread to speed up TTFF (Time-to-first-frame)
        lifecycleScope.launch(Dispatchers.IO) {
            // Configure Coil globally
            val imageLoader = ImageLoader.Builder(applicationContext)
                .memoryCache {
                    MemoryCache.Builder(applicationContext)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizePercent(0.02)
                        .build()
                }
                .respectCacheHeaders(false) // Cache heavily
                .build()
            coil.Coil.setImageLoader(imageLoader)

            val prefs = space.o4bit.projectasteria.data.preferences.NotificationPreferencesRepository(applicationContext)
            if (prefs.dailyNotificationsEnabled.first()) {
                DailySpaceWorker.schedule(applicationContext)
            }
        }

        requestNotificationPermission()

        if (intent.getBooleanExtra("DISMISS_NOTIFICATION", false)) {
            val notificationId = intent.getIntExtra("NOTIFICATION_ID", 0)
            if (notificationId > 0) {
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)
            }
        }
        
        enableEdgeToEdge()
        setContent {
            ThemedApp {
                val openFullscreen = intent.getBooleanExtra("OPEN_FULLSCREEN", false) || 
                                    (intent.data?.scheme == "asteria" && intent.data?.host == "image")
                
                AsteriaApp(
                    openDirectlyFromNotification = openFullscreen
                )
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (intent?.getBooleanExtra(WidgetClickReceiver.EXTRA_FROM_WIDGET, false) == true) {
            @Suppress("DEPRECATION")
            overridePendingTransition(
                R.anim.widget_open_enter,
                R.anim.widget_open_exit
            )
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Toast.makeText(
                        this,
                        "Notifications help you stay updated with daily space discoveries",
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

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

/**
 * Main composable for the Asteria app
 * Uses Jetpack Navigation for detail screens (launch detail, APOD detail from history),
 * while keeping modal overlays (settings, licenses, fullscreen viewer) as boolean state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsteriaApp(
    openDirectlyFromNotification: Boolean = false
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val pagerState = rememberPagerState(pageCount = { MainTab.entries.size })
    val currentTab = MainTab.entries[pagerState.currentPage]
    var showSettings by remember { mutableStateOf(false) }
    var showFullscreenViewer by remember { mutableStateOf(openDirectlyFromNotification) }
    var showExplanationDetail by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var astronomyPicture by remember { mutableStateOf<EnhancedAstronomyPicture?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val application = context.applicationContext as AsteriaApplication
    val database = application.database
    val repository = remember { SpaceRepository(database.apodDao()) }
    val launchRepository = remember { 
        LaunchRepository(
            launchDao = database.launchDao(),
            sortingPreferences = application.sortingPreferences
        ) 
    }
    val launches by launchRepository.launches.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    
    val backgroundPrefs = remember { BackgroundPreferencesRepository(context) }
    val uiHintsPrefs = remember { UiHintsPreferencesRepository(context) }
    val backgroundTypeName by backgroundPrefs.backgroundType.collectAsState(initial = BackgroundType.SPACE.name)
    val backgroundType = BackgroundType.fromName(backgroundTypeName)
    val mainTabsHintShown by uiHintsPrefs.mainTabsHintShown.collectAsState(initial = false)
    val hyperdriveThresholdMinutes by backgroundPrefs.hyperdriveThresholdMinutes.collectAsState(initial = 1)

    // Notification preferences
    val notificationPrefs = remember { space.o4bit.projectasteria.data.preferences.NotificationPreferencesRepository(context) }
    val notificationsEnabled by notificationPrefs.dailyNotificationsEnabled.collectAsState(initial = true)

    // Launch Speed State
    var launchSpeedMultiplier by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(launches, hyperdriveThresholdMinutes) {
        while (true) {
            val now = System.currentTimeMillis()
            val isLaunchActive = launches.any { launch ->
                launch.statusName.equals("In Flight", ignoreCase = true) ||
                    (now >= launch.netMillis - hyperdriveThresholdMinutes * 60 * 1000L && now - launch.netMillis < 15 * 60 * 1000L)
            }
            launchSpeedMultiplier = if (isLaunchActive) 15f else 1f
            kotlinx.coroutines.delay(1000L)
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null
                astronomyPicture = repository.getTodaysAstronomyPicture()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load space picture"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collectLatest { page ->
                if (page != 0 && !mainTabsHintShown) {
                    uiHintsPrefs.setMainTabsHintShown(true)
                }
            }
    }

    BackHandler(enabled = showSettings || showFullscreenViewer || showExplanationDetail || showHistory || showLicenses) {
        when {
            showLicenses -> showLicenses = false
            showSettings -> showSettings = false
            showHistory -> showHistory = false
            showExplanationDetail -> showExplanationDetail = false
            showFullscreenViewer -> showFullscreenViewer = false
        }
    }

    AnimatedBackground(
        type = backgroundType,
        modifier = Modifier.fillMaxSize(),
        launchSpeedMultiplier = launchSpeedMultiplier
    ) {
        NavHost(
            navController = navController,
            startDestination = "main",
            enterTransition = { fadeIn(tween(300)) + slideInHorizontally(tween(300)) { (it * 0.1f).toInt() } },
            exitTransition = { fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { -(it * 0.1f).toInt() } },
            popEnterTransition = { fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -(it * 0.1f).toInt() } },
            popExitTransition = { fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { (it * 0.1f).toInt() } }
        ) {
            // Main tabbed screen
            composable("main") {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        AsteriaBottomNavigation(
                            tabs = MainTab.entries.map { it.title },
                            selectedIcons = MainTab.entries.map { it.selectedIcon },
                            unselectedIcons = MainTab.entries.map { it.unselectedIcon },
                            selectedIndex = pagerState.currentPage,
                            onTabSelected = { index ->
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
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
                                MainTab.APOD -> {
                                    MainScreen(
                                        astronomyPicture = astronomyPicture,
                                        isLoading = isLoading,
                                        errorMessage = errorMessage,
                                        backgroundType = backgroundType,
                                        onSettingsClick = { showSettings = true },
                                        onHistoryClick = { showHistory = true },
                                        onCardClick = { showFullscreenViewer = true },
                                        onExplanationClick = { showExplanationDetail = true },
                                        onRetryClick = {
                                            scope.launch {
                                                try {
                                                    isLoading = true
                                                    errorMessage = null
                                                    astronomyPicture = repository.getTodaysAstronomyPicture()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    )
                                }
                                MainTab.ASTEROIDS -> {
                                    AsteroidScreen(onSettingsClick = { showSettings = true })
                                }
                                MainTab.LAUNCHES -> {
                                    LaunchScreen(
                                        onSettingsClick = { showSettings = true },
                                        onLaunchClick = { launchId ->
                                            navController.navigate("launch_detail/$launchId")
                                        }
                                    )
                                }
                                MainTab.ISS -> {
                                    IssScreen(onSettingsClick = { showSettings = true })
                                }
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

            // Launch detail screen with countdown
            composable(
                route = "launch_detail/{launchId}",
                arguments = listOf(navArgument("launchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val launchId = backStackEntry.arguments?.getString("launchId") ?: return@composable
                LaunchDetailScreen(
                    launchId = launchId,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // APOD detail from History (shows full APOD without settings button)
            composable(
                route = "apod_detail/{date}",
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getString("date") ?: return@composable
                ApodDetailScreen(
                    date = date,
                    repository = repository,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        // --- Overlays with Animations ---

        AnimatedVisibility(
            visible = showHistory,
            enter = slideInVertically(animationSpec = tween(400), initialOffsetY = { it }),
            exit = slideOutVertically(animationSpec = tween(400), targetOffsetY = { it })
        ) {
            AnimatedBackground(type = backgroundType) {
                HistoryScreen(
                    repository = repository,
                    onNavigateUp = { showHistory = false },
                    onApodClick = { selectedApod ->
                        showHistory = false
                        navController.navigate("apod_detail/${selectedApod.astronomyPicture.date}")
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = slideInVertically(animationSpec = tween(400), initialOffsetY = { it }),
            exit = slideOutVertically(animationSpec = tween(400), targetOffsetY = { it })
        ) {
            AnimatedBackground(type = backgroundType) {
                SettingsScreen(
                    onNavigateBack = { showSettings = false },
                    onShowLicenses = { showLicenses = true }
                )
            }
        }

        AnimatedVisibility(
            visible = showExplanationDetail && astronomyPicture != null,
            enter = slideInVertically(animationSpec = tween(400), initialOffsetY = { it }),
            exit = slideOutVertically(animationSpec = tween(400), targetOffsetY = { it })
        ) {
            AnimatedBackground(type = backgroundType) {
                astronomyPicture?.let { ap ->
                    ExplanationDetailScreen(
                        astronomyPicture = ap.astronomyPicture,
                        onBackPressed = { showExplanationDetail = false }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showFullscreenViewer && astronomyPicture != null,
            enter = fadeIn(tween(400)) + slideInVertically(animationSpec = tween(400), initialOffsetY = { it / 4 }),
            exit = fadeOut(tween(400)) + slideOutVertically(animationSpec = tween(400), targetOffsetY = { it / 4 })
        ) {
            AnimatedBackground(type = backgroundType) {
                astronomyPicture?.let { ap ->
                    FullscreenImageViewer(
                        astronomyPicture = ap.astronomyPicture,
                        onBackPressed = { showFullscreenViewer = false }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showLicenses,
            enter = slideInVertically(animationSpec = tween(400), initialOffsetY = { it }),
            exit = slideOutVertically(animationSpec = tween(400), targetOffsetY = { it })
        ) {
            AnimatedBackground(type = backgroundType) {
                OssLicensesScreen(
                    onNavigateUp = { showLicenses = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    astronomyPicture: EnhancedAstronomyPicture?,
    isLoading: Boolean,
    errorMessage: String?,
    backgroundType: BackgroundType = BackgroundType.DEFAULT,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onCardClick: () -> Unit,
    onExplanationClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    val context = LocalContext.current
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    var isSpinning by remember { mutableStateOf(false) }
                    var rotationTarget by remember { mutableFloatStateOf(0f) }

                    val rotation by animateFloatAsState(
                        targetValue = rotationTarget,
                        animationSpec = tween(
                            durationMillis = 1400,
                            easing = FastOutSlowInEasing
                        ),
                        finishedListener = { isSpinning = false },
                        label = "LogoSpin"
                    )

                    // Spin once on initial load
                    LaunchedEffect(Unit) {
                        isSpinning = true
                        rotationTarget += 360f * 6
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (!isSpinning) {
                                isSpinning = true
                                rotationTarget += 360f * 6
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.orbit_24px),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .graphicsLayer { rotationZ = rotation },
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Project Asteria",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Previous APODs"
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
            FloatingActionButton(
                onClick = onSettingsClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.semantics {
                    contentDescription = "Open settings"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    LoadingScreen()
                }
                errorMessage != null -> {
                    ErrorScreen(
                        message = errorMessage,
                        onRetryClick = onRetryClick
                    )
                }
                astronomyPicture != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        item {
                            AstronomyPictureCard(
                                enhancedPicture = astronomyPicture,
                                onCardClick = onCardClick,
                                onShareClick = {
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, 
                                            "Check out this amazing astronomy picture: ${astronomyPicture.astronomyPicture.title}\n" +
                                            "${astronomyPicture.astronomyPicture.url ?: astronomyPicture.astronomyPicture.hdUrl ?: "NASA APOD"}\n\n" +
                                            "From Project Asteria"
                                        )
                                        type = "text/plain"
                                    }
                                    val shareChooser = Intent.createChooser(shareIntent, "Share Astronomy Picture")
                                    context.startActivity(shareChooser)
                                },
                                onAddToHomeScreenClick = {
                                    space.o4bit.projectasteria.utils.WidgetPinningUtils.showAddToHomeScreenMessage(context)
                                    space.o4bit.projectasteria.utils.WidgetPinningUtils.pinWidgetToHomeScreen(context)
                                }
                            )
                        }

                        item {
                            AstronomyExplanationCard(
                                explanation = astronomyPicture.astronomyPicture.explanation,
                                onCardClick = onExplanationClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .rotate(rotation),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }

        Text(
            text = "Exploring the cosmos...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetryClick: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Houston, we have a problem!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onRetryClick
            ) {
                Text("Try Again")
            }
        }
    }
}

