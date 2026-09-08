package space.o4bit.projectasteria.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Universal video player for NASA APOD media.
 * - Uses Media3 [ExoPlayer] with hardware acceleration for direct video files (.mp4, .webm, .mov)
 * - Uses configured [WebView] for web embeds (YouTube, Vimeo)
 * - Includes buffering indicator, error state with retry, and external app launch fallback
 */
@Composable
fun ApodVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    var reloadKey by remember(videoUrl) { mutableStateOf(0) }

    val isDirectVideo = remember(videoUrl) {
        val lower = videoUrl.lowercase()
        lower.endsWith(".mp4") ||
                lower.endsWith(".webm") ||
                lower.endsWith(".mov") ||
                lower.endsWith(".m4v") ||
                lower.endsWith(".m3u8") ||
                lower.contains(".mp4?") ||
                (lower.contains("/image/") && lower.contains(".mp4"))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (isDirectVideo) {
            ExoVideoPlayer(
                videoUrl = videoUrl,
                reloadKey = reloadKey,
                onRetry = { reloadKey++ },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            WebEmbedPlayer(
                videoUrl = videoUrl,
                reloadKey = reloadKey,
                onRetry = { reloadKey++ },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ExoVideoPlayer(
    videoUrl: String,
    reloadKey: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isBuffering by remember(videoUrl, reloadKey) { mutableStateOf(true) }
    var hasError by remember(videoUrl, reloadKey) { mutableStateOf(false) }
    var errorMessage by remember(videoUrl, reloadKey) { mutableStateOf<String?>(null) }

    val exoPlayer = remember(videoUrl, reloadKey) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        hasError = false
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                    }
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                hasError = true
                errorMessage = error.localizedMessage ?: "Failed to stream video"
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                    useController = true
                    controllerShowTimeoutMs = 3000
                    controllerAutoShow = true
                    player = exoPlayer
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && !hasError) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        if (hasError) {
            VideoErrorOverlay(
                message = errorMessage ?: "Unable to stream video",
                videoUrl = videoUrl,
                onRetry = onRetry
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebEmbedPlayer(
    videoUrl: String,
    reloadKey: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isBuffering by remember(videoUrl, reloadKey) { mutableStateOf(true) }
    var hasError by remember(videoUrl, reloadKey) { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(videoUrl, reloadKey) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            webViewRef = null
        }
    }

    val isYouTube = remember(videoUrl) {
        videoUrl.contains("youtube", ignoreCase = true) || videoUrl.contains("youtu.be", ignoreCase = true)
    }

    val formattedUrl = remember(videoUrl) { formatVideoUrl(videoUrl) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        allowFileAccess = false
                        allowContentAccess = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        userAgentString = "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            if (newProgress >= 70) {
                                isBuffering = false
                            }
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isBuffering = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                hasError = true
                                isBuffering = false
                            }
                        }
                    }

                    if (isYouTube) {
                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                <style>
                                    body, html { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; justify-content: center; align-items: center; }
                                    iframe { width: 100%; height: 100%; border: none; }
                                </style>
                            </head>
                            <body>
                                <iframe src="$formattedUrl" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
                            </body>
                            </html>
                        """.trimIndent()
                        loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "utf-8", null)
                    } else {
                        loadUrl(formattedUrl)
                    }

                    webViewRef = this
                }
            },
            update = {
                // webview active
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && !hasError) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        if (hasError) {
            VideoErrorOverlay(
                message = "Unable to load video embed",
                videoUrl = videoUrl,
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun VideoErrorOverlay(
    message: String,
    videoUrl: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        color = Color(0xDD12141C),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .padding(24.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry")
                }
                Button(
                    onClick = { openVideoInExternalApp(context, videoUrl) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open Externally",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open")
                }
            }
        }
    }
}

fun openVideoInExternalApp(context: Context, videoUrl: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open Video"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun formatVideoUrl(url: String): String {
    return when {
        url.contains("youtube.com/watch?v=", ignoreCase = true) -> {
            val videoId = url.substringAfter("v=").substringBefore("&")
            "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&mute=0&playsinline=1&rel=0&enablejsapi=1"
        }
        url.contains("youtu.be/", ignoreCase = true) -> {
            val videoId = url.substringAfter("youtu.be/").substringBefore("?")
            "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&mute=0&playsinline=1&rel=0&enablejsapi=1"
        }
        url.contains("youtube.com/embed/", ignoreCase = true) -> {
            val base = url.replace("www.youtube.com/embed/", "www.youtube-nocookie.com/embed/")
            if (!base.contains("autoplay=")) {
                if (base.contains("?")) "$base&autoplay=1&playsinline=1" else "$base?autoplay=1&playsinline=1"
            } else base
        }
        url.contains("vimeo.com/", ignoreCase = true) -> {
            val videoId = url.substringAfter("vimeo.com/").substringBefore("?")
            if (videoId.isNotBlank()) "https://player.vimeo.com/video/$videoId?autoplay=1&title=0&byline=0&portrait=0" else url
        }
        else -> url
    }
}
