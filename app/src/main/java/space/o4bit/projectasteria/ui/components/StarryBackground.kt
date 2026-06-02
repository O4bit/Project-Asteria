package space.o4bit.projectasteria.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import space.o4bit.projectasteria.ui.components.backgrounds.rememberParallaxState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.min
import kotlin.random.Random

/**
 * A composable that creates a starry space background with subtle animation
 * that aligns with Material 3 theming
 */
@Composable
fun StarryBackground(
    modifier: Modifier = Modifier,
    starsCount: Int = 80,
    enableParallax: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val parallaxState = rememberParallaxState(
        enableParallax = enableParallax,
        sensitivity = 0.4f,
        context = context,
        coroutineScope = coroutineScope
    )
    
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            surfaceColor.copy(alpha = 0.98f),
            surfaceVariant.copy(alpha = 0.95f),
            surfaceColor.copy(alpha = 0.92f),
            surfaceColor
        )
    )

    val stars = remember {
        List(starsCount) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 2.5f + 0.8f,
                alpha = Random.nextFloat() * 0.6f + 0.4f,
                twinkleSpeed = Random.nextFloat() * 2000 + 3000
            )
        }
    }

    val nebulae = remember {
        List(5) {
            Nebula(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 0.15f + 0.05f,
                color = when (it % 4) {
                    0 -> primaryColor.copy(alpha = 0.08f)
                    1 -> secondaryColor.copy(alpha = 0.08f)
                    2 -> tertiaryColor.copy(alpha = 0.08f)
                    else -> onSurface.copy(alpha = 0.05f)
                },
                pulseSpeed = Random.nextFloat() * 4000 + 6000
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "starryBackground")
    val twinkleBase = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "baseTwinkle"
    )

    val nebulaPulse = infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nebulaPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.9f)
        ) {
            val tiltX = parallaxState.tiltX.value
            val tiltY = parallaxState.tiltY.value
            
            nebulae.forEachIndexed { index, nebula ->
                val depth = 0.2f + (index * 0.1f)
                val pX = tiltX * 100f * depth
                val pY = tiltY * 100f * depth
                drawNebula(nebula, nebulaPulse.value, pX, pY)
            }

            stars.forEachIndexed { index, star ->
                val depth = 0.5f + (index % 5) * 0.1f
                val pX = tiltX * 150f * depth
                val pY = tiltY * 150f * depth
                val individualTwinkle = (twinkleBase.value + 
                    kotlin.math.sin((System.currentTimeMillis() % star.twinkleSpeed.toLong()) / star.twinkleSpeed * 2 * kotlin.math.PI).toFloat() * 0.3f)
                    .coerceIn(0.4f, 1f)
                
                drawStar(star, onSurface, individualTwinkle, pX, pY)
            }
        }
        content()
    }
}

/**
 * Data class representing a star with twinkling properties
 */
private data class Star(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val twinkleSpeed: Float
)

/**
 * Data class representing a nebula with pulse animation
 */
private data class Nebula(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color,
    val pulseSpeed: Float
)

/**
 * Extension function to draw a star with twinkling effect
 */
private fun DrawScope.drawStar(star: Star, starColor: Color, twinkleMultiplier: Float, offsetX: Float = 0f, offsetY: Float = 0f) {
    val x = star.x * size.width + offsetX
    val y = star.y * size.height + offsetY
    val finalAlpha = (star.alpha * twinkleMultiplier).coerceIn(0.1f, 1f)

    drawCircle(
        color = starColor.copy(alpha = finalAlpha),
        radius = star.size,
        center = Offset(x, y)
    )
    
    if (star.size > 2f) {
        drawCircle(
            color = starColor.copy(alpha = finalAlpha * 0.3f),
            radius = star.size * 1.8f,
            center = Offset(x, y)
        )
    }
}

/**
 * Extension function to draw a nebula with pulse effect
 */
private fun DrawScope.drawNebula(nebula: Nebula, pulseMultiplier: Float, offsetX: Float = 0f, offsetY: Float = 0f) {
    val x = nebula.x * size.width + offsetX
    val y = nebula.y * size.height + offsetY
    val radius = min(size.width, size.height) * nebula.radius * pulseMultiplier

    drawCircle(
        color = nebula.color,
        radius = radius,
        center = Offset(x, y)
    )
    
    drawCircle(
        color = nebula.color.copy(alpha = nebula.color.alpha * 0.5f),
        radius = radius * 1.5f,
        center = Offset(x, y)
    )
}
