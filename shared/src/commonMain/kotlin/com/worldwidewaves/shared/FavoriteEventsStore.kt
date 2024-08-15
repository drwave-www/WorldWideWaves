package com.worldwidewaves.shared

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.worldwidewaves.shared.events.WWWEvent
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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

class FavoriteEventsStore(
    private val dataStore: DataStore<Preferences>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private fun favoriteKey(eventId: String): Preferences.Key<Boolean> =
        booleanPreferencesKey("favorite_$eventId")

    suspend fun setFavoriteStatus(eventId: String, isFavorite: Boolean) = withContext(dispatcher) {
        val key = favoriteKey(eventId)
        dataStore.edit { preferences ->
            preferences[key] = isFavorite
        }
    }

    suspend fun isFavorite(eventId: String): Boolean = withContext(dispatcher) {
        val key = favoriteKey(eventId)
        dataStore.data
            .catch { exception ->
                Napier.e("Error reading favoris", exception)
                emit(emptyPreferences())
            }
            .map { preferences ->
                preferences[key] ?: false
            }.firstOrNull() ?: false
    }
}

// ----------------------------

class InitFavoriteEvent(private val favoriteEventsStore: FavoriteEventsStore) {
    suspend fun call(event: WWWEvent) {
        event.favorite = favoriteEventsStore.isFavorite(event.id)
    }
}

class SetEventFavorite(private val favoriteEventsStore: FavoriteEventsStore) {
    suspend fun call(event: WWWEvent, isFavorite: Boolean) {
        event.favorite = isFavorite
        favoriteEventsStore.setFavoriteStatus(event.id, event.favorite)
    }
}
