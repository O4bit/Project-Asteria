package space.o4bit.projectasteria.ui.components.backgrounds

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun ParticlesBackground(
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
        enableParallax = enableParallax, sensitivity = 0.2f,
        context = context, coroutineScope = coroutineScope
    )

    // Reduced from 65 → 40 particles; still visually dense but ~63% fewer connection checks
    val particles = remember {
        mutableStateListOf<Particle>().apply {
            repeat(40) {
                val radius = when (Random.nextFloat()) {
                    in 0f..0.6f -> 2.5f + Random.nextFloat() * 3f
                    in 0.6f..0.9f -> 4f + Random.nextFloat() * 4f
                    else -> 7f + Random.nextFloat() * 5f
                }
                add(Particle(
                    x = Random.nextFloat(), y = Random.nextFloat(),
                    vx = (Random.nextFloat() - 0.5f) * 0.00018f,
                    vy = (Random.nextFloat() - 0.5f) * 0.00018f,
                    radius = radius, colorIndex = it % 3
                ))
            }
        }
    }

    val targetSpeedState = remember { mutableFloatStateOf(speedMultiplier) }
    SideEffect { targetSpeedState.floatValue = speedMultiplier }

    LaunchedEffect(Unit) {
        var lastFrameMs = withInfiniteAnimationFrameMillis { it }
        var currentSpeed = targetSpeedState.floatValue
        while (true) {
            withInfiniteAnimationFrameMillis { frameMs ->
                val delta = (frameMs - lastFrameMs).coerceIn(0L, 64L).toFloat()
                lastFrameMs = frameMs
                currentSpeed += (targetSpeedState.floatValue - currentSpeed) * (delta / 1000f) * 2.5f
                val speedScale = currentSpeed * (delta / 16.67f)

                particles.forEachIndexed { index, p ->
                    var nx = p.x + p.vx * speedScale
                    var ny = p.y + p.vy * speedScale
                    var nvx = p.vx; var nvy = p.vy

                    if (nx < 0.02f) { nvx = abs(nvx); nx = 0.02f }
                    if (nx > 0.98f) { nvx = -abs(nvx); nx = 0.98f }
                    if (ny < 0.02f) { nvy = abs(nvy); ny = 0.02f }
                    if (ny > 0.98f) { nvy = -abs(nvy); ny = 0.98f }

                    nvx += (Random.nextFloat() - 0.5f) * 0.000004f
                    nvy += (Random.nextFloat() - 0.5f) * 0.000004f

                    val speed = sqrt(nvx * nvx + nvy * nvy)
                    val maxSpeed = 0.00025f
                    if (speed > maxSpeed) { nvx = nvx / speed * maxSpeed; nvy = nvy / speed * maxSpeed }

                    particles[index] = p.copy(x = nx, y = ny, vx = nvx, vy = nvy)
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val tiltX = parallaxState.tiltX.value
        val tiltY = parallaxState.tiltY.value
        val parallaxStrength = 25f

        // Pre-compute pixel positions once per frame
        val positions = Array(particles.size) { i ->
            val p = particles[i]
            Offset(
                p.x * size.width + tiltX * parallaxStrength,
                p.y * size.height + tiltY * parallaxStrength
            )
        }

        // Optimized connection: use reduced connect distance (18% vs 22%) to significantly
        // cut the number of pairs that pass the distance test — fewer drawLine calls
        val connectDist = size.width * 0.18f
        val connectDistSq = connectDist * connectDist

        for (i in particles.indices) {
            for (j in i + 1 until particles.size) {
                val dx = positions[i].x - positions[j].x
                // Early-exit on X before computing Y and doing sqrt — much cheaper
                if (dx * dx > connectDistSq) continue
                val dy = positions[i].y - positions[j].y
                val distSq = dx * dx + dy * dy
                if (distSq < connectDistSq) {
                    val proximity = 1f - sqrt(distSq) / connectDist
                    val alpha = proximity * proximity * 0.10f
                    val color = when ((particles[i].colorIndex + particles[j].colorIndex) % 3) {
                        0 -> primaryColor; 1 -> secondaryColor; else -> tertiaryColor
                    }
                    drawLine(
                        color = color.copy(alpha = alpha.coerceIn(0f, 0.18f)),
                        start = positions[i], end = positions[j], strokeWidth = 1.2f
                    )
                }
            }
        }

        particles.forEachIndexed { index, p ->
            val pos = positions[index]
            val color = when (p.colorIndex) { 0 -> primaryColor; 1 -> secondaryColor; else -> tertiaryColor }
            drawCircle(color = color.copy(alpha = 0.11f), radius = p.radius * 2f, center = pos)
            drawCircle(color = color.copy(alpha = 0.55f), radius = p.radius, center = pos)
        }
    }
}

private data class Particle(
    val x: Float, val y: Float, val vx: Float, val vy: Float,
    val radius: Float, val colorIndex: Int
)
