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
 * Real integration tests for stability and crash prevention.
 *
 * These tests verify app stability under stress conditions:
 * - Monkey testing for random interactions
 * - Long-running stress tests (30+ minutes)
 * - Memory leak detection over time
 * - Background task timeout validation
 */
@RunWith(AndroidJUnit4::class)
class RealStabilityCrashPreventionTest : BaseRealIntegrationTest() {

    /**
     * Test monkey-style random interactions for crash detection
     */
    @Test
    fun realStability_monkeyTesting_preventsCrashes() = runTest {
        val trace = startPerformanceTrace("monkey_testing_stability")

        try {
            // Create diverse test data for comprehensive testing
            repeat(100) { index ->
                createTestEvent(
                    eventId = "monkey_event_$index",
                    latitude = 40.7589 + (index * 0.0001),
                    longitude = -73.9851 + (index * 0.0001),
                    startsSoonInSeconds = index * 30,
                    durationSeconds = kotlin.random.Random.nextInt(300, 1800)
                )
            }

            waitForDataSync(10000)

            // Ensure app is ready for monkey testing
            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("main_screen"),
                timeoutMillis = 15000
            )

            val monkeyActions = listOf<MonkeyAction>(
                MonkeyAction("random_scroll", 0.3) {
                    composeTestRule.onNodeWithTag("event_list")
                        .performScrollToIndex(kotlin.random.Random.nextInt(0, 80))
                },
                MonkeyAction("rapid_navigation", 0.2) {
                    composeTestRule.onAllNodesWithContentDescription("View event details")
                        .onFirst()
                        .performClick()
                    delay(kotlin.random.Random.nextInt(100, 1000).toLong()) // Random delay
                    composeTestRule.onNodeWithContentDescription("Navigate back")
                        .performClick()
                },
                MonkeyAction("stress_scroll", 0.2) {
                    repeat(kotlin.random.Random.nextInt(3, 8)) {
                        composeTestRule.onNodeWithTag("event_list")
                            .performScrollToIndex(kotlin.random.Random.nextInt(0, 50))
                        delay(50)
                    }
                },
                MonkeyAction("interaction_burst", 0.15) {
                    // Rapid fire interactions
                    repeat(5) {
                        try {
                            composeTestRule.onAllNodesWithContentDescription("View event details")
                                .onFirst()
                                .performClick()
                            delay(200)
                            composeTestRule.onNodeWithContentDescription("Navigate back")
                                .performClick()
                            delay(100)
                        } catch (e: Exception) {
                            // Ignore individual failures in burst mode
                        }
                    }
                },
                MonkeyAction("idle_pause", 0.15) {
                    delay(kotlin.random.Random.nextInt(2000, 5000).toLong()) // Random idle time
                }
            )

            var totalActions = 0
            var crashCount = 0
            var errorCount = 0
            val testDuration = 10.minutes // Reduced from full monkey test for CI
            val startTime = System.currentTimeMillis()

            println("🐒 Starting monkey testing for ${testDuration.inWholeMinutes} minutes...")

            while (System.currentTimeMillis() - startTime < testDuration.inWholeMilliseconds) {
                val selectedAction = selectRandomAction(monkeyActions)
                totalActions++

                try {
                    selectedAction.action()

                    // Check if app is still responsive after action
                    if (totalActions % 20 == 0) {
                        composeTestRule.onNodeWithTag("main_screen").assertExists()

                        // Record memory usage periodically
                        val memoryUsage = deviceStateManager.getMemoryUsage()
                        if (memoryUsage.usedMemoryMB > 500) { // Alert if over 500MB
                            println("⚠️  High memory usage detected: ${memoryUsage.usedMemoryMB}MB at action $totalActions")
                        }
                    }

                } catch (e: AssertionError) {
                    // App might have crashed or become unresponsive
                    crashCount++
                    println("💥 Potential crash detected at action $totalActions: ${e.message}")

                    // Try to recover
                    try {
                        composeTestRule.waitUntilAtLeastOneExists(
                            hasTestTag("main_screen"),
                            timeoutMillis = 5000
                        )
                    } catch (recoveryE: Exception) {
                        println("❌ Unable to recover from crash, test may be compromised")
                        break
                    }

                } catch (e: Exception) {
                    errorCount++
                    if (errorCount > totalActions * 0.1) { // More than 10% error rate
                        println("⚠️  High error rate detected: $errorCount/$totalActions")
                    }
                }

                // Brief pause between actions
                delay(kotlin.random.Random.nextInt(50, 300).toLong())
            }

            val testDurationActual = System.currentTimeMillis() - startTime
            trace.addMetric("monkey_actions_total", totalActions.toLong())
            trace.addMetric("monkey_crashes_detected", crashCount.toLong())
            trace.addMetric("monkey_errors", errorCount.toLong())
            trace.addMetric("monkey_duration_ms", testDurationActual)

            // Verify stability requirements
            val crashRate = (crashCount.toDouble() / totalActions) * 100
            val errorRate = (errorCount.toDouble() / totalActions) * 100

            assertTrue(
                crashRate < 1.0, // Less than 1% crash rate
                "Crash rate should be <1%: $crashRate% ($crashCount crashes in $totalActions actions)"
            )

            assertTrue(
                errorRate < 15.0, // Less than 15% error rate (some UI state errors are expected)
                "Error rate should be <15%: $errorRate% ($errorCount errors in $totalActions actions)"
            )

            // Verify app is still functional after monkey testing
            composeTestRule.onNodeWithTag("event_list")
                .performScrollToIndex(10)

            composeTestRule.onAllNodesWithContentDescription("View event details")
                .onFirst()
                .performClick()

            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("event_detail_screen"),
                timeoutMillis = 5000
            )

            println("✅ Monkey testing completed: $totalActions actions, $crashCount crashes (${crashRate}%), $errorCount errors (${errorRate}%)")

        } finally {
            stopPerformanceTrace()
        }
    }

    /**
     * Test long-running stress scenarios for extended stability
     */
    @Test
    fun realStability_longRunningStress_maintainsPerformance() = runTest {
        val trace = startPerformanceTrace("long_running_stress_test")

        try {
            // Create substantial test data for long-running test
            repeat(250) { index ->
                createTestEvent(
                    eventId = "stress_event_$index",
                    latitude = 40.7589 + (index * 0.00001),
                    longitude = -73.9851 + (index * 0.00001),
                    startsSoonInSeconds = index * 20,
                    durationSeconds = 1200
                )
            }

            waitForDataSync(15000)

            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("main_screen"),
                timeoutMillis = 20000
            )

            val stressScenarios = listOf(
                StressScenario("continuous_browsing", 15.minutes) {
                    repeat(10) { scrollIteration ->
                        composeTestRule.onNodeWithTag("event_list")
                            .performScrollToIndex(kotlin.random.Random.nextInt(0, 200))
                        delay(500)
                    }
                },
                StressScenario("repeated_navigation", 10.minutes) {
                    repeat(5) {
                        composeTestRule.onAllNodesWithContentDescription("View event details")
                            .onFirst()
                            .performClick()
                        delay(2.seconds)
                        composeTestRule.onNodeWithContentDescription("Navigate back")
                            .performClick()
                        delay(1.seconds)
                    }
                },
                StressScenario("memory_pressure", 8.minutes) {
                    // Create and destroy data to stress memory management
                    repeat(3) { memIndex ->
                        createTestEvent(
                            eventId = "temp_stress_${System.currentTimeMillis()}_$memIndex",
                            latitude = 40.7589,
                            longitude = -73.9851,
                            startsSoonInSeconds = 300,
                            durationSeconds = 600
                        )
                    }
                    waitForDataSync(2000)
                    composeTestRule.onNodeWithTag("event_list")
                        .performScrollToIndex(kotlin.random.Random.nextInt(0, 100))
                }
            )

            val performanceReadings = mutableListOf<PerformanceReading>()
            val stressStartTime = System.currentTimeMillis()

            println("🔥 Starting long-running stress test scenarios...")

            stressScenarios.forEach { scenario ->
                println("🏃 Running ${scenario.name} for ${scenario.duration.inWholeMinutes} minutes")
                val scenarioStartTime = System.currentTimeMillis()

                while (System.currentTimeMillis() - scenarioStartTime < scenario.duration.inWholeMilliseconds) {
                    val iterationStartTime = System.currentTimeMillis()

                    try {
                        scenario.action()

                        // Record performance metrics
                        val iterationTime = System.currentTimeMillis() - iterationStartTime
                        val memoryUsage = deviceStateManager.getMemoryUsage()

                        performanceReadings.add(
                            PerformanceReading(
                                timestamp = System.currentTimeMillis() - stressStartTime,
                                scenario = scenario.name,
                                iterationTime = iterationTime,
                                memoryUsageMB = memoryUsage.usedMemoryMB
                            )
                        )

                        // Check for performance degradation
                        if (iterationTime > 10000) { // Over 10 seconds for an iteration
                            println("⚠️  Performance degradation detected in ${scenario.name}: ${iterationTime}ms")
                        }

                    } catch (e: Exception) {
                        println("⚠️  Error in ${scenario.name}: ${e.message}")
                    }

                    delay(3.seconds) // Pause between stress iterations
                }

                println("✅ ${scenario.name} completed successfully")
            }

            // Analyze performance over the long-running test
            val totalStressTime = System.currentTimeMillis() - stressStartTime
            val avgIterationTime = performanceReadings.map { it.iterationTime }.average()
            val maxIterationTime = performanceReadings.maxOfOrNull { it.iterationTime } ?: 0
            val initialMemory = performanceReadings.firstOrNull()?.memoryUsageMB ?: 0
            val finalMemory = performanceReadings.lastOrNull()?.memoryUsageMB ?: 0
            val memoryGrowth = finalMemory - initialMemory

            trace.addMetric("stress_total_duration_ms", totalStressTime)
            trace.addMetric("stress_avg_iteration_ms", avgIterationTime.toLong())
            trace.addMetric("stress_max_iteration_ms", maxIterationTime)
            trace.addMetric("stress_memory_growth_mb", memoryGrowth)

            // Verify long-running stability requirements
            assertTrue(
                avgIterationTime < 5000, // Average iteration under 5 seconds
                "Average iteration time should remain reasonable: ${avgIterationTime}ms"
            )

            assertTrue(
                maxIterationTime < 30000, // Max iteration under 30 seconds
                "Max iteration time should be reasonable: ${maxIterationTime}ms"
            )

            assertTrue(
                memoryGrowth < 200, // Memory growth under 200MB
                "Memory growth should be controlled: ${memoryGrowth}MB over ${totalStressTime / 1000}s"
            )

            // Verify app is still responsive after stress testing
            composeTestRule.onNodeWithTag("event_list")
                .performScrollToIndex(50)

            composeTestRule.onAllNodesWithContentDescription("View event details")
                .onFirst()
                .performClick()

            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("event_detail_screen"),
                timeoutMillis = 5000
            )

            println("✅ Long-running stress test completed: ${totalStressTime / 1000}s, Avg iteration ${avgIterationTime}ms, Memory growth ${memoryGrowth}MB")

        } finally {
            stopPerformanceTrace()
        }
    }

    /**
     * Test memory leak detection over extended periods
     */
    @Test
    fun realStability_memoryLeakDetection_preventsLeaks() = runTest {
        val trace = startPerformanceTrace("memory_leak_detection")

        try {
            // Create baseline for memory leak testing
            repeat(50) { index ->
                createTestEvent(
                    eventId = "leak_test_event_$index",
                    latitude = 40.7589 + (index * 0.0001),
                    longitude = -73.9851 + (index * 0.0001),
                    startsSoonInSeconds = index * 60,
                    durationSeconds = 900
                )
            }

            waitForDataSync(5000)

            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("main_screen"),
                timeoutMillis = 15000
            )

            val memorySnapshots = mutableListOf<MemorySnapshot>()
            val leakTestDuration = 20.minutes // Extended period for leak detection
            val startTime = System.currentTimeMillis()

            println("🔍 Starting memory leak detection test for ${leakTestDuration.inWholeMinutes} minutes...")

            var cycleCount = 0
            while (System.currentTimeMillis() - startTime < leakTestDuration.inWholeMilliseconds) {

                // Perform memory-intensive operations that could cause leaks
                val cycleOperations = listOf(
                    {
                        // Event creation and deletion cycle
                        repeat(5) { tempIndex ->
                            createTestEvent(
                                eventId = "temp_leak_test_${cycleCount}_$tempIndex",
                                latitude = 40.7589,
                                longitude = -73.9851,
                                startsSoonInSeconds = 300,
                                durationSeconds = 600
                            )
                        }
                        waitForDataSync(2000)
                    },
                    {
                        // Navigation cycle
                        repeat(3) {
                            composeTestRule.onAllNodesWithContentDescription("View event details")
                                .onFirst()
                                .performClick()
                            delay(1.seconds)
                            composeTestRule.onNodeWithContentDescription("Navigate back")
                                .performClick()
                            delay(500)
                        }
                    },
                    {
                        // Scrolling cycle
                        repeat(10) { scrollIndex ->
                            composeTestRule.onNodeWithTag("event_list")
                                .performScrollToIndex(scrollIndex * 5)
                            delay(200)
                        }
                    }
                )

                // Execute all operations in cycle
                cycleOperations.forEach { operation ->
                    try {
                        operation()
                    } catch (e: Exception) {
                        println("⚠️  Operation failed in cycle $cycleCount: ${e.message}")
                    }
                }

                // Take memory snapshot after each cycle
                val memoryUsage = deviceStateManager.getMemoryUsage()
                memorySnapshots.add(
                    MemorySnapshot(
                        cycle = cycleCount,
                        timestamp = System.currentTimeMillis() - startTime,
                        usedMemoryMB = memoryUsage.usedMemoryMB,
                        availableMemoryMB = memoryUsage.availableMemoryMB
                    )
                )

                cycleCount++

                // Force garbage collection attempt (Android will decide)
                System.gc()
                delay(2.seconds) // Allow GC to run
            }

            // Analyze memory usage patterns for leaks
            val memoryTrend = analyzeMemoryTrend(memorySnapshots)
            val initialMemory = memorySnapshots.first().usedMemoryMB
            val finalMemory = memorySnapshots.last().usedMemoryMB
            val totalMemoryGrowth = finalMemory - initialMemory
            val peakMemory = memorySnapshots.maxOf { it.usedMemoryMB }

            trace.addMetric("leak_test_cycles", cycleCount.toLong())
            trace.addMetric("leak_initial_memory_mb", initialMemory)
            trace.addMetric("leak_final_memory_mb", finalMemory)
            trace.addMetric("leak_peak_memory_mb", peakMemory)
            trace.addMetric("leak_memory_growth_mb", totalMemoryGrowth)
            trace.addMetric("leak_memory_trend", (memoryTrend * 1000).toLong()) // Scale for precision

            // Verify no significant memory leaks
            val acceptableGrowthRate = 2.0 // MB per minute
            val actualGrowthRate = totalMemoryGrowth / (leakTestDuration.inWholeMinutes)

            assertTrue(
                actualGrowthRate < acceptableGrowthRate,
                "Memory growth rate should be <${acceptableGrowthRate}MB/min: ${actualGrowthRate}MB/min"
            )

            assertTrue(
                memoryTrend < 1.0, // Trend should be less than 1MB per cycle
                "Memory trend should be minimal: ${memoryTrend}MB/cycle"
            )

            // Check for memory stability in final third of test
            val finalThirdSnapshots = memorySnapshots.takeLast(memorySnapshots.size / 3)
            val finalStdDev = calculateStandardDeviation(finalThirdSnapshots.map { it.usedMemoryMB.toDouble() })

            assertTrue(
                finalStdDev < 30, // Standard deviation under 30MB in final period
                "Memory usage should be stable in final period: StdDev ${finalStdDev}MB"
            )

            println("✅ Memory leak detection completed: $cycleCount cycles, Growth ${totalMemoryGrowth}MB (${actualGrowthRate}MB/min), Trend ${memoryTrend}MB/cycle")

        } finally {
            stopPerformanceTrace()
        }
    }

    /**
     * Test background task timeout validation
     */
    @Test
    fun realStability_backgroundTaskTimeouts_preventsANR() = runTest {
        val trace = startPerformanceTrace("background_task_timeout_test")

        try {
            // Create events that will trigger background processing
            repeat(30) { index ->
                createTestEvent(
                    eventId = "background_task_event_$index",
                    latitude = 40.7589 + (index * 0.0002),
                    longitude = -73.9851 + (index * 0.0002),
                    startsSoonInSeconds = index * 90,
                    durationSeconds = 1800
                )
            }

            waitForDataSync(8000)

            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("main_screen"),
                timeoutMillis = 15000
            )

            val backgroundTasks = listOf(
                BackgroundTask("data_sync_timeout", 10000) { // 10 second timeout
                    // Simulate data synchronization
                    waitForDataSync(5000)
                },
                BackgroundTask("event_processing_timeout", 15000) { // 15 second timeout
                    // Simulate heavy event processing
                    repeat(20) { index ->
                        composeTestRule.onNodeWithTag("event_list")
                            .performScrollToIndex(index)
                        delay(100)
                    }
                },
                BackgroundTask("network_operation_timeout", 12000) { // 12 second timeout
                    // Simulate network operations
                    createTestEvent(
                        eventId = "bg_network_${System.currentTimeMillis()}",
                        latitude = 40.7589,
                        longitude = -73.9851,
                        startsSoonInSeconds = 300,
                        durationSeconds = 600
                    )
                    waitForDataSync(3000)
                }
            )

            println("⏱️ Testing background task timeout prevention...")

            var timeoutCount = 0
            var anrRiskCount = 0

            backgroundTasks.forEach { task ->
                println("🔄 Testing ${task.name} with ${task.timeoutMs}ms timeout")

                repeat(5) { iteration ->
                    val taskStartTime = System.currentTimeMillis()
                    var taskCompleted = false

                    try {
                        // Execute task with monitoring
                        task.action()
                        val taskDuration = System.currentTimeMillis() - taskStartTime
                        taskCompleted = true

                        trace.addMetric("${task.name}_duration_ms", taskDuration)

                        // Check for potential ANR risk (over 5 seconds)
                        if (taskDuration > 5000) {
                            anrRiskCount++
                            println("⚠️  ANR risk detected in ${task.name} iteration $iteration: ${taskDuration}ms")
                        }

                        // Verify task completed within timeout
                        assertTrue(
                            taskDuration < task.timeoutMs,
                            "${task.name} should complete within timeout: ${taskDuration}ms vs ${task.timeoutMs}ms"
                        )

                    } catch (e: Exception) {
                        val taskDuration = System.currentTimeMillis() - taskStartTime

                        if (taskDuration >= task.timeoutMs) {
                            timeoutCount++
                            println("⏰ Timeout detected in ${task.name} iteration $iteration: ${taskDuration}ms")
                        } else {
                            println("❌ Task failed in ${task.name} iteration $iteration: ${e.message}")
                        }
                    }

                    // Verify UI responsiveness after each background task
                    try {
                        composeTestRule.onNodeWithTag("main_screen").assertExists()

                        // Quick responsiveness test
                        val responsivenesStartTime = System.currentTimeMillis()
                        composeTestRule.onNodeWithTag("event_list")
                            .performScrollToIndex(5)
                        val responsivenessTime = System.currentTimeMillis() - responsivenesStartTime

                        if (responsivenessTime > 2000) { // Over 2 seconds indicates UI blocking
                            anrRiskCount++
                            println("⚠️  UI blocking detected after ${task.name}: ${responsivenessTime}ms")
                        }

                    } catch (e: Exception) {
                        println("❌ UI unresponsive after ${task.name} iteration $iteration: ${e.message}")
                        anrRiskCount++
                    }

                    delay(2.seconds) // Pause between iterations
                }

                println("✅ ${task.name} testing completed")
            }

            trace.addMetric("timeout_count", timeoutCount.toLong())
            trace.addMetric("anr_risk_count", anrRiskCount.toLong())

            // Verify background task stability requirements
            val totalTaskExecutions = backgroundTasks.size * 5
            val timeoutRate = (timeoutCount.toDouble() / totalTaskExecutions) * 100
            val anrRiskRate = (anrRiskCount.toDouble() / totalTaskExecutions) * 100

            assertTrue(
                timeoutRate < 10.0, // Less than 10% timeout rate
                "Background task timeout rate should be <10%: ${timeoutRate}% ($timeoutCount/$totalTaskExecutions)"
            )

            assertTrue(
                anrRiskRate < 5.0, // Less than 5% ANR risk
                "ANR risk rate should be <5%: ${anrRiskRate}% ($anrRiskCount/$totalTaskExecutions)"
            )

            // Final UI responsiveness verification
            composeTestRule.onNodeWithTag("event_list")
                .performScrollToIndex(15)

            composeTestRule.onAllNodesWithContentDescription("View event details")
                .onFirst()
                .performClick()

            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("event_detail_screen"),
                timeoutMillis = 3000
            )

            println("✅ Background task timeout testing completed: $timeoutCount timeouts (${timeoutRate}%), $anrRiskCount ANR risks (${anrRiskRate}%)")

        } finally {
            stopPerformanceTrace()
        }
    }

    // Helper classes and methods

    private data class MonkeyAction(
        val name: String,
        val probability: Double,
        val action: suspend () -> Unit
    )

    private data class StressScenario(
        val name: String,
        val duration: kotlin.time.Duration,
        val action: suspend () -> Unit
    )

    private data class PerformanceReading(
        val timestamp: Long,
        val scenario: String,
        val iterationTime: Long,
        val memoryUsageMB: Long
    )

    private data class MemorySnapshot(
        val cycle: Int,
        val timestamp: Long,
        val usedMemoryMB: Long,
        val availableMemoryMB: Long
    )

    private data class BackgroundTask(
        val name: String,
        val timeoutMs: Long,
        val action: suspend () -> Unit
    )

    private fun selectRandomAction(actions: List<MonkeyAction>): MonkeyAction {
        val random = kotlin.random.Random.nextDouble()
        var cumulativeProbability = 0.0

        actions.forEach { action ->
            cumulativeProbability += action.probability
            if (random <= cumulativeProbability) {
                return action
            }
        }

        return actions.last() // Fallback
    }

    private fun analyzeMemoryTrend(snapshots: List<MemorySnapshot>): Double {
        if (snapshots.size < 2) return 0.0

        val x = snapshots.map { it.cycle.toDouble() }
        val y = snapshots.map { it.usedMemoryMB.toDouble() }

        // Simple linear regression to find trend
        val n = snapshots.size
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y) { xi, yi -> xi * yi }.sum()
        val sumX2 = x.map { it * it }.sum()

        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
    }

    private fun calculateStandardDeviation(values: List<Double>): Double {
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}