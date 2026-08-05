package space.o4bit.projectasteria.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.backgroundDataStore: DataStore<Preferences> by preferencesDataStore(name = "background_preferences")

class BackgroundPreferencesRepository(private val context: Context) {

    companion object {
        private val BACKGROUND_TYPE_KEY = stringPreferencesKey("background_type")
        private val ENABLE_PARALLAX_KEY = booleanPreferencesKey("enable_parallax")
        private val HYPERDRIVE_THRESHOLD_MINUTES_KEY = stringPreferencesKey("hyperdrive_threshold_minutes")
        private val ISS_WIDGET_UPDATE_INTERVAL_HOURS_KEY = intPreferencesKey("iss_widget_update_interval_hours")
        private val PINNED_LAUNCH_IDS_KEY = stringSetPreferencesKey("pinned_launch_ids")
        private val AUTO_REMOVE_PINNED_LAUNCHES_KEY = booleanPreferencesKey("auto_remove_pinned_launches")
        private val NEVER_ASK_REMOVE_PINNED_KEY = booleanPreferencesKey("never_ask_remove_pinned")

        const val DEFAULT_BACKGROUND_TYPE = "SPACE"
        const val DEFAULT_ENABLE_PARALLAX = true
        const val DEFAULT_HYPERDRIVE_THRESHOLD_MINUTES = 1
        const val DEFAULT_ISS_WIDGET_UPDATE_INTERVAL_HOURS = 1
    }

    val backgroundType: Flow<String> = context.backgroundDataStore.data.map { preferences ->
        preferences[BACKGROUND_TYPE_KEY] ?: DEFAULT_BACKGROUND_TYPE
    }

    val enableParallax: Flow<Boolean> = context.backgroundDataStore.data.map { preferences ->
        preferences[ENABLE_PARALLAX_KEY] ?: DEFAULT_ENABLE_PARALLAX
    }

    val hyperdriveThresholdMinutes: Flow<Int> = context.backgroundDataStore.data.map { preferences ->
        preferences[HYPERDRIVE_THRESHOLD_MINUTES_KEY]?.toIntOrNull() ?: DEFAULT_HYPERDRIVE_THRESHOLD_MINUTES
    }

    val issWidgetUpdateIntervalHours: Flow<Int> = context.backgroundDataStore.data.map { preferences ->
        preferences[ISS_WIDGET_UPDATE_INTERVAL_HOURS_KEY] ?: DEFAULT_ISS_WIDGET_UPDATE_INTERVAL_HOURS
    }

    val pinnedLaunchIds: Flow<Set<String>> = context.backgroundDataStore.data.map { preferences ->
        preferences[PINNED_LAUNCH_IDS_KEY] ?: emptySet()
    }

    val autoRemovePinnedLaunches: Flow<Boolean> = context.backgroundDataStore.data.map { preferences ->
        preferences[AUTO_REMOVE_PINNED_LAUNCHES_KEY] ?: false
    }

    val neverAskRemovePinned: Flow<Boolean> = context.backgroundDataStore.data.map { preferences ->
        preferences[NEVER_ASK_REMOVE_PINNED_KEY] ?: false
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

    suspend fun updateHyperdriveThresholdMinutes(minutes: Int) {
        context.backgroundDataStore.edit { preferences ->
            preferences[HYPERDRIVE_THRESHOLD_MINUTES_KEY] = minutes.toString()
        }
    }

    suspend fun updateIssWidgetUpdateIntervalHours(hours: Int) {
        context.backgroundDataStore.edit { preferences ->
            preferences[ISS_WIDGET_UPDATE_INTERVAL_HOURS_KEY] = hours
        }
    }

    suspend fun togglePinLaunch(launchId: String) {
        context.backgroundDataStore.edit { preferences ->
            val current = preferences[PINNED_LAUNCH_IDS_KEY] ?: emptySet()
            val updated = if (current.contains(launchId)) current - launchId else current + launchId
            preferences[PINNED_LAUNCH_IDS_KEY] = updated
        }
    }

    suspend fun removePinnedLaunch(launchId: String) {
        context.backgroundDataStore.edit { preferences ->
            val current = preferences[PINNED_LAUNCH_IDS_KEY] ?: emptySet()
            preferences[PINNED_LAUNCH_IDS_KEY] = current - launchId
        }
    }

    suspend fun updateAutoRemovePinnedLaunches(autoRemove: Boolean) {
        context.backgroundDataStore.edit { preferences ->
            preferences[AUTO_REMOVE_PINNED_LAUNCHES_KEY] = autoRemove
        }
    }

    suspend fun updateNeverAskRemovePinned(neverAsk: Boolean) {
        context.backgroundDataStore.edit { preferences ->
            preferences[NEVER_ASK_REMOVE_PINNED_KEY] = neverAsk
        }
    }
}
