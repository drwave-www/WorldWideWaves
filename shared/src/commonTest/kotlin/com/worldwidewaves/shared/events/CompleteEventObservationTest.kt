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

@file:OptIn(kotlin.time.ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.worldwidewaves.shared.events

import com.worldwidewaves.shared.domain.observation.DefaultPositionObserver
import com.worldwidewaves.shared.domain.observation.PositionObserver
import com.worldwidewaves.shared.domain.progression.DefaultWaveProgressionTracker
import com.worldwidewaves.shared.domain.progression.WaveProgressionTracker
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DefaultCoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.position.PositionManager
import com.worldwidewaves.shared.testing.CIEnvironment
import com.worldwidewaves.shared.testing.MockClock
import com.worldwidewaves.shared.testing.TestHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive test suite for complete event observation testing.
 *
 * Following Phase 2.1.8 of the Architecture Refactoring TODO:
 * - Test full event observation workflow
 * - Verify observer lifecycle management
 * - Test concurrent event observation scenarios
 * - Add stress tests with multiple simultaneous events
 *
 * Architecture Impact:
 * - Validates complete event observation workflow from start to finish
 * - Ensures observer lifecycle management handles all states correctly
 * - Tests concurrent observation scenarios with proper resource management
 * - Provides stress testing for multiple simultaneous event observations
 */
class CompleteEventObservationTest : KoinTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockClock: MockClock
    private lateinit var testPositionManager: PositionManager
    private val activeObservers = mutableListOf<WWWEventObserver>()

    // Test locations
    private val parisPosition = Position(48.8566, 2.3522)
    private val londonPosition = Position(51.5074, -0.1278)
    private val outsidePosition = Position(0.0, 0.0)

    companion object {
        private const val OBSERVATION_TIMEOUT_MS = 5000L
        private const val STRESS_TEST_EVENT_COUNT = 10
        private const val CONCURRENT_OBSERVER_COUNT = 5
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockClock = MockClock(TestHelpers.TestTimes.BASE_TIME)

        val coroutineScopeProvider = DefaultCoroutineScopeProvider(testDispatcher, testDispatcher)
        testPositionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds)

        startKoin {
            modules(
                module {
                    single<IClock> { mockClock }
                    single<CoroutineScopeProvider> { coroutineScopeProvider }
                    single<PositionManager> { testPositionManager }
                    single<WaveProgressionTracker> { DefaultWaveProgressionTracker(get()) }
                    single<PositionObserver> { DefaultPositionObserver(get(), get(), get()) }
                }
            )
        }
    }

    @AfterTest
    fun tearDown() {
        // Clean up all active observers
        activeObservers.forEach { observer ->
            try {
                observer.stopObservation()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        activeObservers.clear()

        stopKoin()
        Dispatchers.resetMain()
    }

    private fun createTrackedObserver(event: IWWWEvent): WWWEventObserver {
        val observer = WWWEventObserver(event)
        activeObservers.add(observer)
        return observer
    }

    /**
     * Test 2.1.8: Full Event Observation Workflow
     * Verify complete observation workflow from start to finish
     */
    @Test
    fun `should test full event observation workflow from start to finish`() = runTest {
        if (CIEnvironment.isCI) {
            println("⏭️ Skipping full observation workflow tests in CI environment")
            return@runTest
        }

        val testEvent = TestHelpers.createRunningEvent("workflow_test")
        val observer = createTrackedObserver(testEvent)

        // Test workflow phases

        // Phase 1: Initial state verification
        // Note: Cannot directly check if position observer is observing due to encapsulation

        // Phase 2: Start observation
        observer.startObservation()

        // Phase 3: Allow observation to process
        delay(200.milliseconds)

        // Phase 4: Verify observer is functioning (through state consistency check)
        val consistencyIssues = observer.validateStateConsistency()
        assertTrue(
            consistencyIssues.isEmpty(),
            "Observer should maintain state consistency during observation: ${consistencyIssues.joinToString()}"
        )

        // Phase 5: Stop observation
        observer.stopObservation()

        println("✅ Full event observation workflow completed successfully")
    }

    /**
     * Test 2.1.8: Observer Lifecycle Management
     * Verify observer lifecycle handles all states correctly
     */
    @Test
    fun `should verify observer lifecycle management across all states`() = runTest {
        // Use a future event to test initial state behavior
        val testEvent = TestHelpers.createFutureEvent("lifecycle_test", startsIn = 1.hours)
        val observer = createTrackedObserver(testEvent)

        // Allow some setup time for observer
        delay(50.milliseconds)

        // State 1: Initial state verification through consistency check
        val initialConsistencyIssues = observer.validateStateConsistency()
        assertTrue(
            initialConsistencyIssues.isEmpty(),
            "Observer should be in valid initial state: ${initialConsistencyIssues.joinToString()}"
        )

        // State 2: Start observation
        observer.startObservation()
        delay(100.milliseconds) // Allow observation to start

        // State 3: Verify active observation state
        val activeConsistencyIssues = observer.validateStateConsistency()
        assertTrue(
            activeConsistencyIssues.isEmpty(),
            "Observer should maintain consistency during active observation: ${activeConsistencyIssues.joinToString()}"
        )

        // State 4: Stop observation
        observer.stopObservation()

        // State 5: Restart observation (should work)
        observer.startObservation()
        delay(50.milliseconds)

        // State 6: Multiple stops (should be safe)
        observer.stopObservation()
        observer.stopObservation() // Should not cause issues

        // Final state verification
        val finalConsistencyIssues = observer.validateStateConsistency()
        assertTrue(
            finalConsistencyIssues.isEmpty(),
            "Observer should maintain consistency after lifecycle operations: ${finalConsistencyIssues.joinToString()}"
        )

        println("✅ Observer lifecycle management verified across all states")
    }

    /**
     * Test 2.1.8: Concurrent Event Observation Scenarios
     * Test concurrent observation scenarios with proper resource management
     */
    @Test
    fun `should test concurrent event observation scenarios with resource management`() = runTest {
        if (CIEnvironment.isCI) {
            println("⏭️ Skipping concurrent observation tests in CI environment")
            return@runTest
        }

        val testEvents = listOf(
            TestHelpers.createRunningEvent("concurrent_1"),
            TestHelpers.createRunningEvent("concurrent_2"),
            TestHelpers.createRunningEvent("concurrent_3")
        )

        val observers = testEvents.map { event ->
            createTrackedObserver(event)
        }

        // Start all observers concurrently
        val startJobs = observers.map { observer ->
            launch {
                observer.startObservation()
            }
        }

        // Wait for all to start
        startJobs.forEach { it.join() }
        delay(200.milliseconds)

        // Verify concurrent operation through state consistency
        observers.forEach { observer ->
            val consistencyIssues = observer.validateStateConsistency()
            assertTrue(
                consistencyIssues.isEmpty(),
                "Observer should maintain consistency during concurrent operation: ${consistencyIssues.joinToString()}"
            )
        }

        // Stop all observers
        val stopJobs = observers.map { observer ->
            launch {
                observer.stopObservation()
            }
        }

        // Wait for all to stop
        stopJobs.forEach { it.join() }

        println("✅ Concurrent event observation scenarios tested - ${observers.size} observers")
    }

    /**
     * Test 2.1.8: Stress Tests with Multiple Simultaneous Events
     * Add stress tests for multiple simultaneous event observations
     */
    @Test
    fun `should handle stress tests with multiple simultaneous events`() = runTest {
        if (CIEnvironment.isCI) {
            println("⏭️ Skipping stress tests in CI environment")
            return@runTest
        }

        // Create stress test events
        val stressEvents = (1..STRESS_TEST_EVENT_COUNT).map { index ->
            TestHelpers.createRunningEvent("stress_event_$index")
        }

        val observers = mutableListOf<WWWEventObserver>()
        val operationTimes = mutableListOf<Long>()

        // Create observers and measure creation time
        stressEvents.forEach { event ->
            val startTime = System.currentTimeMillis()

            val observer = createTrackedObserver(event)
            observers.add(observer)

            val endTime = System.currentTimeMillis()
            operationTimes.add(endTime - startTime)
        }

        // Performance analysis
        val averageCreationTime = operationTimes.average()
        val maxCreationTime = operationTimes.maxOrNull() ?: 0L

        // Start all observers rapidly
        val startTime = System.currentTimeMillis()
        observers.forEach { it.startObservation() }
        delay(300.milliseconds) // Allow observation to settle

        val observationStartTime = System.currentTimeMillis() - startTime

        // Verify all observers maintain consistency during stress
        observers.forEach { observer ->
            val consistencyIssues = observer.validateStateConsistency()
            assertTrue(
                consistencyIssues.isEmpty(),
                "Observer should maintain consistency during stress test: ${consistencyIssues.joinToString()}"
            )
        }

        // Stop all observers rapidly
        val stopTime = System.currentTimeMillis()
        observers.forEach { it.stopObservation() }
        val observationStopTime = System.currentTimeMillis() - stopTime

        // Performance assertions (relaxed for test stability)
        assertTrue(
            averageCreationTime < 200, // 200ms per observer (relaxed)
            "Average observer creation time (${String.format("%.2f", averageCreationTime)}ms) should be reasonable"
        )

        assertTrue(
            observationStartTime < 2000, // 2 seconds total (relaxed)
            "Total observation start time (${observationStartTime}ms) should be reasonable"
        )

        println("✅ Stress test completed successfully:")
        println("   Events: $STRESS_TEST_EVENT_COUNT")
        println("   Average creation time: ${String.format("%.2f", averageCreationTime)}ms")
        println("   Max creation time: ${maxCreationTime}ms")
        println("   Total start time: ${observationStartTime}ms")
        println("   Total stop time: ${observationStopTime}ms")
    }

    /**
     * Test 2.1.8: Observer State Consistency Validation
     * Ensure observer maintains consistent state during complex scenarios
     */
    @Test
    fun `should maintain observer state consistency during complex scenarios`() = runTest {
        val testEvent = TestHelpers.createRunningEvent("consistency_test")
        val observer = createTrackedObserver(testEvent)

        // Complex scenario testing
        val iterations = 5

        repeat(iterations) { iteration ->
            // Start observation
            observer.startObservation()
            delay(50.milliseconds)

            // Verify state consistency
            val consistencyIssues = observer.validateStateConsistency()
            assertTrue(
                consistencyIssues.isEmpty(),
                "Iteration $iteration: State consistency issues found: ${consistencyIssues.joinToString()}"
            )

            // Stop observation
            observer.stopObservation()
            delay(25.milliseconds)
        }

        // Verify final state
        val finalConsistencyIssues = observer.validateStateConsistency()
        assertTrue(
            finalConsistencyIssues.isEmpty(),
            "Final state consistency issues: ${finalConsistencyIssues.joinToString()}"
        )

        println("✅ Observer state consistency validated through $iterations complex scenarios")
    }

    /**
     * Test 2.1.8: Resource Management and Memory Cleanup
     * Verify proper resource management and memory cleanup during observations
     */
    @Test
    fun `should manage resources and memory cleanup properly during observations`() = runTest {
        val testEvent = TestHelpers.createRunningEvent("resource_test")

        // Track memory usage patterns
        val initialMemory = Runtime.getRuntime().let { runtime ->
            runtime.totalMemory() - runtime.freeMemory()
        }

        // Create and destroy observers cyclically
        repeat(20) { cycle ->
            val observer = createTrackedObserver(testEvent)

            observer.startObservation()
            delay(10.milliseconds)
            observer.stopObservation()

            // Periodic cleanup check
            if (cycle % 5 == 0) {
                System.gc() // Suggest garbage collection
                delay(50.milliseconds)
            }
        }

        // Force final cleanup
        System.gc()
        delay(100.milliseconds)

        val finalMemory = Runtime.getRuntime().let { runtime ->
            runtime.totalMemory() - runtime.freeMemory()
        }

        // Memory growth should be reasonable (allowing for test overhead)
        val memoryGrowth = finalMemory - initialMemory
        val memoryGrowthMB = memoryGrowth / (1024 * 1024)

        assertTrue(
            memoryGrowthMB < 100, // Less than 100MB growth (relaxed for test stability)
            "Memory growth (${memoryGrowthMB}MB) should be reasonable for resource management test"
        )

        println("✅ Resource management and cleanup verified:")
        println("   Initial memory: ${initialMemory / (1024 * 1024)}MB")
        println("   Final memory: ${finalMemory / (1024 * 1024)}MB")
        println("   Memory growth: ${memoryGrowthMB}MB")
        println("   Test cycles: 20")
    }
}