package space.o4bit.projectasteria.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture
import space.o4bit.projectasteria.data.repository.SpaceRepository

/**
 * Self-contained detail screen for a specific APOD, navigated from History.
 * Handles its own fullscreen image viewer and explanation view internally.
 * Does NOT mutate any parent state — completely isolated.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApodDetailScreen(
    date: String,
    repository: SpaceRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    var currentApod by remember { mutableStateOf<EnhancedAstronomyPicture?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Internal sub-screen state
    var showFullscreen by remember { mutableStateOf(false) }
    var showExplanation by remember { mutableStateOf(false) }

    LaunchedEffect(date) {
        isLoading = true
        try {
            currentApod = repository.getApodByDate(date)
        } catch (_: Exception) { }
        isLoading = false
    }

    // Handle back from internal sub-screens before popping the nav stack
    BackHandler(enabled = showFullscreen || showExplanation) {
        when {
            showExplanation -> showExplanation = false
            showFullscreen -> showFullscreen = false
        }
    }

    // Internal sub-screens
    when {
        showExplanation && currentApod != null -> {
            ExplanationDetailScreen(
                astronomyPicture = currentApod!!.astronomyPicture,
                onBackPressed = { showExplanation = false }
            )
        }
        showFullscreen && currentApod != null -> {
            FullscreenImageViewer(
                astronomyPicture = currentApod!!.astronomyPicture,
                onBackPressed = { showFullscreen = false }
            )
        }
        else -> {
            // Main detail view
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = currentApod?.astronomyPicture?.title ?: "APOD",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (currentApod != null) {
                                IconButton(onClick = {
                                    val pic = currentApod!!.astronomyPicture
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Check out this amazing astronomy picture: ${pic.title}\n" +
                                                    "${pic.url ?: pic.hdUrl ?: "NASA APOD"}\n\nFrom Project Asteria"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share APOD"))
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        )
                    )
                }
            ) { innerPadding ->
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (currentApod == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Could not load APOD for $date")
                    }
                } else {
                    val pic = currentApod!!.astronomyPicture
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Media header
                        item {
                            ApodMediaHeader(
                                mediaType = pic.mediaType,
                                url = pic.url,
                                hdUrl = pic.hdUrl,
                                thumbnail = pic.thumbnail,
                                title = pic.title,
                                onImageClick = { showFullscreen = true }
                            )
                        }

                        // Title + Date
                        item {
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                                Text(
                                    text = pic.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val formattedDate = remember(pic.date) {
                                    try {
                                        val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                        val displayFmt = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.US)
                                        parser.parse(pic.date)?.let { displayFmt.format(it) } ?: pic.date
                                    } catch (_: Exception) { pic.date }
                                }
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (pic.copyright != null) {
                                    Text(
                                        text = "\u00A9 ${pic.copyright.trim()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Explanation
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clickable { showExplanation = true },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Explanation",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = pic.explanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 8,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (pic.explanation.length > 400) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tap to read full explanation",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom spacer
                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }
}

/**
 * Media header that handles image, video (with YouTube thumbnail + play overlay), and fallback.
 */
@Composable
private fun ApodMediaHeader(
    mediaType: String,
    url: String?,
    hdUrl: String?,
    thumbnail: String?,
    title: String,
    onImageClick: () -> Unit
) {
    val context = LocalContext.current
    val imageUrl = url ?: hdUrl

    when {
        mediaType == "image" && imageUrl != null -> {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(hdUrl ?: url)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clickable { onImageClick() }
            )
        }
        mediaType == "video" -> {
            val thumbUrl = thumbnail ?: extractYouTubeThumbnailUrl(imageUrl)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        if (imageUrl != null) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl)))
                            } catch (_: Exception) { }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (thumbUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Video Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }
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
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Media not available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Extract YouTube thumbnail from embed/watch/shorts URL.
 */
private fun extractYouTubeThumbnailUrl(url: String?): String? {
    if (url == null) return null
    val patterns = listOf(
        Regex("""youtube\.com/embed/([a-zA-Z0-9_-]+)"""),
        Regex("""youtu\.be/([a-zA-Z0-9_-]+)"""),
        Regex("""youtube\.com/watch\?v=([a-zA-Z0-9_-]+)""")
    )
    for (pattern in patterns) {
        val match = pattern.find(url)
        if (match != null) {
            return "https://img.youtube.com/vi/${match.groupValues[1]}/hqdefault.jpg"
        }
    }
    return null
}
