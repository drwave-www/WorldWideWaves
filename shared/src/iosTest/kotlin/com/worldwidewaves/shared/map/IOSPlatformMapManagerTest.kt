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

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * iOS-specific tests for IOSPlatformMapManager.
 *
 * These tests verify the iOS asset bundle-based map management approach.
 */
class IOSPlatformMapManagerTest {
    @Test
    fun `isMapAvailable returns false for non-existent maps`() {
        val mapManager = IOSPlatformMapManager()

        // Test with a map that definitely doesn't exist in bundle
        val isAvailable = mapManager.isMapAvailable("non_existent_city")
        assertFalse(isAvailable)
    }

    @Test
    fun `downloadMap simulates progress and completion`() =
        runTest {
            val mapManager = IOSPlatformMapManager()
            var progressUpdates = mutableListOf<Int>()
            var downloadSucceeded = false
            var downloadFailed = false

            mapManager.downloadMap(
                mapId = "test_city",
                onProgress = { progress ->
                    progressUpdates.add(progress)
                },
                onSuccess = {
                    downloadSucceeded = true
                },
                onError = { _, _ ->
                    downloadFailed = true
                },
            )

            // Should have received progress updates
            assertTrue(progressUpdates.isNotEmpty())

            // Progress should start from 0 and reach 100
            assertTrue(progressUpdates.contains(0))
            assertTrue(progressUpdates.contains(100))

            // Since test_city doesn't exist in bundle, it should fail
            assertFalse(downloadSucceeded)
            assertTrue(downloadFailed)
        }

    @Test
    fun `cancelDownload is handled gracefully`() {
        val mapManager = IOSPlatformMapManager()

        // Should not throw exception even if no download is active
        mapManager.cancelDownload("test_city")
    }

    @Test
    fun `downloadMap handles bundle verification correctly`() =
        runTest {
            val mapManager = IOSPlatformMapManager()
            var errorReceived = false
            var errorMessage: String? = null

            mapManager.downloadMap(
                mapId = "definitely_missing_map",
                onProgress = { },
                onSuccess = { },
                onError = { _, message ->
                    errorReceived = true
                    errorMessage = message
                },
            )

            // Should receive error for missing bundle
            assertTrue(errorReceived)
            assertNotNull(errorMessage)
            assertTrue(errorMessage!!.contains("bundle"))
        }
}
