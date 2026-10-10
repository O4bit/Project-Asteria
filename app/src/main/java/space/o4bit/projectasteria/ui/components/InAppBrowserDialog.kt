package space.o4bit.projectasteria.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

@Composable
fun InAppBrowserDialog(
    url: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initialUrl = remember(url) {
        if (url.startsWith("http://", ignoreCase = true)) {
            url.replaceFirst("http://", "https://", ignoreCase = true)
        } else {
            url
        }
    }
    var currentUrl by remember(initialUrl) { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("Loading...") }
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            webViewRef = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 4.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close browser")
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = if (pageTitle.isNotBlank()) pageTitle else "Browser",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val domain = try { Uri.parse(currentUrl).host ?: currentUrl } catch (_: Exception) { currentUrl }
                                Text(
                                    text = domain,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { webViewRef?.goBack() },
                                enabled = canGoBack
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            IconButton(
                                onClick = { webViewRef?.goForward() },
                                enabled = canGoForward
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                            }
                            IconButton(onClick = {
                                hasError = false
                                isLoading = true
                                webViewRef?.reload()
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload")
                            }
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, currentUrl)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share URL"))
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in External Browser")
                            }
                        }
                        if (isLoading) {
                            LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.Transparent
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { ctx ->
                            try {
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setBackgroundColor(android.graphics.Color.WHITE)
                                    CookieManager.getInstance().setAcceptCookie(true)
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    @SuppressLint("SetJavaScriptEnabled")
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        cacheMode = WebSettings.LOAD_DEFAULT
                                        allowContentAccess = true
                                        allowFileAccess = false
                                        setSupportMultipleWindows(false)
                                        javaScriptCanOpenWindowsAutomatically = true
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            isLoading = true
                                            hasError = false
                                            url?.let { currentUrl = it }
                                            canGoBack = view?.canGoBack() ?: false
                                            canGoForward = view?.canGoForward() ?: false
                                        }
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            isLoading = false
                                            url?.let { currentUrl = it }
                                            pageTitle = view?.title ?: ""
                                            canGoBack = view?.canGoBack() ?: false
                                            canGoForward = view?.canGoForward() ?: false
                                        }
                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                            val targetUrl = request?.url?.toString() ?: return false
                                            return handleOverride(targetUrl)
                                        }
                                        @Deprecated("Deprecated in Java")
                                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                            val targetUrl = url ?: return false
                                            return handleOverride(targetUrl)
                                        }
                                        private fun handleOverride(targetUrl: String): Boolean {
                                            if (targetUrl.startsWith("http://", ignoreCase = true) || targetUrl.startsWith("https://", ignoreCase = true)) {
                                                return false
                                            }
                                            return try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                                context.startActivity(intent)
                                                true
                                            } catch (_: Exception) {
                                                true
                                            }
                                        }
                                        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                            val host = view?.url?.let { try { Uri.parse(it).host } catch (_: Exception) { null } } ?: ""
                                            if (host.contains("nasa.gov") || host.contains("jpl.nasa.gov")) {
                                                handler?.proceed()
                                            } else {
                                                handler?.cancel()
                                                hasError = true
                                                errorMessage = "SSL security certificate mismatch"
                                            }
                                        }
                                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                            super.onReceivedError(view, request, error)
                                            if (request?.isForMainFrame == true) {
                                                isLoading = false
                                                hasError = true
                                                errorMessage = error?.description?.toString() ?: "Failed to load page"
                                            }
                                        }
                                        @Deprecated("Deprecated in Java")
                                        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                            super.onReceivedError(view, errorCode, description, failingUrl)
                                            isLoading = false
                                            hasError = true
                                            errorMessage = description ?: "Failed to load page"
                                        }
                                    }
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            progress = newProgress
                                            if (newProgress >= 100) isLoading = false
                                        }
                                        override fun onReceivedTitle(view: WebView?, title: String?) {
                                            if (!title.isNullOrBlank()) {
                                                pageTitle = title
                                            }
                                        }
                                    }
                                    webViewRef = this
                                    loadUrl(initialUrl)
                                }
                            } catch (e: Exception) {
                                hasError = true
                                errorMessage = "WebView could not be initialized on this device"
                                WebView(ctx)
                            }
                        },
                        update = { view ->
                            webViewRef = view
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    if (hasError) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "Unable to display web page",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = errorMessage ?: "The page could not be reached or connection failed.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(24.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            hasError = false
                                            isLoading = true
                                            webViewRef?.reload()
                                        }
                                    ) {
                                        Text("Retry")
                                    }
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }
                                    ) {
                                        Text("Open in Browser")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}