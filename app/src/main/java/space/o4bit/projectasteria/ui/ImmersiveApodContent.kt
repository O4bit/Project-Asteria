package space.o4bit.projectasteria.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.data.local.LaunchEntity
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture
import space.o4bit.projectasteria.ui.components.AstronomyPictureCard
import space.o4bit.projectasteria.ui.components.PinnedLaunchCard
import space.o4bit.projectasteria.ui.viewmodels.ApodUiState

/**
 * Immersive APOD reading experience with a peeking preview card and pinned launches.
 */
@Composable
fun ImmersiveApodContent(
    state: ApodUiState.Success,
    onCardClick: () -> Unit,
    onToggleFavorite: (EnhancedAstronomyPicture) -> Unit,
    pinnedLaunches: List<LaunchEntity> = emptyList(),
    onLaunchClick: (String) -> Unit = {},
    onUnpinLaunch: (String) -> Unit = {},
    autoRemoveSetting: Boolean = false,
    neverAskSetting: Boolean = false,
    onUpdateAutoRemove: (Boolean) -> Unit = {},
    onUpdateNeverAsk: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Scroll range over which transition occurs
    val transitionRangePx = 650

    val rawFraction by remember {
        derivedStateOf { (scrollState.value.toFloat() / transitionRangePx).coerceIn(0f, 1f) }
    }

    val fraction by animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "immersiveFraction"
    )

    val cardAlpha = (1f - fraction * 1.5f).coerceIn(0f, 1f)
    val cardScale = 1f - fraction * 0.12f
    val textAlpha = ((fraction - 0.12f) / 0.78f).coerceIn(0f, 1f)
    val textSlide = (1f - textAlpha) * 40f

    val onSurface = MaterialTheme.colorScheme.onSurface
    val picture = state.picture

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            // ── 0. Pinned Launches Carousel/List ─────────────────────────────
            if (pinnedLaunches.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = cardAlpha
                        }
                ) {
                    Column {
                        pinnedLaunches.forEach { launch ->
                            PinnedLaunchCard(
                                launch = launch,
                                onLaunchClick = { onLaunchClick(launch.id) },
                                onUnpinClick = { onUnpinLaunch(launch.id) },
                                autoRemoveSetting = autoRemoveSetting,
                                neverAskSetting = neverAskSetting,
                                onUpdateAutoRemove = onUpdateAutoRemove,
                                onUpdateNeverAsk = onUpdateNeverAsk
                            )
                        }
                    }
                }
            }

            // ── 1. APOD Picture Card ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = cardAlpha
                        scaleX = cardScale
                        scaleY = cardScale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                    }
            ) {
                AstronomyPictureCard(
                    enhancedPicture = picture,
                    onCardClick = onCardClick,
                    onToggleFavorite = { onToggleFavorite(picture) }
                )
            }

            // ── 2. Peeking Preview Card (Visible at rest to signal scroll) ─────
            AnimatedVisibility(
                visible = fraction < 0.35f,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            scope.launch { scrollState.animateScrollTo(600) }
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ABOUT THIS IMAGE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "• Tap or scroll to read",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = picture.astronomyPicture.explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll down to read full explanation",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // ── 3. Full Reading Content Section ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = textAlpha
                        translationY = textSlide
                    }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Title
                        Text(
                            text = picture.astronomyPicture.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = onSurface,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(6.dp))

                        // Date
                        Text(
                            text = picture.astronomyPicture.date,
                            style = MaterialTheme.typography.labelMedium,
                            color = onSurface.copy(alpha = 0.75f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(20.dp))

                        // Divider line
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        ) {}

                        Spacer(Modifier.height(20.dp))

                        // Full Explanation Body (High Contrast White / onSurface)
                        Text(
                            text = picture.astronomyPicture.explanation,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.55f
                            ),
                            color = onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(36.dp))

                        // Return hint
                        Text(
                            text = "↑ Scroll up to return",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Bottom space
            Spacer(Modifier.height(100.dp))
        }
    }
}
