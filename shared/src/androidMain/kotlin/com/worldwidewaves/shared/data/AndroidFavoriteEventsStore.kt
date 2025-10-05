package com.worldwidewaves.shared.data

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
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

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.worldwidewaves.shared.WWWGlobals.FileSystem
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

/**
 * Retrieves the file path for the key-value store.
 *
 * This function constructs the file path for the key-value store by accessing the application's
 * files directory and appending the specified folder and file name for the data store.
 *
 */
actual fun keyValueStorePath(): String {
    val context: Context by inject(Context::class.java)
    return context
        .filesDir
        .resolve("${FileSystem.DATASTORE_FOLDER}/$DATA_STORE_FILE_NAME")
        .absolutePath
}

class AndroidFavoriteEventsStore(
    private val dataStore: DataStore<Preferences>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FavoriteEventsStore {
    private fun favoriteKey(eventId: String): Preferences.Key<Boolean> = booleanPreferencesKey("favorite_$eventId")

    override suspend fun setFavoriteStatus(
        eventId: String,
        isFavorite: Boolean,
    ) = withContext(dispatcher) {
        try {
            dataStore.edit { it[favoriteKey(eventId)] = isFavorite }
            Log.d("FavoriteEventsStore", "Successfully set favorite status for event $eventId: $isFavorite")
        } catch (e: Exception) {
            Log.e("FavoriteEventsStore", "Failed to set favorite status for event $eventId", throwable = e)
            throw DataStoreException("Failed to update favorite status for event $eventId: ${e.message}", e)
        }
    }

    override suspend fun isFavorite(eventId: String): Boolean =
        withContext(dispatcher) {
            dataStore.data
                .catch {
                    Log.e(::isFavorite.name, "Error reading favorites", throwable = it)
                    emit(emptyPreferences())
                }.map { it[favoriteKey(eventId)] ?: false }
                .firstOrNull() ?: false
        }
}
