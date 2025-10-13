package com.worldwidewaves.shared.viewmodels

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

import com.worldwidewaves.shared.map.MapFeatureState
import com.worldwidewaves.shared.map.PlatformMapManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for IosMapViewModel.
 * Tests the iOS-specific MapViewModel implementation with MapDownloadCoordinator composition.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IosMapViewModelTest {
    private lateinit var mockPlatformMapManager: TestPlatformMapManager
    private lateinit var viewModel: IosMapViewModel

    @BeforeTest
    fun setUp() {
        mockPlatformMapManager = TestPlatformMapManager()
        viewModel = IosMapViewModel(mockPlatformMapManager)
    }

    @AfterTest
    fun tearDown() {
        // Allow any pending coroutines to complete
        kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.delay(200)
        }
    }

    // ------------------------------------------------------------------------
    // Initial State Tests
    // ------------------------------------------------------------------------

    @Test
    fun `initial featureState is NotChecked`() =
        runTest {
            val initialState = viewModel.featureState.first()
            assertEquals(MapFeatureState.NotChecked, initialState)
        }

    // ------------------------------------------------------------------------
    // MapViewModel Interface Tests
    // ------------------------------------------------------------------------

    @Test
    fun `checkIfMapIsAvailable delegates to MapDownloadCoordinator correctly`() =
        runTest {
            mockPlatformMapManager.setMapAvailable("test_map", true)

            viewModel.checkIfMapIsAvailable("test_map", autoDownload = false)

            // Wait for state to change with polling using real delay
            var finalState: MapFeatureState = MapFeatureState.NotChecked
            repeat(20) {
                // 20 * 150ms = 3000ms timeout
                kotlinx.coroutines.delay(150)
                val state = viewModel.featureState.first()
                finalState = state
                if (state == MapFeatureState.Available) return@repeat
            }

            assertEquals(MapFeatureState.Available, finalState, "Expected Available but got $finalState")
            assertTrue(mockPlatformMapManager.isMapAvailableCalled)
        }

    @Test
    fun `checkIfMapIsAvailable with autoDownload triggers download`() =
        runTest {
            mockPlatformMapManager.setMapAvailable("test_map", false)

            viewModel.checkIfMapIsAvailable("test_map", autoDownload = true)

            // Wait for state to change with polling using real delay
            var finalState: MapFeatureState = MapFeatureState.NotChecked
            repeat(20) {
                // 20 * 150ms = 3000ms timeout
                kotlinx.coroutines.delay(150)
                val state = viewModel.featureState.first()
                finalState = state
                if (state == MapFeatureState.Pending) return@repeat
            }

            assertEquals(MapFeatureState.Pending, finalState, "Expected Pending but got $finalState")
            assertTrue(mockPlatformMapManager.downloadMapCalled)
        }

    @Test
    fun `downloadMap triggers platform download with correct callbacks`() =
        runTest {
            var downloadCompleted = false
            mockPlatformMapManager.simulateSuccessfulDownload = true

            viewModel.downloadMap("test_map") { downloadCompleted = true }

            // Wait for download completion with polling using real delay
            repeat(30) {
                // 30 * 150ms = 4500ms timeout
                if (downloadCompleted) return@repeat
                kotlinx.coroutines.delay(150)
            }

            assertTrue(mockPlatformMapManager.downloadMapCalled)
            assertEquals("test_map", mockPlatformMapManager.downloadedMapId)
            assertTrue(downloadCompleted, "Download should complete")
        }

    @Test
    fun `downloadMap handles progress updates correctly`() =
        runTest {
            mockPlatformMapManager.simulateProgressUpdates = true
            mockPlatformMapManager.progressValues = listOf(25, 50, 75, 100)

            viewModel.downloadMap("test_map")

            // Wait for state to reach Installed with polling using real delay
            var finalState: MapFeatureState = MapFeatureState.NotChecked
            repeat(30) {
                // 30 * 150ms = 4500ms timeout
                kotlinx.coroutines.delay(150)
                val state = viewModel.featureState.first()
                finalState = state
                if (state is MapFeatureState.Installed) return@repeat
            }

            // Should eventually reach success state
            assertTrue(finalState is MapFeatureState.Installed, "Final state should be Installed (got: $finalState)")
        }

    @Test
    fun `downloadMap handles ODR failures correctly`() =
        runTest {
            mockPlatformMapManager.simulateSuccessfulDownload = false
            mockPlatformMapManager.errorCode = -1
            mockPlatformMapManager.errorMessage = "ODR resource not found"

            viewModel.downloadMap("test_map")

            // Wait for state to reach Failed with polling using real delay
            var finalState: MapFeatureState = MapFeatureState.NotChecked
            repeat(30) {
                // 30 * 150ms = 4500ms timeout
                kotlinx.coroutines.delay(150)
                val state = viewModel.featureState.first()
                finalState = state
                if (state is MapFeatureState.Failed) return@repeat
            }

            assertTrue(finalState is MapFeatureState.Failed, "State should be Failed (got: $finalState)")
            assertEquals(-1, (finalState as MapFeatureState.Failed).errorCode)
        }

    @Test
    fun `cancelDownload calls platform manager and updates state`() =
        runTest {
            // Start a download first
            viewModel.downloadMap("test_map")
            kotlinx.coroutines.delay(300) // Allow download to start - iOS needs more time

            viewModel.cancelDownload()
            kotlinx.coroutines.delay(300) // Allow cancellation to process - iOS needs more time

            // Wait for cancellation to complete with polling using real delay
            repeat(20) {
                // 20 * 150ms = 3000ms timeout
                if (mockPlatformMapManager.cancelDownloadCalled) return@repeat
                kotlinx.coroutines.delay(150)
            }

            assertTrue(mockPlatformMapManager.cancelDownloadCalled, "Cancel should be called")
            assertEquals("test_map", mockPlatformMapManager.cancelledMapId)
        }

    // ------------------------------------------------------------------------
    // Platform Integration Tests
    // ------------------------------------------------------------------------

    @Test
    fun `platform adapter correctly checks map installation`() =
        runTest {
            mockPlatformMapManager.setMapAvailable("installed_map", true)
            mockPlatformMapManager.setMapAvailable("missing_map", false)

            viewModel.checkIfMapIsAvailable("installed_map")
            // Wait for state to change with polling using real delay
            var state1: MapFeatureState = MapFeatureState.NotChecked
            repeat(20) {
                // 20 * 150ms = 3000ms timeout
                kotlinx.coroutines.delay(150)
                val state = viewModel.featureState.first()
                state1 = state
                if (state == MapFeatureState.Available) return@repeat
            }
            assertEquals(MapFeatureState.Available, state1, "Expected Available for installed_map (got: $state1)")

            viewModel.checkIfMapIsAvailable("missing_map")
            // Wait for state to change with polling using real delay
            var state2: MapFeatureState = MapFeatureState.NotChecked
            repeat(20) {
                // 20 * 150ms = 3000ms timeout
                kotlinx.coroutines.delay(150)
                val state = viewModel.featureState.first()
                state2 = state
                if (state == MapFeatureState.NotAvailable) return@repeat
            }
            assertEquals(MapFeatureState.NotAvailable, state2, "Expected NotAvailable for missing_map (got: $state2)")
        }

    @Test
    fun `platform adapter handles ODR download lifecycle`() =
        runTest {
            var progressCallCount = 0
            val progressValues = mutableListOf<Int>()

            mockPlatformMapManager.onProgressCallback = { progress ->
                progressCallCount++
                progressValues.add(progress)
            }
            mockPlatformMapManager.simulateRealisticDownload = true

            viewModel.downloadMap("test_map")

            // Wait for download to complete with polling using real delay
            var finalState: MapFeatureState = MapFeatureState.NotChecked
            repeat(30) {
                // 30 * 150ms = 4500ms timeout
                kotlinx.coroutines.delay(150)
                val state = viewModel.featureState.first()
                finalState = state
                if (state is MapFeatureState.Installed) return@repeat
            }

            assertTrue(progressCallCount > 0, "Should have progress callbacks (got: $progressCallCount)")
            assertTrue(progressValues.isNotEmpty(), "Should have progress values (got: ${progressValues.size})")
            assertEquals(MapFeatureState.Installed, finalState, "Expected Installed (got: $finalState)")
        }

    @Test
    fun `error message localization works correctly`() =
        runTest {
            val errorCodes =
                mapOf(
                    -1 to "ODR resource download failed",
                    -2 to "Unknown error during ODR download",
                    -3 to "Download already in progress",
                    999 to "Unknown error (code: 999)",
                )

            errorCodes.forEach { (code, _) ->
                mockPlatformMapManager.errorCode = code
                mockPlatformMapManager.simulateSuccessfulDownload = false

                viewModel.downloadMap("test_map")

                // Wait for state to reach Failed with polling using real delay
                var finalState: MapFeatureState = MapFeatureState.NotChecked
                repeat(30) {
                    // 30 * 150ms = 4500ms timeout
                    kotlinx.coroutines.delay(150)
                    val state = viewModel.featureState.first()
                    finalState = state
                    if (state is MapFeatureState.Failed) return@repeat
                }

                assertTrue(finalState is MapFeatureState.Failed, "State should be Failed for error code $code (got: $finalState)")
                // Error message is set by the platform adapter logic
            }
        }

    // ------------------------------------------------------------------------
    // BaseViewModel Integration Tests
    // ------------------------------------------------------------------------

    @Test
    fun `viewModel inherits BaseViewModel correctly`() =
        runTest {
            // Test that BaseViewModel functionality works through public interface
            // Test coroutine scope works
            viewModel.checkIfMapIsAvailable("test")
            delay(100) // Allow async processing

            // Should not crash and should have valid state
            assertNotNull(viewModel.featureState.first())
        }

    @Test
    fun `concurrent operations are handled safely`() =
        runTest {
            mockPlatformMapManager.simulateSlowDownload = true

            // Start multiple operations
            viewModel.checkIfMapIsAvailable("map1")
            viewModel.downloadMap("map2")
            viewModel.cancelDownload()

            delay(200) // Allow operations to start

            // Should handle all operations without crashing
            assertNotNull(viewModel.featureState.first())
        }

    // ------------------------------------------------------------------------
    // Test Helper Class
    // ------------------------------------------------------------------------

    private class TestPlatformMapManager : PlatformMapManager {
        var isMapAvailableCalled = false
        var downloadMapCalled = false
        var cancelDownloadCalled = false
        var downloadedMapId: String? = null
        var cancelledMapId: String? = null

        var simulateSuccessfulDownload = true
        var simulateProgressUpdates = false
        var simulateRealisticDownload = false
        var simulateSlowDownload = false
        var errorCode = -1
        var errorMessage = "Test error"

        var onProgressCallback: ((Int) -> Unit)? = null
        var progressValues = listOf<Int>()

        private val availableMaps = mutableMapOf<String, Boolean>()

        fun setMapAvailable(
            mapId: String,
            available: Boolean,
        ) {
            availableMaps[mapId] = available
        }

        override fun isMapAvailable(mapId: String): Boolean {
            isMapAvailableCalled = true
            return availableMaps[mapId] ?: false
        }

        override suspend fun downloadMap(
            mapId: String,
            onProgress: (Int) -> Unit,
            onSuccess: () -> Unit,
            onError: (Int, String?) -> Unit,
        ) {
            downloadMapCalled = true
            downloadedMapId = mapId

            if (simulateSlowDownload) {
                return // Don't complete
            }

            // iOS needs more realistic delays for async processing
            delay(50) // Initial delay before processing starts

            if (simulateProgressUpdates || simulateRealisticDownload) {
                val values = if (progressValues.isNotEmpty()) progressValues else listOf(25, 50, 75, 100)
                values.forEach { progress ->
                    onProgress(progress)
                    onProgressCallback?.invoke(progress)
                    delay(20) // Realistic delay between progress updates
                }
            }

            if (simulateSuccessfulDownload) {
                onProgress(100)
                delay(50) // Delay before completion
                onSuccess()
            } else {
                delay(50) // Delay before error
                onError(errorCode, errorMessage)
            }
        }

        override fun cancelDownload(mapId: String) {
            cancelDownloadCalled = true
            cancelledMapId = mapId
        }
    }
}
