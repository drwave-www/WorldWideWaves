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

internal const val DATA_STORE_FILE_NAME = "wwwaves.preferences_pb"

/**
 * DataStore factory interface that provides testable DataStore creation.
 * This replaces the global singleton pattern to improve testability.
 */
interface DataStoreFactory {
    fun create(producePath: () -> String): DataStore<Preferences>
}

/**
 * Production implementation of DataStoreFactory that creates actual DataStore instances.
 */
class DefaultDataStoreFactory : DataStoreFactory {
    private var dataStore: DataStore<Preferences>? = null
    private val lock = SynchronizedObject()

    override fun create(producePath: () -> String): DataStore<Preferences> =
        synchronized(lock) {
            dataStore?.let { existing ->
                Log.v("DataStore", "DataStore already initialized with path: ${producePath()}")
                return existing
            }

            try {
                val path = producePath()
                Log.i("DataStore", "Creating DataStore with path: $path")
                val newDataStore = PreferenceDataStoreFactory.createWithPath { path.toPath() }
                Log.d("DataStore", "DataStore created successfully")
                dataStore = newDataStore
                newDataStore
            } catch (e: Exception) {
                Log.e("DataStore", "Failed to create DataStore", throwable = e)
                throw DataStoreException("DataStore creation failed: ${e.message}", e)
            }
        }
}

/**
 * Test implementation of DataStoreFactory that creates in-memory DataStore instances.
 * Each test gets a clean, isolated DataStore instance.
 */
@VisibleForTesting
class TestDataStoreFactory : DataStoreFactory {
    override fun create(producePath: () -> String): DataStore<Preferences> {
        try {
            val testPath = "/tmp/test_datastore_${kotlin.random.Random.nextInt()}_${kotlin.random.Random.nextInt()}.preferences_pb"
            Log.v("DataStore", "Creating test DataStore for path: ${producePath()}, actual test path: $testPath")
            return PreferenceDataStoreFactory.createWithPath { testPath.toPath() }
        } catch (e: Exception) {
            Log.e("DataStore", "Failed to create test DataStore for path: ${producePath()}", throwable = e)
            throw DataStoreException("Test DataStore creation failed: ${e.message}", e)
        }
    }
}


/**
 * Bridge function for backward compatibility during migration to DataStoreFactory.
 * Creates a TestDataStoreFactory for testing purposes.
 *
 * @deprecated Tests should use testDatastoreModule with TestDataStoreFactory instead
 */
@VisibleForTesting
@Deprecated("Tests should use testDatastoreModule with TestDataStoreFactory instead", ReplaceWith("TestDataStoreFactory().create(producePath)"))
fun createDataStore(producePath: () -> String): DataStore<Preferences> {
    return TestDataStoreFactory().create(producePath)
}

/** Returns the platform-specific absolute path used to store the preferences DataStore file. */
expect fun keyValueStorePath(): String
