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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive tests for MapDownloadUtils.
 * Tests all shared utility functions extracted from MapViewModel.
 */
class MapDownloadUtilsTest {
    @Test
    fun `calculateProgressPercent returns correct percentage`() {
        // Test normal case
        assertEquals(50, MapDownloadUtils.calculateProgressPercent(100L, 50L))
        assertEquals(75, MapDownloadUtils.calculateProgressPercent(1000L, 750L))
        assertEquals(100, MapDownloadUtils.calculateProgressPercent(500L, 500L))

        // Test edge cases
        assertEquals(0, MapDownloadUtils.calculateProgressPercent(100L, 0L))
        assertEquals(0, MapDownloadUtils.calculateProgressPercent(0L, 0L))
        assertEquals(0, MapDownloadUtils.calculateProgressPercent(0L, 50L))

        // Test large numbers (use exact division)
        assertEquals(50, MapDownloadUtils.calculateProgressPercent(1000000000L, 500000000L))
    }

    @Test
    fun `isActiveDownload correctly identifies active states`() {
        // Active download states
        assertTrue(MapDownloadUtils.isActiveDownload(MapFeatureState.Downloading(50)))
        assertTrue(MapDownloadUtils.isActiveDownload(MapFeatureState.Pending))
        assertTrue(MapDownloadUtils.isActiveDownload(MapFeatureState.Installing))

        // Non-active states
        assertFalse(MapDownloadUtils.isActiveDownload(MapFeatureState.NotChecked))
        assertFalse(MapDownloadUtils.isActiveDownload(MapFeatureState.Available))
        assertFalse(MapDownloadUtils.isActiveDownload(MapFeatureState.NotAvailable))
        assertFalse(MapDownloadUtils.isActiveDownload(MapFeatureState.Installed))
        assertFalse(MapDownloadUtils.isActiveDownload(MapFeatureState.Failed(123, "Error")))
        assertFalse(MapDownloadUtils.isActiveDownload(MapFeatureState.Canceling))
        assertFalse(MapDownloadUtils.isActiveDownload(MapFeatureState.Unknown))
    }

    @Test
    fun `RetryManager handles retry logic correctly`() {
        val retryManager = MapDownloadUtils.RetryManager()

        // Initial state
        assertTrue(retryManager.canRetry())
        assertEquals(0, retryManager.getCurrentRetryCount())

        // First retry - delay calculated before increment
        assertEquals(MapDownloadUtils.RetryManager.BASE_RETRY_DELAY_MS, retryManager.getNextRetryDelay())
        assertEquals(1, retryManager.incrementRetryCount())
        assertTrue(retryManager.canRetry())

        // Second retry (exponential backoff)
        assertEquals(MapDownloadUtils.RetryManager.BASE_RETRY_DELAY_MS * 2, retryManager.getNextRetryDelay())
        assertEquals(2, retryManager.incrementRetryCount())
        assertTrue(retryManager.canRetry())

        // Third retry
        assertEquals(MapDownloadUtils.RetryManager.BASE_RETRY_DELAY_MS * 4, retryManager.getNextRetryDelay())
        assertEquals(3, retryManager.incrementRetryCount())
        assertFalse(retryManager.canRetry()) // Exceeded MAX_RETRIES

        // Reset
        retryManager.resetRetryCount()
        assertEquals(0, retryManager.getCurrentRetryCount())
        assertTrue(retryManager.canRetry())
    }

    @Test
    fun `RetryManager exponential backoff calculation`() {
        val retryManager = MapDownloadUtils.RetryManager()

        // Test exponential backoff progression
        val expectedDelays =
            listOf(
                MapDownloadUtils.RetryManager.BASE_RETRY_DELAY_MS, // 1000ms
                MapDownloadUtils.RetryManager.BASE_RETRY_DELAY_MS * 2, // 2000ms
                MapDownloadUtils.RetryManager.BASE_RETRY_DELAY_MS * 4, // 4000ms
            )

        expectedDelays.forEachIndexed { index, expectedDelay ->
            assertEquals(
                expectedDelay,
                retryManager.getNextRetryDelay(),
                "Retry delay incorrect for attempt ${index + 1}",
            )
            retryManager.incrementRetryCount()
        }
    }

    @Test
    fun `RetryManager constants are reasonable`() {
        // Verify retry constants are sensible
        assertEquals(3, MapDownloadUtils.RetryManager.MAX_RETRIES)
        assertEquals(1000L, MapDownloadUtils.RetryManager.BASE_RETRY_DELAY_MS)

        // Max retry delay should be reasonable (8 seconds max)
        val retryManager = MapDownloadUtils.RetryManager()
        repeat(MapDownloadUtils.RetryManager.MAX_RETRIES) {
            retryManager.incrementRetryCount()
        }
        val maxDelay = retryManager.getNextRetryDelay()
        assertTrue(maxDelay <= 10000L, "Max retry delay too high: ${maxDelay}ms")
    }
}
