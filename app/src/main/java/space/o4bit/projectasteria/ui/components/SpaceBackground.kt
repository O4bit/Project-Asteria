package space.o4bit.projectasteria.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import space.o4bit.projectasteria.ui.components.backgrounds.rememberParallaxState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Animated space background with stars moving towards the viewer and occasional meteors.
 * Stars fly in 3D perspective from the distance towards the camera, creating a warp-speed effect.
 */
@Composable
fun SpaceBackground(
    modifier: Modifier = Modifier,
    enableParallax: Boolean = true,
    speedMultiplier: Float = 1f,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val parallaxState = rememberParallaxState(
        enableParallax = enableParallax,
        sensitivity = 0.5f,
        context = context,
        coroutineScope = coroutineScope
    )
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkTheme = surfaceColor.luminance() < 0.5f
    val starColor = if (isDarkTheme) Color.White else Color(0xFF1A2530)

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            surfaceColor.copy(alpha = 0.98f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            surfaceColor.copy(alpha = 0.92f),
            surfaceColor
        )
    )

    val stars = remember(isDarkTheme) {
        mutableStateListOf<SpaceStarData>().apply {
            addAll(generateStarPool())
        }
    }

    var baseProgress by remember { mutableFloatStateOf(0f) }
    val currentSpeedMultiplier = remember { Animatable(1f) }
    
    LaunchedEffect(speedMultiplier) {
        currentSpeedMultiplier.animateTo(
            targetValue = speedMultiplier,
            animationSpec = tween(durationMillis = 3000, easing = LinearOutSlowInEasing)
        )
    }

    // Continuous star animation loop
    LaunchedEffect(Unit) {
        var lastFrameMs = 0L
        while (true) {
            withInfiniteAnimationFrameMillis { frameMs ->
                val delta = if (lastFrameMs == 0L) 16L else (frameMs - lastFrameMs)
                lastFrameMs = frameMs
                baseProgress += (delta / 16f) * 0.0008f * currentSpeedMultiplier.value

                // Regenerate stars that have passed the camera
                stars.forEachIndexed { index, star ->
                    val adjustedProgress = ((baseProgress * star.speed) + star.initialOffset) % 1f

                    if (adjustedProgress > 0.98f || adjustedProgress < 0.01f) {
                        if (star.lastRegen != baseProgress.toInt()) {
                            var newX: Float
                            var newY: Float
                            var newDistance: Float

                            do {
                                val newAngle = Random.nextFloat() * 360f
                                newDistance = sqrt(Random.nextFloat()) * 1.5f
                                val newAngleRad = newAngle * (Math.PI / 180f).toFloat()
                                newX = cos(newAngleRad) * newDistance
                                newY = sin(newAngleRad) * newDistance
                            } while (newDistance < 0.15f)

                            stars[index] = star.copy(
                                x = newX,
                                y = newY,
                                lastRegen = baseProgress.toInt()
                            )
                        }
                    }
                }
            }
        }
    }

    var meteor by remember { mutableStateOf<SpaceMeteorState?>(null) }
    val meteorProgress = remember { Animatable(0f) }

    // Meteor spawner
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(40_000, 60_000))

            val direction = Random.nextInt(2)
            val angle = when (direction) {
                0 -> 130f + Random.nextFloat() * 20f
                else -> 30f + Random.nextFloat() * 20f
            }

            val newMeteor = SpaceMeteorState(
                startX = Random.nextFloat(),
                startY = Random.nextFloat() * 0.3f,
                angle = angle,
                length = 200f + Random.nextFloat() * 150f,
                depth = 0.4f + Random.nextFloat() * 0.6f,
                thickness = 4f
            )

            meteor = newMeteor
            meteorProgress.snapTo(0f)
            meteorProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(1200, easing = LinearOutSlowInEasing)
            )
            meteor = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f
            val tiltX = parallaxState.tiltX.value
            val tiltY = parallaxState.tiltY.value

            // Render stars with 3D perspective
            stars.forEach { star ->
                val adjustedProgress = ((baseProgress * star.speed) + star.initialOffset) % 1f
                val z = 1f - adjustedProgress

                if (z < 0.05f || z > 1.2f) return@forEach

                val perspectiveFactor = 1f / z.coerceAtLeast(0.1f)

                val baseX = star.x * width * 0.5f
                val baseY = star.y * height * 0.5f

                val projectedX = baseX * perspectiveFactor
                val projectedY = baseY * perspectiveFactor

                val sizeFactor = perspectiveFactor * 0.65f
                val finalSize = star.size * sizeFactor
                
                // Far stars move less with parallax, close stars move more
                val parallaxFactor = 1f / z.coerceAtLeast(0.1f)
                val parallaxX = tiltX * 250f * parallaxFactor
                val parallaxY = tiltY * 250f * parallaxFactor

                val finalX = centerX + projectedX + parallaxX
                val finalY = centerY + projectedY + parallaxY

                // Cull stars outside screen bounds
                if (finalX < -150 || finalX > width + 150 ||
                    finalY < -150 || finalY > height + 150
                ) {
                    return@forEach
                }


                // Smooth fade in from distance
                val fadeIn = when {
                    z > 1.0f -> ((1.2f - z) / 0.2f).coerceIn(0f, 1f)
                    else -> 1f
                }

                // Smooth fade out when approaching camera
                val fadeOut = when {
                    z < 0.15f -> (z / 0.15f).coerceIn(0f, 1f)
                    else -> 1f
                }

                val distanceAlpha = when {
                    z > 0.6f -> ((1f - z) / 0.4f).coerceIn(0f, 1f)
                    z < 0.3f -> (z / 0.3f).coerceIn(0f, 1f)
                    else -> 1f
                }

                val combinedAlpha = (star.baseAlpha * distanceAlpha * fadeIn * fadeOut).coerceIn(0f, 1f)

                // Outer glow
                drawCircle(
                    color = starColor,
                    radius = finalSize * 1.8f,
                    center = Offset(finalX, finalY),
                    alpha = combinedAlpha * 0.2f
                )

                // Main star body
                drawCircle(
                    color = starColor,
                    radius = finalSize * 1.1f,
                    center = Offset(finalX, finalY),
                    alpha = combinedAlpha
                )
            }

            // Render meteor
            meteor?.let { m ->
                val p = meteorProgress.value
                val angleRad = m.angle * (Math.PI / 180f).toFloat()

                val travelDistance = width * 2f * p

                val cosAngle = cos(angleRad)
                val sinAngle = sin(angleRad)

                val curX = (m.startX * width) + (travelDistance * cosAngle)
                val curY = (m.startY * height) + (travelDistance * sinAngle)

                val tailX = curX - (m.length * cosAngle)
                val tailY = curY - (m.length * sinAngle)

                // Outer glow
                drawLine(
                    brush = Brush.linearGradient(
                        0.0f to starColor.copy(alpha = 0.3f),
                        0.6f to starColor.copy(alpha = 0.15f),
                        1.0f to Color.Transparent,
                        start = Offset(curX, curY),
                        end = Offset(tailX, tailY)
                    ),
                    start = Offset(curX, curY),
                    end = Offset(tailX, tailY),
                    strokeWidth = m.thickness * 4f,
                    cap = StrokeCap.Round
                )

                // Core trail
                drawLine(
                    brush = Brush.linearGradient(
                        0.0f to starColor.copy(alpha = 0.95f),
                        0.5f to starColor.copy(alpha = 0.6f),
                        1.0f to Color.Transparent,
                        start = Offset(curX, curY),
                        end = Offset(tailX, tailY)
                    ),
                    start = Offset(curX, curY),
                    end = Offset(tailX, tailY),
                    strokeWidth = m.thickness,
                    cap = StrokeCap.Round
                )
            }
        }
        content()
    }
}

/**
 * Simple luminance check for Color
 */
private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}

/**
 * Generates initial pool of stars with varied properties
 */
private fun generateStarPool(): List<SpaceStarData> {
    return List(300) { index ->
        val depthLayer = index / 300f

        var x: Float
        var y: Float
        var distance: Float

        do {
            val angle = (index * 137.5f) % 360f
            distance = sqrt(Random.nextFloat()) * 1.5f
            val angleRad = angle * (Math.PI / 180f).toFloat()
            x = cos(angleRad) * distance
            y = sin(angleRad) * distance
        } while (distance < 0.15f)

        SpaceStarData(
            x = x,
            y = y,
            size = 2f + Random.nextFloat() * 3.5f,
            baseAlpha = 0.6f + Random.nextFloat() * 0.4f,
            depth = depthLayer,
            speed = 0.5f + depthLayer * 1f,
            initialOffset = Random.nextFloat(),
            lastRegen = -1
        )
    }
}

private data class SpaceStarData(
    val x: Float,
    val y: Float,
    val size: Float,
    val baseAlpha: Float,
    val depth: Float,
    val speed: Float,
    val initialOffset: Float,
    val lastRegen: Int
)

private data class SpaceMeteorState(
    val startX: Float,
    val startY: Float,
    val angle: Float,
    val length: Float,
    val depth: Float,
    val thickness: Float
)
