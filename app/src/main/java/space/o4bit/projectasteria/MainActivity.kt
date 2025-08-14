package space.o4bit.projectasteria

import android.Manifest
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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedButton
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
import space.o4bit.projectasteria.widget.WidgetClickReceiver
import space.o4bit.projectasteria.ui.components.ExplanationDetailScreen
import space.o4bit.projectasteria.ui.components.FullscreenImageViewer
import space.o4bit.projectasteria.ui.components.SettingsScreen
import space.o4bit.projectasteria.ui.components.StarryBackground
import space.o4bit.projectasteria.ui.theme.ThemedApp
import space.o4bit.projectasteria.utils.CrashReportingUtils

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
        
        CrashReportingUtils.initialize(this)

        DailySpaceWorker.schedule(this)

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
            overridePendingTransition(
                space.o4bit.projectasteria.R.anim.widget_open_enter,
                space.o4bit.projectasteria.R.anim.widget_open_exit
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

/**
 * Main composable for the Asteria app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsteriaApp(
    openDirectlyFromNotification: Boolean = false
) {
    var showSettings by remember { mutableStateOf(false) }
    var showFullscreenViewer by remember { mutableStateOf(openDirectlyFromNotification) }
    var showExplanationDetail by remember { mutableStateOf(false) }
    var astronomyPicture by remember { mutableStateOf<EnhancedAstronomyPicture?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val repository = remember { SpaceRepository() }
    val scope = rememberCoroutineScope()

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

    BackHandler(enabled = showSettings || showFullscreenViewer || showExplanationDetail) {
        when {
            showSettings -> showSettings = false
            showExplanationDetail -> showExplanationDetail = false
            showFullscreenViewer -> showFullscreenViewer = false
        }
    }

    when {
        showSettings -> {
            SettingsScreen(
                notificationsEnabled = true,
                onNotificationsToggled = { _ ->
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
    val context = LocalContext.current
    
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
            
            OutlinedButton(
                onClick = {
                    try {
                        val errorReport = CrashReportingUtils.formatCrashDataForGitHub(
                            error = message,
                            additionalContext = "Error occurred in main astronomy picture loading"
                        )
                        
                        CrashReportingUtils.reportError(
                            throwable = Exception("User-reported error: $message"),
                            message = "Manual error report from ErrorScreen",
                            additionalData = mapOf(
                                "error_message" to message,
                                "screen" to "MainActivity",
                                "user_action" to "manual_report"
                            )
                        )
                        
                        // Open GitHub issues with pre-filled report
                        val githubIssueUrl = "https://github.com/O4bit/Project-Asteria/issues/new?" +
                                "title=${Uri.encode("Error: $message")}&" +
                                "body=${Uri.encode(errorReport)}&" +
                                "labels=bug"
                        
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubIssueUrl))
                        context.startActivity(intent)
                        
                        Toast.makeText(context, "Report sent and GitHub opened", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to open GitHub: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            ) {
                Text("Report Issue")
            }
        }
    }
}
