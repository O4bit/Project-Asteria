package space.o4bit.projectasteria.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.reminderDataStore by preferencesDataStore(name = "reminder_prefs")

class ReminderPreferencesRepository(private val context: Context) {
    companion object {
        val REMINDER_LAUNCH_IDS = stringSetPreferencesKey("reminder_launch_ids")
    }

    val remindedLaunchIds: Flow<Set<String>> = context.reminderDataStore.data.map { preferences ->
        preferences[REMINDER_LAUNCH_IDS] ?: emptySet()
    }

    suspend fun setReminder(launchId: String) {
        context.reminderDataStore.edit { preferences ->
            val current = preferences[REMINDER_LAUNCH_IDS] ?: emptySet()
            preferences[REMINDER_LAUNCH_IDS] = current + launchId
        }
    }

    suspend fun removeReminder(launchId: String) {
        context.reminderDataStore.edit { preferences ->
            val current = preferences[REMINDER_LAUNCH_IDS] ?: emptySet()
            preferences[REMINDER_LAUNCH_IDS] = current - launchId
        }
    }
}
