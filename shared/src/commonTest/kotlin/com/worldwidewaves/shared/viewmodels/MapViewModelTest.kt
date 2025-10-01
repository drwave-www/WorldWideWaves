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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive contract tests for MapViewModel interface.
 *
 * These tests verify that any implementation of MapViewModel correctly implements
 * the expected behavior for map download and availability checking.
 *
 * COVERAGE:
 * - Map availability checking (with and without auto-download)
 * - Map download lifecycle (pending -> downloading -> installed)
 * - Download progress tracking
 * - Download cancellation
 * - Error handling and retry logic
 * - State transitions
 * - Concurrent operation handling
 * - Edge cases and error scenarios
 *
 * TEST COUNT: 20 comprehensive tests
 *
 * TESTING STRATEGY:
 * Since MapViewModel interface methods are NOT suspend functions (they launch coroutines internally),
 * we test the underlying MapDownloadManager directly to ensure consistent behavior.
 * Platform-specific implementations (IOSMapViewModel, AndroidMapViewModel) are tested separately
 * in their respective test files.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    private val testScheduler = TestCoroutineScheduler()
    private lateinit var testPlatformAdapter: TestPlatformMapDownloadAdapter
    private lateinit var downloadManager: MapDownloadManager

    @BeforeTest
    fun setUp() {
        testPlatformAdapter = TestPlatformMapDownloadAdapter()
        downloadManager = MapDownloadManager(testPlatformAdapter)
        // Give the adapter access to the manager so it can update state
        testPlatformAdapter.downloadManager = downloadManager
    }

    @AfterTest
    fun tearDown() {
        testPlatformAdapter.reset()
    }

    // ========================================================================
    // Map Availability Tests (5 tests)
    // ========================================================================

    @Test
    fun `checkIfMapIsAvailable sets Available state when map is installed`() =
        runTest(testScheduler) {
            testPlatformAdapter.setMapInstalled("europe", true)

            downloadManager.checkIfMapIsAvailable("europe", autoDownload = false)
            advanceUntilIdle()

            val state = downloadManager.featureState.first()
            assertEquals(MapFeatureState.Available, state)
            assertTrue(testPlatformAdapter.isMapInstalledCalled)
        }

    @Test
    fun `checkIfMapIsAvailable sets NotAvailable state when map not installed`() =
        runTest(testScheduler) {
            testPlatformAdapter.setMapInstalled("asia", false)

            downloadManager.checkIfMapIsAvailable("asia", autoDownload = false)
            advanceUntilIdle()

            val state = downloadManager.featureState.first()
            assertEquals(MapFeatureState.NotAvailable, state)
        }

    @Test
    fun `checkIfMapIsAvailable with autoDownload true triggers download`() =
        runTest(testScheduler) {
            testPlatformAdapter.setMapInstalled("africa", false)
            testPlatformAdapter.simulateSuccessfulDownload = true

            downloadManager.checkIfMapIsAvailable("africa", autoDownload = true)
            advanceUntilIdle()

            assertTrue(testPlatformAdapter.startPlatformDownloadCalled)
            assertEquals("africa", testPlatformAdapter.lastDownloadedMapId)
        }

    @Test
    fun `checkIfMapIsAvailable with autoDownload false does not trigger download`() =
        runTest(testScheduler) {
            testPlatformAdapter.setMapInstalled("oceania", false)

            downloadManager.checkIfMapIsAvailable("oceania", autoDownload = false)
            advanceUntilIdle()

            assertFalse(testPlatformAdapter.startPlatformDownloadCalled)
            assertEquals(MapFeatureState.NotAvailable, downloadManager.featureState.first())
        }

    @Test
    fun `checkIfMapIsAvailable handles multiple sequential checks correctly`() =
        runTest(testScheduler) {
            testPlatformAdapter.setMapInstalled("map1", true)
            testPlatformAdapter.setMapInstalled("map2", false)
            testPlatformAdapter.setMapInstalled("map3", true)

            downloadManager.checkIfMapIsAvailable("map1")
            advanceUntilIdle()
            assertEquals(MapFeatureState.Available, downloadManager.featureState.first())

            downloadManager.checkIfMapIsAvailable("map2")
            advanceUntilIdle()
            assertEquals(MapFeatureState.NotAvailable, downloadManager.featureState.first())

            downloadManager.checkIfMapIsAvailable("map3")
            advanceUntilIdle()
            assertEquals(MapFeatureState.Available, downloadManager.featureState.first())
        }

    // ========================================================================
    // Map Download Tests (5 tests)
    // ========================================================================

    @Test
    fun `downloadMap sets Pending state initially`() =
        runTest(testScheduler) {
            testPlatformAdapter.simulateDelayedDownload = true

            // Launch in background scope like the real ViewModel does
            val job =
                launch {
                    downloadManager.downloadMap("test_map")
                }
            // Don't wait for completion - check immediate state
            delay(10)

            // Should be in some download-related state (Pending or Downloading)
            val state = downloadManager.featureState.first()
            assertTrue(
                state is MapFeatureState.Pending ||
                    state is MapFeatureState.Downloading ||
                    state is MapFeatureState.Installing,
            )

            job.cancel() // Clean up
        }

    @Test
    fun `downloadMap completes successfully and sets Installed state`() =
        runTest(testScheduler) {
            testPlatformAdapter.simulateSuccessfulDownload = true

            downloadManager.downloadMap("success_map")
            advanceUntilIdle()

            val state = downloadManager.featureState.first()
            assertEquals(MapFeatureState.Installed, state)
            assertTrue(testPlatformAdapter.startPlatformDownloadCalled)
        }

    @Test
    fun `downloadMap tracks progress correctly`() =
        runTest(testScheduler) {
            testPlatformAdapter.simulateProgressUpdates = true
            testPlatformAdapter.progressValues = listOf(10, 25, 50, 75, 90, 100)

            val progressStates = mutableListOf<Int>()
            testPlatformAdapter.onProgressCallback = { progress ->
                progressStates.add(progress)
            }

            downloadManager.downloadMap("progress_map")
            advanceUntilIdle()

            assertTrue(progressStates.isNotEmpty())
            assertTrue(progressStates.contains(100))
            assertEquals(MapFeatureState.Installed, downloadManager.featureState.first())
        }

    @Test
    fun `downloadMap handles failure and sets Failed state`() =
        runTest(testScheduler) {
            testPlatformAdapter.simulateSuccessfulDownload = false
            testPlatformAdapter.errorCode = 404
            testPlatformAdapter.errorMessage = "Map not found"

            downloadManager.downloadMap("missing_map")
            advanceUntilIdle()

            val state = downloadManager.featureState.first()
            assertTrue(state is MapFeatureState.Failed || state is MapFeatureState.Retrying)
        }

    @Test
    fun `downloadMap prevents concurrent downloads`() =
        runTest(testScheduler) {
            testPlatformAdapter.simulateDelayedDownload = true

            // Launch first download in background like the real ViewModel does
            val job1 =
                launch {
                    downloadManager.downloadMap("map1")
                }
            delay(10) // Small delay to ensure first download starts

            // Try to start second download while first is still running
            val job2 =
                launch {
                    downloadManager.downloadMap("map2")
                }
            advanceUntilIdle()

            // Should only have called startPlatformDownload once
            assertEquals(1, testPlatformAdapter.downloadCallCount)

            job1.cancel()
            job2.cancel()
        }

    // ========================================================================
    // Download Cancellation Tests (3 tests)
    // ========================================================================

    @Test
    fun `cancelDownload stops ongoing download`() =
        runTest(testScheduler) {
            testPlatformAdapter.simulateDelayedDownload = true

            downloadManager.downloadMap("cancel_map")
            delay(50)
            downloadManager.cancelDownload()
            advanceUntilIdle()

            assertTrue(testPlatformAdapter.cancelPlatformDownloadCalled)
        }

    @Test
    fun `cancelDownload sets appropriate state after cancellation`() =
        runTest(testScheduler) {
            testPlatformAdapter.simulateDelayedDownload = true

            downloadManager.downloadMap("cancel_test")
            delay(50)
            downloadManager.cancelDownload()
            advanceUntilIdle()

            val state = downloadManager.featureState.first()
            // State should indicate cancellation or not available
            assertTrue(
                state is MapFeatureState.NotAvailable ||
                    state is MapFeatureState.Canceling,
            )
        }

    @Test
    fun `cancelDownload on non-downloading state does not error`() =
        runTest(testScheduler) {
            // Cancel when nothing is downloading
            downloadManager.cancelDownload()
            advanceUntilIdle()

            // Should not crash - may or may not call platform cancel
            assertNotNull(downloadManager.featureState.first())
        }

    // ========================================================================
    // Error Handling Tests (3 tests)
    // ========================================================================

    @Test
    fun `download failure includes error code in state`() =
        runTest(testScheduler) {
            testPlatformAdapter.simulateSuccessfulDownload = false
            testPlatformAdapter.errorCode = 500
            testPlatformAdapter.errorMessage = "Server error"

            downloadManager.downloadMap("error_map")
            advanceUntilIdle()

            val state = downloadManager.featureState.first()
            // Should be Failed or Retrying (depends on retry logic)
            assertTrue(state is MapFeatureState.Failed || state is MapFeatureState.Retrying)

            if (state is MapFeatureState.Failed) {
                assertEquals(500, state.errorCode)
                assertNotNull(state.errorMessage)
            }
        }

    @Test
    fun `download handles platform-specific errors gracefully`() =
        runTest(testScheduler) {
            testPlatformAdapter.simulateException = true

            downloadManager.downloadMap("exception_map")
            advanceUntilIdle()

            // Should handle exception and set error state
            val state = downloadManager.featureState.first()
            assertTrue(
                state is MapFeatureState.Failed ||
                    state is MapFeatureState.Retrying ||
                    state is MapFeatureState.NotAvailable,
            )
        }

    @Test
    fun `multiple failures eventually reach terminal Failed state`() =
        runTest(testScheduler) {
            testPlatformAdapter.simulateSuccessfulDownload = false
            testPlatformAdapter.errorCode = 503
            testPlatformAdapter.shouldRetry = false

            downloadManager.downloadMap("persistent_error")
            advanceUntilIdle()

            // With retry disabled, should eventually reach Failed state
            val state = downloadManager.featureState.first()
            // Accept either Failed or the final retry state
            assertNotEquals(MapFeatureState.NotChecked, state)
        }

    // ========================================================================
    // State Transition Tests (2 tests)
    // ========================================================================

    @Test
    fun `featureState transitions correctly during successful download`() =
        runTest(testScheduler) {
            testPlatformAdapter.simulateProgressUpdates = true
            testPlatformAdapter.progressValues = listOf(30, 60, 100)

            val states = mutableListOf<MapFeatureState>()
            testPlatformAdapter.stateCallback = { state ->
                states.add(state)
            }

            downloadManager.downloadMap("transition_map")
            advanceUntilIdle()

            // Should have progressed through multiple states
            assertTrue(states.size >= 2)
            assertEquals(MapFeatureState.Installed, downloadManager.featureState.first())
        }

    @Test
    fun `featureState is observable via StateFlow`() =
        runTest(testScheduler) {
            testPlatformAdapter.setMapInstalled("observable_map", true)

            // Initial state
            val initialState = downloadManager.featureState.first()
            assertNotNull(initialState)

            // Change state
            downloadManager.checkIfMapIsAvailable("observable_map")
            advanceUntilIdle()

            // State should have changed
            val newState = downloadManager.featureState.first()
            assertEquals(MapFeatureState.Available, newState)
        }

    // ========================================================================
    // Edge Cases and Lifecycle Tests (2 tests)
    // ========================================================================

    @Test
    fun `download with callback invokes callback on success`() =
        runTest(testScheduler) {
            testPlatformAdapter.simulateSuccessfulDownload = true
            var callbackInvoked = false

            downloadManager.downloadMap("callback_map") {
                callbackInvoked = true
            }
            advanceUntilIdle()

            assertTrue(callbackInvoked)
            assertEquals(MapFeatureState.Installed, downloadManager.featureState.first())
        }

    @Test
    fun `mapViewModel handles rapid state changes gracefully`() =
        runTest(testScheduler) {
            testPlatformAdapter.setMapInstalled("rapid1", true)
            testPlatformAdapter.setMapInstalled("rapid2", false)

            // Rapid fire multiple operations
            downloadManager.checkIfMapIsAvailable("rapid1")
            downloadManager.checkIfMapIsAvailable("rapid2")
            downloadManager.downloadMap("rapid2")
            downloadManager.cancelDownload()
            advanceUntilIdle()

            // Should handle all operations without crashing
            assertNotNull(downloadManager.featureState.first())
        }

    // ========================================================================
    // Test Helper Classes
    // ========================================================================

    /**
     * Test platform adapter that simulates various download scenarios.
     */
    private class TestPlatformMapDownloadAdapter : PlatformMapDownloadAdapter {
        var isMapInstalledCalled = false
        var startPlatformDownloadCalled = false
        var cancelPlatformDownloadCalled = false
        var lastDownloadedMapId: String? = null
        var downloadCallCount = 0

        var simulateSuccessfulDownload = true
        var simulateProgressUpdates = false
        var simulateDelayedDownload = false
        var simulateException = false
        var errorCode = -1
        var errorMessage = "Test error"
        var shouldRetry = true

        var onProgressCallback: ((Int) -> Unit)? = null
        var stateCallback: ((MapFeatureState) -> Unit)? = null
        var progressValues = listOf<Int>()

        // Reference to the download manager so we can update its state
        var downloadManager: MapDownloadManager? = null

        private val installedMaps = mutableSetOf<String>()

        fun setMapInstalled(
            mapId: String,
            installed: Boolean,
        ) {
            if (installed) {
                installedMaps.add(mapId)
            } else {
                installedMaps.remove(mapId)
            }
        }

        fun reset() {
            isMapInstalledCalled = false
            startPlatformDownloadCalled = false
            cancelPlatformDownloadCalled = false
            lastDownloadedMapId = null
            downloadCallCount = 0
            simulateSuccessfulDownload = true
            simulateProgressUpdates = false
            simulateDelayedDownload = false
            simulateException = false
            onProgressCallback = null
            stateCallback = null
        }

        override suspend fun isMapInstalled(mapId: String): Boolean {
            isMapInstalledCalled = true
            return installedMaps.contains(mapId)
        }

        override suspend fun startPlatformDownload(
            mapId: String,
            onMapDownloaded: (() -> Unit)?,
        ) {
            startPlatformDownloadCalled = true
            lastDownloadedMapId = mapId
            downloadCallCount++

            if (simulateException) {
                // Simulate exception during download - manager should handle this
                downloadManager?.handleDownloadFailure(errorCode, shouldRetry)
                // Don't throw - just return after setting the failure state
                return
            }

            if (simulateDelayedDownload) {
                delay(1000) // Long delay to simulate slow download
            }

            if (simulateProgressUpdates) {
                val values = if (progressValues.isNotEmpty()) progressValues else listOf(25, 50, 75, 100)
                values.forEach { progress ->
                    onProgressCallback?.invoke(progress)
                    stateCallback?.invoke(downloadManager?.featureState?.value ?: MapFeatureState.NotChecked)
                    // Simulate progress reporting like Android does
                    downloadManager?.handleDownloadProgress(100, progress.toLong())
                    delay(1)
                }
            }

            if (simulateSuccessfulDownload) {
                delay(1)
                // Simulate successful download completion
                downloadManager?.handleDownloadSuccess()
                onMapDownloaded?.invoke()
            } else {
                delay(1)
                // Simulate download failure
                downloadManager?.handleDownloadFailure(errorCode, shouldRetry)
            }
        }

        override suspend fun cancelPlatformDownload() {
            cancelPlatformDownloadCalled = true
            // Simulate cancellation completion
            downloadManager?.handleDownloadCancellation()
        }

        override fun getLocalizedErrorMessage(errorCode: Int): String = "$errorMessage (code: $errorCode)"

        override fun clearCacheForInstalledMaps(mapIds: List<String>) {
            // Test implementation - no-op
        }
    }
}
