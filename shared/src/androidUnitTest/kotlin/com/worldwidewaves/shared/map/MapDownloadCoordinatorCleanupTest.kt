package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for MapDownloadCoordinator cleanup functionality to prevent memory leaks.
 */
class MapDownloadCoordinatorCleanupTest {
    @Test
    fun `should remove completed downloads from cache`() =
        runTest {
            // Given
            val platformMapManager = mockk<PlatformMapManager>(relaxed = true)
            coEvery { platformMapManager.isMapAvailable("completed-map-1") } returns true
            coEvery { platformMapManager.isMapAvailable("completed-map-2") } returns true

            val coordinator = MapDownloadCoordinator(platformMapManager)

            // Simulate completed downloads
            coEvery { platformMapManager.downloadMap(any(), any(), any(), any()) } answers {
                val onSuccess = thirdArg<() -> Unit>()
                onSuccess()
            }

            coordinator.downloadMap("completed-map-1")
            coordinator.downloadMap("completed-map-2")

            // Verify downloads are tracked
            val countBefore = coordinator.getTrackedDownloadCount()
            assertEquals(2, countBefore, "Should track 2 downloads before cleanup")

            // When
            coordinator.clearCompletedDownloads()

            // Then
            val countAfter = coordinator.getTrackedDownloadCount()
            assertEquals(0, countAfter, "Should remove completed downloads from cache")
        }

    @Test
    fun `should preserve active downloads`() =
        runTest {
            // Given
            val platformMapManager = mockk<PlatformMapManager>(relaxed = true)
            coEvery { platformMapManager.isMapAvailable(any()) } returns false

            val coordinator = MapDownloadCoordinator(platformMapManager)

            // Simulate active download (never completes)
            coEvery { platformMapManager.downloadMap(eq("active-map"), any(), any(), any()) } answers {
                val onProgress = secondArg<(Int) -> Unit>()
                onProgress(50) // 50% progress, still downloading
            }

            // Simulate completed download
            coEvery { platformMapManager.downloadMap(eq("completed-map"), any(), any(), any()) } answers {
                val onSuccess = thirdArg<() -> Unit>()
                onSuccess()
            }

            coordinator.downloadMap("active-map")
            coordinator.downloadMap("completed-map")

            val countBefore = coordinator.getTrackedDownloadCount()
            assertEquals(2, countBefore, "Should track 2 downloads before cleanup")

            // When
            coordinator.clearCompletedDownloads()

            // Then
            val countAfter = coordinator.getTrackedDownloadCount()
            assertEquals(1, countAfter, "Should preserve active download")

            // Verify the active download is still tracked
            val activeState = coordinator.getDownloadState("active-map").value
            assertTrue(activeState.isDownloading, "Active download should still be marked as downloading")
        }

    @Test
    fun `should preserve failed downloads`() =
        runTest {
            // Given
            val platformMapManager = mockk<PlatformMapManager>(relaxed = true)
            coEvery { platformMapManager.isMapAvailable(any()) } returns false

            val coordinator = MapDownloadCoordinator(platformMapManager)

            // Simulate failed download
            coEvery { platformMapManager.downloadMap(eq("failed-map"), any(), any(), any()) } answers {
                val onError = lastArg<(Int, String?) -> Unit>()
                onError(500, "Download failed")
            }

            // Simulate completed download
            coEvery { platformMapManager.downloadMap(eq("completed-map"), any(), any(), any()) } answers {
                val onSuccess = thirdArg<() -> Unit>()
                onSuccess()
            }

            coordinator.downloadMap("failed-map")
            coordinator.downloadMap("completed-map")

            val countBefore = coordinator.getTrackedDownloadCount()
            assertEquals(2, countBefore, "Should track 2 downloads before cleanup")

            // When
            coordinator.clearCompletedDownloads()

            // Then
            val countAfter = coordinator.getTrackedDownloadCount()
            assertEquals(1, countAfter, "Should preserve failed download")

            // Verify the failed download is still tracked
            val failedState = coordinator.getDownloadState("failed-map").value
            assertEquals("Download failed", failedState.error, "Failed download should still have error")
        }

    @Test
    fun `should handle empty cache gracefully`() =
        runTest {
            // Given
            val platformMapManager = mockk<PlatformMapManager>(relaxed = true)
            val coordinator = MapDownloadCoordinator(platformMapManager)

            // When
            coordinator.clearCompletedDownloads()

            // Then - Should not throw exception
            val count = coordinator.getTrackedDownloadCount()
            assertEquals(0, count, "Empty cache should remain empty")
        }

    @Test
    fun `should handle multiple cleanup calls`() =
        runTest {
            // Given
            val platformMapManager = mockk<PlatformMapManager>(relaxed = true)
            coEvery { platformMapManager.isMapAvailable(any()) } returns true

            val coordinator = MapDownloadCoordinator(platformMapManager)

            // Simulate completed download
            coEvery { platformMapManager.downloadMap(any(), any(), any(), any()) } answers {
                val onSuccess = thirdArg<() -> Unit>()
                onSuccess()
            }

            coordinator.downloadMap("test-map")

            // When - Call cleanup multiple times
            coordinator.clearCompletedDownloads()
            coordinator.clearCompletedDownloads()
            coordinator.clearCompletedDownloads()

            // Then - Should not throw exception
            val count = coordinator.getTrackedDownloadCount()
            assertEquals(0, count, "Cache should remain empty after multiple cleanups")
        }

    @Test
    fun `should clear only completed downloads in mixed state`() =
        runTest {
            // Given
            val platformMapManager = mockk<PlatformMapManager>(relaxed = true)
            coEvery { platformMapManager.isMapAvailable(any()) } returns false

            val coordinator = MapDownloadCoordinator(platformMapManager)

            // Simulate completed download
            coEvery { platformMapManager.downloadMap(eq("completed-1"), any(), any(), any()) } answers {
                val onSuccess = thirdArg<() -> Unit>()
                onSuccess()
            }

            // Simulate active download
            coEvery { platformMapManager.downloadMap(eq("active-1"), any(), any(), any()) } answers {
                val onProgress = secondArg<(Int) -> Unit>()
                onProgress(30)
            }

            // Simulate failed download
            coEvery { platformMapManager.downloadMap(eq("failed-1"), any(), any(), any()) } answers {
                val onError = lastArg<(Int, String?) -> Unit>()
                onError(404, "Not found")
            }

            // Simulate another completed download
            coEvery { platformMapManager.downloadMap(eq("completed-2"), any(), any(), any()) } answers {
                val onSuccess = thirdArg<() -> Unit>()
                onSuccess()
            }

            coordinator.downloadMap("completed-1")
            coordinator.downloadMap("active-1")
            coordinator.downloadMap("failed-1")
            coordinator.downloadMap("completed-2")

            val countBefore = coordinator.getTrackedDownloadCount()
            assertEquals(4, countBefore, "Should track 4 downloads before cleanup")

            // When
            coordinator.clearCompletedDownloads()

            // Then
            val countAfter = coordinator.getTrackedDownloadCount()
            assertEquals(2, countAfter, "Should preserve active and failed downloads only")

            // Verify active download is preserved
            val activeState = coordinator.getDownloadState("active-1").value
            assertTrue(activeState.isDownloading, "Active download should be preserved")

            // Verify failed download is preserved
            val failedState = coordinator.getDownloadState("failed-1").value
            assertEquals("Not found", failedState.error, "Failed download should be preserved")
        }
}
