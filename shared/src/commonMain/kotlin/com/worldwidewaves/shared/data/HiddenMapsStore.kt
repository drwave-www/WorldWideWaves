package com.worldwidewaves.shared.data

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.worldwidewaves.shared.events.utils.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Persists a set of map IDs that have been marked as "hidden" (typically after uninstallation).
 * 
 * This store is used to keep track of maps that have been uninstalled but might still appear
 * in the installed modules list until the system completes the deferred uninstallation.
 * 
 * A string set preference is stored under the key "hidden_maps".
 * The class offers:
 * • `add(mapId)`        - suspend function to add a map ID to the hidden set
 * • `remove(mapId)`     - suspend function to remove a map ID from the hidden set
 * • `getAll()`          - suspend function to get all hidden map IDs
 * • `isHidden(mapId)`   - suspend function to check if a map ID is hidden
 *
 * All I/O happens on the supplied [dispatcher] (defaults to `Dispatchers.IO`).
 */
class HiddenMapsStore(
    private val dataStore: DataStore<Preferences>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private val HIDDEN_MAPS_KEY = stringSetPreferencesKey("hidden_maps")
    }

    /**
     * Adds a map ID to the hidden set.
     */
    suspend fun add(mapId: String) = withContext(dispatcher) {
        dataStore.edit { preferences ->
            val currentSet = preferences[HIDDEN_MAPS_KEY] ?: emptySet()
            preferences[HIDDEN_MAPS_KEY] = currentSet + mapId
        }
    }

    /**
     * Removes a map ID from the hidden set.
     */
    suspend fun remove(mapId: String) = withContext(dispatcher) {
        dataStore.edit { preferences ->
            val currentSet = preferences[HIDDEN_MAPS_KEY] ?: emptySet()
            if (mapId in currentSet) {
                preferences[HIDDEN_MAPS_KEY] = currentSet - mapId
            }
        }
    }

    /**
     * Gets all hidden map IDs.
     */
    suspend fun getAll(): Set<String> = withContext(dispatcher) {
        dataStore.data
            .catch {
                Log.e(::getAll.name, "Error reading hidden maps", throwable = it)
                emit(emptyPreferences())
            }
            .map { it[HIDDEN_MAPS_KEY] ?: emptySet() }
            .firstOrNull() ?: emptySet()
    }

    /**
     * Checks if a map ID is hidden.
     */
    suspend fun isHidden(mapId: String): Boolean = withContext(dispatcher) {
        dataStore.data
            .catch {
                Log.e(::isHidden.name, "Error reading hidden maps", throwable = it)
                emit(emptyPreferences())
            }
            .map { preferences -> 
                val hiddenMaps = preferences[HIDDEN_MAPS_KEY] ?: emptySet()
                mapId in hiddenMaps
            }
            .firstOrNull() ?: false
    }
}
