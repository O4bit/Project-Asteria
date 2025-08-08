package space.o4bit.projectasteria.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController

// Define color schemes
private val DarkColorScheme = darkColorScheme(
    primary = CosmicAccent,
    secondary = StardustSilver,
    tertiary = NebulaViolet,
    background = SpaceBlack,
    surface = DeepSpace,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = StardustSilver,
    onSurface = StardustSilver
)

private val LightColorScheme = lightColorScheme(
    primary = CosmicAccent,
    secondary = StardustSilver,
    tertiary = NebulaViolet,
    background = Color.White,
    surface = Color(0xFFF8F9FA),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = SpaceBlack,
    onSurface = SpaceBlack
)

/**
 * Simplified theme implementation that only uses dynamic Material You theming
 * with a light/dark toggle.
 */
@Composable
fun ProjectAsteriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Use dynamic color scheme if available (Android 12+)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // For devices below Android 12, use static color schemes
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Apply edge-to-edge design with the system UI controller
    val systemUiController = rememberSystemUiController()
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Set system bars to be transparent for edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, false)

            systemUiController.setSystemBarsColor(
                color = Color.Transparent,
                darkIcons = !darkTheme
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
