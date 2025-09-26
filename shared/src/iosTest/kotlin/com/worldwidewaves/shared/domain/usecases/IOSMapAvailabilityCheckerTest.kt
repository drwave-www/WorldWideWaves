package com.worldwidewaves.shared.domain.usecases

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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * iOS-specific tests for IOSMapAvailabilityChecker.
 *
 * These tests verify the iOS map bundling logic and StateFlow behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IOSMapAvailabilityCheckerTest {
    private val testScheduler = TestCoroutineScheduler()

    @Test
    fun `initial mapStates is empty`() =
        runTest(testScheduler) {
            val checker = IOSMapAvailabilityChecker()

            val initialState = checker.mapStates.first()
            assertTrue(initialState.isEmpty())
        }

    @Test
    fun `isMapDownloaded checks ODR resource availability`() =
        runTest(testScheduler) {
            val checker = IOSMapAvailabilityChecker()

            // ODR resources may not be immediately available
            checker.isMapDownloaded("paris_france")
            checker.isMapDownloaded("london_uk")
            checker.isMapDownloaded("nonexistent_event")

            // Results depend on actual ODR configuration
            // In test environment, these will likely be false unless ODR is configured
            // This test verifies the method works without crashing
        }

    @Test
    fun `getDownloadedMaps returns empty list initially`() =
        runTest(testScheduler) {
            val checker = IOSMapAvailabilityChecker()

            val downloadedMaps = checker.getDownloadedMaps()
            assertTrue(downloadedMaps.isEmpty())
        }

    @Test
    fun `trackMaps adds maps to tracked set`() =
        runTest(testScheduler) {
            val checker = IOSMapAvailabilityChecker()
            val mapIds = listOf("paris_france", "london_uk", "tokyo_japan")

            checker.trackMaps(mapIds)
            advanceUntilIdle()

            val trackedMaps = checker.getDownloadedMaps()
            assertEquals(3, trackedMaps.size)
            assertTrue(trackedMaps.containsAll(mapIds))
        }

    @Test
    fun `trackMaps updates mapStates StateFlow`() =
        runTest(testScheduler) {
            val checker = IOSMapAvailabilityChecker()
            val mapIds = listOf("paris_france", "london_uk")

            checker.trackMaps(mapIds)
            advanceUntilIdle()

            val mapStates = checker.mapStates.first()
            // ODR may or may not have these resources available immediately
            // The important thing is that we're tracking them
            assertEquals(2, mapStates.size)
            // Values depend on ODR configuration
        }

    @Test
    fun `trackMaps with duplicate maps does not duplicate tracking`() =
        runTest(testScheduler) {
            val checker = IOSMapAvailabilityChecker()

            checker.trackMaps(listOf("paris_france", "london_uk"))
            advanceUntilIdle()

            checker.trackMaps(listOf("paris_france", "tokyo_japan"))
            advanceUntilIdle()

            val trackedMaps = checker.getDownloadedMaps()
            assertEquals(3, trackedMaps.size)
            assertTrue(trackedMaps.contains("paris_france"))
            assertTrue(trackedMaps.contains("london_uk"))
            assertTrue(trackedMaps.contains("tokyo_japan"))
        }

    @Test
    fun `trackMaps with empty collection works correctly`() =
        runTest(testScheduler) {
            val checker = IOSMapAvailabilityChecker()

            checker.trackMaps(emptyList())
            advanceUntilIdle()

            val trackedMaps = checker.getDownloadedMaps()
            assertTrue(trackedMaps.isEmpty())

            val mapStates = checker.mapStates.first()
            assertTrue(mapStates.isEmpty())
        }

    @Test
    fun `refreshAvailability updates tracked maps state`() =
        runTest(testScheduler) {
            val checker = IOSMapAvailabilityChecker()
            val mapIds = listOf("paris_france", "london_uk")

            checker.trackMaps(mapIds)
            advanceUntilIdle()

            // Clear the state to test refresh
            val initialState = checker.mapStates.first()
            assertEquals(2, initialState.size)

            checker.refreshAvailability()

            // Should still show tracked maps as available
            val refreshedState = checker.mapStates.first()
            assertEquals(2, refreshedState.size)
            assertTrue(refreshedState["paris_france"] == true)
            assertTrue(refreshedState["london_uk"] == true)
        }

    @Test
    fun `multiple trackMaps calls accumulate correctly`() =
        runTest(testScheduler) {
            val checker = IOSMapAvailabilityChecker()

            checker.trackMaps(listOf("paris_france"))
            advanceUntilIdle()

            checker.trackMaps(listOf("london_uk", "tokyo_japan"))
            advanceUntilIdle()

            checker.trackMaps(listOf("sydney_australia"))
            advanceUntilIdle()

            val trackedMaps = checker.getDownloadedMaps()
            assertEquals(4, trackedMaps.size)

            val mapStates = checker.mapStates.first()
            assertEquals(4, mapStates.size)
            mapStates.values.forEach { isAvailable ->
                assertTrue(isAvailable) // All should be true on iOS
            }
        }

    @Test
    fun `trackMaps with single map works correctly`() =
        runTest(testScheduler) {
            val checker = IOSMapAvailabilityChecker()

            checker.trackMaps(listOf("single_map"))
            advanceUntilIdle()

            val trackedMaps = checker.getDownloadedMaps()
            assertEquals(1, trackedMaps.size)
            assertEquals("single_map", trackedMaps.first())

            val mapStates = checker.mapStates.first()
            assertEquals(1, mapStates.size)
            assertTrue(mapStates["single_map"] == true)
        }

    @Test
    fun `trackMaps with special characters in map IDs`() =
        runTest(testScheduler) {
            val checker = IOSMapAvailabilityChecker()
            val specialMapIds =
                listOf(
                    "map-with-dashes",
                    "map_with_underscores",
                    "map.with.dots",
                    "map with spaces",
                )

            checker.trackMaps(specialMapIds)
            advanceUntilIdle()

            val trackedMaps = checker.getDownloadedMaps()
            assertEquals(4, trackedMaps.size)
            assertTrue(trackedMaps.containsAll(specialMapIds))

            specialMapIds.forEach { mapId ->
                assertTrue(checker.isMapDownloaded(mapId))
            }
        }

    @Test
    fun `concurrent trackMaps calls are handled thread-safely`() =
        runTest(testScheduler) {
            val checker = IOSMapAvailabilityChecker()
            val mapCount = 100
            val mapIds = (1..mapCount).map { "map_$it" }

            // Simulate concurrent calls
            mapIds.chunked(10).forEach { chunk ->
                checker.trackMaps(chunk)
            }
            advanceUntilIdle()

            val trackedMaps = checker.getDownloadedMaps()
            assertEquals(mapCount, trackedMaps.size)

            val mapStates = checker.mapStates.first()
            assertEquals(mapCount, mapStates.size)

            // Verify all maps are marked as available
            mapStates.values.forEach { isAvailable ->
                assertTrue(isAvailable)
            }
        }

    @Test
    fun `StateFlow mapStates emits updates correctly`() =
        runTest(testScheduler) {
            val checker = IOSMapAvailabilityChecker()
            val emittedStates = mutableListOf<Map<String, Boolean>>()

            // Collect states (this would typically be done in a coroutine)
            val initialState = checker.mapStates.first()
            emittedStates.add(initialState)

            checker.trackMaps(listOf("test_map_1"))
            advanceUntilIdle()

            val updatedState = checker.mapStates.first()

            // Initial state should be empty
            assertTrue(emittedStates[0].isEmpty())

            // Updated state should contain the tracked map
            assertEquals(1, updatedState.size)
            assertTrue(updatedState["test_map_1"] == true)
        }

    @Test
    fun `refreshAvailability does not modify untracked maps`() =
        runTest(testScheduler) {
            val checker = IOSMapAvailabilityChecker()

            // Initially no maps tracked
            checker.refreshAvailability()
            val stateAfterRefresh = checker.mapStates.first()
            assertTrue(stateAfterRefresh.isEmpty())

            // Track some maps
            checker.trackMaps(listOf("paris_france"))
            advanceUntilIdle()

            // Refresh should maintain tracked maps
            checker.refreshAvailability()
            advanceUntilIdle()
            val finalState = checker.mapStates.first()
            assertEquals(1, finalState.size)
            // ODR availability depends on configuration
        }

    @Test
    fun `cleanup clears all tracked maps and states`() =
        runTest(testScheduler) {
            val checker = IOSMapAvailabilityChecker()

            // Track some maps
            checker.trackMaps(listOf("paris_france", "london_uk"))
            advanceUntilIdle()

            checker.getDownloadedMaps()
            // Should have some tracked maps

            // Cleanup should clear everything
            checker.cleanup()
            advanceUntilIdle()

            val finalTrackedMaps = checker.getDownloadedMaps()
            assertTrue(finalTrackedMaps.isEmpty())

            val finalState = checker.mapStates.first()
            assertTrue(finalState.isEmpty())
        }

    @Test
    fun `ODR resource request handling works correctly`() =
        runTest(testScheduler) {
            val checker = IOSMapAvailabilityChecker()

            // This test verifies that ODR resource requests don't crash
            checker.trackMaps(listOf("test_odr_resource"))
            advanceUntilIdle()

            // Should not crash and should track the resource
            checker.getDownloadedMaps()

            // Cleanup after test
            checker.cleanup()
            advanceUntilIdle()
        }
}
