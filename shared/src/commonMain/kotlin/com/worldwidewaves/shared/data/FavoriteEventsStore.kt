package com.worldwidewaves.shared.data

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
 *
 * Licensed under the Apache License, Version 2.0 (the "LiBooleancense");
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
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Small wrapper around Android/Multiplatform `DataStore` that persists the
 * “favorite” flag for each event.
 *
 * A simple boolean preference is stored under the key `"favorite_<eventId>"`.
 * The class offers:
 * • `setFavoriteStatus()` – suspend function to update the flag
 * • `isFavorite()`        – suspend function to read the current value
 *
 * All I/O happens on the supplied [dispatcher] (defaults to `Dispatchers.IO`).
 */
class FavoriteEventsStore(
    private val dataStore: DataStore<Preferences>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private fun favoriteKey(eventId: String): Preferences.Key<Boolean> = booleanPreferencesKey("favorite_$eventId")

    suspend fun setFavoriteStatus(
        eventId: String,
        isFavorite: Boolean,
    ) = withContext(dispatcher) {
        dataStore.edit { it[favoriteKey(eventId)] = isFavorite }
    }

    suspend fun isFavorite(eventId: String): Boolean =
        withContext(dispatcher) {
            dataStore.data
                .catch {
                    Log.e(::isFavorite.name, "Error reading favorites", throwable = it)
                    emit(emptyPreferences())
                }.map { it[favoriteKey(eventId)] ?: false }
                .firstOrNull() ?: false
        }
}

// ----------------------------

/**
 * Reads the persisted favorite state for an [IWWWEvent] and copies it into
 * `event.favorite`.  Called once after the event list has been instantiated so
 * UI layers start with the correct value.
 */
class InitFavoriteEvent(
    private val favoriteEventsStore: FavoriteEventsStore,
) {
    suspend fun call(event: IWWWEvent) {
        event.favorite = favoriteEventsStore.isFavorite(event.id)
    }
}

/**
 * Convenience wrapper to toggle the *favorite* flag for a given [IWWWEvent].
 * Persists the flag through [FavoriteEventsStore] **and** mirrors the change
 * back into `event.favorite` so callers do not have to mutate the model
 * themselves.
 */
class SetEventFavorite(
    private val favoriteEventsStore: FavoriteEventsStore,
) {
    suspend fun call(
        event: IWWWEvent,
        isFavorite: Boolean,
    ) = favoriteEventsStore
        .setFavoriteStatus(event.id, isFavorite)
        .also { event.favorite = isFavorite }
}
