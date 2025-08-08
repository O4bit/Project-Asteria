package space.o4bit.projectasteria

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture
import space.o4bit.projectasteria.data.repository.SpaceRepository
import space.o4bit.projectasteria.data.worker.DailySpaceWorker
import space.o4bit.projectasteria.ui.components.AstronomyExplanationCard
import space.o4bit.projectasteria.ui.components.AstronomyPictureCard
import space.o4bit.projectasteria.ui.components.ExplanationDetailScreen
import space.o4bit.projectasteria.ui.components.FullscreenImageViewer
import space.o4bit.projectasteria.ui.components.SettingsScreen
import space.o4bit.projectasteria.ui.components.StarryBackground
import space.o4bit.projectasteria.ui.theme.ThemedApp

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

        // Schedule the daily worker to fetch space images and send notifications
        DailySpaceWorker.schedule(this)

        // Request notification permission for Android 13+
        requestNotificationPermission()

        // Handle notification dismissal if needed
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
                // Check if we should open directly to fullscreen from notification
                val openFullscreen = intent.getBooleanExtra("OPEN_FULLSCREEN", false) || 
                                    (intent.data?.scheme == "asteria" && intent.data?.host == "image")
                
                AsteriaApp(
                    openDirectlyFromNotification = openFullscreen
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale if needed
                    Toast.makeText(
                        this,
                        "Notifications help you stay updated with daily space discoveries",
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Directly request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

/**
 * Main composable for the Asteria app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsteriaApp(
    openDirectlyFromNotification: Boolean = false
) {
    // App state
    var showSettings by remember { mutableStateOf(false) }
    var showFullscreenViewer by remember { mutableStateOf(openDirectlyFromNotification) }
    var showExplanationDetail by remember { mutableStateOf(false) }
    var astronomyPicture by remember { mutableStateOf<EnhancedAstronomyPicture?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val repository = remember { SpaceRepository() }
    val scope = rememberCoroutineScope()

    // Fetch today's astronomy picture
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

    // Handle back navigation
    BackHandler(enabled = showSettings || showFullscreenViewer || showExplanationDetail) {
        when {
            showSettings -> showSettings = false
            showExplanationDetail -> showExplanationDetail = false
            showFullscreenViewer -> showFullscreenViewer = false
        }
    }

    // UI based on current state
    when {
        showSettings -> {
            SettingsScreen(
                notificationsEnabled = true,
                onNotificationsToggled = { _ ->
                    // Handle notification toggle
                },
                onDismiss = { showSettings = false }
            )
        }
        showExplanationDetail && astronomyPicture != null -> {
            ExplanationDetailScreen(
                astronomyPicture = astronomyPicture!!.astronomyPicture,
                onBackPressed = { showExplanationDetail = false }
            )
        }
        showFullscreenViewer && astronomyPicture != null -> {
            FullscreenImageViewer(
                astronomyPicture = astronomyPicture!!.astronomyPicture,
                onBackPressed = { showFullscreenViewer = false }
            )
        }
        else -> {
            MainScreen(
                astronomyPicture = astronomyPicture,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onSettingsClick = { showSettings = true },
                onCardClick = { showFullscreenViewer = true },
                onExplanationClick = { showExplanationDetail = true },
                onRetryClick = {
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
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    astronomyPicture: EnhancedAstronomyPicture?,
    isLoading: Boolean,
    errorMessage: String?,
    onSettingsClick: () -> Unit,
    onCardClick: () -> Unit,
    onExplanationClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Project Asteria") },
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
            // Animated starry background
            StarryBackground {
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
                        val context = LocalContext.current
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            item {
                                AstronomyPictureCard(
                                    enhancedPicture = astronomyPicture,
                                    onCardClick = onCardClick,
                                    onShareClick = {
                                        // Create share intent for the image
                                        val shareIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, 
                                                "Check out this amazing astronomy picture: ${astronomyPicture.astronomyPicture.title}\n" +
                                                "${astronomyPicture.astronomyPicture.url}\n\n" +
                                                "From Project Asteria"
                                            )
                                            type = "text/plain"
                                        }
                                        val shareChooser = Intent.createChooser(shareIntent, "Share Astronomy Picture")
                                        context.startActivity(shareChooser)
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

        // Add a retry button
        FilledTonalButton(
            onClick = onRetryClick,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Try Again")
        }
    }
}
