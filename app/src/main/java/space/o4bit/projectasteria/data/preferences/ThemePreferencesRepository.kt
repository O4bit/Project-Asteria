package space.o4bit.projectasteria.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemePreferencesRepository(private val context: Context) {

    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val FOLLOW_SYSTEM_KEY = booleanPreferencesKey("follow_system")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        private val PURE_BLACK_KEY = booleanPreferencesKey("pure_black")
        private val CUSTOM_ACCENT_KEY = stringPreferencesKey("custom_accent")
        private val CUSTOM_THEME_COLOR_KEY = stringPreferencesKey("custom_theme_color")
        const val DEFAULT_FOLLOW_SYSTEM = true
    }

    val isDarkMode: Flow<Boolean> = context.themeDataStore.data.map { it[DARK_MODE_KEY] ?: false }
    val followSystem: Flow<Boolean> = context.themeDataStore.data.map { it[FOLLOW_SYSTEM_KEY] ?: DEFAULT_FOLLOW_SYSTEM }
    val dynamicColor: Flow<Boolean> = context.themeDataStore.data.map { it[DYNAMIC_COLOR_KEY] ?: true }
    val pureBlack: Flow<Boolean> = context.themeDataStore.data.map { it[PURE_BLACK_KEY] ?: false }
    val customAccent: Flow<String?> = context.themeDataStore.data.map { it[CUSTOM_ACCENT_KEY] }
    val customThemeColor: Flow<String?> = context.themeDataStore.data.map { it[CUSTOM_THEME_COLOR_KEY] }

    suspend fun updateDarkMode(isDark: Boolean) {
        context.themeDataStore.edit { it[DARK_MODE_KEY] = isDark }
    }

    suspend fun updateFollowSystem(follow: Boolean) {
        context.themeDataStore.edit { it[FOLLOW_SYSTEM_KEY] = follow }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        context.themeDataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    }

    suspend fun updatePureBlack(enabled: Boolean) {
        context.themeDataStore.edit { it[PURE_BLACK_KEY] = enabled }
    }

    suspend fun updateCustomAccent(hexColor: String?) {
        context.themeDataStore.edit {
            if (hexColor != null) it[CUSTOM_ACCENT_KEY] = hexColor
            else it.remove(CUSTOM_ACCENT_KEY)
        }
    }

    suspend fun updateCustomThemeColor(hexColor: String?) {
        context.themeDataStore.edit {
            if (hexColor != null) it[CUSTOM_THEME_COLOR_KEY] = hexColor
            else it.remove(CUSTOM_THEME_COLOR_KEY)
        }
    }
}
