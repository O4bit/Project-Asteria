package space.o4bit.projectasteria.ui.components.backgrounds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun MeshBackground(
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
        enableParallax = enableParallax, sensitivity = 0.4f,
        context = context, coroutineScope = coroutineScope
    )
    val meshNodes = remember { generateMeshGrid() }
    val time = rememberAnimatedTime(speedMultiplier)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val tiltX = parallaxState.tiltX.value
        val tiltY = parallaxState.tiltY.value
        val rows = 12; val cols = 12
        val cameraZ = 1.6f; val gridTilt = 10f

        val t = (time.value % 40000f).let { raw ->
            if (raw < 20000f) raw * PI.toFloat() / 20000f
            else (40000f - raw) * PI.toFloat() / 20000f
        }

        fun projectNode(node: MeshNode): Offset {
            val xFreq = 1.0f + node.baseX * 0.5f
            val yFreq = 1.2f + node.baseY * 0.6f
            val zFreq = 0.8f + (node.baseX + node.baseY) * 0.4f
            val xPhase = node.baseX * 2f * PI.toFloat()
            val yPhase = node.baseY * 3f * PI.toFloat()
            val zPhase = (node.baseX + node.baseY) * 1.5f * PI.toFloat()

            val x = node.baseX + node.offsetX * sin(t * xFreq + xPhase)
            val y = node.baseY + node.offsetY * cos(t * yFreq + yPhase)
            val z = node.zAmplitude * sin(t * zFreq + zPhase)

            val normalizedX = (x - 0.5f) * 3.8f
            val normalizedY = (y - 0.5f) * 2.8f
            val tiltRad = Math.toRadians(gridTilt.toDouble())
            val rotatedY = normalizedY * cos(tiltRad).toFloat() - z * sin(tiltRad).toFloat()
            val rotatedZ = normalizedY * sin(tiltRad).toFloat() + z * cos(tiltRad).toFloat()
            val parallaxStrength = node.baseDepth * 60f
            val perspective = cameraZ / (cameraZ - rotatedZ)

            return Offset(
                normalizedX * perspective * width * 0.48f + width * 0.5f + tiltX * parallaxStrength,
                rotatedY * perspective * height * 0.48f + height * 0.5f + tiltY * parallaxStrength
            )
        }

        meshNodes.forEachIndexed { index, node ->
            val row = index / cols; val col = index % cols
            if (row >= rows - 1 || col >= cols - 1) return@forEachIndexed

            val p1 = projectNode(meshNodes[index])
            val p2 = projectNode(meshNodes[index + 1])
            val p3 = projectNode(meshNodes[index + cols])
            val p4 = projectNode(meshNodes[index + cols + 1])

            val color = when ((row + col) % 3) { 0 -> primaryColor; 1 -> secondaryColor; else -> tertiaryColor }
            val alpha = 0.14f + node.baseDepth * 0.05f

            drawPath(
                Path().apply { moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close() },
                color.copy(alpha = alpha), style = Stroke(width = 3.5f)
            )
            drawPath(
                Path().apply { moveTo(p2.x, p2.y); lineTo(p4.x, p4.y); lineTo(p3.x, p3.y); close() },
                color.copy(alpha = alpha), style = Stroke(width = 3.5f)
            )
        }
    }
}

private fun generateMeshGrid(): List<MeshNode> {
    val rows = 12; val cols = 12
    val nodes = mutableListOf<MeshNode>()
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val baseX = col / (cols - 1f); val baseY = row / (rows - 1f)
            val offsetX = (Random.nextFloat() - 0.5f) * 0.04f
            val offsetY = (Random.nextFloat() - 0.5f) * 0.04f
            val zAmplitude = Random.nextFloat() * 0.15f
            val centerDistX = (baseX - 0.5f) * 2f; val centerDistY = (baseY - 0.5f) * 2f
            val baseDepth = sqrt(centerDistX * centerDistX + centerDistY * centerDistY) / sqrt(2f)
            nodes.add(MeshNode(baseX, baseY, offsetX, offsetY, baseDepth, zAmplitude))
        }
    }
    return nodes
}

private data class MeshNode(
    val baseX: Float, val baseY: Float, val offsetX: Float, val offsetY: Float,
    val baseDepth: Float, val zAmplitude: Float
)
