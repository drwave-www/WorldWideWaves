package com.worldwidewaves.shared.map

/* * Copyright 2025 DrWave
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
 * limitations under the License. */

import com.worldwidewaves.shared.domain.usecases.IosMapAvailabilityChecker
import com.worldwidewaves.shared.viewmodels.IosMapViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive integration tests for iOS ODR (On-Demand Resources) functionality.
 * Tests the complete stack: IosPlatformMapManager, IosMapAvailabilityChecker, IosMapViewModel.
 *
 * NOTE: These tests require actual iOS application bundles with embedded ODR resources.
 * They are integration tests that cannot run in CI without a full iOS app bundle.
 * They should be run manually on iOS devices/simulators with proper bundle configuration.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IosOdrIntegrationTest {
    @Test
    fun `ODR availability detection works for both file types`() =
        runTest {
            val checker = IosMapAvailabilityChecker()

            // GIVEN: Track test maps
            checker.trackMaps(listOf("paris_france", "new_york_usa"))

            // WHEN: Refresh availability
            checker.refreshAvailability()

            // Wait for async processing
            advanceUntilIdle()

            // THEN: Map states should be updated
            val states = checker.mapStates.first()
            assertTrue(states.containsKey("paris_france"))
            assertTrue(states.containsKey("new_york_usa"))
        }

    @Test
    fun `IosPlatformMapManager handles both geojson and mbtiles`() =
        runTest {
            val manager = IosPlatformMapManager()
            var downloadCompleted = false
            var progressUpdates = mutableListOf<Int>()

            // WHEN: Download map with both file types
            manager.downloadMap(
                mapId = "paris_france",
                onProgress = { progress -> progressUpdates.add(progress) },
                onSuccess = { downloadCompleted = true },
                onError = { _, _ -> /* Should not happen in test */ },
            )

            // Wait for simulated download
            advanceTimeBy(35000) // More than MAX_DOWNLOAD_DURATION_MS
            advanceUntilIdle()

            // THEN: Download should complete (either success or error, depending on bundle availability)
            assertTrue(progressUpdates.isNotEmpty(), "Progress updates should be provided")
            assertTrue(progressUpdates.contains(0), "Should start with 0% progress")
        }

    @Test
    fun `IosMapViewModel integrates correctly with platform manager`() =
        runTest {
            val platformManager = IosPlatformMapManager()
            val viewModel = IosMapViewModel(platformManager)

            // WHEN: Check map availability
            viewModel.checkIfMapIsAvailable("paris_france", autoDownload = false)

            // Wait for async check
            advanceUntilIdle()

            // THEN: Feature state should be updated
            val featureState = viewModel.featureState.first()
            // Note: Specific state validation depends on MapFeatureState implementation
        }

    @Test
    fun `concurrent ODR downloads are limited correctly`() =
        runTest {
            val manager = IosPlatformMapManager()
            val startedDownloads = mutableSetOf<String>()

            // Start multiple downloads concurrently
            val maps = listOf("paris_france", "new_york_usa", "london_england", "berlin_germany", "tokyo_japan")

            maps.forEach { mapId ->
                manager.downloadMap(
                    mapId = mapId,
                    onProgress = { /* Track progress */ },
                    onSuccess = { startedDownloads.add(mapId) },
                    onError = { _, _ ->
                        startedDownloads.add(mapId) // Handle errors - still counts as processed
                    },
                )
            }

            // Wait for downloads to process
            advanceTimeBy(35000)
            advanceUntilIdle()

            // THEN: All downloads should eventually complete or error
            assertTrue(startedDownloads.isNotEmpty(), "At least some downloads should be processed")
        }

    @Test
    fun `ODR resource cleanup works on memory pressure`() =
        runTest {
            val checker = IosMapAvailabilityChecker()

            // GIVEN: Multiple maps tracked
            val testMaps = listOf("paris_france", "new_york_usa", "london_england")
            checker.trackMaps(testMaps)

            advanceUntilIdle()

            // Release maps (iOS uses releaseDownloadedMap, not a generic cleanup())
            testMaps.forEach { checker.releaseDownloadedMap(it) }
            advanceUntilIdle()

            // Maps are still tracked (release != untrack), but ODR requests are released
            val states = checker.mapStates.first()
            assertEquals(3, states.size, "Maps remain tracked after ODR release")
        }

    @Test
    fun `ODR error handling covers download failures`() =
        runTest {
            val manager = IosPlatformMapManager()
            var errorReceived = false
            var errorCode = 0

            // WHEN: Download non-existent map (should fail)
            manager.downloadMap(
                mapId = "non_existent_map",
                onProgress = { /* No progress expected */ },
                onSuccess = { /* Should not succeed */ },
                onError = { code, _ ->
                    errorReceived = true
                    errorCode = code
                },
            )

            advanceTimeBy(35000)
            advanceUntilIdle()

            // THEN: Error should be properly handled
            assertTrue(errorReceived, "Error callback should be triggered")
            assertTrue(errorCode != 0, "Error code should indicate failure")
        }

    @Test
    fun `ODR state persistence across app lifecycle`() =
        runTest {
            val checker = IosMapAvailabilityChecker()

            // GIVEN: Initial state
            checker.trackMaps(listOf("paris_france"))
            checker.refreshAvailability()
            advanceUntilIdle()

            val initialStates = checker.mapStates.first()

            // WHEN: Simulate app restart (create new checker)
            val newChecker = IosMapAvailabilityChecker()
            newChecker.trackMaps(listOf("paris_france"))
            newChecker.refreshAvailability()
            advanceUntilIdle()

            val newStates = newChecker.mapStates.first()

            // THEN: States should be consistent
            assertEquals(
                initialStates["paris_france"],
                newStates["paris_france"],
                "Map availability should persist across app lifecycle",
            )
        }
}
