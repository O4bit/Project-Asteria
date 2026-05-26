package space.o4bit.projectasteria.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import space.o4bit.projectasteria.R
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture
import space.o4bit.projectasteria.utils.TextUtils

/**
 * A card displaying the astronomy picture of the day with its details
 */
@Composable
fun AstronomyPictureCard(
    enhancedPicture: EnhancedAstronomyPicture,
    modifier: Modifier = Modifier,
    onShareClick: () -> Unit = {},
    onCardClick: () -> Unit = {},
    onAddToHomeScreenClick: () -> Unit = {}
) {
    val astronomyPicture = enhancedPicture.astronomyPicture
    var isImageLoaded by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    // Format the date
    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val displayFormat = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.US)
    val formattedDate = try {
        val date = dateFormat.parse(astronomyPicture.date)
        date?.let { displayFormat.format(it) } ?: astronomyPicture.date
    } catch (_: Exception) {
        astronomyPicture.date
    }

    // Staggered animations
    LaunchedEffect(isImageLoaded) {
        if (isImageLoaded) {
            delay(300)
            showContent = true
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, label = "scale")

    space.o4bit.projectasteria.ui.components.settings.AsteriaCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onCardClick
            )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Handle different media types
            val imageUrl = astronomyPicture.url ?: astronomyPicture.hdUrl
            val isVideo = astronomyPicture.mediaType == "video"
            val context = LocalContext.current
            val haptic = LocalHapticFeedback.current
            
            when {
                // IMAGE type: load async with Coil
                !isVideo && imageUrl != null -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = astronomyPicture.title,
                        contentScale = ContentScale.Crop,
                        onSuccess = { isImageLoaded = true },
                        onError = { 
                            isImageLoaded = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )
                }
                // VIDEO type: show thumbnail with play overlay, tap opens external browser/YouTube
                isVideo -> {
                    val thumbnailUrl = astronomyPicture.thumbnail ?: extractYouTubeThumbnail(imageUrl)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                // Open video URL in external browser/YouTube app
                                if (imageUrl != null) {
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(imageUrl)
                                        )
                                        context.startActivity(intent)
                                    } catch (_: Exception) { }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
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
                                modifier = Modifier.matchParentSize()
                            )
                        } else {
                            LaunchedEffect(Unit) { delay(300); isImageLoaded = true }
                        }
                        // Play button overlay
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play video",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
                // FALLBACK: unknown media type or no URL
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Media not available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    LaunchedEffect(Unit) { delay(300); isImageLoaded = true }
                }
            }

            // Semi-transparent gradient overlay for better text readability
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .clip(RoundedCornerShape(24.dp))
            )

            // Content
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section with date
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Bottom section with title and actions
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 2 }
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Title
                            Text(
                                text = astronomyPicture.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            // Action buttons row
                            Row {
                                // Add to Home Screen button
                                FilledTonalIconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onAddToHomeScreenClick()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.baseline_widgets_24),
                                        contentDescription = "Add Astronomy Widget to Home Screen",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Share button
                                FilledTonalIconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onShareClick()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share this astronomy discovery",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Space fact
                        // Random Space Facts section with expandable functionality
                        var factExpanded by remember { mutableStateOf(false) }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                .clickable(
                                    onClickLabel = if (factExpanded) "Collapse fact" else "Expand fact"
                                ) { 
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    factExpanded = !factExpanded 
                                }
                                .padding(12.dp)
                                .semantics { role = Role.Button }
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
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
                                
                                if (factExpanded || enhancedPicture.shortFact.length > 80) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (factExpanded) "Tap to collapse" else "Tap to expand",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                            
                            Icon(
                                imageVector = if (factExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A card displaying the explanation for an astronomy picture
 */
@Composable
fun AstronomyExplanationCard(
    explanation: String,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {}
) {
    // Strip HTML tags from explanation
    val cleanExplanation = TextUtils.stripHtml(explanation)
    
    space.o4bit.projectasteria.ui.components.settings.AsteriaCard(
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Read more",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

/**
 * Extract a YouTube video thumbnail URL from a YouTube embed URL.
 * Supports youtube.com/embed/ID and youtu.be/ID formats.
 * Returns null if the URL is not a recognized YouTube format.
 */
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
