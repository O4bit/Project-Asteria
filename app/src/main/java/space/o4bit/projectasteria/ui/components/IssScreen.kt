package space.o4bit.projectasteria.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.ui.res.painterResource
import space.o4bit.projectasteria.R
import space.o4bit.projectasteria.widget.IssAppWidgetProvider
import space.o4bit.projectasteria.ui.viewmodels.IssViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssScreen(
    viewModel: IssViewModel = viewModel(),
    onSettingsClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "ISS Tracker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (state.isLive) {
                                Spacer(Modifier.width(8.dp))
                                LivePulsingBadge()
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val appWidgetManager = AppWidgetManager.getInstance(context)
                            val provider = ComponentName(context, IssAppWidgetProvider::class.java)
                            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                appWidgetManager.requestPinAppWidget(provider, null, null)
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_widgets_24),
                            contentDescription = "Add ISS Widget to Home Screen"
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh ISS position")
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
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error / update banner
                if (state.errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (state.lastUpdateTime > 0) {
                    val timeAgoSecs = (System.currentTimeMillis() - state.lastUpdateTime) / 1000
                    val updateText = if (timeAgoSecs < 5) "Updated just now" else "Updated ${timeAgoSecs}s ago"
                    Text(
                        text = updateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ── Interactive globe ──────────────────────────────────────────
                IssGlobe(
                    issPosition = state.location,
                    orbitTrail = state.orbitTrail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )

                val position = state.location
                if (position != null) {
                    // 2×2 stat grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IssStatCard(
                            label = "Latitude",
                            value = String.format(java.util.Locale.US, "%.4f°", position.latitude),
                            icon = Icons.Default.MyLocation,
                            modifier = Modifier.weight(1f)
                        )
                        IssStatCard(
                            label = "Longitude",
                            value = String.format(java.util.Locale.US, "%.4f°", position.longitude),
                            icon = Icons.Default.Place,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IssStatCard(
                            label = "Altitude",
                            value = "${String.format(java.util.Locale.US, "%.1f", position.altitude)} km",
                            icon = Icons.Default.Terrain,
                            modifier = Modifier.weight(1f)
                        )
                        IssStatCard(
                            label = "Velocity",
                            value = "${String.format(java.util.Locale.US, "%.0f", position.velocity)} km/h",
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Map button
                    FilledTonalButton(
                        onClick = {
                            val uri = "geo:${position.latitude},${position.longitude}?q=${position.latitude},${position.longitude}(International Space Station)"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                            intent.setPackage("com.google.android.apps.maps")
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                val webUri = "https://www.google.com/maps/search/?api=1&query=${position.latitude},${position.longitude}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri)))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("View on Map", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Animated pulsing satellite orb ────────────────────────────────────────────

@Composable
private fun SatelliteOrb(isLive: Boolean, isLoading: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isLive) 1.18f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        // Outer pulse ring
        if (isLive) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = ringAlpha),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = ringAlpha * 0.6f),
                        shape = CircleShape
                    )
            )
        }

        // Core icon surface
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .size(100.dp)
                .border(
                    1.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    CircleShape
                )
                .semantics { contentDescription = if (isLive) "ISS live tracking active" else "ISS position loading" }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Satellite,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (isLive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Stat tile ─────────────────────────────────────────────────────────────────

@Composable
private fun IssStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.border(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            RoundedCornerShape(16.dp)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Blinking LIVE badge ────────────────────────────────────────────────────────

@Composable
private fun LivePulsingBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "liveBadge")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDot"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .border(
                1.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error.copy(alpha = alpha),
            modifier = Modifier.size(7.dp)
        ) {}
        Text(
            text = "LIVE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            letterSpacing = 1.sp
        )
    }
}

// Keep DataItem for any other usage
@Composable
fun DataItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
