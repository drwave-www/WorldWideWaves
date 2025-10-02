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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for MapDownloadCoordinator core business logic.
 */
class MapDownloadCoordinatorTest {
    private lateinit var mockAdapter: TestPlatformMapDownloadAdapter
    private lateinit var manager: MapDownloadCoordinator

    @BeforeTest
    fun setUp() {
        mockAdapter = TestPlatformMapDownloadAdapter()
        manager = MapDownloadCoordinator(mockAdapter)
    }

    @Test
    fun `initial featureState is NotChecked`() =
        runTest {
            val state = manager.featureState.first()
            assertEquals(MapFeatureState.NotChecked, state)
            assertNull(manager.currentMapId)
        }

    @Test
    fun `checkIfMapIsAvailable sets Available when map installed`() =
        runTest {
            mockAdapter.setMapInstalled("test_map", true)

            manager.checkIfMapIsAvailable("test_map", autoDownload = false)

            assertEquals(MapFeatureState.Available, manager.featureState.first())
            assertEquals("test_map", manager.currentMapId)
            assertTrue(mockAdapter.isMapInstalledCalled)
            assertFalse(mockAdapter.startPlatformDownloadCalled)
        }

    @Test
    fun `downloadMap sets state to Pending and calls platform adapter`() =
        runTest {
            manager.downloadMap("test_map")

            assertEquals(MapFeatureState.Pending, manager.featureState.first())
            assertEquals("test_map", manager.currentMapId)
            assertTrue(mockAdapter.startPlatformDownloadCalled)
        }

    @Test
    fun `handleDownloadSuccess sets Installed state`() =
        runTest {
            manager.handleDownloadSuccess()
            assertEquals(MapFeatureState.Installed, manager.featureState.first())
        }

    @Test
    fun `handleDownloadFailure sets Failed state`() =
        runTest {
            mockAdapter.errorMessage = "Network error"

            manager.handleDownloadFailure(errorCode = 500, shouldRetry = false)

            val state = manager.featureState.first()
            assertTrue(state is MapFeatureState.Failed)
            assertEquals(500, (state as MapFeatureState.Failed).errorCode)
        }

    @Test
    fun `state update methods work correctly`() =
        runTest {
            manager.setStateInstalling()
            assertEquals(MapFeatureState.Installing, manager.featureState.first())

            manager.setStatePending()
            assertEquals(MapFeatureState.Pending, manager.featureState.first())

            manager.setStateCanceling()
            assertEquals(MapFeatureState.Canceling, manager.featureState.first())

            manager.setStateUnknown()
            assertEquals(MapFeatureState.Unknown, manager.featureState.first())
        }

    // Test helper class
    private class TestPlatformMapDownloadAdapter : PlatformMapDownloadAdapter {
        var isMapInstalledCalled = false
        var startPlatformDownloadCalled = false
        var errorMessage: String = "Test error"

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

        override suspend fun isMapInstalled(mapId: String): Boolean {
            isMapInstalledCalled = true
            return installedMaps.contains(mapId)
        }

        override suspend fun startPlatformDownload(
            mapId: String,
            onMapDownloaded: (() -> Unit)?,
        ) {
            startPlatformDownloadCalled = true
            onMapDownloaded?.invoke()
        }

        override suspend fun cancelPlatformDownload() {}

        override fun getLocalizedErrorMessage(errorCode: Int): String = errorMessage

        override fun clearCacheForInstalledMaps(mapIds: List<String>) {}
    }
}
