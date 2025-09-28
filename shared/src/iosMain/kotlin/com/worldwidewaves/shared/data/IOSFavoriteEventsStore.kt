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

import com.worldwidewaves.shared.utils.Log
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.Foundation.NSUserDefaults

/**
 * iOS-native implementation of FavoriteEventsStore using NSUserDefaults for persistence.
 *
 * This implementation provides:
 * - Thread-safe operations with Mutex
 * - Proper error handling with DataStoreException
 * - Efficient NSUserDefaults-based storage
 * - Coroutine-based async operations
 * - Platform-native iOS persistence
 */
class IOSFavoriteEventsStore(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : FavoriteEventsStore {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val mutex = Mutex()

    private fun favoriteKey(eventId: String): String = "favorite_$eventId"

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun setFavoriteStatus(
        eventId: String,
        isFavorite: Boolean,
    ) = withContext(dispatcher) {
        mutex.withLock {
            try {
                val key = favoriteKey(eventId)
                userDefaults.setBool(isFavorite, key)
                userDefaults.synchronize()
                Log.d("IOSFavoriteEventsStore", "Successfully set favorite status for event $eventId: $isFavorite")
            } catch (e: Exception) {
                Log.e("IOSFavoriteEventsStore", "Failed to set favorite status for event $eventId", throwable = e)
                throw DataStoreException("Failed to update favorite status for event $eventId: ${e.message}", e)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun isFavorite(eventId: String): Boolean =
        withContext(dispatcher) {
            mutex.withLock {
                try {
                    val key = favoriteKey(eventId)
                    userDefaults.boolForKey(key)
                } catch (e: Exception) {
                    Log.e("IOSFavoriteEventsStore", "Error reading favorite status for event $eventId", throwable = e)
                    false
                }
            }
        }
}
