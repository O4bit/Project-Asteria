package space.o4bit.projectasteria.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ApodVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val isDirectVideo = remember(videoUrl) {
        videoUrl.contains(".mp4", ignoreCase = true) ||
                videoUrl.contains(".webm", ignoreCase = true) ||
                videoUrl.contains(".mov", ignoreCase = true) ||
                videoUrl.contains(".m4v", ignoreCase = true)
    }

    val isGif = remember(videoUrl) {
        videoUrl.contains(".gif", ignoreCase = true)
    }

    val htmlBody = remember(videoUrl, isDirectVideo, isGif) {
        when {
            isDirectVideo -> {
                """<video src="$videoUrl" autoplay loop controls playsinline style="width:100%; height:100%; object-fit:contain;"></video>"""
            }
            isGif -> {
                """<img src="$videoUrl" style="width:100%; height:100%; object-fit:contain;" />"""
            }
            else -> {
                val embedUrl = formatVideoUrl(videoUrl)
                """<iframe src="$embedUrl" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen style="width:100%; height:100%; border:none;"></iframe>"""
            }
        }
    }

    val isYouTube = remember(videoUrl) {
        videoUrl.contains("youtube", ignoreCase = true) || videoUrl.contains("youtu.be", ignoreCase = true)
    }

    val baseUrl = if (isYouTube) "https://www.youtube.com" else "https://apod.nasa.gov"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    @Suppress("DEPRECATION")
                    settings.pluginState = WebSettings.PluginState.ON
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true

                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()

                    val htmlData = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                            <style>
                                body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000; overflow: hidden; display: flex; justify-content: center; align-items: center; }
                                iframe, video, img { width: 100%; height: 100%; border: none; outline: none; object-fit: contain; }
                            </style>
                        </head>
                        <body>
                            $htmlBody
                        </body>
                        </html>
                    """.trimIndent()

                    loadDataWithBaseURL(baseUrl, htmlData, "text/html", "utf-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun formatVideoUrl(url: String): String {
    return when {
        url.contains("youtube.com/watch?v=", ignoreCase = true) -> {
            val videoId = url.substringAfter("v=").substringBefore("&")
            "https://www.youtube.com/embed/$videoId?autoplay=1&mute=0&playsinline=1&rel=0&enablejsapi=1"
        }
        url.contains("youtu.be/", ignoreCase = true) -> {
            val videoId = url.substringAfter("youtu.be/").substringBefore("?")
            "https://www.youtube.com/embed/$videoId?autoplay=1&mute=0&playsinline=1&rel=0&enablejsapi=1"
        }
        url.contains("youtube.com/embed/", ignoreCase = true) -> {
            if (!url.contains("autoplay=")) {
                if (url.contains("?")) "$url&autoplay=1&playsinline=1" else "$url?autoplay=1&playsinline=1"
            } else url
        }
        url.contains("vimeo.com/", ignoreCase = true) -> {
            val videoId = url.substringAfter("vimeo.com/").substringBefore("?")
            if (videoId.isNotBlank()) "https://player.vimeo.com/video/$videoId?autoplay=1&title=0&byline=0&portrait=0" else url
        }
        else -> url
    }
}
