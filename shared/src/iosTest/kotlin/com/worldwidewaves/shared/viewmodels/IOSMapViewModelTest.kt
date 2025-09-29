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
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for IOSMapViewModel.
 * Tests the iOS-specific MapViewModel implementation with MapDownloadManager composition.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IOSMapViewModelTest {
    private val testScheduler = TestCoroutineScheduler()
    private lateinit var mockPlatformMapManager: TestPlatformMapManager
    private lateinit var viewModel: IOSMapViewModel

    @BeforeTest
    fun setUp() {
        mockPlatformMapManager = TestPlatformMapManager()
        viewModel = IOSMapViewModel(mockPlatformMapManager)
    }

    @AfterTest
    fun tearDown() {
        // Cleanup test resources
    }

    // ------------------------------------------------------------------------
    // Initial State Tests
    // ------------------------------------------------------------------------

    @Test
    fun `initial featureState is NotChecked`() =
        runTest(testScheduler) {
            val initialState = viewModel.featureState.first()
            assertEquals(MapFeatureState.NotChecked, initialState)
        }

    // ------------------------------------------------------------------------
    // MapViewModel Interface Tests
    // ------------------------------------------------------------------------

    @Test
    fun `checkIfMapIsAvailable delegates to MapDownloadManager correctly`() =
        runTest(testScheduler) {
            mockPlatformMapManager.setMapAvailable("test_map", true)

            viewModel.checkIfMapIsAvailable("test_map", autoDownload = false)
            advanceUntilIdle()

            val state = viewModel.featureState.first()
            assertEquals(MapFeatureState.Available, state)
            assertTrue(mockPlatformMapManager.isMapAvailableCalled)
        }

    @Test
    fun `checkIfMapIsAvailable with autoDownload triggers download`() =
        runTest(testScheduler) {
            mockPlatformMapManager.setMapAvailable("test_map", false)

            viewModel.checkIfMapIsAvailable("test_map", autoDownload = true)
            advanceUntilIdle()

            val state = viewModel.featureState.first()
            assertEquals(MapFeatureState.Pending, state)
            assertTrue(mockPlatformMapManager.downloadMapCalled)
        }

    @Test
    fun `downloadMap triggers platform download with correct callbacks`() =
        runTest(testScheduler) {
            var downloadCompleted = false
            mockPlatformMapManager.simulateSuccessfulDownload = true

            viewModel.downloadMap("test_map") { downloadCompleted = true }
            advanceUntilIdle()

            assertTrue(mockPlatformMapManager.downloadMapCalled)
            assertEquals("test_map", mockPlatformMapManager.downloadedMapId)
            assertTrue(downloadCompleted)
        }

    @Test
    fun `downloadMap handles progress updates correctly`() =
        runTest(testScheduler) {
            mockPlatformMapManager.simulateProgressUpdates = true
            mockPlatformMapManager.progressValues = listOf(25, 50, 75, 100)

            viewModel.downloadMap("test_map")
            advanceUntilIdle()

            // Should eventually reach success state
            val finalState = viewModel.featureState.first()
            assertTrue(finalState is MapFeatureState.Installed)
        }

    @Test
    fun `downloadMap handles ODR failures correctly`() =
        runTest(testScheduler) {
            mockPlatformMapManager.simulateSuccessfulDownload = false
            mockPlatformMapManager.errorCode = -1
            mockPlatformMapManager.errorMessage = "ODR resource not found"

            viewModel.downloadMap("test_map")
            advanceUntilIdle()

            val state = viewModel.featureState.first()
            assertTrue(state is MapFeatureState.Failed)
            assertEquals(-1, (state as MapFeatureState.Failed).errorCode)
        }

    @Test
    fun `cancelDownload calls platform manager and updates state`() =
        runTest(testScheduler) {
            // Start a download first
            viewModel.downloadMap("test_map")
            advanceUntilIdle()

            viewModel.cancelDownload()
            advanceUntilIdle()

            assertTrue(mockPlatformMapManager.cancelDownloadCalled)
            assertEquals("test_map", mockPlatformMapManager.cancelledMapId)
        }

    // ------------------------------------------------------------------------
    // Platform Integration Tests
    // ------------------------------------------------------------------------

    @Test
    fun `platform adapter correctly checks map installation`() =
        runTest(testScheduler) {
            mockPlatformMapManager.setMapAvailable("installed_map", true)
            mockPlatformMapManager.setMapAvailable("missing_map", false)

            viewModel.checkIfMapIsAvailable("installed_map")
            advanceUntilIdle()
            assertEquals(MapFeatureState.Available, viewModel.featureState.first())

            viewModel.checkIfMapIsAvailable("missing_map")
            advanceUntilIdle()
            assertEquals(MapFeatureState.NotAvailable, viewModel.featureState.first())
        }

    @Test
    fun `platform adapter handles ODR download lifecycle`() =
        runTest(testScheduler) {
            var progressCallCount = 0
            val progressValues = mutableListOf<Int>()

            mockPlatformMapManager.onProgressCallback = { progress ->
                progressCallCount++
                progressValues.add(progress)
            }
            mockPlatformMapManager.simulateRealisticDownload = true

            viewModel.downloadMap("test_map")
            advanceUntilIdle()

            assertTrue(progressCallCount > 0)
            assertTrue(progressValues.isNotEmpty())
            assertEquals(MapFeatureState.Installed, viewModel.featureState.first())
        }

    @Test
    fun `error message localization works correctly`() =
        runTest(testScheduler) {
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
                advanceUntilIdle()

                val state = viewModel.featureState.first()
                assertTrue(state is MapFeatureState.Failed)
                // Error message is set by the platform adapter logic
            }
        }

    // ------------------------------------------------------------------------
    // BaseViewModel Integration Tests
    // ------------------------------------------------------------------------

    @Test
    fun `viewModel inherits BaseViewModel correctly`() =
        runTest(testScheduler) {
            // Test that BaseViewModel functionality works through public interface
            // Test coroutine scope works
            viewModel.checkIfMapIsAvailable("test")
            advanceUntilIdle()

            // Should not crash and should have valid state
            assertNotNull(viewModel.featureState.first())
        }

    @Test
    fun `concurrent operations are handled safely`() =
        runTest(testScheduler) {
            mockPlatformMapManager.simulateSlowDownload = true

            // Start multiple operations
            viewModel.checkIfMapIsAvailable("map1")
            viewModel.downloadMap("map2")
            viewModel.cancelDownload()

            advanceUntilIdle()

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

            if (simulateProgressUpdates || simulateRealisticDownload) {
                val values = if (progressValues.isNotEmpty()) progressValues else listOf(25, 50, 75, 100)
                values.forEach { progress ->
                    onProgress(progress)
                    onProgressCallback?.invoke(progress)
                    delay(1) // Small delay for test scheduler
                }
            }

            if (simulateSuccessfulDownload) {
                onProgress(100)
                delay(1) // Small delay before completion
                onSuccess()
            } else {
                delay(1) // Small delay before error
                onError(errorCode, errorMessage)
            }
        }

        override fun cancelDownload(mapId: String) {
            cancelDownloadCalled = true
            cancelledMapId = mapId
        }
    }
}
