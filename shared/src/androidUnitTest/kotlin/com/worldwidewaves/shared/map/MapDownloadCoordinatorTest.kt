package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapDownloadCoordinatorTest {
    private lateinit var mockPlatformManager: MockPlatformMapManager
    private lateinit var coordinator: MapDownloadCoordinator

    @BeforeTest
    fun setup() {
        mockPlatformManager = MockPlatformMapManager()
        coordinator = MapDownloadCoordinator(mockPlatformManager)
    }

    @Test
    fun checkAvailability_updatesState_whenMapAvailable() =
        runTest {
            // Given
            mockPlatformManager.availableMaps.add("paris_france")

            // When
            coordinator.checkAvailability("paris_france")

            // Then
            val state = coordinator.getDownloadState("paris_france").first()
            assertTrue(state.isAvailable)
            assertFalse(state.isDownloading)
            assertNull(state.error)
        }

    @Test
    fun checkAvailability_updatesState_whenMapNotAvailable() =
        runTest {
            // Given - mockPlatformManager has no available maps

            // When
            coordinator.checkAvailability("paris_france")

            // Then
            val state = coordinator.getDownloadState("paris_france").first()
            assertFalse(state.isAvailable)
            assertFalse(state.isDownloading)
            assertNull(state.error)
        }

    @Test
    fun downloadMap_tracksProgressAndCompletesSuccessfully() =
        runTest {
            // Given
            val mapId = "paris_france"
            mockPlatformManager.shouldSucceed = true

            // When
            coordinator.downloadMap(mapId)

            // Then - wait for download to complete
            delay(500)
            val finalState = coordinator.getDownloadState(mapId).first()

            assertTrue(finalState.isAvailable)
            assertFalse(finalState.isDownloading)
            assertEquals(100, finalState.progress)
            assertNull(finalState.error)
        }

    @Test
    fun downloadMap_handlesErrorsCorrectly() =
        runTest {
            // Given
            val mapId = "invalid_map"
            mockPlatformManager.shouldSucceed = false
            mockPlatformManager.errorMessage = "Network error"

            // When
            coordinator.downloadMap(mapId)

            // Then
            delay(500)
            val finalState = coordinator.getDownloadState(mapId).first()

            assertFalse(finalState.isAvailable)
            assertFalse(finalState.isDownloading)
            assertNotNull(finalState.error)
            assertTrue(finalState.error!!.contains("Network error"))
        }

    @Test
    fun autoDownloadIfNeeded_triggersDownload_whenNotAvailableAndEnabled() =
        runTest {
            // Given
            val mapId = "paris_france"
            mockPlatformManager.shouldSucceed = true

            // When
            coordinator.autoDownloadIfNeeded(mapId, autoDownload = true)

            // Then - download should be triggered
            delay(500)
            val state = coordinator.getDownloadState(mapId).first()
            // Either still downloading or completed
            assertTrue(state.isDownloading || state.isAvailable)
        }

    @Test
    fun autoDownloadIfNeeded_doesNotDownload_whenAutoDownloadDisabled() =
        runTest {
            // Given
            val mapId = "paris_france"

            // When
            coordinator.autoDownloadIfNeeded(mapId, autoDownload = false)

            // Then - should only check availability, not download
            val state = coordinator.getDownloadState(mapId).first()
            assertFalse(state.isDownloading)
            assertFalse(state.isAvailable) // Not available and didn't download
        }

    @Test
    fun autoDownloadIfNeeded_doesNotDownload_whenAlreadyAvailable() =
        runTest {
            // Given
            val mapId = "paris_france"
            mockPlatformManager.availableMaps.add(mapId)

            // When
            coordinator.autoDownloadIfNeeded(mapId, autoDownload = true)

            // Then - should not trigger download since already available
            val state = coordinator.getDownloadState(mapId).first()
            assertTrue(state.isAvailable)
            assertFalse(state.isDownloading)
        }

    @Test
    fun cancelDownload_stopsDownload() =
        runTest {
            // Given
            val mapId = "paris_france"
            mockPlatformManager.downloadDelay = 5000 // Long delay to allow cancel

            // When
            coordinator.downloadMap(mapId)
            coordinator.cancelDownload(mapId)

            // Then
            val state = coordinator.getDownloadState(mapId).first()
            assertFalse(state.isDownloading)
            assertTrue(mockPlatformManager.cancelledMaps.contains(mapId))
        }

    @Test
    fun multipleMaps_canBeTrackedIndependently() =
        runTest {
            // Given
            val map1 = "paris_france"
            val map2 = "tokyo_japan"
            mockPlatformManager.availableMaps.add(map1)

            // When
            coordinator.checkAvailability(map1)
            coordinator.checkAvailability(map2)

            // Then
            assertTrue(coordinator.getDownloadState(map1).first().isAvailable)
            assertFalse(coordinator.getDownloadState(map2).first().isAvailable)
        }

    // Mock PlatformMapManager for testing
    private class MockPlatformMapManager : PlatformMapManager {
        val availableMaps = mutableSetOf<String>()
        val cancelledMaps = mutableSetOf<String>()
        var shouldSucceed = true
        var errorMessage = "Download failed"
        var downloadDelay = 100L

        override fun isMapAvailable(mapId: String): Boolean = availableMaps.contains(mapId)

        override suspend fun downloadMap(
            mapId: String,
            onProgress: (Int) -> Unit,
            onSuccess: () -> Unit,
            onError: (code: Int, message: String?) -> Unit,
        ) {
            onProgress(0)
            delay(downloadDelay)
            onProgress(50)
            delay(downloadDelay)
            onProgress(100)

            if (shouldSucceed) {
                availableMaps.add(mapId)
                onSuccess()
            } else {
                onError(-1, errorMessage)
            }
        }

        override fun cancelDownload(mapId: String) {
            cancelledMaps.add(mapId)
        }
    }
}
