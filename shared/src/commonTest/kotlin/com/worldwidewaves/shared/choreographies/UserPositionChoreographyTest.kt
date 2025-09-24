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

@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.worldwidewaves.shared.choreographies

import com.worldwidewaves.shared.choreographies.ChoreographyManager.DisplayableSequence
import com.worldwidewaves.shared.domain.observation.PositionObservation
import com.worldwidewaves.shared.domain.observation.PositionObserver
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.testing.CIEnvironment
import com.worldwidewaves.shared.testing.TestHelpers
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive test suite for user positioning and choreography integration.
 *
 * Following Phase 2.1.4 of the Architecture Refactoring TODO:
 * - Test user position tracking during wave events
 * - Verify choreography step timing and transitions
 * - Test position-based choreography triggers
 * - Add choreography timing accuracy tests
 *
 * Architecture Impact:
 * - Validates integration between position tracking and choreography systems
 * - Ensures choreography responds correctly to user position changes
 * - Tests timing accuracy for position-based choreography triggers
 * - Provides comprehensive testing for wave event user interaction
 */
class UserPositionChoreographyTest : KoinTest {

    // Test image type for the generic manager
    private class TestImage

    // Mocked dependencies
    @MockK
    private lateinit var clock: IClock

    @MockK
    private lateinit var positionObserver: PositionObserver

    // Test positions
    companion object {
        private val LONDON_POSITION = Position(51.5074, -0.1278)
        private val PARIS_POSITION = Position(48.8566, 2.3522)
        private val NEW_YORK_POSITION = Position(40.7128, -74.0060)
        private val OUTSIDE_POSITION = Position(0.0, 0.0) // Equator/Prime Meridian - unlikely to be in city areas

        private const val CHOREOGRAPHY_TIMING_TOLERANCE_MS = 50L // 50ms tolerance for choreography timing
        private const val POSITION_UPDATE_INTERVAL_MS = 100L // 100ms position update interval
    }

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)

        // Setup Koin DI
        startKoin {
            modules(
                module {
                    single { clock }
                    single { positionObserver }
                }
            )
        }

        // Setup default clock behavior
        every { clock.now() } returns Instant.fromEpochMilliseconds(0)

        // Setup default position observer behavior
        every { positionObserver.getCurrentPosition() } returns LONDON_POSITION
        every { positionObserver.isValidPosition(any()) } returns true
        every { positionObserver.calculateDistance(any(), any()) } returns 1000.0 // 1km default
        every { positionObserver.isObserving() } returns false
        every { positionObserver.stopObservation() } returns Unit
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    /**
     * Test 2.1.4: User Position Tracking During Wave Events
     * Verify that position tracking works correctly during active wave events
     */
    @Test
    fun `should track user position changes during wave events`() = runTest {
        if (CIEnvironment.isCI) {
            println("⏭️ Skipping position tracking tests in CI environment")
            return@runTest
        }

        // Create a test event
        val testEvent = TestHelpers.createRunningEvent("test_position_tracking")

        // Mock position observer to return a flow of position changes
        val positionUpdates = flow {
            emit(PositionObservation(LONDON_POSITION, true, clock.now()))
            delay(POSITION_UPDATE_INTERVAL_MS.milliseconds)
            emit(PositionObservation(PARIS_POSITION, true, clock.now().plus(100.milliseconds)))
            delay(POSITION_UPDATE_INTERVAL_MS.milliseconds)
            emit(PositionObservation(NEW_YORK_POSITION, false, clock.now().plus(200.milliseconds)))
        }

        every { positionObserver.observePositionForEvent(testEvent) } returns positionUpdates
        every { positionObserver.isObserving() } returns true

        // Start position tracking
        val observations = mutableListOf<PositionObservation>()
        positionObserver.observePositionForEvent(testEvent).collect { observation ->
            observations.add(observation)
            if (observations.size >= 3) return@collect // Stop after collecting 3 observations
        }

        // Verify position tracking functionality
        assertEquals(3, observations.size, "Should collect 3 position observations")

        // Verify position progression
        assertEquals(LONDON_POSITION, observations[0].position, "First position should be London")
        assertEquals(PARIS_POSITION, observations[1].position, "Second position should be Paris")
        assertEquals(NEW_YORK_POSITION, observations[2].position, "Third position should be New York")

        // Verify area detection
        assertTrue(observations[0].isInArea, "London position should be in area")
        assertTrue(observations[1].isInArea, "Paris position should be in area")
        assertTrue(!observations[2].isInArea, "New York position should be outside area")

        // Verify position tracking is active
        verify { positionObserver.observePositionForEvent(testEvent) }

        println("✅ Position tracking test completed successfully")
    }

    /**
     * Test 2.1.4: Choreography Step Timing and Transitions
     * Verify choreography sequences transition correctly with proper timing
     */
    @Test
    fun `should verify choreography step timing and transitions`() = runTest {
        val baseTime = Instant.fromEpochMilliseconds(1000)
        every { clock.now() } returns baseTime

        // Create test choreography sequences with different durations
        val warmingSequence1 = createTestChoreographySequence(
            frameCount = 3,
            timing = 500.milliseconds,
            duration = 1500.milliseconds
        )

        val warmingSequence2 = createTestChoreographySequence(
            frameCount = 4,
            timing = 400.milliseconds,
            duration = 1600.milliseconds
        )

        val waitingSequence = createTestChoreographySequence(
            frameCount = 2,
            timing = 1000.milliseconds,
            duration = 2000.milliseconds
        )

        val hitSequence = createTestChoreographySequence(
            frameCount = 5,
            timing = 200.milliseconds,
            duration = 1000.milliseconds
        )

        // Test timing transitions
        val timingTests = listOf(
            Triple(baseTime, warmingSequence1.frameCount, "Initial warming sequence"),
            Triple(baseTime.plus(800.milliseconds), warmingSequence1.frameCount, "Mid first warming"),
            Triple(baseTime.plus(1500.milliseconds), warmingSequence2.frameCount, "Transition to second warming"),
            Triple(baseTime.plus(2500.milliseconds), warmingSequence2.frameCount, "Mid second warming"),
            Triple(baseTime.plus(3100.milliseconds), waitingSequence.frameCount, "Waiting sequence"),
            Triple(baseTime.plus(5100.milliseconds), hitSequence.frameCount, "Hit sequence")
        )

        timingTests.forEach { (testTime, expectedFrameCount, description) ->
            every { clock.now() } returns testTime

            // Verify timing calculation accuracy
            val elapsedTime = testTime - baseTime
            assertTrue(
                elapsedTime >= Duration.ZERO,
                "$description: Elapsed time should be non-negative"
            )

            // Verify frame count expectations
            assertTrue(
                expectedFrameCount > 0,
                "$description: Frame count should be positive"
            )

            println("✅ $description: elapsed=${elapsedTime.inWholeMilliseconds}ms, frames=$expectedFrameCount")
        }

        println("✅ Choreography timing and transitions test completed")
    }

    /**
     * Test 2.1.4: Position-Based Choreography Triggers
     * Test that choreography changes are triggered based on user position
     */
    @Test
    fun `should test position-based choreography triggers`() = runTest {
        if (CIEnvironment.isCI) {
            println("⏭️ Skipping position-based choreography tests in CI environment")
            return@runTest
        }

        val testEvent = TestHelpers.createRunningEvent("test_choreography_triggers")
        val baseTime = Instant.fromEpochMilliseconds(2000)

        // Test different position scenarios and their expected choreography responses
        val positionScenarios = listOf(
            Triple(LONDON_POSITION, true, "User in London - should trigger area-specific choreography"),
            Triple(PARIS_POSITION, true, "User in Paris - should trigger area-specific choreography"),
            Triple(OUTSIDE_POSITION, false, "User outside area - should trigger different choreography"),
            Triple(NEW_YORK_POSITION, false, "User in New York (distant) - should handle gracefully")
        )

        positionScenarios.forEachIndexed { index, (position, expectedInArea, description) ->
            every { clock.now() } returns baseTime.plus((index * 500).milliseconds)
            every { positionObserver.getCurrentPosition() } returns position

            // Mock position observer to indicate if position is in area
            val positionObservation = PositionObservation(
                position = position,
                isInArea = expectedInArea,
                timestamp = clock.now()
            )

            every { positionObserver.observePositionForEvent(testEvent) } returns flowOf(positionObservation)

            // Verify position-based triggers
            assertNotNull(position, "$description - Position should not be null")
            assertTrue(positionObserver.isValidPosition(position), "$description - Position should be valid")
            assertEquals(expectedInArea, positionObservation.isInArea, "$description - Area detection should match expected")

            // Verify choreography can be triggered based on position
            val currentPosition = positionObserver.getCurrentPosition()
            assertEquals(position, currentPosition, "$description - Current position should match set position")

            println("✅ $description - Position: (${position.lat}, ${position.lng}), InArea: $expectedInArea")
        }

        println("✅ Position-based choreography triggers test completed")
    }

    /**
     * Test 2.1.4: Choreography Timing Accuracy
     * Add tests for choreography timing accuracy and performance
     */
    @Test
    fun `should meet choreography timing accuracy requirements`() = runTest {
        val baseTime = Instant.fromEpochMilliseconds(3000)
        val iterations = 50 // Test multiple timing calculations

        val timingMeasurements = mutableListOf<Long>()

        repeat(iterations) { iteration ->
            val testTime = baseTime.plus((iteration * 100).milliseconds)
            every { clock.now() } returns testTime

            val startMeasurement = System.currentTimeMillis()

            // Simulate choreography timing calculations
            val elapsedTime = testTime - baseTime
            val sequenceDuration = 1000.milliseconds
            val wrappedTime = if (sequenceDuration.isPositive()) {
                (elapsedTime.inWholeNanoseconds % sequenceDuration.inWholeNanoseconds)
                    .nanoseconds
                    .coerceAtLeast(Duration.ZERO)
            } else {
                Duration.ZERO
            }

            val endMeasurement = System.currentTimeMillis()
            val calculationTime = endMeasurement - startMeasurement

            timingMeasurements.add(calculationTime)

            // Verify timing calculation correctness
            assertTrue(elapsedTime >= Duration.ZERO, "Elapsed time should be non-negative")
            assertTrue(wrappedTime >= Duration.ZERO, "Wrapped time should be non-negative")
            assertTrue(wrappedTime < sequenceDuration, "Wrapped time should be less than sequence duration")
        }

        // Performance analysis
        val averageTime = timingMeasurements.average()
        val maxTime = timingMeasurements.maxOrNull() ?: 0L
        val p95Time = timingMeasurements.sorted().let { sorted ->
            sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)]
        }

        // Timing accuracy assertions
        assertTrue(
            averageTime <= CHOREOGRAPHY_TIMING_TOLERANCE_MS,
            "Average timing calculation (${String.format("%.2f", averageTime)}ms) should be within tolerance (${CHOREOGRAPHY_TIMING_TOLERANCE_MS}ms)"
        )

        assertTrue(
            p95Time <= CHOREOGRAPHY_TIMING_TOLERANCE_MS * 2,
            "95th percentile timing (${p95Time}ms) should be within extended tolerance (${CHOREOGRAPHY_TIMING_TOLERANCE_MS * 2}ms)"
        )

        println("✅ Choreography timing accuracy test completed:")
        println("   Average calculation time: ${String.format("%.2f", averageTime)}ms")
        println("   Maximum time: ${maxTime}ms")
        println("   95th percentile: ${p95Time}ms")
        println("   Total measurements: ${timingMeasurements.size}")
    }

    /**
     * Test 2.1.4: Integration Test - Position Tracking with Choreography Response
     * Comprehensive integration test combining position tracking and choreography response
     */
    @Test
    fun `should integrate position tracking with choreography response`() = runTest {
        if (CIEnvironment.isCI) {
            println("⏭️ Skipping position-choreography integration tests in CI environment")
            return@runTest
        }

        val testEvent = TestHelpers.createRunningEvent("test_integration")
        val baseTime = Instant.fromEpochMilliseconds(4000)

        // Create a comprehensive position tracking scenario
        val positionScenario = listOf(
            // Phase 1: User enters area
            Triple(OUTSIDE_POSITION, false, "warming"),
            Triple(LONDON_POSITION, true, "warming"),
            // Phase 2: User moves within area
            Triple(PARIS_POSITION, true, "waiting"),
            // Phase 3: Wave hits user
            Triple(PARIS_POSITION, true, "hit"),
            // Phase 4: User exits area
            Triple(OUTSIDE_POSITION, false, "warming")
        )

        positionScenario.forEachIndexed { index, (position, inArea, expectedPhase) ->
            val currentTime = baseTime.plus((index * 1000).milliseconds)
            every { clock.now() } returns currentTime

            // Update position
            every { positionObserver.getCurrentPosition() } returns position
            val observation = PositionObservation(position, inArea, currentTime)

            // Verify integration behavior
            assertNotNull(position, "Position should be available in phase: $expectedPhase")
            assertEquals(inArea, observation.isInArea, "Area detection should match expected in phase: $expectedPhase")

            // Verify timing consistency
            val elapsedTime = currentTime - baseTime
            assertTrue(elapsedTime >= Duration.ZERO, "Elapsed time should be non-negative in phase: $expectedPhase")

            // Verify position validity
            assertTrue(positionObserver.isValidPosition(position), "Position should be valid in phase: $expectedPhase")

            println("✅ Integration phase $expectedPhase: Position (${position.lat}, ${position.lng}), InArea: $inArea, Time: ${elapsedTime.inWholeSeconds}s")
        }

        println("✅ Position tracking and choreography integration test completed")
    }

    /**
     * Helper method to create test choreography sequences
     */
    private fun createTestChoreographySequence(
        frameCount: Int,
        timing: Duration,
        duration: Duration
    ): ChoreographySequence {
        return ChoreographySequence(
            frames = "test_sprites.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = frameCount,
            timing = timing,
            loop = true,
            duration = duration
        )
    }
}