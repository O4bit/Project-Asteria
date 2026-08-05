package space.o4bit.projectasteria.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import space.o4bit.projectasteria.data.preferences.BackgroundPreferencesRepository
import space.o4bit.projectasteria.ui.components.backgrounds.CirclesBackground
import space.o4bit.projectasteria.ui.components.backgrounds.GridBackground
import space.o4bit.projectasteria.ui.components.backgrounds.MeshBackground
import space.o4bit.projectasteria.ui.components.backgrounds.ParticlesBackground
import space.o4bit.projectasteria.ui.components.backgrounds.RingsBackground
import space.o4bit.projectasteria.ui.components.backgrounds.ShapesBackground

@Composable
fun AnimatedBackground(
    modifier: Modifier = Modifier,
    type: BackgroundType = BackgroundType.DEFAULT,
    launchSpeedMultiplier: Float = 1f,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    // remember prevents re-creating the repository (and re-subscribing DataStore) on every recompose
    val bgPrefs = remember(context) { BackgroundPreferencesRepository(context) }
    // collectAsStateWithLifecycle pauses collection when the app is in the background
    val enableParallax by bgPrefs.enableParallax.collectAsStateWithLifecycle(initialValue = true)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (type) {
                BackgroundType.STARRY -> StarryBackground(
                    modifier = Modifier.fillMaxSize(),
                    enableParallax = enableParallax
                ) {}
                BackgroundType.SPACE -> SpaceBackground(
                    modifier = Modifier.fillMaxSize(),
                    enableParallax = enableParallax,
                    speedMultiplier = launchSpeedMultiplier
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
                BackgroundType.NONE -> {}
            }

            content()
        }
    }
}
