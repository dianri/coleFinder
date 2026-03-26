package es.colefinder.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val HAS_DISCOVERED_LONG_PRESS = booleanPreferencesKey("has_discovered_long_press")
        val LONG_PRESS_HINT_COUNT = intPreferencesKey("long_press_hint_count")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val hasDiscoveredLongPress = preferences[PreferencesKeys.HAS_DISCOVERED_LONG_PRESS] ?: false
            val longPressHintCount = preferences[PreferencesKeys.LONG_PRESS_HINT_COUNT] ?: 0
            UserPreferences(hasDiscoveredLongPress, longPressHintCount)
        }

    suspend fun updateHasDiscoveredLongPress(hasDiscovered: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_DISCOVERED_LONG_PRESS] = hasDiscovered
        }
    }

    suspend fun incrementHintCount() {
        dataStore.edit { preferences ->
            val currentCount = preferences[PreferencesKeys.LONG_PRESS_HINT_COUNT] ?: 0
            preferences[PreferencesKeys.LONG_PRESS_HINT_COUNT] = currentCount + 1
        }
    }
}

data class UserPreferences(
    val hasDiscoveredLongPress: Boolean,
    val longPressHintCount: Int
)
