package space.o4bit.projectasteria.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to get DataStore instance
val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

// Theme preference repository - simplified to just handle dark mode toggle
class ThemePreferencesRepository(private val context: Context) {

    // Keys for preferences
    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val FOLLOW_SYSTEM_KEY = booleanPreferencesKey("follow_system")

        // Default is to follow system
        const val DEFAULT_FOLLOW_SYSTEM = true
    }

    // Get whether to use dark mode
    val isDarkMode: Flow<Boolean> = context.themeDataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }

    // Get whether to follow system setting
    val followSystem: Flow<Boolean> = context.themeDataStore.data.map { preferences ->
        preferences[FOLLOW_SYSTEM_KEY] ?: DEFAULT_FOLLOW_SYSTEM
    }

    // Update dark mode preference
    suspend fun updateDarkMode(isDark: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDark
        }
    }

    // Update follow system preference
    suspend fun updateFollowSystem(follow: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[FOLLOW_SYSTEM_KEY] = follow
        }
    }
}
