package space.o4bit.projectasteria.ui.components.backgrounds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun RingsBackground(
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

    val ringConfigs = remember {
        listOf(
            RingConfig(0.2f, 0.2f, 0.3f, 0.25f, 9000, 8000, listOf(140f, 190f, 240f), 0.8f),
            RingConfig(0.85f, 0.15f, 0.8f, 0.2f, 10000, 7500, listOf(130f, 180f), 0.6f),
            RingConfig(0.5f, 0.5f, 0.55f, 0.55f, 8500, 9500, listOf(110f, 160f, 210f), 0.5f),
            RingConfig(0.15f, 0.75f, 0.2f, 0.8f, 7000, 8000, listOf(150f, 200f), 0.7f),
            RingConfig(0.8f, 0.85f, 0.85f, 0.8f, 8800, 7600, listOf(120f, 170f, 220f), 0.6f),
            RingConfig(0.75f, 0.4f, 0.8f, 0.45f, 9200, 8400, listOf(135f, 185f), 0.4f)
        )
    }

    val time = rememberAnimatedTime(speedMultiplier)

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = time.value
        val tiltX = parallaxState.tiltX.value
        val tiltY = parallaxState.tiltY.value
        val twoPi = 2f * PI.toFloat()

        ringConfigs.forEachIndexed { index, config ->
            val halfX = (config.endX - config.startX) / 2f
            val halfY = (config.endY - config.startY) / 2f
            val cx = config.startX + halfX + halfX * sin(t * twoPi / config.durationX)
            val cy = config.startY + halfY + halfY * sin(t * twoPi / config.durationY)

            val parallaxStrength = config.depth * 50f
            val center = Offset(
                size.width * cx + tiltX * parallaxStrength,
                size.height * cy + tiltY * parallaxStrength
            )

            val baseColor = when (index % 3) {
                0 -> primaryColor; 1 -> secondaryColor; else -> tertiaryColor
            }

            config.radii.forEachIndexed { ringIndex, radius ->
                val alpha = when (ringIndex) { 0 -> 0.14f; 1 -> 0.10f; 2 -> 0.07f; else -> 0.06f }
                val strokeWidth = when (ringIndex) { 0 -> 6f; 1 -> 5f; else -> 4f }
                drawCircle(
                    color = baseColor.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}

private data class RingConfig(
    val startX: Float, val startY: Float, val endX: Float, val endY: Float,
    val durationX: Int, val durationY: Int, val radii: List<Float>, val depth: Float
)
