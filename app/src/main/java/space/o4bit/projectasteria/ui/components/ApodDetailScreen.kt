package space.o4bit.projectasteria.ui.components

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture
import space.o4bit.projectasteria.data.repository.SpaceRepository
import space.o4bit.projectasteria.utils.TextUtils
import space.o4bit.projectasteria.utils.VideoEmbedUtils

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
    var showFullscreen by remember { mutableStateOf(false) }
    var showExplanation by remember { mutableStateOf(false) }

    LaunchedEffect(date) {
        isLoading = true
        try {
            val apod = repository.getApodByDate(date)
            if (apod != null) {
                currentApod = apod
            } else {
                currentApod = null
            }
        } catch (_: Exception) {
            currentApod = null
        }
        isLoading = false
    }

    BackHandler(enabled = showFullscreen || showExplanation) {
        when {
            showExplanation -> showExplanation = false
            showFullscreen -> showFullscreen = false
        }
    }

    when {
        showExplanation && currentApod != null -> {
            ExplanationDetailScreen(
                astronomyPicture = currentApod!!.astronomyPicture,
                onBackPressed = { showExplanation = false }
            )
        }
        showFullscreen && currentApod != null -> {
            val pic = currentApod!!.astronomyPicture
            if (pic.mediaType.equals("video", ignoreCase = true)) {
                val videoUrl = pic.url?.takeIf { it.isNotBlank() }
                    ?: pic.hdUrl?.takeIf { it.isNotBlank() }
                    ?: ""
                FullscreenVideoViewer(
                    videoUrl = videoUrl,
                    title = pic.title,
                    onBackPressed = { showFullscreen = false }
                )
            } else {
                FullscreenImageViewer(
                    astronomyPicture = pic,
                    onBackPressed = { showFullscreen = false }
                )
            }
        }
        else -> {
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
                                val pic = currentApod!!.astronomyPicture
                                if (pic.mediaType.equals("video", ignoreCase = true)) {
                                    val videoUrl = pic.url?.takeIf { it.isNotBlank() }
                                        ?: pic.hdUrl?.takeIf { it.isNotBlank() }
                                    if (!videoUrl.isNullOrBlank()) {
                                        IconButton(onClick = {
                                            VideoEmbedUtils.openVideoInExternalApp(context, videoUrl)
                                        }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = "Open video externally"
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = {
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
                    val configuration = LocalConfiguration.current
                    val isTabletOrLandscape = configuration.smallestScreenWidthDp >= 600 ||
                            (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && configuration.screenWidthDp >= 640)

                    if (isTabletOrLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .consumeWindowInsets(innerPadding)
                                .padding(innerPadding)
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                            ) {
                                ApodMediaHeader(
                                    mediaType = pic.mediaType,
                                    url = pic.url,
                                    hdUrl = pic.hdUrl,
                                    thumbnail = pic.thumbnail,
                                    title = pic.title,
                                    isExpanded = true,
                                    onImageClick = { showFullscreen = true },
                                    onFullscreenVideoClick = { showFullscreen = true }
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ApodDetailMetaCard(pic = pic, onExplanationClick = { showExplanation = true })
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .consumeWindowInsets(innerPadding),
                            contentPadding = innerPadding
                        ) {
                            item {
                                ApodMediaHeader(
                                    mediaType = pic.mediaType,
                                    url = pic.url,
                                    hdUrl = pic.hdUrl,
                                    thumbnail = pic.thumbnail,
                                    title = pic.title,
                                    isExpanded = false,
                                    onImageClick = { showFullscreen = true },
                                    onFullscreenVideoClick = { showFullscreen = true }
                                )
                            }
                            item {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    ApodDetailMetaCard(pic = pic, onExplanationClick = { showExplanation = true })
                                }
                            }
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullscreenVideoViewer(
    videoUrl: String,
    title: String,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    BackHandler(onBack = onBackPressed)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        ApodVideoPlayer(
            videoUrl = videoUrl,
            showExternalButton = false,
            modifier = Modifier.fillMaxSize(),
            onToggleFullscreen = onBackPressed
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilledTonalIconButton(
                onClick = onBackPressed,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.65f),
                    contentColor = Color.White
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )
            FilledTonalIconButton(
                onClick = { VideoEmbedUtils.openVideoInExternalApp(context, videoUrl) },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.65f),
                    contentColor = Color.White
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open externally",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ApodDetailMetaCard(
    pic: space.o4bit.projectasteria.data.model.AstronomyPicture,
    onExplanationClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = pic.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            val formattedDate = remember(pic.date) {
                try {
                    val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val displayFmt = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.US)
                    parser.parse(pic.date)?.let { displayFmt.format(it) } ?: pic.date
                } catch (_: Exception) {
                    pic.date
                }
            }
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            if (!pic.copyright.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                val linkColor = MaterialTheme.colorScheme.primary
                val copyrightText = remember(pic.copyright, linkColor) {
                    TextUtils.htmlToAnnotatedString(
                        html = pic.copyright,
                        prefix = "\u00A9 ",
                        linkColor = linkColor
                    )
                }
                Text(
                    text = copyrightText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExplanationClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Explanation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = pic.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 10,
                overflow = TextOverflow.Ellipsis
            )
            if (pic.explanation.length > 300) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to read full explanation",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ApodMediaHeader(
    mediaType: String,
    url: String?,
    hdUrl: String?,
    thumbnail: String?,
    title: String,
    isExpanded: Boolean,
    onImageClick: () -> Unit,
    onFullscreenVideoClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val imageUrl = hdUrl?.takeIf { it.isNotBlank() } ?: url?.takeIf { it.isNotBlank() }
    val heightModifier = if (isExpanded) {
        Modifier.fillMaxSize()
    } else {
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    }

    when {
        mediaType.equals("image", ignoreCase = true) && !imageUrl.isNullOrBlank() -> {
            val isGif = imageUrl.contains(".gif", ignoreCase = true)
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .apply {
                        if (isGif) {
                            decoderFactory(GifDecoder.Factory())
                            crossfade(false)
                        } else {
                            crossfade(true)
                        }
                    }
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = heightModifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onImageClick() }
            )
        }
        mediaType.equals("video", ignoreCase = true) -> {
            val videoUrl = url?.takeIf { it.isNotBlank() } ?: hdUrl?.takeIf { it.isNotBlank() }
            if (!videoUrl.isNullOrBlank()) {
                Box(
                    modifier = heightModifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black)
                ) {
                    ApodVideoPlayer(
                        videoUrl = videoUrl,
                        onToggleFullscreen = onFullscreenVideoClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = heightModifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Video unavailable")
                }
            }
        }
        else -> {
            Box(
                modifier = heightModifier
                    .clip(RoundedCornerShape(20.dp))
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