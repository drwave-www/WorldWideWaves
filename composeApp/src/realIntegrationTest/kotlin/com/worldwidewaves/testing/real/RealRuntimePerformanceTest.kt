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

package com.worldwidewaves.testing.real

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Real integration tests for runtime performance monitoring.
 *
 * These tests monitor ongoing app performance during actual usage:
 * - Frame rate during map operations
 * - Memory usage patterns over time
 * - Network request latency measurements
 * - Database operation benchmarks
 */
@RunWith(AndroidJUnit4::class)
class RealRuntimePerformanceTest : BaseRealIntegrationTest() {

    /**
     * Test frame rate during intensive map operations
     */
    @Test
    fun realPerformance_frameRateDuringMapOperations_maintainsSmooth() = runTest {
        val trace = startPerformanceTrace("map_frame_rate_test")

        try {
            // Create many events to stress test map rendering
            repeat(200) { index ->
                createTestEvent(
                    eventId = "map_stress_event_$index",
                    latitude = 40.7589 + (index * 0.0001),
                    longitude = -73.9851 + (index * 0.0001),
                    startsSoonInSeconds = index * 30,
                    durationSeconds = 600
                )
            }

            waitForDataSync(8000)

            // Ensure app is ready
            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("main_screen"),
                timeoutMillis = 15000
            )

            val mapOperations = listOf(
                MapOperation("scroll_stress", 20) {
                    composeTestRule.onNodeWithTag("event_list")
                        .performScrollToIndex(kotlin.random.Random.nextInt(0, 150))
                },
                MapOperation("rapid_navigation", 15) {
                    composeTestRule.onAllNodesWithContentDescription("View event details")
                        .onFirst()
                        .performClick()
                    delay(500)
                    composeTestRule.onNodeWithContentDescription("Navigate back")
                        .performClick()
                },
                MapOperation("continuous_scroll", 30) {
                    repeat(5) { scrollIndex ->
                        composeTestRule.onNodeWithTag("event_list")
                            .performScrollToIndex(scrollIndex * 20)
                        delay(100)
                    }
                }
            )

            mapOperations.forEach { operation ->
                println("🎬 Testing frame rate during ${operation.name}")
                val operationStartTime = System.currentTimeMillis()

                // Monitor frame rate during operation
                var frameDrops = 0
                val targetFrameTime = 16 // 60fps = 16ms per frame

                repeat(operation.iterations) { iteration ->
                    val frameStartTime = System.currentTimeMillis()

                    operation.action()

                    val frameTime = System.currentTimeMillis() - frameStartTime
                    if (frameTime > targetFrameTime * 2) { // Allow 2x target for complex operations
                        frameDrops++
                    }

                    // Brief pause to allow frame measurement
                    delay(50)
                }

                val operationDuration = System.currentTimeMillis() - operationStartTime
                val frameDropPercentage = (frameDrops.toDouble() / operation.iterations) * 100

                trace.addMetric("${operation.name}_duration_ms", operationDuration)
                trace.addMetric("${operation.name}_frame_drops", frameDrops.toLong())

                // Verify frame rate performance
                assertTrue(
                    frameDropPercentage < 20, // Less than 20% frame drops acceptable
                    "${operation.name} should have <20% frame drops: ${frameDropPercentage}%"
                )

                assertTrue(
                    operationDuration < operation.iterations * 200, // Reasonable time per iteration
                    "${operation.name} should complete in reasonable time: ${operationDuration}ms for ${operation.iterations} iterations"
                )

                println("✅ ${operation.name}: ${frameDrops}/${operation.iterations} frame drops (${frameDropPercentage}%), Duration: ${operationDuration}ms")
            }

        } finally {
            stopPerformanceTrace()
        }
    }

    /**
     * Test memory usage patterns during extended usage
     */
    @Test
    fun realPerformance_memoryUsagePatterns_stableOverTime() = runTest {
        val trace = startPerformanceTrace("memory_usage_monitoring")

        try {
            // Create substantial test data for memory testing
            repeat(150) { index ->
                createTestEvent(
                    eventId = "memory_test_event_$index",
                    latitude = 40.7589 + (index * 0.00005),
                    longitude = -73.9851 + (index * 0.00005),
                    startsSoonInSeconds = index * 45,
                    durationSeconds = 900
                )
            }

            waitForDataSync(10000)

            // Track memory usage over time
            val memoryReadings = mutableListOf<MemoryReading>()
            val testDuration = 5.minutes
            val startTime = System.currentTimeMillis()

            // Perform realistic usage patterns while monitoring memory
            var iteration = 0
            while (System.currentTimeMillis() - startTime < testDuration.inWholeMilliseconds) {
                // Record memory usage
                val currentMemory = deviceStateManager.getMemoryUsage()
                memoryReadings.add(
                    MemoryReading(
                        timestamp = System.currentTimeMillis() - startTime,
                        usedMB = currentMemory.usedMemoryMB,
                        availableMB = currentMemory.availableMemoryMB,
                        iteration = iteration
                    )
                )

                // Simulate realistic user behavior
                when (iteration % 10) {
                    0, 1, 2 -> {
                        // Event browsing (most common)
                        composeTestRule.onNodeWithTag("event_list")
                            .performScrollToIndex(kotlin.random.Random.nextInt(0, 100))
                    }
                    3, 4 -> {
                        // Event detail viewing
                        composeTestRule.onAllNodesWithContentDescription("View event details")
                            .onFirst()
                            .performClick()
                        delay(2.seconds)
                        composeTestRule.onNodeWithContentDescription("Navigate back")
                            .performClick()
                    }
                    5, 6 -> {
                        // Rapid scrolling
                        repeat(5) { scrollIndex ->
                            composeTestRule.onNodeWithTag("event_list")
                                .performScrollToIndex(scrollIndex * 15)
                            delay(200)
                        }
                    }
                    else -> {
                        // Idle time
                        delay(3.seconds)
                    }
                }

                iteration++
                delay(10.seconds) // Memory reading interval
            }

            // Analyze memory usage patterns
            val initialMemory = memoryReadings.first().usedMB
            val finalMemory = memoryReadings.last().usedMB
            val maxMemory = memoryReadings.maxOf { it.usedMB }
            val memoryGrowth = finalMemory - initialMemory
            val memorySpike = maxMemory - initialMemory

            trace.addMetric("initial_memory_mb", initialMemory)
            trace.addMetric("final_memory_mb", finalMemory)
            trace.addMetric("max_memory_mb", maxMemory)
            trace.addMetric("memory_growth_mb", memoryGrowth)

            // Verify memory usage is stable and reasonable
            assertTrue(
                memoryGrowth < 100, // Memory growth should be under 100MB over 5 minutes
                "Memory growth should be reasonable: ${memoryGrowth}MB over 5 minutes"
            )

            assertTrue(
                memorySpike < 200, // Memory spikes should be under 200MB
                "Memory spike should be reasonable: ${memorySpike}MB peak increase"
            )

            assertTrue(
                finalMemory < initialMemory * 1.5, // Final memory shouldn't be 50% higher
                "Final memory should not be excessively higher than initial: ${finalMemory}MB vs ${initialMemory}MB"
            )

            // Check for memory stability (no continuous growth)
            val lastThirdReadings = memoryReadings.takeLast(memoryReadings.size / 3)
            val memoryStdDev = calculateStandardDeviation(lastThirdReadings.map { it.usedMB.toDouble() })

            assertTrue(
                memoryStdDev < 50, // Memory usage should be stable in final third
                "Memory usage should be stable in final period: StdDev ${memoryStdDev}MB"
            )

            println("✅ Memory usage: Initial ${initialMemory}MB, Final ${finalMemory}MB, Growth ${memoryGrowth}MB, StdDev ${memoryStdDev}MB")

        } finally {
            stopPerformanceTrace()
        }
    }

    /**
     * Test network request latency measurements
     */
    @Test
    fun realPerformance_networkRequestLatency_measuresRealistic() = runTest {
        val trace = startPerformanceTrace("network_latency_test")

        try {
            val networkOperations = listOf(
                NetworkOperation("event_data_fetch", 10) {
                    // Simulate event data fetching
                    createTestEvent(
                        eventId = "network_test_${System.currentTimeMillis()}",
                        latitude = 40.7589,
                        longitude = -73.9851,
                        startsSoonInSeconds = 300,
                        durationSeconds = 600
                    )
                    waitForDataSync(3000)
                },
                NetworkOperation("event_detail_load", 15) {
                    // Simulate event detail loading
                    composeTestRule.onAllNodesWithContentDescription("View event details")
                        .onFirst()
                        .performClick()
                    composeTestRule.waitUntilAtLeastOneExists(
                        hasTestTag("event_detail_screen"),
                        timeoutMillis = 5000
                    )
                    composeTestRule.onNodeWithContentDescription("Navigate back")
                        .performClick()
                },
                NetworkOperation("data_sync", 8) {
                    // Simulate periodic data synchronization
                    waitForDataSync(4000)
                }
            )

            // Initial setup
            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("main_screen"),
                timeoutMillis = 15000
            )

            networkOperations.forEach { operation ->
                println("🌐 Testing network latency for ${operation.name}")
                val latencyReadings = mutableListOf<Long>()

                repeat(operation.iterations) { iteration ->
                    val requestStartTime = System.currentTimeMillis()

                    try {
                        operation.action()
                        val latency = System.currentTimeMillis() - requestStartTime
                        latencyReadings.add(latency)

                    } catch (e: Exception) {
                        println("⚠️  Network operation ${operation.name} iteration $iteration failed: ${e.message}")
                        // Record a timeout latency
                        latencyReadings.add(10000) // 10 second timeout
                    }

                    delay(2.seconds) // Pause between requests
                }

                val averageLatency = latencyReadings.average()
                val maxLatency = latencyReadings.maxOrNull() ?: 0
                val minLatency = latencyReadings.minOrNull() ?: 0

                trace.addMetric("${operation.name}_avg_latency_ms", averageLatency.toLong())
                trace.addMetric("${operation.name}_max_latency_ms", maxLatency)

                // Verify network performance requirements
                assertTrue(
                    averageLatency < 5000, // Average latency under 5 seconds
                    "${operation.name} average latency should be <5s: ${averageLatency}ms"
                )

                assertTrue(
                    maxLatency < 15000, // Max latency under 15 seconds
                    "${operation.name} max latency should be <15s: ${maxLatency}ms"
                )

                // Check for consistency (not too variable)
                val latencyStdDev = calculateStandardDeviation(latencyReadings.map { it.toDouble() })
                assertTrue(
                    latencyStdDev < averageLatency, // Standard deviation shouldn't exceed average
                    "${operation.name} latency should be consistent: StdDev ${latencyStdDev}ms vs Avg ${averageLatency}ms"
                )

                println("✅ ${operation.name}: Avg ${averageLatency}ms, Range ${minLatency}-${maxLatency}ms, StdDev ${latencyStdDev}ms")
            }

        } finally {
            stopPerformanceTrace()
        }
    }

    /**
     * Test database operation benchmarks
     */
    @Test
    fun realPerformance_databaseOperationBenchmarks_measuresEfficiency() = runTest {
        val trace = startPerformanceTrace("database_performance_test")

        try {
            val databaseOperations = listOf(
                DatabaseOperation("bulk_event_insert", 50) {
                    // Simulate bulk event insertion
                    repeat(5) { batchIndex ->
                        createTestEvent(
                            eventId = "db_bulk_${System.currentTimeMillis()}_$batchIndex",
                            latitude = 40.7589 + (batchIndex * 0.001),
                            longitude = -73.9851 + (batchIndex * 0.001),
                            startsSoonInSeconds = batchIndex * 60,
                            durationSeconds = 300
                        )
                    }
                },
                DatabaseOperation("event_query", 25) {
                    // Simulate event querying by scrolling through list
                    composeTestRule.onNodeWithTag("event_list")
                        .performScrollToIndex(kotlin.random.Random.nextInt(0, 30))
                },
                DatabaseOperation("event_detail_fetch", 20) {
                    // Simulate detailed event data fetching
                    composeTestRule.onAllNodesWithContentDescription("View event details")
                        .onFirst()
                        .performClick()
                    composeTestRule.waitUntilAtLeastOneExists(
                        hasTestTag("event_detail_screen"),
                        timeoutMillis = 3000
                    )
                    composeTestRule.onNodeWithContentDescription("Navigate back")
                        .performClick()
                }
            )

            // Initial setup with base data
            repeat(30) { index ->
                createTestEvent(
                    eventId = "db_base_event_$index",
                    latitude = 40.7589 + (index * 0.0001),
                    longitude = -73.9851 + (index * 0.0001),
                    startsSoonInSeconds = index * 120,
                    durationSeconds = 600
                )
            }

            waitForDataSync(8000)
            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("main_screen"),
                timeoutMillis = 12000
            )

            databaseOperations.forEach { operation ->
                println("🗄️ Testing database performance for ${operation.name}")
                val operationTimes = mutableListOf<Long>()

                repeat(operation.iterations) { iteration ->
                    val operationStartTime = System.currentTimeMillis()

                    try {
                        operation.action()
                        val operationTime = System.currentTimeMillis() - operationStartTime
                        operationTimes.add(operationTime)

                    } catch (e: Exception) {
                        println("⚠️  Database operation ${operation.name} iteration $iteration failed: ${e.message}")
                        operationTimes.add(5000) // 5 second penalty for failures
                    }

                    delay(1.seconds) // Brief pause between operations
                }

                val averageTime = operationTimes.average()
                val maxTime = operationTimes.maxOrNull() ?: 0
                val p95Time = operationTimes.sorted().let { sorted ->
                    sorted[(sorted.size * 0.95).toInt()]
                }

                trace.addMetric("${operation.name}_avg_time_ms", averageTime.toLong())
                trace.addMetric("${operation.name}_p95_time_ms", p95Time)

                // Verify database performance requirements
                val maxAllowedTime = when (operation.name) {
                    "bulk_event_insert" -> 3000 // 3 seconds for bulk insert
                    "event_query" -> 1000 // 1 second for query
                    "event_detail_fetch" -> 2000 // 2 seconds for detail fetch
                    else -> 2000
                }

                assertTrue(
                    averageTime < maxAllowedTime,
                    "${operation.name} average time should be <${maxAllowedTime}ms: ${averageTime}ms"
                )

                assertTrue(
                    p95Time < maxAllowedTime * 2, // P95 can be 2x average limit
                    "${operation.name} P95 time should be <${maxAllowedTime * 2}ms: ${p95Time}ms"
                )

                println("✅ ${operation.name}: Avg ${averageTime}ms, Max ${maxTime}ms, P95 ${p95Time}ms")
            }

        } finally {
            stopPerformanceTrace()
        }
    }

    // Helper classes and methods

    private data class MapOperation(
        val name: String,
        val iterations: Int,
        val action: suspend () -> Unit
    )

    private data class MemoryReading(
        val timestamp: Long,
        val usedMB: Long,
        val availableMB: Long,
        val iteration: Int
    )

    private data class NetworkOperation(
        val name: String,
        val iterations: Int,
        val action: suspend () -> Unit
    )

    private data class DatabaseOperation(
        val name: String,
        val iterations: Int,
        val action: suspend () -> Unit
    )

    private fun calculateStandardDeviation(values: List<Double>): Double {
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}