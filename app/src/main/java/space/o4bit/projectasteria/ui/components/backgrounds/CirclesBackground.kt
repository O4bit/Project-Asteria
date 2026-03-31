package space.o4bit.projectasteria.ui.components.backgrounds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun CirclesBackground(
    modifier: Modifier = Modifier,
    enableParallax: Boolean = true,
    speedMultiplier: Float = 1f
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val parallaxState = rememberParallaxState(
        enableParallax = enableParallax, sensitivity = 0.3f,
        context = context, coroutineScope = coroutineScope
    )
    val time = rememberAnimatedTime(speedMultiplier)

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = time.value
        val tiltX = parallaxState.tiltX.value
        val tiltY = parallaxState.tiltY.value
        val twoPi = 2f * PI.toFloat()

        val circles = listOf(
            CircleData(0.20f + 0.05f * sin(t * twoPi / 8000f), 0.225f + 0.025f * sin(t * twoPi / 7000f), 400f, primaryColor, 0.05f, 0.8f),
            CircleData(0.85f + 0.03f * sin(t * twoPi / 9000f), 0.185f + 0.035f * sin(t * twoPi / 6500f), 280f, tertiaryColor, 0.035f, 0.6f),
            CircleData(0.715f + 0.035f * sin(t * twoPi / 7500f), 0.44f + 0.04f * sin(t * twoPi / 8500f), 200f, tertiaryColor, 0.04f, 0.4f),
            CircleData(0.815f + 0.035f * sin(t * twoPi / 9500f), 0.785f + 0.035f * sin(t * twoPi / 7200f), 320f, secondaryColor, 0.035f, 0.7f),
            CircleData(0.24f + 0.04f * sin(t * twoPi / 8200f), 0.765f + 0.035f * sin(t * twoPi / 6800f), 180f, primaryColor, 0.04f, 0.5f),
            CircleData(0.525f + 0.025f * sin(t * twoPi / 8800f), 0.895f + 0.025f * sin(t * twoPi / 7800f), 220f, secondaryColor, 0.04f, 0.6f),
        )

        circles.forEach { circle ->
            val parallaxStrength = circle.depth * 50f
            val center = Offset(
                size.width * circle.x + tiltX * parallaxStrength,
                size.height * circle.y + tiltY * parallaxStrength
            )
            drawCircle(
                color = circle.color.copy(alpha = circle.alpha),
                radius = circle.radius,
                center = center
            )
        }
    }
}

private data class CircleData(
    val x: Float, val y: Float, val radius: Float,
    val color: androidx.compose.ui.graphics.Color, val alpha: Float, val depth: Float
)
