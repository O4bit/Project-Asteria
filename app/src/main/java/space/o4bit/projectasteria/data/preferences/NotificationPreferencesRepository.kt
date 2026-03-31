package space.o4bit.projectasteria.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
        private val NOTIFICATION_HOUR_KEY = intPreferencesKey("notification_hour")
        private val NOTIFICATION_MINUTE_KEY = intPreferencesKey("notification_minute")
        private val WIFI_ONLY_PREFETCH_KEY = booleanPreferencesKey("wifi_only_prefetch")
        
        // Default is notifications enabled, 9:00 AM, false for wifi only
        const val DEFAULT_NOTIFICATIONS_ENABLED = true
        const val DEFAULT_NOTIFICATION_HOUR = 9
        const val DEFAULT_NOTIFICATION_MINUTE = 0
        const val DEFAULT_WIFI_ONLY = false
    }

    /**
     * Flow to observe whether daily notifications are enabled
     */
    val dailyNotificationsEnabled: Flow<Boolean> = context.notificationDataStore.data.map { preferences ->
        preferences[DAILY_NOTIFICATIONS_ENABLED_KEY] ?: DEFAULT_NOTIFICATIONS_ENABLED
    }
    
    val notificationHour: Flow<Int> = context.notificationDataStore.data.map { preferences ->
        preferences[NOTIFICATION_HOUR_KEY] ?: DEFAULT_NOTIFICATION_HOUR
    }
    
    val notificationMinute: Flow<Int> = context.notificationDataStore.data.map { preferences ->
        preferences[NOTIFICATION_MINUTE_KEY] ?: DEFAULT_NOTIFICATION_MINUTE
    }
    
    val wifiOnlyPrefetch: Flow<Boolean> = context.notificationDataStore.data.map { preferences ->
        preferences[WIFI_ONLY_PREFETCH_KEY] ?: DEFAULT_WIFI_ONLY
    }

    /**
     * Update daily notifications preference
     */
    suspend fun updateDailyNotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[DAILY_NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun updateNotificationTime(hour: Int, minute: Int) {
        context.notificationDataStore.edit { preferences ->
            preferences[NOTIFICATION_HOUR_KEY] = hour
            preferences[NOTIFICATION_MINUTE_KEY] = minute
        }
    }
    
    suspend fun updateWifiOnlyPrefetch(wifiOnly: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[WIFI_ONLY_PREFETCH_KEY] = wifiOnly
        }
    }
}
