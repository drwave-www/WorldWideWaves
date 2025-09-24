package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for shared MapStateManager component.
 */
class MapStateManagerTest {

    @Test
    fun `initial state is NotChecked`() = runTest {
        val mockPlatformManager = MockPlatformMapManager()
        val mapStateManager = MapStateManager(mockPlatformManager)

        val initialState = mapStateManager.featureState.first()
        assertEquals(MapFeatureState.NotChecked, initialState)
    }

    @Test
    fun `checkMapAvailability sets Available when map exists`() = runTest {
        val mockPlatformManager = MockPlatformMapManager(availableMaps = setOf("paris_france"))
        val mapStateManager = MapStateManager(mockPlatformManager)

        mapStateManager.checkMapAvailability("paris_france")

        val state = mapStateManager.featureState.first()
        assertEquals(MapFeatureState.Available, state)

        val mapStates = mapStateManager.mapStates.first()
        assertEquals(true, mapStates["paris_france"])
    }

    @Test
    fun `checkMapAvailability sets NotAvailable when map missing`() = runTest {
        val mockPlatformManager = MockPlatformMapManager(availableMaps = emptySet())
        val mapStateManager = MapStateManager(mockPlatformManager)

        mapStateManager.checkMapAvailability("paris_france")

        val state = mapStateManager.featureState.first()
        assertEquals(MapFeatureState.NotAvailable, state)

        val mapStates = mapStateManager.mapStates.first()
        assertEquals(false, mapStates["paris_france"])
    }

    @Test
    fun `downloadMap calls platform manager and updates state`() = runTest {
        val mockPlatformManager = MockPlatformMapManager()
        val mapStateManager = MapStateManager(mockPlatformManager)

        mapStateManager.downloadMap("paris_france")

        // Verify platform manager was called
        assertTrue(mockPlatformManager.downloadCalled)
        assertEquals("paris_france", mockPlatformManager.lastDownloadedMapId)
    }

    @Test
    fun `cancelDownload calls platform manager`() = runTest {
        val mockPlatformManager = MockPlatformMapManager()
        val mapStateManager = MapStateManager(mockPlatformManager)

        // Start a download first
        mapStateManager.downloadMap("paris_france")
        mapStateManager.cancelDownload()

        assertTrue(mockPlatformManager.cancelCalled)
    }

    /**
     * Mock implementation of PlatformMapManager for testing.
     */
    private class MockPlatformMapManager(
        private val availableMaps: Set<String> = emptySet()
    ) : PlatformMapManager {

        var downloadCalled = false
        var cancelCalled = false
        var lastDownloadedMapId: String? = null

        override fun isMapAvailable(mapId: String): Boolean {
            return availableMaps.contains(mapId)
        }

        override suspend fun downloadMap(
            mapId: String,
            onProgress: (Int) -> Unit,
            onSuccess: () -> Unit,
            onError: (Int, String?) -> Unit
        ) {
            downloadCalled = true
            lastDownloadedMapId = mapId

            // Simulate successful download
            onProgress(50)
            onProgress(100)
            onSuccess()
        }

        override fun cancelDownload(mapId: String) {
            cancelCalled = true
        }
    }
}