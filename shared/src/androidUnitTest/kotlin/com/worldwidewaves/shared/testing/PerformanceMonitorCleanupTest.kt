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

package com.worldwidewaves.shared.testing

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tests for PerformanceMonitor cleanup and bounded storage functionality.
 */
class PerformanceMonitorCleanupTest {
    @Test
    fun `should clear all traces on cleanup`() =
        runTest {
            // Given
            val monitor = PerformanceMonitor()
            monitor.startTrace("trace1")
            monitor.startTrace("trace2")
            monitor.startTrace("trace3")

            val statsBefore = monitor.getMemoryStats()
            assertEquals(3, statsBefore.tracesCount, "Should have 3 active traces")

            // When
            monitor.cleanup()

            // Then
            val statsAfter = monitor.getMemoryStats()
            assertEquals(0, statsAfter.tracesCount, "Should clear all traces")
        }

    @Test
    fun `should clear all metrics on cleanup`() =
        runTest {
            // Given
            val monitor = PerformanceMonitor()
            monitor.recordMetric("metric1", 100.0)
            monitor.recordMetric("metric1", 200.0)
            monitor.recordMetric("metric2", 300.0)
            monitor.recordMetric("metric3", 400.0)

            val statsBefore = monitor.getMemoryStats()
            assertEquals(3, statsBefore.metricsCount, "Should have 3 metric keys")
            assertEquals(4, statsBefore.totalMetricValues, "Should have 4 total metric values")

            // When
            monitor.cleanup()

            // Then
            val statsAfter = monitor.getMemoryStats()
            assertEquals(0, statsAfter.metricsCount, "Should clear all metrics")
            assertEquals(0, statsAfter.totalMetricValues, "Should clear all metric values")
        }

    @Test
    fun `should clear all events on cleanup`() =
        runTest {
            // Given
            val monitor = PerformanceMonitor()
            monitor.recordEvent("event1")
            monitor.recordEvent("event2")
            monitor.recordEvent("event3")

            val statsBefore = monitor.getMemoryStats()
            assertEquals(3, statsBefore.eventsCount, "Should have 3 events")

            // When
            monitor.cleanup()

            // Then
            val statsAfter = monitor.getMemoryStats()
            assertEquals(0, statsAfter.eventsCount, "Should clear all events")
        }

    @Test
    fun `should enforce bounded metrics storage`() =
        runTest {
            // Given
            val monitor = PerformanceMonitor()
            val maxMetrics = 1000

            // When - Record more than MAX_METRICS_PER_KEY values
            for (i in 1..1500) {
                monitor.recordMetric("test_metric", i.toDouble())
            }

            // Then - Should keep only last 1000 values
            val stats = monitor.getMemoryStats()
            assertTrue(
                stats.totalMetricValues <= maxMetrics,
                "Should not exceed max metrics per key (expected <= $maxMetrics, got ${stats.totalMetricValues})",
            )
        }

    @Test
    fun `should enforce bounded events storage`() =
        runTest {
            // Given
            val monitor = PerformanceMonitor()
            val maxEvents = 500

            // When - Record more than MAX_EVENTS events
            for (i in 1..750) {
                monitor.recordEvent("test_event_$i")
            }

            // Then - Should keep only last 500 events
            val stats = monitor.getMemoryStats()
            assertTrue(
                stats.eventsCount <= maxEvents,
                "Should not exceed max events (expected <= $maxEvents, got ${stats.eventsCount})",
            )
        }

    @Test
    fun `should enforce bounded traces storage`() =
        runTest {
            // Given
            val monitor = PerformanceMonitor()
            val maxTraces = 100

            // When - Start more than MAX_TRACES traces
            for (i in 1..150) {
                monitor.startTrace("trace_$i")
            }

            // Then - Should keep only last 100 traces
            val stats = monitor.getMemoryStats()
            assertTrue(
                stats.tracesCount <= maxTraces,
                "Should not exceed max traces (expected <= $maxTraces, got ${stats.tracesCount})",
            )
        }

    @Test
    fun `should handle multiple cleanup calls`() =
        runTest {
            // Given
            val monitor = PerformanceMonitor()
            monitor.startTrace("trace1")
            monitor.recordMetric("metric1", 100.0)
            monitor.recordEvent("event1")

            // When - Call cleanup multiple times
            monitor.cleanup()
            monitor.cleanup()
            monitor.cleanup()

            // Then - Should not throw exception
            val stats = monitor.getMemoryStats()
            assertEquals(0, stats.tracesCount)
            assertEquals(0, stats.metricsCount)
            assertEquals(0, stats.eventsCount)
        }

    @Test
    fun `should support usage after cleanup`() =
        runTest {
            // Given
            val monitor = PerformanceMonitor()
            monitor.startTrace("trace1")
            monitor.recordMetric("metric1", 100.0)
            monitor.recordEvent("event1")

            // When
            monitor.cleanup()

            // Then - Should be able to record new data
            monitor.startTrace("trace2")
            monitor.recordMetric("metric2", 200.0)
            monitor.recordEvent("event2")

            val stats = monitor.getMemoryStats()
            assertEquals(1, stats.tracesCount, "Should have new trace")
            assertEquals(1, stats.metricsCount, "Should have new metric")
            assertEquals(1, stats.eventsCount, "Should have new event")
        }

    @Test
    fun `should preserve last N values when bounded metrics storage is enforced`() =
        runTest {
            // Given
            val monitor = PerformanceMonitor()

            // When - Record 1100 values
            for (i in 1..1100) {
                monitor.recordMetric("test_metric", i.toDouble())
            }

            // Then - Should keep last 1000 values (values 101-1100)
            val stats = monitor.getMemoryStats()
            assertEquals(1000, stats.totalMetricValues, "Should keep exactly 1000 most recent values")
        }

    @Test
    fun `should record wave timing and choreography metrics with bounded storage`() =
        runTest {
            // Given
            val monitor = PerformanceMonitor()

            // When - Record various wave metrics
            monitor.recordWaveTimingAccuracy(1000, 995)
            monitor.recordWaveParticipation("event-1", true)
            monitor.recordChoreographyPerformance("sequence-1", 100.milliseconds)

            // Then - Should have recorded metrics and events
            val stats = monitor.getMemoryStats()
            assertTrue(stats.metricsCount > 0, "Should have recorded metrics")
            assertTrue(stats.eventsCount > 0, "Should have recorded events")
        }

    @Test
    fun `should record UI performance metrics with bounded storage`() =
        runTest {
            // Given
            val monitor = PerformanceMonitor()

            // When - Record UI metrics
            monitor.recordScreenLoad("EventsScreen", 500.milliseconds)
            monitor.recordUserInteraction("tap_event", 50.milliseconds)
            monitor.recordAnimationPerformance("wave_animation", 2)

            // Then - Should have recorded metrics and events
            val stats = monitor.getMemoryStats()
            assertTrue(stats.metricsCount > 0, "Should have recorded metrics")
            assertTrue(stats.eventsCount > 0, "Should have recorded events")
        }

    @Test
    fun `should record system metrics with bounded storage`() =
        runTest {
            // Given
            val monitor = PerformanceMonitor()

            // When - Record system metrics
            monitor.recordMemoryUsage(8000, 10000)
            monitor.recordNetworkLatency("api.worldwidewaves.net", 100.milliseconds)
            monitor.recordLocationAccuracy(10.0f)

            // Then - Should have recorded metrics
            val stats = monitor.getMemoryStats()
            assertTrue(stats.metricsCount > 0, "Should have recorded metrics")
        }
}
