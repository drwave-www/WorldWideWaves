package com.worldwidewaves.shared

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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath

// ----------------------------

private lateinit var dataStore: DataStore<Preferences>
private val lock = SynchronizedObject()

fun createDataStore(): DataStore<Preferences> = synchronized(lock) {
    if (::dataStore.isInitialized) {
        dataStore
    } else {
        PreferenceDataStoreFactory
            .createWithPath(produceFile = { keyValueStorePath().toPath() })
            .also { createdDataStore -> dataStore = createdDataStore }
    }
}

expect fun keyValueStorePath(): String

internal const val dataStoreFileName = "wwwaves.preferences_pb"

// ----------------------------

class FavoriteEventsStore(
    private val dataStore: DataStore<Preferences>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private fun favoriteKey(eventId: String): Preferences.Key<Boolean> = booleanPreferencesKey("favorite_$eventId")

    suspend fun setFavoriteStatus(eventId: String, isFavorite: Boolean) = withContext(dispatcher) {
        val key = favoriteKey(eventId)
        dataStore.edit { preferences ->
            preferences[key] = isFavorite
        }
    }

    suspend fun isFavorite(eventId: String): Boolean = withContext(dispatcher) {
        val key = favoriteKey(eventId)
        dataStore.data
            .catch {
                // TODO log the error
                emit(emptyPreferences())
            }
            .map { preferences ->
            preferences[key] ?: false
        }.firstOrNull()
            ?: false
    }
}