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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for IMapDownloadManager interface implementation compliance.
 * Ensures all implementations follow the expected interface contract.
 */
class IMapDownloadManagerTest {
    private class MockMapDownloadManager : IMapDownloadManager {
        private val _featureState = MutableStateFlow<MapFeatureState>(MapFeatureState.NotChecked)
        override val featureState: StateFlow<MapFeatureState> = _featureState

        var lastCheckedMapId: String? = null
        var lastDownloadedMapId: String? = null
        var cancelCalled = false

        override suspend fun checkIfMapIsAvailable(
            mapId: String,
            autoDownload: Boolean,
        ) {
            lastCheckedMapId = mapId
            _featureState.value =
                if (mapId == "available_map") {
                    MapFeatureState.Available
                } else {
                    MapFeatureState.NotAvailable
                }

            if (autoDownload && _featureState.value == MapFeatureState.NotAvailable) {
                downloadMap(mapId)
            }
        }

        override suspend fun downloadMap(
            mapId: String,
            onMapDownloaded: (() -> Unit)?,
        ) {
            lastDownloadedMapId = mapId
            _featureState.value = MapFeatureState.Downloading(0)
            onMapDownloaded?.invoke()
        }

        override suspend fun cancelDownload() {
            cancelCalled = true
            _featureState.value = MapFeatureState.NotAvailable
        }

        override fun getErrorMessage(errorCode: Int): String = "Error $errorCode"
    }

    @Test
    fun `interface contract - checkIfMapIsAvailable behavior`() =
        runTest {
            val manager = MockMapDownloadManager()

            // Test available map
            manager.checkIfMapIsAvailable("available_map")
            assertEquals("available_map", manager.lastCheckedMapId)
            assertEquals(MapFeatureState.Available, manager.featureState.value)

            // Test unavailable map
            manager.checkIfMapIsAvailable("unavailable_map")
            assertEquals("unavailable_map", manager.lastCheckedMapId)
            assertEquals(MapFeatureState.NotAvailable, manager.featureState.value)
        }

    @Test
    fun `interface contract - autoDownload triggers download`() =
        runTest {
            val manager = MockMapDownloadManager()

            manager.checkIfMapIsAvailable("unavailable_map", autoDownload = true)

            assertEquals("unavailable_map", manager.lastCheckedMapId)
            assertEquals("unavailable_map", manager.lastDownloadedMapId)
            assertEquals(MapFeatureState.Downloading(0), manager.featureState.value)
        }

    @Test
    fun `interface contract - downloadMap behavior`() =
        runTest {
            val manager = MockMapDownloadManager()
            var callbackCalled = false

            manager.downloadMap("test_map") {
                callbackCalled = true
            }

            assertEquals("test_map", manager.lastDownloadedMapId)
            assertEquals(MapFeatureState.Downloading(0), manager.featureState.value)
            assertTrue(callbackCalled)
        }

    @Test
    fun `interface contract - cancelDownload behavior`() =
        runTest {
            val manager = MockMapDownloadManager()

            manager.cancelDownload()

            assertTrue(manager.cancelCalled)
            assertEquals(MapFeatureState.NotAvailable, manager.featureState.value)
        }

    @Test
    fun `interface contract - getErrorMessage behavior`() {
        val manager = MockMapDownloadManager()

        val errorMessage = manager.getErrorMessage(404)

        assertEquals("Error 404", errorMessage)
    }

    @Test
    fun `interface contract - featureState is reactive`() =
        runTest {
            val manager = MockMapDownloadManager()

            // Initial state
            assertEquals(MapFeatureState.NotChecked, manager.featureState.value)

            // State changes should be reflected
            manager.checkIfMapIsAvailable("available_map")
            assertEquals(MapFeatureState.Available, manager.featureState.value)

            manager.downloadMap("test_map")
            assertEquals(MapFeatureState.Downloading(0), manager.featureState.value)

            manager.cancelDownload()
            assertEquals(MapFeatureState.NotAvailable, manager.featureState.value)
        }
}
