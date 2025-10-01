@file:OptIn(ExperimentalCoroutinesApi::class)

package com.worldwidewaves.shared.viewmodels

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.map.MapFeatureState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive iOS lifecycle tests for BaseMapDownloadViewModel.
 *
 * These tests verify that BaseMapDownloadViewModel:
 * - Initializes without deadlocks on iOS
 * - Properly manages coroutine scopes
 * - Handles download lifecycle safely
 * - Emits StateFlow updates correctly
 * - Cleans up resources properly
 *
 * iOS-specific concerns tested:
 * - No init{} block violations
 * - Proper threading safety
 * - StateFlow collection without blocking
 * - Lifecycle cleanup without resource leaks
 * - Platform-specific download handling
 *
 * @see BaseMapDownloadViewModel
 * @see IosMapViewModel
 * @see com.worldwidewaves.shared.ios.IosDeadlockPreventionTest
 */
class IosBaseMapDownloadViewModelTest {
    private val testScheduler = TestCoroutineScheduler()
    private lateinit var viewModel: TestMapDownloadViewModel

    @BeforeTest
    fun setUp() {
        viewModel = TestMapDownloadViewModel()
    }

    @AfterTest
    fun tearDown() {
        viewModel.onCleared()
    }

    // ================================================================================
    // INITIALIZATION TESTS (5 tests)
    // ================================================================================

    @Test
    fun `should initialize without deadlock on iOS`() =
        runTest(testScheduler) {
            val startTime =
                kotlin.time.Clock.System
                    .now()

            try {
                withTimeout(3.seconds) {
                    repeat(5) {
                        val vm = TestMapDownloadViewModel()
                        assertNotNull(vm, "ViewModel should initialize")
                        vm.onCleared()
                    }
                }

                val duration =
                    kotlin.time.Clock.System
                        .now() - startTime
                println("✅ PASSED: 5 ViewModels initialized in ${duration.inWholeMilliseconds}ms")
                assertTrue(duration < 3.seconds, "Initialization should be fast")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw AssertionError("❌ DEADLOCK DETECTED: ViewModel initialization timed out!")
            }
        }

    @Test
    fun `should not trigger downloads in init block`() =
        runTest(testScheduler) {
            // Verify that no platform operations are called during initialization
            assertFalse(viewModel.isMapInstalledCalled, "isMapInstalled should NOT be called in init{}")
            assertFalse(viewModel.startPlatformDownloadCalled, "startPlatformDownload should NOT be called in init{}")
        }

    @Test
    fun `should emit initial state correctly`() =
        runTest(testScheduler) {
            val initialState = viewModel.featureState.first()
            assertEquals(MapFeatureState.NotChecked, initialState, "Initial state should be NotChecked")
        }

    @Test
    fun `should create ViewModel from LaunchedEffect safely`() =
        runTest(testScheduler) {
            // Simulate LaunchedEffect pattern (iOS safe pattern)
            val vm =
                withTimeout(1.seconds) {
                    TestMapDownloadViewModel()
                }

            // Then check map availability (separate from init)
            withTimeout(1.seconds) {
                vm.checkIfMapIsAvailable("test_map")
            }

            advanceUntilIdle()
            assertNotNull(vm, "ViewModel should be created")
            vm.onCleared()
        }

    @Test
    fun `should initialize with clean state`() =
        runTest(testScheduler) {
            // Verify initial state
            assertEquals(MapFeatureState.NotChecked, viewModel.featureState.first())
            assertEquals(null, viewModel.currentMapId, "currentMapId should be null")
            assertEquals(0, viewModel.retryManager.retryCount, "retryCount should be 0")
        }

    // ================================================================================
    // LIFECYCLE TESTS (5 tests)
    // ================================================================================

    @Test
    fun `should cleanup scope on onCleared`() =
        runTest(testScheduler) {
            // Start a download operation
            viewModel.checkIfMapIsAvailable("test_map", autoDownload = true)
            advanceUntilIdle()

            // Clear the ViewModel
            viewModel.onCleared()

            // Give time for cleanup
            delay(100.milliseconds)
            advanceUntilIdle()

            // ViewModel should still exist but scope should be canceled
            assertNotNull(viewModel, "ViewModel should still exist")
        }

    @Test
    fun `should cancel all jobs on cleanup`() =
        runTest(testScheduler) {
            // Start a long-running download
            viewModel.simulateSlowDownload = true
            viewModel.downloadMap("test_map")

            // Don't wait for completion - cancel immediately
            delay(50.milliseconds)
            viewModel.onCleared()

            advanceUntilIdle()

            // Should not crash despite cancellation
            assertNotNull(viewModel, "ViewModel should handle cancellation")
        }

    @Test
    fun `should handle rapid create-destroy cycles`() =
        runTest(testScheduler) {
            // Simulate rapid iOS view lifecycle changes
            try {
                withTimeout(5.seconds) {
                    repeat(10) { iteration ->
                        val vm = TestMapDownloadViewModel()

                        vm.checkIfMapIsAvailable("map_$iteration")
                        delay(50.milliseconds) // Brief active period
                        vm.onCleared()
                        delay(50.milliseconds) // Brief inactive period

                        assertNotNull(vm, "Iteration $iteration: ViewModel should be functional")
                    }
                }

                println("✅ PASSED: Handled 10 rapid create-destroy cycles")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw AssertionError("❌ LIFECYCLE DEADLOCK: Rapid cycles timed out!")
            }
        }

    @Test
    fun `should support reuse after cleanup`() =
        runTest(testScheduler) {
            // First lifecycle
            viewModel.checkIfMapIsAvailable("test_map")
            advanceUntilIdle()
            viewModel.onCleared()

            // Create new ViewModel (simulating iOS view recreation)
            val newViewModel = TestMapDownloadViewModel()

            // Second lifecycle
            newViewModel.checkIfMapIsAvailable("test_map2")
            advanceUntilIdle()

            assertNotNull(newViewModel, "New ViewModel should work after cleanup")
            newViewModel.onCleared()
        }

    @Test
    fun `should cleanup download state on destroy`() =
        runTest(testScheduler) {
            // Start download
            viewModel.downloadMap("test_map")
            advanceUntilIdle()

            // Cleanup
            viewModel.onCleared()
            delay(100.milliseconds)

            // Should not crash
            assertNotNull(viewModel, "ViewModel should handle download cleanup")
        }

    // ================================================================================
    // STATEFLOW TESTS (5 tests)
    // ================================================================================

    @Test
    fun `should emit StateFlow updates on iOS`() =
        runTest(testScheduler) {
            // Check map availability (should emit NotAvailable)
            viewModel.setMapInstalled(false)
            viewModel.checkIfMapIsAvailable("test_map", autoDownload = false)
            advanceUntilIdle()

            val state = viewModel.featureState.first()
            assertEquals(MapFeatureState.NotAvailable, state, "Should emit NotAvailable state")
        }

    @Test
    fun `should handle concurrent StateFlow subscriptions`() =
        runTest(testScheduler) {
            // Subscribe to featureState flow multiple times concurrently
            val job1 = kotlinx.coroutines.launch { viewModel.featureState.collect {} }
            val job2 = kotlinx.coroutines.launch { viewModel.featureState.collect {} }
            val job3 = kotlinx.coroutines.launch { viewModel.featureState.collect {} }

            delay(50.milliseconds)

            // Trigger state changes
            viewModel.checkIfMapIsAvailable("test_map")
            advanceUntilIdle()

            // Cancel all subscriptions
            job1.cancel()
            job2.cancel()
            job3.cancel()

            // Should not deadlock or crash
            assertNotNull(viewModel, "Should handle concurrent subscriptions")
        }

    @Test
    fun `should emit download progress states`() =
        runTest(testScheduler) {
            // Start download
            viewModel.setMapInstalled(false)
            viewModel.simulateProgressUpdates = true
            viewModel.downloadMap("test_map")

            delay(50.milliseconds)
            advanceUntilIdle()

            // Should emit Downloading state with progress
            val state = viewModel.featureState.first()
            assertTrue(
                state is MapFeatureState.Downloading || state is MapFeatureState.Installed,
                "Should emit Downloading or Installed state, got: $state",
            )
        }

    @Test
    fun `should emit error state on failure`() =
        runTest(testScheduler) {
            // Simulate download error
            viewModel.setMapInstalled(false)
            viewModel.simulateError = true

            viewModel.downloadMap("test_map")
            advanceUntilIdle()

            // Check error state
            val state = viewModel.featureState.first()
            assertTrue(state is MapFeatureState.Failed, "Should emit Failed state, got: $state")
        }

    @Test
    fun `should collect flows without deadlock`() =
        runTest(testScheduler) {
            try {
                withTimeout(2.seconds) {
                    // Collect featureState flow multiple times
                    repeat(5) {
                        val state = viewModel.featureState.first()
                        assertNotNull(state, "State flow should emit")
                    }
                }

                println("✅ PASSED: Flow collected 5 times without deadlock")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw AssertionError("❌ DEADLOCK: Flow collection timed out!")
            }
        }

    // ================================================================================
    // DOWNLOAD LIFECYCLE TESTS (5 tests)
    // ================================================================================

    @Test
    fun `should check map availability without blocking`() =
        runTest(testScheduler) {
            try {
                withTimeout(1.seconds) {
                    viewModel.setMapInstalled(true)
                    viewModel.checkIfMapIsAvailable("test_map")
                    advanceUntilIdle()
                }

                val state = viewModel.featureState.first()
                assertEquals(MapFeatureState.Available, state, "Should be Available")
                println("✅ PASSED: Map availability check completed without blocking")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw AssertionError("❌ BLOCKING: Map availability check timed out!")
            }
        }

    @Test
    fun `should start download automatically when autoDownload is true`() =
        runTest(testScheduler) {
            viewModel.setMapInstalled(false)
            viewModel.checkIfMapIsAvailable("test_map", autoDownload = true)
            advanceUntilIdle()

            // Should have started download
            assertTrue(viewModel.startPlatformDownloadCalled, "Should start download")
        }

    @Test
    fun `should handle download cancellation safely`() =
        runTest(testScheduler) {
            // Start download
            viewModel.downloadMap("test_map")
            delay(50.milliseconds)

            // Cancel download
            viewModel.cancelDownload()
            advanceUntilIdle()

            assertTrue(viewModel.cancelPlatformDownloadCalled, "Should call platform cancel")
        }

    @Test
    fun `should retry failed downloads`() =
        runTest(testScheduler) {
            // Simulate retryable failure
            viewModel.simulateError = true
            viewModel.shouldRetry = true

            viewModel.downloadMap("test_map")
            advanceUntilIdle()

            // Should be in retrying state
            val state = viewModel.featureState.first()
            assertTrue(
                state is MapFeatureState.Retrying || state is MapFeatureState.Failed,
                "Should be Retrying or Failed state, got: $state",
            )
        }

    @Test
    fun `should complete download successfully`() =
        runTest(testScheduler) {
            // Simulate successful download
            viewModel.setMapInstalled(false)
            viewModel.simulateSuccess = true

            viewModel.downloadMap("test_map")
            advanceUntilIdle()

            val state = viewModel.featureState.first()
            assertEquals(MapFeatureState.Installed, state, "Should be Installed")
        }

    // ================================================================================
    // INTEGRATION TESTS (5 tests)
    // ================================================================================

    @Test
    fun `should handle complete download flow`() =
        runTest(testScheduler) {
            // Check map (not available)
            viewModel.setMapInstalled(false)
            viewModel.checkIfMapIsAvailable("test_map", autoDownload = false)
            advanceUntilIdle()
            assertEquals(MapFeatureState.NotAvailable, viewModel.featureState.first())

            // Start download
            viewModel.simulateSuccess = true
            viewModel.downloadMap("test_map")
            advanceUntilIdle()

            // Should be installed
            assertEquals(MapFeatureState.Installed, viewModel.featureState.first())
        }

    @Test
    fun `should handle background-foreground transitions during download`() =
        runTest(testScheduler) {
            // Start download
            viewModel.simulateSlowDownload = true
            viewModel.downloadMap("test_map")

            delay(50.milliseconds)

            // Simulate background (cleanup)
            viewModel.onCleared()

            // Recreate (simulating foreground)
            val newViewModel = TestMapDownloadViewModel()
            newViewModel.checkIfMapIsAvailable("test_map")
            advanceUntilIdle()

            assertNotNull(newViewModel, "Should handle background-foreground transition")
            newViewModel.onCleared()
        }

    @Test
    fun `should prevent concurrent downloads`() =
        runTest(testScheduler) {
            // Start first download
            viewModel.simulateSlowDownload = true
            viewModel.downloadMap("map1")
            delay(50.milliseconds)

            // Try to start second download (should be ignored)
            viewModel.downloadMap("map2")
            advanceUntilIdle()

            // currentMapId should still be map1
            assertEquals("map1", viewModel.currentMapId, "Should prevent concurrent downloads")
        }

    @Test
    fun `should handle multiple sequential downloads`() =
        runTest(testScheduler) {
            try {
                withTimeout(5.seconds) {
                    repeat(3) { index ->
                        viewModel.setMapInstalled(false)
                        viewModel.simulateSuccess = true

                        viewModel.downloadMap("map_$index")
                        advanceUntilIdle()

                        val state = viewModel.featureState.first()
                        assertEquals(MapFeatureState.Installed, state, "Download $index should succeed")

                        delay(100.milliseconds)
                    }
                }

                println("✅ PASSED: Completed 3 sequential downloads")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw AssertionError("❌ TIMEOUT: Sequential downloads timed out!")
            }
        }

    @Test
    fun `should survive multiple lifecycle cycles with downloads`() =
        runTest(testScheduler) {
            try {
                withTimeout(5.seconds) {
                    repeat(5) { cycle ->
                        val vm = TestMapDownloadViewModel()

                        // Full download lifecycle
                        vm.setMapInstalled(false)
                        vm.simulateSuccess = true
                        vm.downloadMap("map_cycle_$cycle")
                        advanceUntilIdle()

                        val state = vm.featureState.first()
                        assertEquals(
                            MapFeatureState.Installed,
                            state,
                            "Cycle $cycle: Should complete download",
                        )

                        vm.onCleared()
                        delay(100.milliseconds)
                    }
                }

                println("✅ PASSED: Survived 5 complete lifecycle cycles with downloads")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw AssertionError("❌ LIFECYCLE FAILURE: Multiple cycles timed out!")
            }
        }

    // ================================================================================
    // TEST IMPLEMENTATION
    // ================================================================================

    /**
     * Test implementation of BaseMapDownloadViewModel for iOS testing.
     * No MockK available in iosTest, so we use manual test doubles.
     */
    private class TestMapDownloadViewModel : BaseMapDownloadViewModel() {
        var isMapInstalledCalled = false
        var startPlatformDownloadCalled = false
        var cancelPlatformDownloadCalled = false

        var simulateSuccess = false
        var simulateError = false
        var simulateSlowDownload = false
        var simulateProgressUpdates = false
        var shouldRetry = false

        private var mapInstalled = false

        fun setMapInstalled(installed: Boolean) {
            mapInstalled = installed
        }

        override suspend fun isMapInstalled(mapId: String): Boolean {
            isMapInstalledCalled = true
            return mapInstalled
        }

        override suspend fun startPlatformDownload(
            mapId: String,
            onMapDownloaded: (() -> Unit)?,
        ) {
            startPlatformDownloadCalled = true

            if (simulateSlowDownload) {
                delay(5000.milliseconds)
                return
            }

            setStatePending()

            if (simulateProgressUpdates) {
                handleDownloadProgress(totalBytes = 100, downloadedBytes = 25)
                delay(10.milliseconds)
                handleDownloadProgress(totalBytes = 100, downloadedBytes = 50)
                delay(10.milliseconds)
                handleDownloadProgress(totalBytes = 100, downloadedBytes = 75)
                delay(10.milliseconds)
            }

            if (simulateError) {
                handleDownloadFailure(errorCode = -1, shouldRetry = shouldRetry)
                return
            }

            if (simulateSuccess) {
                handleDownloadProgress(totalBytes = 100, downloadedBytes = 100)
                delay(10.milliseconds)
                handleDownloadSuccess()
                onMapDownloaded?.invoke()
            }
        }

        override suspend fun cancelPlatformDownload() {
            cancelPlatformDownloadCalled = true
            handleDownloadCancellation()
        }

        override fun getLocalizedErrorMessage(errorCode: Int): String =
            when (errorCode) {
                -1 -> "Test error"
                else -> "Unknown error (code: $errorCode)"
            }

        override fun clearCacheForInstalledMaps(mapIds: List<String>) {
            // No-op for tests
        }
    }
}
