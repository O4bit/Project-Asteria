package space.o4bit.projectasteria.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import space.o4bit.projectasteria.data.preferences.ThemePreferencesRepository

/**
 * A composable wrapper that applies the user's full theme preferences.
 */
@Composable
fun ThemedApp(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val themePreferences = ThemePreferencesRepository(context)

    val followSystem by themePreferences.followSystem.collectAsState(initial = true)
    val isDarkMode by themePreferences.isDarkMode.collectAsState(initial = false)
    val dynamicColor by themePreferences.dynamicColor.collectAsState(initial = true)
    val pureBlack by themePreferences.pureBlack.collectAsState(initial = false)
    val customAccent by themePreferences.customAccent.collectAsState(initial = null)
    val customThemeColor by themePreferences.customThemeColor.collectAsState(initial = null)

    val darkTheme = if (followSystem) isSystemInDarkTheme() else isDarkMode

    ProjectAsteriaTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        pureBlack = pureBlack,
        customAccentHex = customAccent,
        customThemeColorHex = customThemeColor
    ) {
        content()
    }
}
