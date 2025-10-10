package space.o4bit.projectasteria.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to get DataStore instance for notifications
val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_preferences")

/**
 * Repository for managing notification preferences
 * Specifically for Daily Space Discoveries notifications
 */
class NotificationPreferencesRepository(private val context: Context) {

    companion object {
        private val DAILY_NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("daily_notifications_enabled")
        
        // Default is notifications enabled
        const val DEFAULT_NOTIFICATIONS_ENABLED = true
    }

    /**
     * Flow to observe whether daily notifications are enabled
     */
    val dailyNotificationsEnabled: Flow<Boolean> = context.notificationDataStore.data.map { preferences ->
        preferences[DAILY_NOTIFICATIONS_ENABLED_KEY] ?: DEFAULT_NOTIFICATIONS_ENABLED
    }

    /**
     * Update daily notifications preference
     */
    suspend fun updateDailyNotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[DAILY_NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }
}
