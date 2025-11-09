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

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

/**
 * Thread-safe log sampling to reduce production log volume.
 *
 * This utility allows sampling high-frequency logs (e.g., log 1 out of every 100 position updates)
 * to prevent overwhelming production log systems while maintaining observability for debugging.
 *
 * ## Use Cases
 * - Position update logs (GPS emits 1-10 updates/second)
 * - Wave progression tracking (high-frequency timer events)
 * - Network retry logs (can occur in bursts)
 * - Map rendering events (frequent during user interaction)
 *
 * ## Thread Safety
 * Uses kotlinx-atomicfu for lock-free atomic operations, safe for concurrent access
 * from multiple coroutines and threads. Counter map access is protected by a reentrant lock.
 *
 * ## Example Usage
 * ```kotlin
 * // Sample 1% of position updates (log 1 out of 100)
 * if (LogSampler.shouldSample("PositionManager.update", sampleRate = 100)) {
 *     Log.v(TAG, "Position update from $source: $position")
 * }
 *
 * // Always log first 10 occurrences, then sample 1%
 * if (LogSampler.shouldSampleAfterFirst("WaveTracker.tick", firstN = 10, sampleRate = 100)) {
 *     Log.v(TAG, "Wave progression tick: $waveState")
 * }
 * ```
 *
 * ## Performance
 * - O(1) atomic increment per call
 * - Reentrant lock only for map access (not counter increment)
 * - Minimal memory footprint (one counter per unique key)
 */
object LogSampler {
    /**
     * Maximum number of unique log locations to track.
     * When exceeded, least recently used counters are evicted.
     */
    private const val MAX_COUNTERS = 1000

    /**
     * Counter storage for each unique log location.
     * Thread-safe via atomic operations. Map access protected by lock.
     * LRU eviction prevents unbounded memory growth.
     */
    private val counters = mutableMapOf<String, kotlinx.atomicfu.AtomicInt>()
    private val accessOrder = mutableListOf<String>()
    private val lock = reentrantLock()

    /**
     * Gets or creates a counter with LRU eviction.
     * Must be called within lock.withLock.
     */
    private fun getOrCreateCounter(key: String): kotlinx.atomicfu.AtomicInt {
        // Check if we need to evict LRU counter
        if (counters.size >= MAX_COUNTERS && !counters.containsKey(key)) {
            val lruKey = accessOrder.firstOrNull()
            if (lruKey != null) {
                counters.remove(lruKey)
                accessOrder.removeAt(0)
            }
        }

        // Update access order
        accessOrder.remove(key)
        accessOrder.add(key)

        // Get or create counter
        return counters.getOrPut(key) { atomic(0) }
    }

    /**
     * Check if this log should be sampled (emitted).
     *
     * Uses modulo sampling: emits log when count % sampleRate == 0.
     * For sampleRate=100, emits on counts 100, 200, 300, etc. (1% sampling).
     *
     * @param key Unique key for this log location (e.g., "PositionManager.update")
     * @param sampleRate Sample 1 out of every N logs (default: 100 = 1% sampling)
     * @return true if this log should be emitted
     *
     * @throws IllegalArgumentException if sampleRate < 1
     */
    fun shouldSample(
        key: String,
        sampleRate: Int = 100,
    ): Boolean {
        require(sampleRate >= 1) { "Sample rate must be >= 1, got: $sampleRate" }

        if (sampleRate == 1) return true

        val counter =
            lock.withLock {
                getOrCreateCounter(key)
            }
        val count = counter.incrementAndGet()

        return count % sampleRate == 0
    }

    /**
     * Always sample the first N occurrences, then switch to sampling.
     *
     * Useful for capturing startup behavior before reducing volume.
     * For example, log all startup events (firstN=10), then sample 1% of steady-state events.
     *
     * @param key Unique key for this log location
     * @param firstN Number of initial occurrences to always log (default: 10)
     * @param sampleRate Sample 1 out of every N logs after firstN (default: 100 = 1%)
     * @return true if this log should be emitted
     *
     * @throws IllegalArgumentException if firstN < 0 or sampleRate < 1
     */
    fun shouldSampleAfterFirst(
        key: String,
        firstN: Int = 10,
        sampleRate: Int = 100,
    ): Boolean {
        require(firstN >= 0) { "firstN must be >= 0, got: $firstN" }
        require(sampleRate >= 1) { "Sample rate must be >= 1, got: $sampleRate" }

        val counter =
            lock.withLock {
                getOrCreateCounter(key)
            }
        val count = counter.incrementAndGet()

        // Always emit first N logs
        if (count <= firstN) return true

        // After firstN, use sampling
        return count % sampleRate == 0
    }

    /**
     * Reset sampling counters (useful for testing).
     *
     * **WARNING**: This method is NOT thread-safe and should only be used in test environments.
     * Do NOT call this in production code.
     */
    fun reset() {
        lock.withLock {
            counters.clear()
            accessOrder.clear()
        }
    }

    /**
     * Get the current count for a specific key (for testing/debugging).
     *
     * @param key The log location key
     * @return The current count, or 0 if key doesn't exist
     */
    fun getCount(key: String): Int =
        lock.withLock {
            counters[key]?.value ?: 0
        }
}
