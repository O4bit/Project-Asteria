package space.o4bit.projectasteria.ui.components.backgrounds

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberAnimatedTime(speedMultiplier: Float): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    val targetSpeed = remember { mutableFloatStateOf(speedMultiplier) }
    SideEffect { targetSpeed.floatValue = speedMultiplier }

    LaunchedEffect(Unit) {
        var lastFrameMs = withInfiniteAnimationFrameMillis { it }
        var currentSpeed = targetSpeed.floatValue
        while (true) {
            withInfiniteAnimationFrameMillis { frameMs ->
                val delta = (frameMs - lastFrameMs).coerceIn(0L, 64L).toFloat()
                lastFrameMs = frameMs
                currentSpeed += (targetSpeed.floatValue - currentSpeed) * (delta / 1000f) * 2.5f
                time.floatValue += delta * currentSpeed
            }
        }
    }
    return time
}

data class ParallaxState(
    val tiltX: State<Float>,
    val tiltY: State<Float>
)

@Composable
fun rememberParallaxState(
    enableParallax: Boolean,
    sensitivity: Float = 0.15f,
    context: Context,
    coroutineScope: CoroutineScope
): ParallaxState {
    val smoothTiltX = remember { Animatable(0f) }
    val smoothTiltY = remember { Animatable(0f) }

    var baselineX by remember { mutableFloatStateOf(0f) }
    var baselineY by remember { mutableFloatStateOf(0f) }
    var isCalibrated by remember { mutableStateOf(false) }

    LaunchedEffect(enableParallax) {
        if (!enableParallax) {
            smoothTiltX.snapTo(0f)
            smoothTiltY.snapTo(0f)
            isCalibrated = false
            baselineX = 0f
            baselineY = 0f
        } else {
            isCalibrated = false
            baselineX = 0f
            baselineY = 0f
        }
    }

    DisposableEffect(enableParallax) {
        if (!enableParallax) {
            return@DisposableEffect onDispose { }
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            return@DisposableEffect onDispose { }
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!isCalibrated) {
                    baselineX = event.values[0]
                    baselineY = event.values[1]
                    isCalibrated = true
                }

                val rawTiltX = event.values[0] - baselineX
                val rawTiltY = event.values[1] - baselineY

                val deadzone = 0.6f
                val finalTiltX = if (kotlin.math.abs(rawTiltX) < deadzone) 0f else rawTiltX - kotlin.math.sign(rawTiltX) * deadzone
                val finalTiltY = if (kotlin.math.abs(rawTiltY) < deadzone) 0f else rawTiltY - kotlin.math.sign(rawTiltY) * deadzone

                coroutineScope.launch {
                    smoothTiltX.animateTo(
                        targetValue = finalTiltX * (sensitivity * 0.2f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                coroutineScope.launch {
                    smoothTiltY.animateTo(
                        targetValue = finalTiltY * (sensitivity * 0.2f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return ParallaxState(
        tiltX = smoothTiltX.asState(),
        tiltY = smoothTiltY.asState()
    )
}
