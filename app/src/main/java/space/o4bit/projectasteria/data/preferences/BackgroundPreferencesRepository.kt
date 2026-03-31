package space.o4bit.projectasteria.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.backgroundDataStore: DataStore<Preferences> by preferencesDataStore(name = "background_preferences")

class BackgroundPreferencesRepository(private val context: Context) {

    companion object {
        private val BACKGROUND_TYPE_KEY = stringPreferencesKey("background_type")
        private val ENABLE_PARALLAX_KEY = booleanPreferencesKey("enable_parallax")
        const val DEFAULT_BACKGROUND_TYPE = "STARRY"
        const val DEFAULT_ENABLE_PARALLAX = true
    }

    val backgroundType: Flow<String> = context.backgroundDataStore.data.map { preferences ->
        preferences[BACKGROUND_TYPE_KEY] ?: DEFAULT_BACKGROUND_TYPE
    }

    val enableParallax: Flow<Boolean> = context.backgroundDataStore.data.map { preferences ->
        preferences[ENABLE_PARALLAX_KEY] ?: DEFAULT_ENABLE_PARALLAX
    }

    suspend fun updateBackgroundType(type: String) {
        context.backgroundDataStore.edit { preferences ->
            preferences[BACKGROUND_TYPE_KEY] = type
        }
    }

    suspend fun updateEnableParallax(enabled: Boolean) {
        context.backgroundDataStore.edit { preferences ->
            preferences[ENABLE_PARALLAX_KEY] = enabled
        }
    }
}
