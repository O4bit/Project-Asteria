package space.o4bit.projectasteria.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.AsteriaApplication
import space.o4bit.projectasteria.data.local.AsteroidEntity
import space.o4bit.projectasteria.data.repository.AsteroidRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsteroidScreen(
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as AsteriaApplication
    val repository = remember {
        AsteroidRepository(
            asteroidDao = application.database.asteroidDao(),
            sortingPreferences = application.sortingPreferences
        )
    }

    val asteroids by repository.asteroids.collectAsState(initial = emptyList())
    var isLoading by remember { mutableStateOf(asteroids.isEmpty()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val isClosest by application.sortingPreferences.isAsteroidsClosest.collectAsState(initial = true)
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (asteroids.isEmpty()) {
            scope.launch {
                isLoading = true
                errorMessage = null
                try {
                    repository.refreshTodaysAsteroids()
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Failed to fetch asteroid data."
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Near-Earth Objects") },
                actions = {
                    FilterChip(
                        selected = !isClosest,
                        onClick = {
                            scope.launch {
                                application.sortingPreferences.toggleAsteroidSort()
                                listState.scrollToItem(0)
                            }
                        },
                        label = { Text(if (isClosest) "Closest" else "Furthest") },
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
            if (isLoading && asteroids.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (!errorMessage.isNullOrEmpty() && asteroids.isEmpty()) {
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
                                repository.refreshTodaysAsteroids()
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
            } else if (asteroids.isEmpty()) {
                Text(
                    text = "No near-earth objects found.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(asteroids, key = { it.id }) { asteroid ->
                        AsteroidCard(asteroid) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(asteroid.nasaJplUrl))
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsteroidCard(asteroid: AsteroidEntity, onClick: () -> Unit) {
    val diameterMin = asteroid.estimatedDiameterMinKm * 1000
    val diameterMax = asteroid.estimatedDiameterMaxKm * 1000

    val speed = asteroid.relativeVelocityKmh.toDoubleOrNull()?.let { String.format("%.0f", it) } ?: "Unknown"
    val distance = if (asteroid.missDistanceKm < Double.MAX_VALUE) String.format("%,.0f", asteroid.missDistanceKm) else "Unknown"

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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = asteroid.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (asteroid.isPotentiallyHazardous) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Hazardous",
                        tint = Color.Red
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Safe",
                        tint = Color.Green
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Estimated Size: ${String.format("%.1f", diameterMin)} - ${String.format("%.1f", diameterMax)} meters",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Close Approach: $distance km away",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Velocity: $speed km/h",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
