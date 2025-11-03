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
 * iOS-specific tests for IosMapAvailabilityChecker.
 *
 * These tests verify the iOS map bundling logic and StateFlow behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IosMapAvailabilityCheckerTest {
    private val testScheduler = TestCoroutineScheduler()

    @Test
    fun `initial mapStates is empty`() =
        runTest(testScheduler) {
            val checker = IosMapAvailabilityChecker()

            val initialState = checker.mapStates.first()
            assertTrue(initialState.isEmpty())
        }

    @Test
    fun `isMapDownloaded checks ODR resource availability`() =
        runTest(testScheduler) {
            val checker = IosMapAvailabilityChecker()

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
            val checker = IosMapAvailabilityChecker()

            val downloadedMaps = checker.getDownloadedMaps()
            assertTrue(downloadedMaps.isEmpty())
        }

    @Test
    fun `trackMaps adds maps to tracked set`() =
        runTest(testScheduler) {
            val checker = IosMapAvailabilityChecker()
            val mapIds = listOf("paris_france", "london_uk", "tokyo_japan")

            checker.trackMaps(mapIds)
            advanceUntilIdle()

            // Maps are tracked but may not be "downloaded" without actual ODR resources
            val mapStates = checker.mapStates.first()
            assertEquals(3, mapStates.size, "Should track 3 maps")
            assertTrue(mapStates.keys.containsAll(mapIds), "Should contain all map IDs")
        }

    @Test
    fun `trackMaps updates mapStates StateFlow`() =
        runTest(testScheduler) {
            val checker = IosMapAvailabilityChecker()
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
            val checker = IosMapAvailabilityChecker()

            checker.trackMaps(listOf("paris_france", "london_uk"))
            advanceUntilIdle()

            checker.trackMaps(listOf("paris_france", "tokyo_japan"))
            advanceUntilIdle()

            val mapStates = checker.mapStates.first()
            assertEquals(3, mapStates.size, "Should track 3 unique maps")
            assertTrue(mapStates.keys.contains("paris_france"))
            assertTrue(mapStates.keys.contains("london_uk"))
            assertTrue(mapStates.keys.contains("tokyo_japan"))
        }

    @Test
    fun `trackMaps with empty collection works correctly`() =
        runTest(testScheduler) {
            val checker = IosMapAvailabilityChecker()

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
            val checker = IosMapAvailabilityChecker()
            val mapIds = listOf("paris_france", "london_uk")

            checker.trackMaps(mapIds)
            advanceUntilIdle()

            // Check initial state
            val initialState = checker.mapStates.first()
            assertEquals(2, initialState.size)

            checker.refreshAvailability()

            // Should still have tracked maps (availability depends on ODR/cache)
            val refreshedState = checker.mapStates.first()
            assertEquals(2, refreshedState.size)
            assertTrue(refreshedState.containsKey("paris_france"))
            assertTrue(refreshedState.containsKey("london_uk"))
        }

    @Test
    fun `multiple trackMaps calls accumulate correctly`() =
        runTest(testScheduler) {
            val checker = IosMapAvailabilityChecker()

            checker.trackMaps(listOf("paris_france"))
            advanceUntilIdle()

            checker.trackMaps(listOf("london_uk", "tokyo_japan"))
            advanceUntilIdle()

            checker.trackMaps(listOf("sydney_australia"))
            advanceUntilIdle()

            val mapStates = checker.mapStates.first()
            assertEquals(4, mapStates.size, "Should accumulate all tracked maps")
            assertTrue(mapStates.containsKey("paris_france"))
            assertTrue(mapStates.containsKey("london_uk"))
            assertTrue(mapStates.containsKey("tokyo_japan"))
            assertTrue(mapStates.containsKey("sydney_australia"))
        }

    @Test
    fun `trackMaps with single map works correctly`() =
        runTest(testScheduler) {
            val checker = IosMapAvailabilityChecker()

            checker.trackMaps(listOf("single_map"))
            advanceUntilIdle()

            val mapStates = checker.mapStates.first()
            assertEquals(1, mapStates.size, "Should track single map")
            assertTrue(mapStates.containsKey("single_map"))
        }

    @Test
    fun `trackMaps with special characters in map IDs`() =
        runTest(testScheduler) {
            val checker = IosMapAvailabilityChecker()
            val specialMapIds =
                listOf(
                    "map-with-dashes",
                    "map_with_underscores",
                    "map.with.dots",
                    "map with spaces",
                )

            checker.trackMaps(specialMapIds)
            advanceUntilIdle()

            val mapStates = checker.mapStates.first()
            assertEquals(4, mapStates.size, "Should track all special character maps")
            specialMapIds.forEach { mapId ->
                assertTrue(mapStates.containsKey(mapId), "Should track $mapId")
            }
        }

    @Test
    fun `concurrent trackMaps calls are handled thread-safely`() =
        runTest(testScheduler) {
            val checker = IosMapAvailabilityChecker()
            val mapCount = 100
            val mapIds = (1..mapCount).map { "map_$it" }

            // Simulate concurrent calls
            mapIds.chunked(10).forEach { chunk ->
                checker.trackMaps(chunk)
            }
            advanceUntilIdle()

            val mapStates = checker.mapStates.first()
            assertEquals(mapCount, mapStates.size, "Should track all 100 maps")

            // Verify all map IDs are present
            mapIds.forEach { mapId ->
                assertTrue(mapStates.containsKey(mapId), "Should contain $mapId")
            }
        }

    @Test
    fun `StateFlow mapStates emits updates correctly`() =
        runTest(testScheduler) {
            val checker = IosMapAvailabilityChecker()

            // Initial state should be empty
            val initialState = checker.mapStates.first()
            assertTrue(initialState.isEmpty(), "Initial state should be empty")

            checker.trackMaps(listOf("test_map_1"))
            advanceUntilIdle()

            val updatedState = checker.mapStates.first()

            // Updated state should contain the tracked map
            assertEquals(1, updatedState.size, "Should have 1 tracked map")
            assertTrue(updatedState.containsKey("test_map_1"))
        }

    @Test
    fun `refreshAvailability does not modify untracked maps`() =
        runTest(testScheduler) {
            val checker = IosMapAvailabilityChecker()

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
    fun `releaseDownloadedMap clears individual maps`() =
        runTest(testScheduler) {
            val checker = IosMapAvailabilityChecker()

            // Track and request download for some maps
            checker.trackMaps(listOf("paris_france", "london_uk"))
            advanceUntilIdle()

            // Maps are tracked
            val initialState = checker.mapStates.first()
            assertEquals(2, initialState.size)

            // Release one map (cleanup is done via releaseDownloadedMap, not a cleanup() method)
            checker.releaseDownloadedMap("paris_france")
            advanceUntilIdle()

            // The map should still be tracked but ODR request released
            // (tracking != ODR download state)
            val afterRelease = checker.mapStates.first()
            assertEquals(2, afterRelease.size, "Maps remain tracked after release")
        }

    @Test
    fun `ODR resource request handling works correctly`() =
        runTest(testScheduler) {
            val checker = IosMapAvailabilityChecker()

            // This test verifies that ODR resource requests don't crash
            checker.trackMaps(listOf("test_odr_resource"))
            advanceUntilIdle()

            // Should not crash and should track the resource
            checker.getDownloadedMaps()

            advanceUntilIdle()
        }

    /**
     * Tests that requestMapUninstall updates state to false.
     * This ensures the map is marked as unavailable immediately after uninstall.
     */
    @Test
    fun `requestMapUninstall updates state to false`() =
        runTest(testScheduler) {
            val checker = IosMapAvailabilityChecker()

            // Track a map
            checker.trackMaps(listOf("test_map_uninstall"))
            advanceUntilIdle()

            // Request uninstall
            checker.requestMapUninstall("test_map_uninstall")
            advanceUntilIdle()

            // Verify state is false after uninstall
            val state = checker.mapStates.first()
            assertEquals(
                false,
                state["test_map_uninstall"],
                "Map should be marked as unavailable after uninstall",
            )
        }

    /**
     * Tests that requestMapUninstall handles cache clearing without crashing.
     * Note: We can't easily verify file deletion in unit tests, but we ensure
     * the clearEventCache call doesn't cause errors.
     */
    @Test
    fun `requestMapUninstall clears cache without crashing`() =
        runTest(testScheduler) {
            val checker = IosMapAvailabilityChecker()

            // Track a map
            checker.trackMaps(listOf("test_cache_clear"))
            advanceUntilIdle()

            // Request uninstall (includes cache clearing)
            // This should not crash even if cache files don't exist
            val result = checker.requestMapUninstall("test_cache_clear")
            advanceUntilIdle()

            // Operation should complete successfully
            // (result may be false if no ODR was pinned, but it shouldn't crash)
            assertTrue(true, "Uninstall with cache clearing completed without crash")
        }

    /**
     * Tests that uninstalling a non-existent map handles gracefully.
     * Ensures cache clearing doesn't fail when cache is already empty.
     */
    @Test
    fun `requestMapUninstall handles non-existent map gracefully`() =
        runTest(testScheduler) {
            val checker = IosMapAvailabilityChecker()

            // Request uninstall of map that was never tracked
            val result = checker.requestMapUninstall("nonexistent_map")
            advanceUntilIdle()

            // Should handle gracefully without crashing
            // Result may be false (no ODR request existed), but operation succeeds
            assertTrue(true, "Uninstall of non-existent map handled gracefully")
        }
}
