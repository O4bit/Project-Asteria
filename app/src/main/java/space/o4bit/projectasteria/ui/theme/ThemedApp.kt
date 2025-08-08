package space.o4bit.projectasteria.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import space.o4bit.projectasteria.data.preferences.ThemePreferencesRepository

/**
 * A composable wrapper that applies the user's theme preference to the app.
 * Simplified to only handle light/dark mode toggle with system default option.
 */
@Composable
fun ThemedApp(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val themePreferences = ThemePreferencesRepository(context)

    // Get user preferences for theme
    val followSystem by themePreferences.followSystem.collectAsState(initial = true)
    val isDarkMode by themePreferences.isDarkMode.collectAsState(initial = false)

    // Determine if we should use dark theme
    val darkTheme = if (followSystem) {
        // Follow system default
        isSystemInDarkTheme()
    } else {
        // Use user preference
        isDarkMode
    }

    // Apply theme with dynamic Material You colors
    ProjectAsteriaTheme(darkTheme = darkTheme) {
        content()
    }
}
