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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    starsCount: Int = 100,
    content: @Composable BoxScope.() -> Unit
) {
    // Get theme colors to use for the space background gradient
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // Background gradient using theme colors for better integration with Material 3
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            surfaceColor.copy(alpha = 0.95f),   // Slightly transparent surface color
            surfaceColor,                       // Base surface color
            surfaceColor.copy(alpha = 0.9f)     // Slightly different shade for gradient effect
        )
    )

    // Generate random stars positions
    val stars = remember {
        List(starsCount) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 2.5f + 0.5f,
                alpha = Random.nextFloat() * 0.7f + 0.3f
            )
        }
    }

    // Nebula positions for subtle color accents using theme colors
    val nebulae = remember {
        List(3) {
            Nebula(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 0.2f + 0.1f,
                color = when (it % 3) {
                    0 -> primaryColor.copy(alpha = 0.1f)     // Very subtle primary color
                    1 -> secondaryColor.copy(alpha = 0.1f)   // Very subtle secondary color
                    else -> tertiaryColor.copy(alpha = 0.1f) // Very subtle tertiary color
                }
            )
        }
    }

    // Animation for twinkling stars
    val infiniteTransition = rememberInfiniteTransition(label = "starTwinkle")
    val twinkleAnimation = infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    // Extract the color for stars here so it can be used in the Canvas
    val starColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
    ) {
        // Draw the starry background
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(twinkleAnimation.value)
        ) {
            // Draw nebulae first (they go behind stars)
            nebulae.forEach { nebula ->
                drawNebula(nebula)
            }

            // Draw stars
            stars.forEach { star ->
                drawStar(star, starColor)
            }
        }

        // Content goes on top of the starry background
        content()
    }
}

private data class Star(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float
)

private data class Nebula(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color
)

private fun DrawScope.drawStar(star: Star, starColor: Color = Color.White) {
    val x = star.x * size.width
    val y = star.y * size.height

    drawCircle(
        color = starColor,
        radius = star.size,
        center = Offset(x, y),
        alpha = star.alpha
    )
}

private fun DrawScope.drawNebula(nebula: Nebula) {
    val x = nebula.x * size.width
    val y = nebula.y * size.height
    val radius = min(size.width, size.height) * nebula.radius

    drawCircle(
        color = nebula.color,
        radius = radius,
        center = Offset(x, y)
    )
}
