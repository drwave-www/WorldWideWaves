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


import com.worldwidewaves.shared.domain.usecases.IOSMapAvailabilityChecker
import com.worldwidewaves.shared.viewmodels.IOSMapViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive integration tests for iOS ODR (On-Demand Resources) functionality.
 * Tests the complete stack: IOSPlatformMapManager, IOSMapAvailabilityChecker, IOSMapViewModel.
 */
class IOSODRIntegrationTest {
    @Test
    fun `ODR availability detection works for both file types`() =
        runTest {
            val checker = IOSMapAvailabilityChecker()

            // GIVEN: Track test maps
            checker.trackMaps(listOf("paris_france", "new_york_usa"))

            // WHEN: Refresh availability
            checker.refreshAvailability()

            // Wait for async processing
            delay(100)

            // THEN: Map states should be updated
            val states = checker.mapStates.first()
            assertTrue(states.containsKey("paris_france"))
            assertTrue(states.containsKey("new_york_usa"))
        }

    @Test
    fun `IOSPlatformMapManager handles both geojson and mbtiles`() =
        runTest {
            val manager = IOSPlatformMapManager()
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
            delay(2.seconds)

            // THEN: Download should complete successfully
            assertTrue(downloadCompleted, "Download should complete successfully")
            assertTrue(progressUpdates.isNotEmpty(), "Progress updates should be provided")
            assertEquals(100, progressUpdates.last(), "Final progress should be 100%")
        }

    @Test
    fun `IOSMapViewModel integrates correctly with platform manager`() =
        runTest {
            val platformManager = IOSPlatformMapManager()
            val viewModel = IOSMapViewModel(platformManager)

            // WHEN: Check map availability
            viewModel.checkIfMapIsAvailable("paris_france", autoDownload = false)

            // Wait for async check
            delay(100)

            // THEN: Feature state should be updated
            val featureState = viewModel.featureState.first()
            assertTrue(featureState.isChecked, "Map availability should be checked")
        }

    @Test
    fun `concurrent ODR downloads are limited correctly`() =
        runTest {
            val manager = IOSPlatformMapManager()
            val startedDownloads = mutableSetOf<String>()

            // Start multiple downloads concurrently
            val maps = listOf("paris_france", "new_york_usa", "london_england", "berlin_germany", "tokyo_japan")

            maps.forEach { mapId ->
                manager.downloadMap(
                    mapId = mapId,
                    onProgress = { /* Track progress */ },
                    onSuccess = { startedDownloads.add(mapId) },
                    onError = { _, _ -> /* Handle errors */ },
                )
            }

            // Wait for downloads to process
            delay(5.seconds)

            // THEN: Should respect concurrent download limits
            assertTrue(startedDownloads.size <= 3, "Should not exceed max concurrent downloads")
        }

    @Test
    fun `ODR resource cleanup works on memory pressure`() =
        runTest {
            val checker = IOSMapAvailabilityChecker()

            // GIVEN: Multiple maps tracked
            val testMaps = listOf("paris_france", "new_york_usa", "london_england")
            checker.trackMaps(testMaps)

            // WHEN: Clear tracking (simulate memory pressure)
            checker.trackMaps(emptyList())
            checker.refreshAvailability()

            delay(100)

            // THEN: Map states should be cleared
            val states = checker.mapStates.first()
            assertTrue(states.isEmpty() || states.values.all { !it }, "Maps should be cleared or marked unavailable")
        }

    @Test
    fun `ODR error handling covers download failures`() =
        runTest {
            val manager = IOSPlatformMapManager()
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

            delay(2.seconds)

            // THEN: Error should be properly handled
            assertTrue(errorReceived, "Error callback should be triggered")
            assertTrue(errorCode != 0, "Error code should indicate failure")
        }

    @Test
    fun `ODR state persistence across app lifecycle`() =
        runTest {
            val checker = IOSMapAvailabilityChecker()

            // GIVEN: Initial state
            checker.trackMaps(listOf("paris_france"))
            checker.refreshAvailability()
            delay(100)

            val initialStates = checker.mapStates.first()

            // WHEN: Simulate app restart (create new checker)
            val newChecker = IOSMapAvailabilityChecker()
            newChecker.trackMaps(listOf("paris_france"))
            newChecker.refreshAvailability()
            delay(100)

            val newStates = newChecker.mapStates.first()

            // THEN: States should be consistent
            assertEquals(
                initialStates["paris_france"],
                newStates["paris_france"],
                "Map availability should persist across app lifecycle",
            )
        }
}
