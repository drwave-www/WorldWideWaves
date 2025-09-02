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

import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.worldwidewaves.shared.events.utils.Log
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import okio.Path.Companion.toPath

// ----------------------------

/**
 * Exception thrown when DataStore operations fail.
 *
 * This exception provides clear error information for storage-related failures,
 * enabling proper error handling and fallback mechanisms in the application.
 */
class DataStoreException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

// ----------------------------

internal const val dataStoreFileName = "wwwaves.preferences_pb"

@VisibleForTesting
lateinit var dataStore: DataStore<Preferences>
private val lock = SynchronizedObject()

/**
 * Builds (or returns) the singleton [DataStore] instance used to persist **key-value
 * preferences** across the application.
 *
 * The function is **idempotent** – subsequent calls will simply return the
 * previously-created instance.  A platform-specific `producePath` lambda is
 * invoked the first time to obtain the absolute file path where the serialized
 * preferences will be stored.  Each invocation is logged to help diagnosing
 * unexpected multiple initialisations.
 *
 * **Error Handling**: If DataStore creation fails, logs the error and rethrows the exception.
 * Callers should implement appropriate fallback mechanisms for storage failures.
 *
 * @param producePath Lambda returning the absolute path where the DataStore
 *                    file must be created (e.g. `context.filesDir.absolutePath`)
 * @return The singleton [DataStore] of type `Preferences`.
 * @throws Exception if DataStore creation fails (e.g., filesystem issues, permissions)
 */
fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    synchronized(lock) {
        if (::dataStore.isInitialized) {
            Log.v("DataStore", "DataStore already initialized with path: ${producePath()}")
            return dataStore
        }

        try {
            val path = producePath()
            Log.i("DataStore", "Creating DataStore with path: $path")
            dataStore = PreferenceDataStoreFactory.createWithPath { path.toPath() }
            Log.d("DataStore", "DataStore created successfully")
            dataStore
        } catch (e: Exception) {
            Log.e("DataStore", "Failed to create DataStore", throwable = e)
            throw DataStoreException("DataStore creation failed: ${e.message}", e)
        }
    }

/** Returns the platform-specific absolute path used to store the preferences DataStore file. */
expect fun keyValueStorePath(): String
