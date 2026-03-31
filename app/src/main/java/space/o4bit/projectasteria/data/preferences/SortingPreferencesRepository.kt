package space.o4bit.projectasteria.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "sorting_prefs")

class SortingPreferencesRepository(private val context: Context) {
    companion object {
        val SORT_LAUNCHES_ASC = booleanPreferencesKey("sort_launches_asc")
        val SORT_ASTEROIDS_CLOSE = booleanPreferencesKey("sort_asteroids_close")
    }

    val isLaunchesAscending: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SORT_LAUNCHES_ASC] ?: true // true = Oldest first
    }

    val isAsteroidsClosest: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SORT_ASTEROIDS_CLOSE] ?: true // true = Closest first
    }

    suspend fun toggleLaunchSort() {
        context.dataStore.edit { preferences ->
            val current = preferences[SORT_LAUNCHES_ASC] ?: true
            preferences[SORT_LAUNCHES_ASC] = !current
        }
    }

    suspend fun toggleAsteroidSort() {
        context.dataStore.edit { preferences ->
            val current = preferences[SORT_ASTEROIDS_CLOSE] ?: true
            preferences[SORT_ASTEROIDS_CLOSE] = !current
        }
    }
}
