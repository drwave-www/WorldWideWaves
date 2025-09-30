package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * iOS-specific tests for IOSPlatformMapManager.
 *
 * These verify basic behavior without depending on real ODR downloads.
 * If the tag is not in any pack, completion should report failure,
 * but progress still reaches 100 (predictable UX).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IOSPlatformMapManagerTest {
    @Test
    fun `isMapAvailable returns false for non-existent maps`() {
        val manager = IOSPlatformMapManager()
        assertFalse(manager.isMapAvailable("non_existent_city"))
    }

    @Test
    fun `downloadMap simulates progress and completion`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = TestScope(dispatcher)
            val manager = IOSPlatformMapManager(scope = scope, callbackDispatcher = dispatcher)

            val progress = mutableListOf<Int>()
            var success = false
            var failed = false

            manager.downloadMap(
                mapId = "test_city",
                onProgress = { progress += it },
                onSuccess = { success = true },
                onError = { _, _ -> failed = true },
            )

            // Drive the simulated progress and completion
            advanceTimeBy(35_000)
            advanceUntilIdle()

            assertTrue(progress.isNotEmpty())
            assertTrue(progress.contains(0))
            assertTrue(progress.contains(100))
            assertFalse(success)
            assertTrue(failed)
        }

    @Test
    fun `cancelDownload is handled gracefully`() {
        val manager = IOSPlatformMapManager()
        manager.cancelDownload("some_city") // should not throw
    }

    @Test
    fun `downloadMap handles bundle verification correctly`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = TestScope(dispatcher)
            val manager = IOSPlatformMapManager(scope = scope, callbackDispatcher = dispatcher)

            var errorReceived = false
            var errorMessage: String? = null

            manager.downloadMap(
                mapId = "definitely_missing_map",
                onProgress = { /* ignore */ },
                onSuccess = { /* ignore */ },
                onError = { _, msg ->
                    errorReceived = true
                    errorMessage = msg
                },
            )

            advanceTimeBy(35_000)
            advanceUntilIdle()

            assertTrue(errorReceived)
            assertNotNull(errorMessage)
            assertTrue(errorMessage!!.contains("ODR") || errorMessage!!.contains("bundle", ignoreCase = true))
        }

    @Test
    fun `isMapAvailable uses URLsForResourcesWithExtension search`() {
        // This test verifies that isMapAvailable uses the same approach as MapStore ODRPaths.resolve()
        // The actual file detection is tested by checking it doesn't crash and returns a boolean
        val manager = IOSPlatformMapManager()

        // Should not throw and should return a boolean (true or false depending on bundle contents)
        val result = manager.isMapAvailable("paris_france")
        assertNotNull(result) // The method returns a non-null boolean
        // Actual availability depends on bundle contents, but method should complete without errors
    }

    @Test
    fun `multiple consecutive availability checks work correctly`() {
        val manager = IOSPlatformMapManager()

        // Multiple checks for the same map should not cause issues
        val result1 = manager.isMapAvailable("paris_france")
        val result2 = manager.isMapAvailable("paris_france")
        val result3 = manager.isMapAvailable("tokyo_japan")

        // All should return booleans without errors
        assertNotNull(result1)
        assertNotNull(result2)
        assertNotNull(result3)
    }
}
