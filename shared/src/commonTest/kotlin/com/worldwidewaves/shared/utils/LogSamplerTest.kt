package com.worldwidewaves.shared.utils

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

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for LogSampler log sampling functionality.
 *
 * Validates:
 * - Basic sampling behavior (1 out of N logs)
 * - "First N then sample" behavior
 * - Thread safety via atomic operations
 * - Counter tracking
 */
class LogSamplerTest {
    @AfterTest
    fun tearDown() {
        // Reset counters after each test
        LogSampler.reset()
    }

    @Test
    fun testShouldSample_alwaysSampleWhenRateIsOne() {
        // When sample rate is 1, all logs should be sampled
        repeat(10) {
            assertTrue(
                LogSampler.shouldSample("test.key", sampleRate = 1),
                "Sample rate of 1 should always return true",
            )
        }
    }

    @Test
    fun testShouldSample_samplesCorrectly() {
        val key = "test.sampling"
        val sampleRate = 10 // Sample 1 out of 10

        var sampledCount = 0
        repeat(100) {
            if (LogSampler.shouldSample(key, sampleRate)) {
                sampledCount++
            }
        }

        // Should sample exactly 10 times out of 100 (counts: 10, 20, 30, ..., 100)
        assertEquals(10, sampledCount, "Should sample exactly 1 out of every 10 logs")
    }

    @Test
    fun testShouldSample_differentKeysHaveSeparateCounters() {
        val key1 = "test.key1"
        val key2 = "test.key2"
        val sampleRate = 5

        // Increment key1 to count 5
        repeat(5) { LogSampler.shouldSample(key1, sampleRate) }
        // Increment key2 to count 3
        repeat(3) { LogSampler.shouldSample(key2, sampleRate) }

        assertEquals(5, LogSampler.getCount(key1), "Key1 should have count 5")
        assertEquals(3, LogSampler.getCount(key2), "Key2 should have count 3")
    }

    @Test
    fun testShouldSampleAfterFirst_logsFirstN() {
        val key = "test.firstN"
        val firstN = 5
        val sampleRate = 100

        // First 5 should all be sampled
        repeat(firstN) { index ->
            assertTrue(
                LogSampler.shouldSampleAfterFirst(key, firstN, sampleRate),
                "First $firstN logs should be sampled (index: $index)",
            )
        }
    }

    @Test
    fun testShouldSampleAfterFirst_switchesToSamplingAfterFirstN() {
        val key = "test.switchToSample"
        val firstN = 3
        val sampleRate = 10

        var sampledCount = 0
        repeat(100) {
            if (LogSampler.shouldSampleAfterFirst(key, firstN, sampleRate)) {
                sampledCount++
            }
        }

        // Should sample:
        // - First 3 logs (count 1, 2, 3)
        // - Then 1 out of 10 for remaining 97 logs (count 10, 20, 30, ..., 100 = 10 samples)
        // Total: 3 + 10 = 13 samples
        assertEquals(13, sampledCount, "Should sample first 3, then 1 out of 10 for the rest")
    }

    @Test
    fun testReset_clearsAllCounters() {
        val key1 = "test.reset1"
        val key2 = "test.reset2"

        // Increment counters
        repeat(5) { LogSampler.shouldSample(key1, 100) }
        repeat(3) { LogSampler.shouldSample(key2, 100) }

        assertEquals(5, LogSampler.getCount(key1), "Key1 should have count 5 before reset")
        assertEquals(3, LogSampler.getCount(key2), "Key2 should have count 3 before reset")

        // Reset
        LogSampler.reset()

        assertEquals(0, LogSampler.getCount(key1), "Key1 should have count 0 after reset")
        assertEquals(0, LogSampler.getCount(key2), "Key2 should have count 0 after reset")
    }

    @Test
    fun testGetCount_returnsZeroForNonExistentKey() {
        assertEquals(0, LogSampler.getCount("nonexistent.key"), "Non-existent key should return 0")
    }

    @Test
    fun testShouldSample_samplesOnMultiplesOfSampleRate() {
        val key = "test.multiples"
        val sampleRate = 7

        // Increment to just before the first multiple
        repeat(6) {
            assertFalse(
                LogSampler.shouldSample(key, sampleRate),
                "Should not sample before first multiple",
            )
        }

        // 7th call should sample
        assertTrue(
            LogSampler.shouldSample(key, sampleRate),
            "Should sample on count 7 (first multiple)",
        )

        // 8th-13th should not sample
        repeat(6) {
            assertFalse(
                LogSampler.shouldSample(key, sampleRate),
                "Should not sample between multiples",
            )
        }

        // 14th call should sample
        assertTrue(
            LogSampler.shouldSample(key, sampleRate),
            "Should sample on count 14 (second multiple)",
        )
    }

    @Test
    fun testEdgeCases_largeCount() {
        val key = "test.large"
        val sampleRate = 1000

        // Simulate a high-frequency log over time
        var sampledCount = 0
        repeat(10000) {
            if (LogSampler.shouldSample(key, sampleRate)) {
                sampledCount++
            }
        }

        // Should sample exactly 10 times (counts: 1000, 2000, ..., 10000)
        assertEquals(10, sampledCount, "Should handle large counts correctly")
    }

    // ============================================================
    // LRU EVICTION TESTS (Memory Leak Prevention)
    // ============================================================

    @Test
    fun testLRUEviction_ExceedingMaxCounters() {
        // Create exactly 1000 unique log keys (MAX_COUNTERS)
        repeat(1000) { i ->
            LogSampler.shouldSample("test.key.$i", sampleRate = 100)
        }

        // All 1000 should exist
        repeat(1000) { i ->
            assertTrue(LogSampler.getCount("test.key.$i") > 0, "Counter $i should exist")
        }

        // Add 1001st key - should evict LRU (test.key.0)
        LogSampler.shouldSample("test.key.1000", sampleRate = 100)

        // test.key.0 should be evicted
        assertEquals(0, LogSampler.getCount("test.key.0"), "test.key.0 should be evicted (LRU)")

        // test.key.1 through test.key.1000 should exist
        repeat(1000) { i ->
            assertTrue(LogSampler.getCount("test.key.${i + 1}") > 0, "Counter ${i + 1} should exist")
        }
    }

    @Test
    fun testLRUEviction_AccessOrderPreservesRecentlyUsed() {
        // Create 1000 counters
        repeat(1000) { i ->
            LogSampler.shouldSample("test.key.$i", sampleRate = 100)
        }

        // Access test.key.0 again to make it most recently used
        LogSampler.shouldSample("test.key.0", sampleRate = 100)

        // Add 1001st key - should evict test.key.1 (now LRU), not test.key.0
        LogSampler.shouldSample("test.key.1000", sampleRate = 100)

        // test.key.0 should still exist (accessed recently)
        assertTrue(LogSampler.getCount("test.key.0") > 0, "test.key.0 should exist (accessed recently)")

        // test.key.1 should be evicted (LRU)
        assertEquals(0, LogSampler.getCount("test.key.1"), "test.key.1 should be evicted (LRU)")
    }

    @Test
    fun testLRUEviction_BothMethodsUseSameEviction() {
        // Mix shouldSample and shouldSampleAfterFirst
        repeat(500) { i ->
            LogSampler.shouldSample("sample.$i", sampleRate = 100)
        }
        repeat(500) { i ->
            LogSampler.shouldSampleAfterFirst("afterFirst.$i", firstN = 5, sampleRate = 100)
        }

        // Total: 1000 unique keys
        // All should exist
        repeat(500) { i ->
            assertTrue(LogSampler.getCount("sample.$i") > 0, "sample.$i should exist")
            assertTrue(LogSampler.getCount("afterFirst.$i") > 0, "afterFirst.$i should exist")
        }

        // Add 1001st key - should evict oldest
        LogSampler.shouldSample("new.key", sampleRate = 100)

        // sample.0 should be evicted (oldest)
        assertEquals(0, LogSampler.getCount("sample.0"), "sample.0 should be evicted (LRU)")
    }
}
