package space.o4bit.projectasteria.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.o4bit.projectasteria.data.model.iss.IssPosition
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.derivedStateOf

/**
 * Production-Grade Dynamic Vector World Map & ISS Tracker.
 *
 * Features:
 *  - Strict Viewport Edge Clamping (Zero blank space outside map boundaries at any zoom level)
 *  - Centroid-aware zooming
 *  - High accuracy SVG world map
 *  - Dynamic MaterialTheme 3 color palette integration
 */
@Composable
fun IssGlobe(
    issPosition: IssPosition?,
    orbitTrail: List<IssPosition>,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Parse the high-accuracy world map path from raw resources
    val worldPath = remember {
        val pathStr = context.resources.openRawResource(space.o4bit.projectasteria.R.raw.world_map_path)
            .bufferedReader().use { it.readText() }
        androidx.core.graphics.PathParser.createPathFromPathData(pathStr).asComposePath()
    }

    // Pan & Zoom state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    // Cache the scaled world path — only recomputed when canvas dimensions change, not every frame
    val scaledWorldPath by remember(canvasSize) {
        derivedStateOf {
            if (canvasSize.width == 0 || canvasSize.height == 0) {
                worldPath // return unscaled until canvas is measured
            } else {
                val scaleX = canvasSize.width / 360f
                val scaleY = canvasSize.height / 180f
                Path().apply {
                    addPath(worldPath)
                    transform(androidx.compose.ui.graphics.Matrix().apply {
                        scale(scaleX, scaleY)
                    })
                }
            }
        }
    }

    // Dynamic Material Theme color tokens
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceLowest = MaterialTheme.colorScheme.surfaceContainerLowest
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    // Map element theme derivations
    val oceanGradientStart = surfaceLowest
    val oceanGradientEnd = surfaceHigh
    val landFillColor = secondaryContainer.copy(alpha = 0.75f)
    val landBorderColor = primaryColor
    val landGlowColor = primaryColor.copy(alpha = 0.35f)
    val gridColor = outlineVariant.copy(alpha = 0.3f)

    // Pulsing target animation
    val infiniteTransition = rememberInfiniteTransition(label = "mapAnim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarRotation"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.85f)
            .clip(RoundedCornerShape(24.dp))
            .border(0.8.dp, outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
        color = oceanGradientStart
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = scale
                            val newScale = (scale * zoom).coerceIn(1f, 4.5f)
                            val scaleFactor = newScale / oldScale

                            val center = Offset(size.width / 2f, size.height / 2f)
                            val centroidFromCenter = centroid - center

                            // Adjust offsets to keep the centroid stationary under the fingers
                            val newOffsetX = offsetX * scaleFactor - centroidFromCenter.x * (scaleFactor - 1f) + pan.x
                            val newOffsetY = offsetY * scaleFactor - centroidFromCenter.y * (scaleFactor - 1f) + pan.y

                            val maxPanX = (size.width * (newScale - 1f)) / 2f
                            val maxPanY = (size.height * (newScale - 1f)) / 2f

                            scale = newScale
                            if (newScale == 1f) {
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                offsetX = newOffsetX.coerceIn(-maxPanX, maxPanX)
                                offsetY = newOffsetY.coerceIn(-maxPanY, maxPanY)
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            ) {
                val w = size.width
                val h = size.height

                clipRect {
                    // 1. Dynamic Ocean Gradient
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(oceanGradientEnd, oceanGradientStart),
                            center = Offset(w / 2f, h / 2f),
                            radius = w * 0.75f
                        )
                    )

                    // 2. Dynamic Lat/Lon Coordinate Grid
                    val gridDash = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
                    for (lat in -60..60 step 30) {
                        val y = (90f - lat) / 180f * h
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f,
                            pathEffect = gridDash
                        )
                    }
                    for (lon in -150..150 step 30) {
                        val x = (lon + 180f) / 360f * w
                        drawLine(
                            color = gridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1f,
                            pathEffect = gridDash
                        )
                    }

                    // Equator & Prime Meridian lines
                    val eqY = 90f / 180f * h
                    val pmX = 180f / 360f * w
                    drawLine(
                        color = primaryColor.copy(alpha = 0.25f),
                        start = Offset(0f, eqY),
                        end = Offset(w, eqY),
                        strokeWidth = 1.2f
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.25f),
                        start = Offset(pmX, 0f),
                        end = Offset(pmX, h),
                        strokeWidth = 1.2f
                    )

                    // 3. Day / Night Solar Terminator Curve
                    drawDayNightTerminatorAccurate(w, h, surfaceColor, tertiaryColor)

                    // 4. High-Fidelity Organic Vector Continents (path cached by canvas size)
                    drawPath(scaledWorldPath, landFillColor)
                    drawPath(scaledWorldPath, landGlowColor, style = Stroke(width = 3.2f))
                    drawPath(scaledWorldPath, landBorderColor, style = Stroke(width = 1.2f))

                    // 5. Dynamic Orbit Trajectory Line
                    if (orbitTrail.size >= 2) {
                        for (i in 0 until orbitTrail.size - 1) {
                            val p1 = orbitTrail[i]
                            val p2 = orbitTrail[i + 1]

                            if (abs(p1.longitude - p2.longitude) > 180) continue

                            val x1 = ((p1.longitude + 180.0) / 360.0 * w).toFloat()
                            val y1 = ((90.0 - p1.latitude) / 180.0 * h).toFloat()
                            val x2 = ((p2.longitude + 180.0) / 360.0 * w).toFloat()
                            val y2 = ((90.0 - p2.latitude) / 180.0 * h).toFloat()

                            val progress = (1f - i.toFloat() / orbitTrail.size).coerceIn(0.15f, 0.95f)

                            drawLine(
                                color = primaryColor.copy(alpha = progress * 0.45f),
                                start = Offset(x1, y1),
                                end = Offset(x2, y2),
                                strokeWidth = 5f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = onSurface.copy(alpha = progress * 0.95f),
                                start = Offset(x1, y1),
                                end = Offset(x2, y2),
                                strokeWidth = 2f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // 6. Dynamic ISS Target Reticle
                    issPosition?.let { pos ->
                        val targetX = ((pos.longitude + 180.0) / 360.0 * w).toFloat()
                        val targetY = ((90.0 - pos.latitude) / 180.0 * h).toFloat()
                        val targetOffset = Offset(targetX, targetY)

                        // Outer pulse ring
                        drawCircle(
                            color = errorColor.copy(alpha = pulseAlpha),
                            radius = 22f * pulseScale,
                            center = targetOffset,
                            style = Stroke(width = 2.5f)
                        )

                        // Radar arc
                        drawArc(
                            color = errorColor,
                            startAngle = radarRotation,
                            sweepAngle = 110f,
                            useCenter = false,
                            topLeft = Offset(targetX - 16f, targetY - 16f),
                            size = Size(32f, 32f),
                            style = Stroke(width = 2f)
                        )

                        // Center glowing core
                        drawCircle(
                            color = errorColor,
                            radius = 6f,
                            center = targetOffset
                        )
                        drawCircle(
                            color = onSurface,
                            radius = 2.5f,
                            center = targetOffset
                        )

                        // Precision Crosshair Ticks
                        val tickLen = 14f
                        val tickGap = 8f
                        drawLine(
                            color = errorColor,
                            start = Offset(targetX - tickLen, targetY),
                            end = Offset(targetX - tickGap, targetY),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = errorColor,
                            start = Offset(targetX + tickGap, targetY),
                            end = Offset(targetX + tickLen, targetY),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = errorColor,
                            start = Offset(targetX, targetY - tickLen),
                            end = Offset(targetX, targetY - tickGap),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = errorColor,
                            start = Offset(targetX, targetY + tickGap),
                            end = Offset(targetX, targetY + tickLen),
                            strokeWidth = 2f
                        )
                    }
                }
            }

            // Top Floating Glass Telemetry HUD Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                shape = CircleShape,
                color = surfaceHigh.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, outlineVariant.copy(alpha = 0.4f)),
                shadowElevation = 4.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SatelliteAlt,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (issPosition != null) {
                            val latDir = if (issPosition.latitude >= 0) "N" else "S"
                            val lonDir = if (issPosition.longitude >= 0) "E" else "W"
                            String.format(
                                java.util.Locale.US,
                                "%.2f°%s, %.2f°%s",
                                abs(issPosition.latitude), latDir,
                                abs(issPosition.longitude), lonDir
                            )
                        } else "LOCATING SATELLITE...",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Bottom-Right Interactive Zoom & Recenter Pill Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (scale > 1.05f || offsetX != 0f || offsetY != 0f) {
                    IconButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = surfaceHigh.copy(alpha = 0.9f),
                            contentColor = primaryColor
                        ),
                        modifier = Modifier
                            .size(36.dp)
                            .border(0.8.dp, outlineVariant.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Recenter",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                }

                Surface(
                    shape = CircleShape,
                    color = surfaceHigh.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, outlineVariant.copy(alpha = 0.4f)),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val newScale = (scale - 0.5f).coerceAtLeast(1f)
                                scale = newScale
                                if (newScale == 1f) {
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    val maxPanX = (canvasSize.width * (newScale - 1f)) / 2f
                                    val maxPanY = (canvasSize.height * (newScale - 1f)) / 2f
                                    offsetX = offsetX.coerceIn(-maxPanX, maxPanX)
                                    offsetY = offsetY.coerceIn(-maxPanY, maxPanY)
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Zoom out",
                                tint = onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = String.format(java.util.Locale.US, "%.1fx", scale),
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        IconButton(
                            onClick = {
                                val newScale = (scale + 0.5f).coerceAtMost(4.5f)
                                scale = newScale
                                if (newScale == 1f) {
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    val maxPanX = (canvasSize.width * (newScale - 1f)) / 2f
                                    val maxPanY = (canvasSize.height * (newScale - 1f)) / 2f
                                    offsetX = offsetX.coerceIn(-maxPanX, maxPanX)
                                    offsetY = offsetY.coerceIn(-maxPanY, maxPanY)
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Zoom in",
                                tint = onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawDayNightTerminatorAccurate(
    w: Float,
    h: Float,
    nightShadowColor: Color,
    horizonBorderColor: Color
) {
    val now = System.currentTimeMillis() / 1000.0
    val dayOfYear = ((now % 31536000) / 86400).toInt()
    val declination = 23.45 * sin(2 * PI * (dayOfYear - 81) / 365)
    val decRad = declination * PI / 180.0
    val sunLon = -((now % 86400) / 86400.0 * 360.0 - 180.0)

    val shadowPath = Path()
    val borderPath = Path()

    shadowPath.moveTo(0f, h)
    var firstPoint = true

    for (xPx in 0..w.toInt() step 4) {
        val lon = (xPx.toFloat() / w) * 360.0 - 180.0
        val lonDiffRad = (lon - sunLon) * PI / 180.0
        val latRad = kotlin.math.atan(-cos(lonDiffRad) / kotlin.math.tan(decRad))
        val lat = latRad * 180.0 / PI
        val yPx = ((90.0 - lat) / 180.0 * h).toFloat().coerceIn(0f, h)

        shadowPath.lineTo(xPx.toFloat(), yPx)
        if (firstPoint) {
            borderPath.moveTo(xPx.toFloat(), yPx)
            firstPoint = false
        } else {
            borderPath.lineTo(xPx.toFloat(), yPx)
        }
    }
    shadowPath.lineTo(w, h)
    shadowPath.close()

    // 1. Dynamic night shadow fill
    drawPath(
        path = shadowPath,
        color = nightShadowColor.copy(alpha = 0.65f)
    )

    // 2. Dynamic twilight horizon border
    drawPath(
        path = borderPath,
        color = horizonBorderColor.copy(alpha = 0.85f),
        style = Stroke(width = 2.5f)
    )
}

