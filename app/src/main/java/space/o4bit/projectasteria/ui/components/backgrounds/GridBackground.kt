package space.o4bit.projectasteria.ui.components.backgrounds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun GridBackground(
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
        enableParallax = enableParallax, sensitivity = 0.15f,
        context = context, coroutineScope = coroutineScope
    )
    val time = rememberAnimatedTime(speedMultiplier)

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = time.value
        val tiltX = parallaxState.tiltX.value
        val tiltY = parallaxState.tiltY.value
        val twoPi = 2f * PI.toFloat()
        val cols = 11; val rows = 20
        val cellW = size.width / (cols - 1).toFloat()
        val cellH = size.height / (rows - 1).toFloat()
        val maxDist = sqrt(size.width * size.width + size.height * size.height) * 0.5f

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val baseX = col * cellW + tiltX * 12f
                val baseY = row * cellH + tiltY * 12f

                val dx = baseX - size.width * 0.5f
                val dy = baseY - size.height * 0.5f
                val dist = sqrt(dx * dx + dy * dy)

                val ripplePhase = dist * 0.012f
                val wave = sin(t * twoPi / 3800f - ripplePhase)
                val baseRadius = 5.0f + wave * 2.5f
                val finalRadius = baseRadius.coerceAtLeast(0.8f)

                val colorPhase = (col + row) % 3
                val color = when (colorPhase) { 0 -> primaryColor; 1 -> secondaryColor; else -> tertiaryColor }

                val edgeDim = (1f - dist / maxDist).coerceIn(0.55f, 1f)
                val baseAlpha = 0.30f + wave * 0.10f
                val finalAlpha = (baseAlpha * edgeDim).coerceIn(0f, 0.50f)

                drawCircle(
                    color = color.copy(alpha = finalAlpha),
                    radius = finalRadius,
                    center = Offset(baseX, baseY)
                )
            }
        }
    }
}
