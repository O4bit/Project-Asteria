package space.o4bit.projectasteria.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.uiHintsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ui_hints_preferences")

class UiHintsPreferencesRepository(private val context: Context) {

    companion object {
        private val MAIN_TABS_HINT_SHOWN = booleanPreferencesKey("main_tabs_hint_shown")
        private val SETTINGS_TABS_HINT_SHOWN = booleanPreferencesKey("settings_tabs_hint_shown")
    }

    val mainTabsHintShown: Flow<Boolean> = context.uiHintsDataStore.data.map { preferences ->
        preferences[MAIN_TABS_HINT_SHOWN] ?: false
    }

    val settingsTabsHintShown: Flow<Boolean> = context.uiHintsDataStore.data.map { preferences ->
        preferences[SETTINGS_TABS_HINT_SHOWN] ?: false
    }

    suspend fun setMainTabsHintShown(shown: Boolean) {
        context.uiHintsDataStore.edit { preferences ->
            preferences[MAIN_TABS_HINT_SHOWN] = shown
        }
    }

    suspend fun setSettingsTabsHintShown(shown: Boolean) {
        context.uiHintsDataStore.edit { preferences ->
            preferences[SETTINGS_TABS_HINT_SHOWN] = shown
        }
    }
}

