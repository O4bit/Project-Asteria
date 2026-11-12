package space.o4bit.projectasteria.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import space.o4bit.projectasteria.utils.VideoEmbedUtils

@Composable
fun ApodVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 16f / 9f,
    showExternalButton: Boolean = true,
    onToggleFullscreen: (() -> Unit)? = null
) {
    val normalizedUrl = remember(videoUrl) { VideoEmbedUtils.normalizeUrl(videoUrl) }
    var reloadKey by remember(normalizedUrl) { mutableIntStateOf(0) }
    var forceWebFallback by remember(normalizedUrl) { mutableStateOf(false) }
    val isDirectVideo = remember(normalizedUrl, forceWebFallback) {
        !forceWebFallback && VideoEmbedUtils.isDirectVideoUrl(normalizedUrl)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (isDirectVideo) {
            ExoVideoPlayer(
                videoUrl = normalizedUrl,
                reloadKey = reloadKey,
                onRetry = { reloadKey++ },
                onFallbackToWeb = { forceWebFallback = true },
                showExternalButton = showExternalButton,
                onToggleFullscreen = onToggleFullscreen,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            WebEmbedPlayer(
                videoUrl = normalizedUrl,
                reloadKey = reloadKey,
                onRetry = { reloadKey++ },
                showExternalButton = showExternalButton,
                onToggleFullscreen = onToggleFullscreen,
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
    onFallbackToWeb: () -> Unit,
    showExternalButton: Boolean,
    onToggleFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isBuffering by remember(videoUrl, reloadKey) { mutableStateOf(true) }
    var hasError by remember(videoUrl, reloadKey) { mutableStateOf(false) }
    var errorMessage by remember(videoUrl, reloadKey) { mutableStateOf<String?>(null) }
    var isExoFullscreen by remember { mutableStateOf(false) }

    val exoPlayer = remember(videoUrl, reloadKey) {
        try {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                setMediaItem(mediaItem)
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
                prepare()
            }
        } catch (t: Throwable) {
            null
        }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (player.playbackState == Player.STATE_READY) {
                        player.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        hasError = false
                    }
                    Player.STATE_ENDED -> isBuffering = false
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                hasError = true
                errorMessage = error.localizedMessage ?: "Failed to stream video"
            }
        }
        player.addListener(listener)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }

    if (exoPlayer == null) {
        VideoErrorOverlay(
            message = "Playback engine failed to initialize",
            videoUrl = videoUrl,
            onRetry = onRetry,
            onFallback = onFallbackToWeb
        )
        return
    }

    if (isExoFullscreen) {
        Dialog(
            onDismissRequest = { isExoFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(AndroidColor.BLACK)
                            useController = true
                            controllerShowTimeoutMs = 3000
                            controllerAutoShow = true
                            player = exoPlayer
                            setFullscreenButtonClickListener {
                                isExoFullscreen = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                FilledTonalIconButton(
                    onClick = { isExoFullscreen = false },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.7f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit fullscreen",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
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
                    setBackgroundColor(AndroidColor.BLACK)
                    useController = true
                    controllerShowTimeoutMs = 3000
                    controllerAutoShow = true
                    player = exoPlayer
                    setFullscreenButtonClickListener {
                        if (onToggleFullscreen != null) {
                            onToggleFullscreen()
                        } else {
                            isExoFullscreen = true
                        }
                    }
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onToggleFullscreen != null) {
                FilledTonalIconButton(
                    onClick = onToggleFullscreen,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Toggle fullscreen",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (showExternalButton && !hasError) {
                FilledTonalIconButton(
                    onClick = { VideoEmbedUtils.openVideoInExternalApp(context, videoUrl) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open in external app",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (isBuffering && !hasError) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }

        if (hasError) {
            VideoErrorOverlay(
                message = errorMessage ?: "Unable to stream video",
                videoUrl = videoUrl,
                onRetry = onRetry,
                onFallback = onFallbackToWeb
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
    showExternalButton: Boolean,
    onToggleFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isBuffering by remember(videoUrl, reloadKey) { mutableStateOf(true) }
    var hasError by remember(videoUrl, reloadKey) { mutableStateOf(false) }
    var errorMessage by remember(videoUrl, reloadKey) { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customFullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val hideCustomFullscreen = remember {
        {
            val decor = activity?.window?.decorView as? ViewGroup
            customFullscreenView?.let { v ->
                decor?.removeView(v)
            }
            customFullscreenCallback?.onCustomViewHidden()
            customFullscreenView = null
            customFullscreenCallback = null
            activity?.window?.let { win ->
                val controller = WindowCompat.getInsetsController(win, win.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(videoUrl, reloadKey) {
        onDispose {
            hideCustomFullscreen()
            webViewRef?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            webViewRef = null
        }
    }

    BackHandler(enabled = customFullscreenView != null) {
        hideCustomFullscreen()
    }

    val isYouTube = remember(videoUrl) {
        VideoEmbedUtils.extractYouTubeVideoId(videoUrl) != null
    }
    val isVimeo = remember(videoUrl) {
        VideoEmbedUtils.extractVimeoVideoId(videoUrl) != null
    }
    val formattedUrl = remember(videoUrl) {
        VideoEmbedUtils.formatEmbedUrl(videoUrl)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                try {
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        setBackgroundColor(AndroidColor.BLACK)
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
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportMultipleWindows(false)
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                if (newProgress >= 65) {
                                    isBuffering = false
                                }
                            }

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                if (view == null) return
                                hideCustomFullscreen()
                                customFullscreenView = view
                                customFullscreenCallback = callback
                                val decor = activity?.window?.decorView as? ViewGroup
                                if (decor != null) {
                                    view.layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    view.setBackgroundColor(AndroidColor.BLACK)
                                    decor.addView(view)
                                    activity.window?.let { win ->
                                        val controller = WindowCompat.getInsetsController(win, win.decorView)
                                        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                                        controller.hide(WindowInsetsCompat.Type.systemBars())
                                    }
                                }
                            }

                            override fun onHideCustomView() {
                                hideCustomFullscreen()
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
                                    errorMessage = error?.description?.toString() ?: "Failed to load embed"
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: WebResourceResponse?
                            ) {
                                if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 200) >= 400) {
                                    hasError = true
                                    isBuffering = false
                                    errorMessage = "HTTP error ${errorResponse?.statusCode}"
                                }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val uri = request?.url ?: return false
                                val scheme = uri.scheme?.lowercase() ?: return false
                                if (scheme != "http" && scheme != "https") {
                                    return handleSpecialScheme(context, uri)
                                }
                                if (request.isForMainFrame != true) {
                                    return false
                                }
                                val host = uri.host?.lowercase() ?: ""
                                val isMediaHost = host.contains("youtube") || host.contains("youtu.be") ||
                                        host.contains("vimeo") || host.contains("googlevideo") ||
                                        host.contains("ytimg") || host.contains("vimeocdn")
                                val initialHost = try { Uri.parse(formattedUrl).host?.lowercase() ?: "" } catch (_: Exception) { "" }
                                if (isMediaHost || (initialHost.isNotEmpty() && host.contains(initialHost))) {
                                    val isWatchLink = (host.endsWith("youtube.com") && (uri.path.orEmpty().startsWith("/watch") || uri.path.orEmpty().startsWith("/shorts"))) || host.endsWith("youtu.be")
                                    if (request.hasGesture() && isWatchLink) {
                                        return handleExternalNavigation(context, uri.toString())
                                    }
                                    return false
                                }
                                if (request.hasGesture()) {
                                    return handleExternalNavigation(context, uri.toString())
                                }
                                return false
                            }

                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                val destination = url ?: return false
                                val uri = Uri.parse(destination)
                                val scheme = uri.scheme?.lowercase() ?: return false
                                if (scheme != "http" && scheme != "https") {
                                    return handleSpecialScheme(context, uri)
                                }
                                val host = uri.host?.lowercase() ?: ""
                                val isMediaHost = host.contains("youtube") || host.contains("youtu.be") ||
                                        host.contains("vimeo") || host.contains("googlevideo") ||
                                        host.contains("ytimg") || host.contains("vimeocdn")
                                val initialHost = try { Uri.parse(formattedUrl).host?.lowercase() ?: "" } catch (_: Exception) { "" }
                                val isWatchLink = (host.endsWith("youtube.com") && (uri.path.orEmpty().startsWith("/watch") || uri.path.orEmpty().startsWith("/shorts"))) || host.endsWith("youtu.be")
                                if (isWatchLink) {
                                    return handleExternalNavigation(context, destination)
                                }
                                if (isMediaHost || (initialHost.isNotEmpty() && host.contains(initialHost))) {
                                    return false
                                }
                                return handleExternalNavigation(context, destination)
                            }
                        }

                        if (isYouTube || isVimeo) {
                            val html = VideoEmbedUtils.buildEmbedHtml(formattedUrl)
                            loadDataWithBaseURL("https://apod.nasa.gov", html, "text/html", "utf-8", null)
                        } else {
                            loadUrl(formattedUrl)
                        }
                        webViewRef = this
                    }
                } catch (t: Throwable) {
                    hasError = true
                    isBuffering = false
                    errorMessage = "WebView unavailable on this device"
                    View(ctx)
                }
            },
            update = {},
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onToggleFullscreen != null) {
                FilledTonalIconButton(
                    onClick = onToggleFullscreen,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Toggle fullscreen",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (showExternalButton && !hasError && customFullscreenView == null) {
                FilledTonalIconButton(
                    onClick = { VideoEmbedUtils.openVideoInExternalApp(context, videoUrl) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open in external app",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (isBuffering && !hasError) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }

        if (hasError) {
            VideoErrorOverlay(
                message = errorMessage ?: "Unable to load video embed",
                videoUrl = videoUrl,
                onRetry = onRetry
            )
        }
    }
}

private fun handleSpecialScheme(context: Context, uri: Uri): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        true
    }
}

private fun handleExternalNavigation(context: Context, url: String): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        true
    }
}

@Composable
private fun VideoErrorOverlay(
    message: String,
    videoUrl: String,
    onRetry: () -> Unit,
    onFallback: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
            .padding(20.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Video Playback Notice",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(percent = 50)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry", style = MaterialTheme.typography.labelLarge)
                }
                if (onFallback != null) {
                    FilledTonalButton(
                        onClick = onFallback,
                        shape = RoundedCornerShape(percent = 50)
                    ) {
                        Text("Web Player", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Button(
                    onClick = { VideoEmbedUtils.openVideoInExternalApp(context, videoUrl) },
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open Externally",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open App", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

fun openVideoInExternalApp(context: Context, videoUrl: String) {
    VideoEmbedUtils.openVideoInExternalApp(context, videoUrl)
}