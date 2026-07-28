package space.o4bit.projectasteria.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import space.o4bit.projectasteria.util.toColorOrNull

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
    onSecondary = SpaceBlack,  // StardustSilver is light (luminance 0.67) — white-on-silver ratio was 1.45:1
    onTertiary = Color.White,  // NebulaViolet is dark (luminance 0.12) — white gives 6.32:1 ✓
    onBackground = SpaceBlack,
    onSurface = SpaceBlack
)

@Composable
fun ProjectAsteriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    pureBlack: Boolean = false,
    customAccentHex: String? = null,
    customThemeColorHex: String? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }.let {
        if (darkTheme && pureBlack) {
            // Override the entire container/surface family so M3 components
            // (cards, sheets, nav bar) don't float as dark-gray islands on true black.
            it.copy(
                background              = Color.Black,
                surface                 = Color.Black,
                surfaceDim              = Color.Black,
                surfaceContainerLowest  = Color.Black,
                surfaceContainerLow     = Color(0xFF0A0A0A),
                surfaceContainer        = Color(0xFF111111),
                surfaceContainerHigh    = Color(0xFF1A1A1A),
                surfaceContainerHighest = Color(0xFF1F1F1F),
                surfaceBright           = Color(0xFF2A2A2A)
            )
        } else it
    }

    val schemeWithAccent = if (!dynamicColor) {
        customAccentHex.toColorOrNull()?.let {
            applyCustomAccent(baseScheme, it, darkTheme)
        } ?: baseScheme
    } else baseScheme

    val finalScheme = if (!dynamicColor) {
        customThemeColorHex.toColorOrNull()?.let {
            applyCustomThemeColor(schemeWithAccent, it, darkTheme)
        } ?: schemeWithAccent
    } else schemeWithAccent

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as Activity
            // enableEdgeToEdge() in MainActivity already clears status/nav bar colors.
            // We only need to set the icon appearance for the current theme.
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = Typography,
        content = content
    )
}

private fun applyCustomAccent(
    colorScheme: ColorScheme,
    accent: Color,
    darkTheme: Boolean
): ColorScheme {
    val primary = accent
    val primaryContainer = accent.adjustLightness(if (darkTheme) 0.25f else -0.25f)
    val secondary = accent.adjustLightness(if (darkTheme) 0.15f else -0.15f)
    val secondaryContainer = accent.adjustLightness(if (darkTheme) 0.35f else -0.35f)
    val tertiary = accent.adjustLightness(if (darkTheme) -0.1f else 0.1f)
    val tertiaryContainer = accent.adjustLightness(if (darkTheme) 0.4f else -0.4f)
    return colorScheme.copy(
        primary = primary,
        onPrimary = primary.contrastingForeground(),
        primaryContainer = primaryContainer,
        onPrimaryContainer = primaryContainer.contrastingForeground(),
        secondary = secondary,
        onSecondary = secondary.contrastingForeground(),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = secondaryContainer.contrastingForeground(),
        tertiary = tertiary,
        onTertiary = tertiary.contrastingForeground(),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = tertiaryContainer.contrastingForeground(),
        surfaceTint = primary,
        inversePrimary = primary.adjustLightness(if (darkTheme) -0.4f else 0.4f)
    )
}

private fun applyCustomThemeColor(
    colorScheme: ColorScheme,
    themeColor: Color,
    darkTheme: Boolean
): ColorScheme {
    val background = if (darkTheme) themeColor.adjustLightness(0.05f) else themeColor.adjustLightness(0.55f)
    val surface = if (darkTheme) themeColor.adjustLightness(0.15f) else themeColor.adjustLightness(0.45f)
    val surfaceVariant = if (darkTheme) themeColor.adjustLightness(0.25f) else themeColor.adjustLightness(0.35f)
    val containerLowest = if (darkTheme) themeColor.adjustLightness(0.0f) else themeColor.adjustLightness(0.5f)
    val containerLow = if (darkTheme) themeColor.adjustLightness(0.08f) else themeColor.adjustLightness(0.48f)
    val container = if (darkTheme) themeColor.adjustLightness(0.18f) else themeColor.adjustLightness(0.42f)
    val containerHigh = if (darkTheme) themeColor.adjustLightness(0.26f) else themeColor.adjustLightness(0.34f)
    val containerHighest = if (darkTheme) themeColor.adjustLightness(0.32f) else themeColor.adjustLightness(0.28f)
    val surfaceBright = if (darkTheme) themeColor.adjustLightness(0.4f) else themeColor.adjustLightness(0.12f)
    val surfaceDim = if (darkTheme) themeColor.adjustLightness(-0.1f) else themeColor.adjustLightness(0.6f)

    val onBackground = background.contrastingForeground()
    val onSurface = surface.contrastingForeground()
    val onSurfaceVariant = surfaceVariant.contrastingForeground()

    return colorScheme.copy(
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = themeColor,
        surfaceContainerLowest = containerLowest,
        surfaceContainerLow = containerLow,
        surfaceContainer = container,
        surfaceContainerHigh = containerHigh,
        surfaceContainerHighest = containerHighest,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim
    )
}

private fun Color.adjustLightness(delta: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[2] = (hsl[2] + delta).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

/**
 * Picks white or black text to overlay this color, using the actual WCAG contrast
 * ratio rather than a naive luminance threshold. The 0.5 threshold fails for
 * mid-tone colors (e.g. medium gray gives ~3.9:1 with white vs. ~5.3:1 with black
 * but the old code always picked white).
 */
private fun Color.contrastingForeground(): Color {
    val lum = ColorUtils.calculateLuminance(this.toArgb())
    val contrastWithWhite = 1.05 / (lum + 0.05)
    val contrastWithBlack = (lum + 0.05) / 0.05
    return if (contrastWithWhite >= contrastWithBlack) Color.White else Color.Black
}
