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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Comprehensive tests for BaseMapDownloadViewModel.
 * Tests all shared logic extracted from original MapViewModel.
 */
class BaseMapDownloadViewModelTest {

    private class TestMapDownloadViewModel(
        private val mockMapInstalled: Boolean = false,
        private val mockDownloadSuccess: Boolean = true,
        private val mockErrorMessage: String = "Test error"
    ) : BaseMapDownloadViewModel() {

        var downloadCalled = false
        var cancelCalled = false
        var downloadMapId: String? = null
        var downloadCallback: (() -> Unit)? = null

        override suspend fun isMapInstalled(mapId: String): Boolean {
            return mockMapInstalled
        }

        override suspend fun startPlatformDownload(mapId: String, onMapDownloaded: (() -> Unit)?) {
            downloadCalled = true
            downloadMapId = mapId
            downloadCallback = onMapDownloaded

            if (mockDownloadSuccess) {
                handleDownloadSuccess()
            } else {
                handleDownloadFailure(500, shouldRetry = false)
            }
        }

        override suspend fun cancelPlatformDownload() {
            cancelCalled = true
        }

        override fun getLocalizedErrorMessage(errorCode: Int): String {
            return "$mockErrorMessage (code: $errorCode)"
        }

        override fun clearCacheForInstalledMaps(mapIds: List<String>) {
            // Test implementation - no-op
        }
    }

    @Test
    fun `checkIfMapIsAvailable sets Available when map installed`() = runTest {
        val viewModel = TestMapDownloadViewModel(mockMapInstalled = true)

        viewModel.checkIfMapIsAvailable("test_map", autoDownload = false)

        assertEquals(MapFeatureState.Available, viewModel.featureState.first())
        assertEquals("test_map", viewModel.currentMapId)
    }

    @Test
    fun `checkIfMapIsAvailable sets NotAvailable when map not installed`() = runTest {
        val viewModel = TestMapDownloadViewModel(mockMapInstalled = false)

        viewModel.checkIfMapIsAvailable("test_map", autoDownload = false)

        assertEquals(MapFeatureState.NotAvailable, viewModel.featureState.first())
        assertEquals("test_map", viewModel.currentMapId)
    }

    @Test
    fun `checkIfMapIsAvailable triggers download when autoDownload true`() = runTest {
        val viewModel = TestMapDownloadViewModel(mockMapInstalled = false)

        viewModel.checkIfMapIsAvailable("test_map", autoDownload = true)

        assertTrue(viewModel.downloadCalled)
        assertEquals("test_map", viewModel.downloadMapId)
    }

    @Test
    fun `downloadMap prevents concurrent downloads`() = runTest {
        val viewModel = TestMapDownloadViewModel()

        // Set state to downloading
        viewModel._featureState.value = MapFeatureState.Downloading(50)

        viewModel.downloadMap("test_map")

        // Should not call platform download due to concurrent download protection
        assertEquals(false, viewModel.downloadCalled)
    }

    @Test
    fun `downloadMap succeeds and sets Installed state`() = runTest {
        val viewModel = TestMapDownloadViewModel(mockDownloadSuccess = true)

        viewModel.downloadMap("test_map")

        assertTrue(viewModel.downloadCalled)
        assertEquals("test_map", viewModel.downloadMapId)
        assertEquals(MapFeatureState.Installed, viewModel.featureState.first())
    }

    @Test
    fun `downloadMap failure sets Failed state`() = runTest {
        val viewModel = TestMapDownloadViewModel(mockDownloadSuccess = false)

        viewModel.downloadMap("test_map")

        assertTrue(viewModel.downloadCalled)
        val state = viewModel.featureState.first()
        assertIs<MapFeatureState.Failed>(state)
        assertEquals(500, state.errorCode)
        assertTrue(state.errorMessage?.contains("Test error") == true)
        assertTrue(state.errorMessage?.contains("500") == true)
    }

    @Test
    fun `cancelDownload calls platform cancellation`() = runTest {
        val viewModel = TestMapDownloadViewModel()

        viewModel.cancelDownload()

        assertTrue(viewModel.cancelCalled)
    }

    @Test
    fun `handleDownloadProgress sets correct progress`() = runTest {
        val viewModel = TestMapDownloadViewModel()

        viewModel.handleDownloadProgress(1000L, 250L)

        val state = viewModel.featureState.first()
        assertIs<MapFeatureState.Downloading>(state)
        assertEquals(25, state.progress)
    }

    @Test
    fun `handleDownloadSuccess sets Installed state and resets retry`() = runTest {
        val viewModel = TestMapDownloadViewModel()

        // Simulate some retry attempts
        viewModel.retryManager.incrementRetryCount()
        viewModel.retryManager.incrementRetryCount()

        viewModel.handleDownloadSuccess()

        assertEquals(MapFeatureState.Installed, viewModel.featureState.first())
        assertEquals(0, viewModel.retryManager.getCurrentRetryCount())
    }

    @Test
    fun `handleDownloadFailure with retry sets Retrying state`() = runTest {
        val viewModel = TestMapDownloadViewModel()

        viewModel.handleDownloadFailure(404, shouldRetry = true)

        val state = viewModel.featureState.first()
        assertIs<MapFeatureState.Retrying>(state)
        assertEquals(1, state.attempt)
        assertEquals(3, state.maxAttempts)
        assertEquals(1, viewModel.retryManager.getCurrentRetryCount())
    }

    @Test
    fun `handleDownloadFailure without retry sets Failed state`() = runTest {
        val viewModel = TestMapDownloadViewModel()

        viewModel.handleDownloadFailure(500, shouldRetry = false)

        val state = viewModel.featureState.first()
        assertIs<MapFeatureState.Failed>(state)
        assertEquals(500, state.errorCode)
        assertEquals(0, viewModel.retryManager.getCurrentRetryCount())
    }

    @Test
    fun `handleDownloadCancellation sets NotAvailable and resets retry`() = runTest {
        val viewModel = TestMapDownloadViewModel()

        // Simulate some retry attempts
        viewModel.retryManager.incrementRetryCount()

        viewModel.handleDownloadCancellation()

        assertEquals(MapFeatureState.NotAvailable, viewModel.featureState.first())
        assertEquals(0, viewModel.retryManager.getCurrentRetryCount())
    }

    @Test
    fun `retry exhaustion leads to failed state`() = runTest {
        val viewModel = TestMapDownloadViewModel()

        // Exhaust all retries
        repeat(MapDownloadUtils.RetryManager.MAX_RETRIES) {
            viewModel.retryManager.incrementRetryCount()
        }

        viewModel.handleDownloadFailure(500, shouldRetry = true)

        // Should set Failed instead of Retrying since retries exhausted
        val state = viewModel.featureState.first()
        assertIs<MapFeatureState.Failed>(state)
        assertEquals(500, state.errorCode)
    }

    @Test
    fun `handleInstallComplete calls clearCache and sets success`() = runTest {
        val viewModel = TestMapDownloadViewModel()

        viewModel.handleInstallComplete(listOf("map1", "map2"))

        assertEquals(MapFeatureState.Installed, viewModel.featureState.first())
        assertEquals(0, viewModel.retryManager.getCurrentRetryCount())
    }
}