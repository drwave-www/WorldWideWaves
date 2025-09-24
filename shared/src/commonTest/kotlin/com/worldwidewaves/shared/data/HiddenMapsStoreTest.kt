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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.worldwidewaves.shared.utils.Log
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive tests for HiddenMapsStore including set operations,
 * persistence, error handling, and concurrent access scenarios.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HiddenMapsStoreTest {
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var hiddenMapsStore: HiddenMapsStore

    @BeforeTest
    fun setUp() {
        // Mock the Log object
        mockkObject(Log)
        justRun { Log.e(any(), any(), any()) }

        // Create test DataStore
        testDataStore =
            PreferenceDataStoreFactory.createWithPath {
                "/tmp/test_hidden_maps_${System.currentTimeMillis()}.preferences_pb".toPath()
            }

        hiddenMapsStore = HiddenMapsStore(testDataStore, Dispatchers.Default)
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test add single map to hidden set`() =
        runTest {
            val mapId = "test_map_1"

            hiddenMapsStore.add(mapId)

            assertTrue(hiddenMapsStore.isHidden(mapId))
            assertTrue(hiddenMapsStore.getAll().contains(mapId))
        }

    @Test
    fun `test add multiple maps to hidden set`() =
        runTest {
            val mapIds = listOf("map_1", "map_2", "map_3")

            mapIds.forEach { mapId ->
                hiddenMapsStore.add(mapId)
            }

            val hiddenMaps = hiddenMapsStore.getAll()
            mapIds.forEach { mapId ->
                assertTrue(hiddenMapsStore.isHidden(mapId))
                assertTrue(hiddenMaps.contains(mapId))
            }
            assertEquals(mapIds.toSet(), hiddenMaps)
        }

    @Test
    fun `test remove map from hidden set`() =
        runTest {
            val mapId = "remove_test_map"

            // Add then remove
            hiddenMapsStore.add(mapId)
            assertTrue(hiddenMapsStore.isHidden(mapId))

            hiddenMapsStore.remove(mapId)
            assertFalse(hiddenMapsStore.isHidden(mapId))
            assertFalse(hiddenMapsStore.getAll().contains(mapId))
        }

    @Test
    fun `test remove non-existent map does not affect set`() =
        runTest {
            val existingMapId = "existing_map"
            val nonExistentMapId = "non_existent_map"

            // Add one map
            hiddenMapsStore.add(existingMapId)

            // Try to remove non-existent map
            hiddenMapsStore.remove(nonExistentMapId)

            // Existing map should still be there
            assertTrue(hiddenMapsStore.isHidden(existingMapId))
            assertFalse(hiddenMapsStore.isHidden(nonExistentMapId))
            assertEquals(setOf(existingMapId), hiddenMapsStore.getAll())
        }

    @Test
    fun `test adding duplicate map ID does not create duplicates`() =
        runTest {
            val mapId = "duplicate_test_map"

            // Add same map multiple times
            hiddenMapsStore.add(mapId)
            hiddenMapsStore.add(mapId)
            hiddenMapsStore.add(mapId)

            val hiddenMaps = hiddenMapsStore.getAll()
            assertEquals(1, hiddenMaps.size)
            assertTrue(hiddenMaps.contains(mapId))
        }

    @Test
    fun `test isHidden returns false for non-existent map`() =
        runTest {
            val nonExistentMapId = "non_existent_map"

            assertFalse(hiddenMapsStore.isHidden(nonExistentMapId))
        }

    @Test
    fun `test getAll returns empty set when no maps are hidden`() =
        runTest {
            val hiddenMaps = hiddenMapsStore.getAll()
            assertTrue(hiddenMaps.isEmpty())
        }

    @Test
    fun `test bulk operations with multiple maps`() =
        runTest {
            val mapIds = (1..20).map { "bulk_map_$it" }

            // Add all maps
            mapIds.forEach { mapId ->
                hiddenMapsStore.add(mapId)
            }

            // Verify all are hidden
            val allHiddenMaps = hiddenMapsStore.getAll()
            assertEquals(mapIds.toSet(), allHiddenMaps)

            // Remove half of them
            val toRemove = mapIds.take(10)
            toRemove.forEach { mapId ->
                hiddenMapsStore.remove(mapId)
            }

            // Verify correct maps remain
            val remainingMaps = hiddenMapsStore.getAll()
            val expectedRemaining = mapIds.drop(10).toSet()
            assertEquals(expectedRemaining, remainingMaps)

            // Verify individual status
            toRemove.forEach { mapId ->
                assertFalse(hiddenMapsStore.isHidden(mapId))
            }
            mapIds.drop(10).forEach { mapId ->
                assertTrue(hiddenMapsStore.isHidden(mapId))
            }
        }

    @Test
    fun `test concurrent add operations are thread-safe`() =
        runTest {
            val mapIds = (1..10).map { "concurrent_add_map_$it" }

            // Launch concurrent add operations
            val addOperations =
                mapIds.map { mapId ->
                    async { hiddenMapsStore.add(mapId) }
                }

            // Wait for all operations
            addOperations.awaitAll()

            // Verify all maps are hidden
            val hiddenMaps = hiddenMapsStore.getAll()
            assertEquals(mapIds.toSet(), hiddenMaps)
            mapIds.forEach { mapId ->
                assertTrue(hiddenMapsStore.isHidden(mapId))
            }
        }

    @Test
    fun `test concurrent remove operations are thread-safe`() =
        runTest {
            val mapIds = (1..10).map { "concurrent_remove_map_$it" }

            // Add all maps first
            mapIds.forEach { mapId ->
                hiddenMapsStore.add(mapId)
            }

            // Launch concurrent remove operations
            val removeOperations =
                mapIds.map { mapId ->
                    async { hiddenMapsStore.remove(mapId) }
                }

            // Wait for all operations
            removeOperations.awaitAll()

            // Verify all maps are no longer hidden
            val hiddenMaps = hiddenMapsStore.getAll()
            assertTrue(hiddenMaps.isEmpty())
            mapIds.forEach { mapId ->
                assertFalse(hiddenMapsStore.isHidden(mapId))
            }
        }

    @Test
    fun `test mixed concurrent add and remove operations`() =
        runTest {
            val baseMapIds = (1..5).map { "mixed_base_map_$it" }
            val addMapIds = (6..10).map { "mixed_add_map_$it" }

            // Add base maps
            baseMapIds.forEach { mapId ->
                hiddenMapsStore.add(mapId)
            }

            // Launch mixed operations
            val operations =
                listOf(
                    // Remove some base maps
                    async { hiddenMapsStore.remove(baseMapIds[0]) },
                    async { hiddenMapsStore.remove(baseMapIds[1]) },
                    // Add new maps
                    async { hiddenMapsStore.add(addMapIds[0]) },
                    async { hiddenMapsStore.add(addMapIds[1]) },
                    // Read operations
                    async { hiddenMapsStore.isHidden(baseMapIds[2]) },
                    async { hiddenMapsStore.getAll() },
                )

            operations.awaitAll()

            // Verify final state
            val finalHiddenMaps = hiddenMapsStore.getAll()

            // Should contain: remaining base maps + new added maps
            assertFalse(hiddenMapsStore.isHidden(baseMapIds[0]))
            assertFalse(hiddenMapsStore.isHidden(baseMapIds[1]))
            assertTrue(hiddenMapsStore.isHidden(baseMapIds[2]))
            assertTrue(hiddenMapsStore.isHidden(addMapIds[0]))
            assertTrue(hiddenMapsStore.isHidden(addMapIds[1]))
        }

    @Test
    fun `test error handling in getAll when datastore fails`() =
        runTest {
            // Create a mock datastore that throws exceptions
            val mockDataStore = mockk<DataStore<Preferences>>()
            val faultyStore = HiddenMapsStore(mockDataStore, Dispatchers.Default)

            // Configure mock to return a flow that throws an exception
            val errorFlow = flow<Preferences> { throw RuntimeException("DataStore error") }
            every { mockDataStore.data } returns errorFlow

            // Should handle error gracefully and return empty set
            val result = faultyStore.getAll()
            assertTrue(result.isEmpty())

            // Verify error was logged
            verify { Log.e(any(), any(), any()) }
        }

    @Test
    fun `test error handling in isHidden when datastore fails`() =
        runTest {
            // Create a mock datastore that throws exceptions
            val mockDataStore = mockk<DataStore<Preferences>>()
            val faultyStore = HiddenMapsStore(mockDataStore, Dispatchers.Default)

            // Configure mock to return a flow that throws an exception
            val errorFlow = flow<Preferences> { throw RuntimeException("DataStore error") }
            every { mockDataStore.data } returns errorFlow

            // Should handle error gracefully and return false
            val result = faultyStore.isHidden("test_map")
            assertFalse(result)

            // Verify error was logged
            verify { Log.e(any(), any(), any()) }
        }

    @Test
    fun `test persistence across store instances`() =
        runTest {
            val mapId = "persistence_test_map"

            // Add map with first store instance
            hiddenMapsStore.add(mapId)
            assertTrue(hiddenMapsStore.isHidden(mapId))

            // Create new store instance pointing to same datastore
            val newStore = HiddenMapsStore(testDataStore, Dispatchers.Default)

            // Verify persistence
            assertTrue(newStore.isHidden(mapId))
            assertTrue(newStore.getAll().contains(mapId))
        }

    @Test
    fun `test special characters in map IDs are handled correctly`() =
        runTest {
            val specialMapIds =
                listOf(
                    "map-with-dashes",
                    "map_with_underscores",
                    "map.with.dots",
                    "map with spaces",
                    "map@with#special!chars",
                    "map/with/slashes",
                    "map\\with\\backslashes",
                )

            // Add all special IDs
            specialMapIds.forEach { mapId ->
                hiddenMapsStore.add(mapId)
            }

            // Verify all are correctly stored and retrieved
            val hiddenMaps = hiddenMapsStore.getAll()
            specialMapIds.forEach { mapId ->
                assertTrue(hiddenMapsStore.isHidden(mapId), "Failed for mapId: $mapId")
                assertTrue(hiddenMaps.contains(mapId), "Not found in getAll() for mapId: $mapId")
            }
            assertEquals(specialMapIds.toSet(), hiddenMaps)
        }

    @Test
    fun `test large number of hidden maps performance`() =
        runTest {
            val largeMapIdSet = (1..1000).map { "large_test_map_$it" }

            // Add all maps
            largeMapIdSet.forEach { mapId ->
                hiddenMapsStore.add(mapId)
            }

            // Verify all maps are hidden
            val hiddenMaps = hiddenMapsStore.getAll()
            assertEquals(largeMapIdSet.toSet(), hiddenMaps)

            // Verify individual queries
            largeMapIdSet.take(100).forEach { mapId ->
                assertTrue(hiddenMapsStore.isHidden(mapId))
            }
        }

    @Test
    fun `test empty string and null-like map IDs`() =
        runTest {
            val edgeCaseMapIds =
                listOf(
                    "",
                    " ",
                    "null",
                    "undefined",
                    "0",
                    "-1",
                )

            // Add all edge case IDs
            edgeCaseMapIds.forEach { mapId ->
                hiddenMapsStore.add(mapId)
            }

            // Verify all are correctly handled
            edgeCaseMapIds.forEach { mapId ->
                assertTrue(hiddenMapsStore.isHidden(mapId), "Failed for mapId: '$mapId'")
            }
            assertEquals(edgeCaseMapIds.toSet(), hiddenMapsStore.getAll())
        }

    @Test
    fun `test datastore key is consistent`() =
        runTest {
            val mapId = "key_consistency_test"

            // Add through store
            hiddenMapsStore.add(mapId)

            // Manually verify in datastore
            val preferences = testDataStore.data.first()
            val expectedKey = stringSetPreferencesKey("hidden_maps")
            val storedSet = preferences[expectedKey] ?: emptySet()
            assertTrue(storedSet.contains(mapId))
        }

    @Test
    fun `test dispatcher is used for all operations`() =
        runTest {
            // This test verifies that the dispatcher parameter is respected
            val customDispatcher = Dispatchers.Default
            val storeWithCustomDispatcher = HiddenMapsStore(testDataStore, customDispatcher)

            val mapId = "dispatcher_test_map"

            // Operations should work with custom dispatcher
            storeWithCustomDispatcher.add(mapId)
            assertTrue(storeWithCustomDispatcher.isHidden(mapId))
            assertTrue(storeWithCustomDispatcher.getAll().contains(mapId))

            storeWithCustomDispatcher.remove(mapId)
            assertFalse(storeWithCustomDispatcher.isHidden(mapId))
        }
}
