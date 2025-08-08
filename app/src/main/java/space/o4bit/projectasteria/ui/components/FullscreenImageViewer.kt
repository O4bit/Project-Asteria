package space.o4bit.projectasteria.ui.components

import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Environment
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import space.o4bit.projectasteria.R
import space.o4bit.projectasteria.data.model.AstronomyPicture
import space.o4bit.projectasteria.ui.icons.WallpaperIcon

/**
 * Fullscreen viewer for astronomy pictures with options to download and share
 */
@Composable
fun FullscreenImageViewer(
    astronomyPicture: AstronomyPicture,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Control UI visibility
    var showControls by remember { mutableStateOf(true) }
    val controlsAlpha by animateFloatAsState(
        targetValue = if (showControls) 1f else 0f,
        label = "controlsAlpha"
    )

    // State for wallpaper confirmation dialog
    var showWallpaperDialog by remember { mutableStateOf(false) }

    // Format date for display
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val displayFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)
    val formattedDate = try {
        dateFormat.parse(astronomyPicture.date)?.let { 
            displayFormat.format(it) 
        } ?: astronomyPicture.date
    } catch (_: Exception) {
        astronomyPicture.date
    }

    // Auto-hide controls after a period of inactivity
    LaunchedEffect(showControls) {
        if (showControls) {
            // Only hide controls automatically if they're currently shown
            try {
                delay(3000)
                // Check if showControls is still true before hiding
                // This prevents hiding right after a user just showed them
                if (showControls) {
                    showControls = false
                }
            } catch (_: Exception) {
                // Catch any cancellation exceptions when recomposing
            }
        }
    }

    // Wallpaper confirmation dialog
    if (showWallpaperDialog) {
        val wallpaperOptions = listOf(
            "Home Screen" to WallpaperManager.FLAG_SYSTEM,
            "Lock Screen" to WallpaperManager.FLAG_LOCK,
            "Both Screens" to (WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
        )
        var selectedOption by remember { mutableStateOf(wallpaperOptions[2]) } // Default to both screens
        
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = { Text("Set as Wallpaper") },
            text = {
                Column {
                    Text("Select where to apply the wallpaper:")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    wallpaperOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOption = option }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedOption == option,
                                onClick = { selectedOption = option }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option.first)
                        }
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        showWallpaperDialog = false
                        coroutineScope.launch {
                            val success = downloadAndSetWallpaper(
                                context, 
                                astronomyPicture,
                                selectedOption.second // Pass the selected wallpaper flag
                            )
                            val message = if (success) {
                                "Wallpaper set successfully"
                            } else {
                                "Failed to set wallpaper"
                            }
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                ) {
                    Text("Set Wallpaper")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWallpaperDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding() // Handle insets for notches and cutouts
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { /* Single tap does nothing - UI toggle only on double tap */ },
                    onDoubleTap = { 
                        // Double-tap toggles UI controls (like Google Photos)
                        showControls = !showControls 
                    }
                )
            }
    ) {
        // The fullscreen image with zoom functionality
        ZoomableImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(astronomyPicture.hdUrl ?: astronomyPicture.url)
                .crossfade(true)
                .build(),
            contentDescription = astronomyPicture.title,
        )

        // Top bar with back button - using theme colors for consistency
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(controlsAlpha)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onBackPressed,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.arrowback),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = astronomyPicture.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Bottom action buttons - using theme colors for consistency
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .alpha(controlsAlpha)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Download button
            FilledTonalIconButton(
                onClick = {
                    val success = downloadImage(context, astronomyPicture)
                    coroutineScope.launch {
                        val message = if (success) {
                            "Downloading image..."
                        } else {
                            "Failed to download image"
                        }
                        snackbarHostState.showSnackbar(message)
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.download),
                    contentDescription = "Download Image",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Share button
            FilledTonalIconButton(
                onClick = { shareImageOnly(context, astronomyPicture) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.share),
                    contentDescription = "Share Image",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Wallpaper button
            FilledTonalIconButton(
                onClick = { showWallpaperDialog = true },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = WallpaperIcon,
                    contentDescription = "Set as Wallpaper",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Snackbar for notifications
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) { snackbarData ->
            Snackbar(
                snackbarData = snackbarData,
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
    }
}

/**
 * Downloads the image using Android's DownloadManager
 */
private fun downloadImage(context: Context, astronomyPicture: AstronomyPicture): Boolean {
    try {
        val imageUrl = astronomyPicture.hdUrl ?: astronomyPicture.url
        val request = DownloadManager.Request(imageUrl.toUri())

        // Use title and date to create a filename
        val filename = "NASA_${astronomyPicture.title.replace(" ", "_")}_${astronomyPicture.date}.jpg"

        request.apply {
            setTitle("Downloading: ${astronomyPicture.title}")
            setDescription("NASA Astronomy Picture")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "Asteria/$filename")
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        return true
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

/**
 * Shares the image URL using Android's share intent
 */
private fun shareImageOnly(context: Context, astronomyPicture: AstronomyPicture) {
    val imageUrl = astronomyPicture.hdUrl ?: astronomyPicture.url
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        // Only share the image URL with no additional text
        putExtra(Intent.EXTRA_TEXT, imageUrl)
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share Space Image"))
}

/**
 * Custom composable that supports zoom and pan gestures for images
 */
@Composable
fun ZoomableImage(
    model: Any,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    // State for zoom level and offset
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // Image size state for constraining pan limits
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Calculate bounds for panning based on scale and image size
    val maxX = (imageSize.width * (scale - 1) / 2f)
    val maxY = (imageSize.height * (scale - 1) / 2f)
    
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        // Update scale with zoom limits
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        
        // Calculate new offset with constraints
        val newOffsetX = offset.x + offsetChange.x
        val newOffsetY = offset.y + offsetChange.y
        
        // Apply constraints only when zoomed in
        offset = if (scale > 1f) {
            Offset(
                x = newOffsetX.coerceIn(-maxX, maxX),
                y = newOffsetY.coerceIn(-maxY, maxY)
            )
        } else {
            // When at normal scale, reset offset
            Offset.Zero
        }
    }
    
    // Reset zoom when content changes
    LaunchedEffect(model) {
        scale = 1f
        offset = Offset.Zero
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .transformable(
                state = state,
                lockRotationOnZoomPan = true
            )
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .onSizeChanged { size ->
                    // Store the size of the composable for pan constraints
                    imageSize = size
                }
        )
    }
}

/**
 * Downloads the image and sets it as wallpaper
 */
private suspend fun downloadAndSetWallpaper(
    context: Context, 
    astronomyPicture: AstronomyPicture,
    wallpaperType: Int = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
): Boolean {
    try {
        val imageUrl = astronomyPicture.hdUrl ?: astronomyPicture.url
        val wallpaperManager = WallpaperManager.getInstance(context)

        // Create a unique file name for the temporary wallpaper
        val fileName = "temp_wallpaper_${System.currentTimeMillis()}.jpg"
        
        // Use app's cache directory instead of external storage to avoid permission issues
        val cacheDir = context.cacheDir
        val wallpaperDir = File(cacheDir, "wallpapers")
        if (!wallpaperDir.exists()) {
            wallpaperDir.mkdirs()
        }
        val wallpaperFile = File(wallpaperDir, fileName)

        // Download the image using Coil instead of DownloadManager for better reliability
        withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(Size.ORIGINAL) // Keep original size
                    .allowHardware(false) // Needed for bitmap extraction
                    .build()
                
                // Get the ImageLoader instance
                val imageLoader = ImageLoader(context)
                
                val result = (imageLoader.execute(request) as SuccessResult).drawable
                val bitmap = (result as BitmapDrawable).bitmap
                
                // Use a safer approach to save bitmap
                FileOutputStream(wallpaperFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                
                // Scale down bitmap if it's too large to avoid OOM errors
                val scaledBitmap = decodeSampledBitmapFromFile(
                    wallpaperFile.absolutePath,
                    Resources.getSystem().displayMetrics.widthPixels,
                    Resources.getSystem().displayMetrics.heightPixels
                )
                
                try {
                    // Use the provided wallpaper type flag
                    wallpaperManager.setBitmap(
                        scaledBitmap,
                        null, // no crop area
                        true, // allow lock screen
                        wallpaperType // Use the parameter passed
                    )
                    return@withContext true
                } catch (e: IOException) {
                    Log.e("Wallpaper", "Error setting wallpaper", e)
                    return@withContext false
                } finally {
                    // Clean up bitmaps to prevent memory leaks
                    scaledBitmap?.recycle()
                    if (bitmap != scaledBitmap) {
                        bitmap.recycle()
                    }
                    // Delete temp file
                    wallpaperFile.delete()
                }
            } catch (e: Exception) {
                Log.e("Wallpaper", "Error downloading image", e)
                return@withContext false
            }
        }
        // If we made it here without exceptions, return true
        return true
    } catch (e: Exception) {
        Log.e("Wallpaper", "Error in wallpaper process", e)
        return false
    }
}

/**
 * Decodes a bitmap from file with sampling to prevent OutOfMemoryError
 */
private fun decodeSampledBitmapFromFile(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
    return try {
        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)
        
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        BitmapFactory.decodeFile(filePath, options)
    } catch (e: Exception) {
        Log.e("Wallpaper", "Error decoding bitmap", e)
        null
    }
}

/**
 * Calculate the optimal sampling size for loading a bitmap
 */
private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1
    
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        
        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    
    return inSampleSize
}