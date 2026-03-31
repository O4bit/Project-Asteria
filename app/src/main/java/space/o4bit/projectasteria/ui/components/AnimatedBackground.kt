package space.o4bit.projectasteria.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import space.o4bit.projectasteria.data.preferences.BackgroundPreferencesRepository
import space.o4bit.projectasteria.ui.components.backgrounds.CirclesBackground
import space.o4bit.projectasteria.ui.components.backgrounds.GridBackground
import space.o4bit.projectasteria.ui.components.backgrounds.MeshBackground
import space.o4bit.projectasteria.ui.components.backgrounds.ParticlesBackground
import space.o4bit.projectasteria.ui.components.backgrounds.RingsBackground
import space.o4bit.projectasteria.ui.components.backgrounds.ShapesBackground

/**
 * Coordinator composable that dispatches to the selected background animation type.
 * Wraps content with the chosen animated background.
 */
@Composable
fun AnimatedBackground(
    type: BackgroundType = BackgroundType.DEFAULT,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val bgPrefs = BackgroundPreferencesRepository(context)
    val enableParallax by bgPrefs.enableParallax.collectAsState(initial = true)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
        color = MaterialTheme.colorScheme.background
    ) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
            // Render the background layer
            when (type) {
            BackgroundType.STARRY -> StarryBackground(
                modifier = Modifier.fillMaxSize()
            ) {}
            BackgroundType.SPACE -> SpaceBackground(
                modifier = Modifier.fillMaxSize()
            ) {}
            BackgroundType.CIRCLES -> CirclesBackground(
                modifier = Modifier.fillMaxSize(),
                enableParallax = enableParallax
            )
            BackgroundType.RINGS -> RingsBackground(
                modifier = Modifier.fillMaxSize(),
                enableParallax = enableParallax
            )
            BackgroundType.MESH -> MeshBackground(
                modifier = Modifier.fillMaxSize(),
                enableParallax = enableParallax
            )
            BackgroundType.GRID -> GridBackground(
                modifier = Modifier.fillMaxSize(),
                enableParallax = enableParallax
            )
            BackgroundType.PARTICLES -> ParticlesBackground(
                modifier = Modifier.fillMaxSize(),
                enableParallax = enableParallax
            )
            BackgroundType.SHAPES -> ShapesBackground(
                modifier = Modifier.fillMaxSize(),
                enableParallax = enableParallax
            )
            BackgroundType.NONE -> { /* No background */ }
        }

            // Render content on top
            content()
        }
    }
}
