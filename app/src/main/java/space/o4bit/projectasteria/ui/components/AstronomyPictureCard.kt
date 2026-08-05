package space.o4bit.projectasteria.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import space.o4bit.projectasteria.R
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture
import space.o4bit.projectasteria.ui.components.settings.AsteriaCard
import space.o4bit.projectasteria.ui.navigation.sharedElementIfAvailable
import space.o4bit.projectasteria.utils.TextUtils

/**
 * Premium APOD Picture Card.
 *
 * Designed with a clean, unobscured image preview (no dark gradient masks covering the photo),
 * accompanied by metadata, short fact box, and sleek tonal action buttons cleanly presented
 * in the card container below the image.
 */
@Composable
fun AstronomyPictureCard(
    enhancedPicture: EnhancedAstronomyPicture,
    modifier: Modifier = Modifier,
    onShareClick: () -> Unit = {},
    onCardClick: () -> Unit = {},
    onAddToHomeScreenClick: () -> Unit = {},
    onToggleFavorite: (Boolean) -> Unit = {}
) {
    val astronomyPicture = enhancedPicture.astronomyPicture
    var isImageLoaded by remember { mutableStateOf(false) }

    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val displayFormat = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.US)
    val formattedDate = try {
        val date = dateFormat.parse(astronomyPicture.date)
        date?.let { displayFormat.format(it) } ?: astronomyPicture.date
    } catch (_: Exception) {
        astronomyPicture.date
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, label = "card_scale")

    AsteriaCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onCardClick
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val imageUrl = astronomyPicture.url ?: astronomyPicture.hdUrl
            val isVideo = astronomyPicture.mediaType == "video"
            val context = LocalContext.current

            // ── 1. Unobscured Media Preview ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                when {
                    !isVideo && imageUrl != null -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = astronomyPicture.title,
                            contentScale = ContentScale.Crop,
                            onSuccess = { isImageLoaded = true },
                            onError = { isImageLoaded = true },
                            modifier = Modifier
                                .sharedElementIfAvailable("apod-image-${astronomyPicture.date}")
                                .fillMaxSize()
                        )
                    }
                    isVideo -> {
                        val videoUrl = astronomyPicture.url ?: astronomyPicture.hdUrl
                        var isPlayingInline by remember { mutableStateOf(false) }

                        if (isPlayingInline && videoUrl != null) {
                            ApodVideoPlayer(
                                videoUrl = videoUrl,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val thumbnailUrl = astronomyPicture.thumbnail ?: extractYouTubeThumbnail(imageUrl)
                            if (thumbnailUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(thumbnailUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Video Thumbnail",
                                    contentScale = ContentScale.Crop,
                                    onSuccess = { isImageLoaded = true },
                                    onError = { isImageLoaded = true },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { isPlayingInline = true }
                                )
                            } else {
                                LaunchedEffect(Unit) { isImageLoaded = true }
                            }
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .align(Alignment.Center)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .clickable { isPlayingInline = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play video",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Media not available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LaunchedEffect(Unit) { isImageLoaded = true }
                    }
                }
            }

            // ── 2. Clean Metadata & Actions Below Image ────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header row: Date pill & Favorite heart button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Favorite Button
                    val isFav = enhancedPicture.isFavorite
                    IconButton(
                        onClick = { onToggleFavorite(!isFav) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isFav) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFav) "Remove from favorites" else "Add to favorites",
                            tint = if (isFav) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title & Action Icon Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = astronomyPicture.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = onAddToHomeScreenClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_widgets_24),
                                contentDescription = "Add to Home Screen",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledTonalIconButton(
                            onClick = onShareClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Short Fact Box (if available)
                if (enhancedPicture.shortFact.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    var factExpanded by remember { mutableStateOf(false) }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { factExpanded = !factExpanded }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = enhancedPicture.shortFact,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = if (factExpanded) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AstronomyExplanationCard(
    explanation: String,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {}
) {
    val cleanExplanation = TextUtils.stripHtml(explanation)

    AsteriaCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onCardClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "About this image",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Read more",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (cleanExplanation.length > 150) {
                    cleanExplanation.take(150) + "..."
                } else cleanExplanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (cleanExplanation.length > 150) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to read more",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

private fun extractYouTubeThumbnail(url: String?): String? {
    if (url == null) return null
    val patterns = listOf(
        Regex("""youtube\.com/embed/([a-zA-Z0-9_-]+)"""),
        Regex("""youtu\.be/([a-zA-Z0-9_-]+)"""),
        Regex("""youtube\.com/watch\?v=([a-zA-Z0-9_-]+)""")
    )
    for (pattern in patterns) {
        val match = pattern.find(url)
        if (match != null) {
            val videoId = match.groupValues[1]
            return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        }
    }
    return null
}
