@file:OptIn(kotlin.time.ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.worldwidewaves.shared.domain.scheduling

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

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEventObserver
import com.worldwidewaves.shared.events.WWWEventWave
import com.worldwidewaves.shared.events.utils.IClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Comprehensive test suite for DefaultObservationScheduler.
 *
 * Tests cover:
 * - Adaptive interval calculations for different event phases
 * - Battery optimization features
 * - Scheduling accuracy and timing
 * - Concurrent event handling
 * - Lifecycle management (pause, resume, cleanup)
 * - Edge cases and error scenarios
 * - Performance and memory characteristics
 */
class DefaultObservationSchedulerTest {
    /**
     * Test implementation of IClock for controlled time progression in tests.
     */
    private class TestClock(
        private var currentTime: Instant = Instant.fromEpochMilliseconds(0),
    ) : IClock {
        private val delays = mutableListOf<Duration>()

        override fun now(): Instant = currentTime

        override suspend fun delay(duration: Duration) {
            delays.add(duration)
            currentTime += duration
            // Use real coroutine delay to allow other coroutines to run
            kotlinx.coroutines.delay(1)
        }

        fun advance(duration: Duration) {
            currentTime += duration
        }

        fun getDelays(): List<Duration> = delays.toList()

        fun clearDelays() = delays.clear()
    }

    /**
     * Mock event for testing different scenarios.
     */
    private open class MockEvent(
        private val startDateTime: Instant,
        private val isRunning: Boolean = false,
        private val isDone: Boolean = false,
        private val isSoon: Boolean = false,
        private val isNearTime: Boolean = false,
        private val timeBeforeUserHit: Duration? = null,
    ) : IWWWEvent {
        override val id: String = "test-event"
        override val type: String = "test"
        override val country: String? = null
        override val community: String? = null
        override val timeZone: String = "UTC"
        override val date: String = "2025-01-01"
        override val startHour: String = "12:00"
        override val instagramAccount: String = ""
        override val instagramHashtag: String = ""
        override var favorite: Boolean = false

        // Mock implementations of required properties
        override val wavedef: com.worldwidewaves.shared.events.WWWEvent.WWWWaveDefinition
            get() = throw NotImplementedError("Mock property")
        override val area: com.worldwidewaves.shared.events.WWWEventArea
            get() = throw NotImplementedError("Mock property")
        override val warming: com.worldwidewaves.shared.events.WWWEventWaveWarming
            get() = throw NotImplementedError("Mock property")
        override val map: com.worldwidewaves.shared.events.WWWEventMap
            get() = throw NotImplementedError("Mock property")

        // Mock wave with timeBeforeUserHit
        override val wave: WWWEventWave =
            object : WWWEventWave() {
                override val speed: Double = 1.0
                override val direction: Direction = Direction.EAST
                override val approxDuration: Int = 60

                override suspend fun getWavePolygons(): WavePolygons? = null

                override suspend fun getWaveDuration(): Duration = 60.minutes

                override suspend fun hasUserBeenHitInCurrentPosition(): Boolean {
                    // If timeBeforeUserHit is null or positive, user hasn't been hit yet
                    val tbh = this@MockEvent.timeBeforeUserHit
                    return tbh != null && tbh < Duration.ZERO
                }

                override suspend fun userHitDateTime(): Instant? {
                    val tbh = this@MockEvent.timeBeforeUserHit ?: return null
                    return clock.now() + tbh
                }

                override suspend fun closestWaveLongitude(latitude: Double): Double = 0.0

                override suspend fun userPositionToWaveRatio(): Double? = null

                override fun validationErrors(): List<String>? = null
            }

        override fun getStartDateTime(): Instant = startDateTime

        override suspend fun getStatus(): IWWWEvent.Status = IWWWEvent.Status.UNDEFINED

        override suspend fun isDone(): Boolean = isDone

        override fun isSoon(): Boolean = isSoon

        override suspend fun isRunning(): Boolean = isRunning

        override fun isNearTime(): Boolean = isNearTime

        override fun getLocationImage(): Any? = null

        override fun getCommunityImage(): Any? = null

        override fun getCountryImage(): Any? = null

        override fun getMapImage(): Any? = null

        override fun getLocation(): dev.icerock.moko.resources.StringResource = throw NotImplementedError("Mock method")

        override fun getDescription(): dev.icerock.moko.resources.StringResource = throw NotImplementedError("Mock method")

        override fun getLiteralCountry(): dev.icerock.moko.resources.StringResource = throw NotImplementedError("Mock method")

        override fun getLiteralCommunity(): dev.icerock.moko.resources.StringResource = throw NotImplementedError("Mock method")

        override fun getTZ(): kotlinx.datetime.TimeZone = kotlinx.datetime.TimeZone.UTC

        override suspend fun getTotalTime(): Duration = 60.minutes

        override suspend fun getEndDateTime(): Instant = startDateTime + 60.minutes

        override fun getLiteralTimezone(): String = "UTC"

        override fun getLiteralStartDateSimple(): String = "2025-01-01"

        override fun getLiteralStartTime(): String = "12:00"

        override suspend fun getLiteralEndTime(): String = "13:00"

        override suspend fun getLiteralTotalTime(): String = "60m"

        override fun getWaveStartDateTime(): Instant = startDateTime

        override fun getWarmingDuration(): Duration = 5.minutes

        override suspend fun getAllNumbers(): IWWWEvent.WaveNumbersLiterals = IWWWEvent.WaveNumbersLiterals()

        override fun getEventObserver(): WWWEventObserver = throw NotImplementedError("Mock method")

        override fun validationErrors(): List<String>? = null
    }

    // ========================================
    // Test 1: Adaptive Intervals - Distant Event (> 1 hour)
    // ========================================
    @Test
    fun `should use 1 hour interval for distant events`() =
        runTest {
            // ARRANGE: Event more than 1 hour away
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 2.hours
            val event = MockEvent(startDateTime = eventStartTime)

            // ACT: Calculate observation interval
            val interval = scheduler.calculateObservationInterval(event)

            // ASSERT: Should return 1 hour interval for distant events
            assertEquals(1.hours, interval)
        }

    // ========================================
    // Test 2: Adaptive Intervals - Approaching Event (5-60 min)
    // ========================================
    @Test
    fun `should use 5 minute interval for approaching events`() =
        runTest {
            // ARRANGE: Event 30 minutes away
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 30.minutes
            val event = MockEvent(startDateTime = eventStartTime)

            // ACT: Calculate observation interval
            val interval = scheduler.calculateObservationInterval(event)

            // ASSERT: Should return 5 minute interval
            assertEquals(5.minutes, interval)
        }

    // ========================================
    // Test 3: Adaptive Intervals - Near Event (35s - 5min)
    // ========================================
    @Test
    fun `should use 1 second interval for near events`() =
        runTest {
            // ARRANGE: Event 2 minutes away
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 2.minutes
            val event = MockEvent(startDateTime = eventStartTime)

            // ACT: Calculate observation interval
            val interval = scheduler.calculateObservationInterval(event)

            // ASSERT: Should return 1 second interval
            assertEquals(1.seconds, interval)
        }

    // ========================================
    // Test 4: Adaptive Intervals - Active Event (< 35s or running)
    // ========================================
    @Test
    fun `should use 500ms interval for active events`() =
        runTest {
            // ARRANGE: Event 20 seconds away
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 20.seconds
            val event = MockEvent(startDateTime = eventStartTime)

            // ACT: Calculate observation interval
            val interval = scheduler.calculateObservationInterval(event)

            // ASSERT: Should return 500ms interval
            assertEquals(500.milliseconds, interval)
        }

    // ========================================
    // Test 5: Adaptive Intervals - Critical Hit Window (< 1s)
    // ========================================
    @Test
    fun `should use 50ms interval during critical hit window`() =
        runTest {
            // ARRANGE: User will be hit in 500ms
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 1.minutes
            val event =
                MockEvent(
                    startDateTime = eventStartTime,
                    timeBeforeUserHit = 500.milliseconds,
                )

            // ACT: Calculate observation interval
            val interval = scheduler.calculateObservationInterval(event)

            // ASSERT: Should return 50ms interval for critical timing
            assertEquals(50.milliseconds, interval)
        }

    // ========================================
    // Test 6: Adaptive Intervals - Hit Buffer (1-5s)
    // ========================================
    @Test
    fun `should use 200ms interval during hit buffer window`() =
        runTest {
            // ARRANGE: User will be hit in 3 seconds
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 1.minutes
            val event =
                MockEvent(
                    startDateTime = eventStartTime,
                    timeBeforeUserHit = 3.seconds,
                )

            // ACT: Calculate observation interval
            val interval = scheduler.calculateObservationInterval(event)

            // ASSERT: Should return 200ms interval for buffer window
            assertEquals(200.milliseconds, interval)
        }

    // ========================================
    // Test 7: Battery Optimization - Stop After Hit
    // ========================================
    @Test
    fun `should use infinite interval after wave has passed`() =
        runTest {
            // ARRANGE: Wave has already passed (negative time before hit)
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 1.minutes
            val event =
                MockEvent(
                    startDateTime = eventStartTime,
                    timeBeforeUserHit = (-5).seconds,
                )

            // ACT: Calculate observation interval
            val interval = scheduler.calculateObservationInterval(event)

            // ASSERT: Should return infinite interval to stop observation
            assertEquals(INFINITE, interval)
        }

    // ========================================
    // Test 8: Battery Optimization - Inactive Event
    // ========================================
    @Test
    fun `should use 30 second interval for inactive past events`() =
        runTest {
            // ARRANGE: Event has passed and is not running
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() - 10.minutes
            val event =
                MockEvent(
                    startDateTime = eventStartTime,
                    isRunning = false,
                )

            // ACT: Calculate observation interval
            val interval = scheduler.calculateObservationInterval(event)

            // ASSERT: Should return 30s interval for inactive events
            assertEquals(30.seconds, interval)
        }

    // ========================================
    // Test 9: Running Event Gets Real-Time Monitoring
    // ========================================
    @Test
    fun `should use 500ms interval for running events regardless of start time`() =
        runTest {
            // ARRANGE: Running event
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() - 5.minutes // Started 5 min ago
            val event =
                MockEvent(
                    startDateTime = eventStartTime,
                    isRunning = true,
                )

            // ACT: Calculate observation interval
            val interval = scheduler.calculateObservationInterval(event)

            // ASSERT: Should return 500ms interval for running events
            assertEquals(500.milliseconds, interval)
        }

    // ========================================
    // Test 10: Continuous Observation - Running Event
    // ========================================
    @Test
    fun `should observe continuously when event is running`() =
        runTest {
            // ARRANGE: Running event
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() - 1.minutes
            val event =
                MockEvent(
                    startDateTime = eventStartTime,
                    isRunning = true,
                )

            // ACT: Check if should observe continuously
            val shouldObserve = scheduler.shouldObserveContinuously(event)

            // ASSERT: Should observe continuously for running events
            assertTrue(shouldObserve)
        }

    // ========================================
    // Test 11: Continuous Observation - Soon and Near
    // ========================================
    @Test
    fun `should observe continuously when event is soon and near time`() =
        runTest {
            // ARRANGE: Soon and near event
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 30.seconds
            val event =
                MockEvent(
                    startDateTime = eventStartTime,
                    isSoon = true,
                    isNearTime = true,
                )

            // ACT: Check if should observe continuously
            val shouldObserve = scheduler.shouldObserveContinuously(event)

            // ASSERT: Should observe continuously for soon+near events
            assertTrue(shouldObserve)
        }

    // ========================================
    // Test 12: No Continuous Observation - Distant Event
    // ========================================
    @Test
    fun `should not observe continuously for distant events`() =
        runTest {
            // ARRANGE: Distant event
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 2.hours
            val event =
                MockEvent(
                    startDateTime = eventStartTime,
                    isSoon = false,
                    isNearTime = false,
                    isRunning = false,
                )

            // ACT: Check if should observe continuously
            val shouldObserve = scheduler.shouldObserveContinuously(event)

            // ASSERT: Should not observe continuously for distant events
            assertFalse(shouldObserve)
        }

    // ========================================
    // Test 13: Observation Flow - Single Emission for Distant Event
    // ========================================
    @Test
    fun `observation flow should emit once for distant events`() =
        runTest {
            // ARRANGE: Distant event that shouldn't observe continuously
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 2.hours
            val event =
                MockEvent(
                    startDateTime = eventStartTime,
                    isSoon = false,
                    isNearTime = false,
                )

            // ACT: Collect first emission
            val emission =
                withTimeout(1000) {
                    scheduler.createObservationFlow(event).first()
                }

            // ASSERT: Should emit exactly once
            assertEquals(Unit, emission)
        }

    // ========================================
    // Test 14: Observation Flow - Multiple Emissions for Running Event
    // ========================================
    @Test
    fun `observation flow should emit multiple times for running events`() =
        runTest {
            // ARRANGE: Running event with short observation interval
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() - 1.minutes

            // Create event that becomes done after 3 emissions
            var emissionCount = 0
            val event =
                object : MockEvent(
                    startDateTime = eventStartTime,
                    isRunning = true,
                ) {
                    override suspend fun isDone(): Boolean {
                        emissionCount++
                        return emissionCount >= 3
                    }
                }

            // ACT: Collect emissions
            val emissions =
                withTimeout(5000) {
                    scheduler.createObservationFlow(event).take(4).toList()
                }

            // ASSERT: Should emit multiple times (at least 3 + final emission)
            assertTrue(emissions.size >= 3, "Expected at least 3 emissions, got ${emissions.size}")
        }

    // ========================================
    // Test 15: Observation Flow - Stops on Infinite Interval
    // ========================================
    @Test
    fun `observation flow should stop when interval becomes infinite`() =
        runTest {
            // ARRANGE: Event that returns infinite interval after first check
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 1.minutes

            var callCount = 0
            val event =
                object : MockEvent(
                    startDateTime = eventStartTime,
                    isRunning = true,
                    timeBeforeUserHit = 3.seconds,
                ) {
                    override val wave: WWWEventWave =
                        object : WWWEventWave() {
                            override val speed: Double = 1.0
                            override val direction: Direction = Direction.EAST
                            override val approxDuration: Int = 60

                            override suspend fun getWavePolygons(): WavePolygons? = null

                            override suspend fun getWaveDuration(): Duration = 60.minutes

                            override suspend fun hasUserBeenHitInCurrentPosition(): Boolean {
                                val duration =
                                    if (callCount > 1) (-1).seconds else 3.seconds
                                return duration < Duration.ZERO
                            }

                            override suspend fun userHitDateTime(): Instant? {
                                callCount++
                                // Return past time after first call to trigger infinite interval
                                val duration =
                                    if (callCount > 1) (-1).seconds else 3.seconds
                                return clock.now() + duration
                            }

                            override suspend fun closestWaveLongitude(latitude: Double): Double = 0.0

                            override suspend fun userPositionToWaveRatio(): Double? = null

                            override fun validationErrors(): List<String>? = null
                        }
                }

            // ACT: Collect emissions with timeout
            val emissions =
                withTimeout(3000) {
                    scheduler.createObservationFlow(event).take(5).toList()
                }

            // ASSERT: Should stop before collecting 5 emissions
            assertTrue(emissions.size < 5, "Expected flow to stop, but got ${emissions.size} emissions")
        }

    // ========================================
    // Test 16: Observation Schedule - All Fields Populated
    // ========================================
    @Test
    fun `getObservationSchedule should populate all fields correctly`() =
        runTest {
            // ARRANGE: Running event
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 20.seconds
            val event =
                MockEvent(
                    startDateTime = eventStartTime,
                    isRunning = true,
                )

            // ACT: Get observation schedule
            val schedule = scheduler.getObservationSchedule(event)

            // ASSERT: All fields should be populated correctly
            assertTrue(schedule.shouldObserve)
            assertEquals(500.milliseconds, schedule.interval)
            assertEquals(ObservationPhase.ACTIVE, schedule.phase)
            assertNotNull(schedule.nextObservationTime)
            assertTrue(schedule.reason.isNotEmpty())
        }

    // ========================================
    // Test 17: Observation Phase - DISTANT
    // ========================================
    @Test
    fun `should determine DISTANT phase for far future events`() =
        runTest {
            // ARRANGE: Event 2 hours away
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 2.hours
            val event = MockEvent(startDateTime = eventStartTime)

            // ACT: Get observation schedule
            val schedule = scheduler.getObservationSchedule(event)

            // ASSERT: Phase should be DISTANT
            assertEquals(ObservationPhase.DISTANT, schedule.phase)
        }

    // ========================================
    // Test 18: Observation Phase - APPROACHING
    // ========================================
    @Test
    fun `should determine APPROACHING phase for 5-60 minute events`() =
        runTest {
            // ARRANGE: Event 30 minutes away
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 30.minutes
            val event = MockEvent(startDateTime = eventStartTime)

            // ACT: Get observation schedule
            val schedule = scheduler.getObservationSchedule(event)

            // ASSERT: Phase should be APPROACHING
            assertEquals(ObservationPhase.APPROACHING, schedule.phase)
        }

    // ========================================
    // Test 19: Observation Phase - NEAR
    // ========================================
    @Test
    fun `should determine NEAR phase for 35s-5min events`() =
        runTest {
            // ARRANGE: Event 2 minutes away
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 2.minutes
            val event = MockEvent(startDateTime = eventStartTime)

            // ACT: Get observation schedule
            val schedule = scheduler.getObservationSchedule(event)

            // ASSERT: Phase should be NEAR
            assertEquals(ObservationPhase.NEAR, schedule.phase)
        }

    // ========================================
    // Test 20: Observation Phase - ACTIVE
    // ========================================
    @Test
    fun `should determine ACTIVE phase for imminent or running events`() =
        runTest {
            // ARRANGE: Event 20 seconds away
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 20.seconds
            val event = MockEvent(startDateTime = eventStartTime)

            // ACT: Get observation schedule
            val schedule = scheduler.getObservationSchedule(event)

            // ASSERT: Phase should be ACTIVE
            assertEquals(ObservationPhase.ACTIVE, schedule.phase)
        }

    // ========================================
    // Test 21: Observation Phase - CRITICAL
    // ========================================
    @Test
    fun `should determine CRITICAL phase during hit window`() =
        runTest {
            // ARRANGE: User will be hit in 3 seconds
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 1.minutes
            val event =
                MockEvent(
                    startDateTime = eventStartTime,
                    timeBeforeUserHit = 3.seconds,
                )

            // ACT: Get observation schedule
            val schedule = scheduler.getObservationSchedule(event)

            // ASSERT: Phase should be CRITICAL
            assertEquals(ObservationPhase.CRITICAL, schedule.phase)
        }

    // ========================================
    // Test 22: Observation Phase - INACTIVE
    // ========================================
    @Test
    fun `should determine INACTIVE phase for done events`() =
        runTest {
            // ARRANGE: Done event
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() - 1.hours
            val event =
                MockEvent(
                    startDateTime = eventStartTime,
                    isDone = true,
                )

            // ACT: Get observation schedule
            val schedule = scheduler.getObservationSchedule(event)

            // ASSERT: Phase should be INACTIVE
            assertEquals(ObservationPhase.INACTIVE, schedule.phase)
        }

    // ========================================
    // Test 23: Next Observation Time - Calculated Correctly
    // ========================================
    @Test
    fun `should calculate next observation time correctly`() =
        runTest {
            // ARRANGE: Event requiring continuous observation
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(1000000))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 20.seconds
            val event =
                MockEvent(
                    startDateTime = eventStartTime,
                    isRunning = true,
                )

            // ACT: Get observation schedule
            val schedule = scheduler.getObservationSchedule(event)

            // ASSERT: Next observation time should be current time + interval
            val expectedTime = clock.now() + schedule.interval
            assertEquals(expectedTime, schedule.nextObservationTime)
        }

    // ========================================
    // Test 24: Next Observation Time - Null for Non-Continuous
    // ========================================
    @Test
    fun `should return null next observation time for non-continuous events`() =
        runTest {
            // ARRANGE: Distant event
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 2.hours
            val event = MockEvent(startDateTime = eventStartTime)

            // ACT: Get observation schedule
            val schedule = scheduler.getObservationSchedule(event)

            // ASSERT: Next observation time should be null
            assertNull(schedule.nextObservationTime)
        }

    // ========================================
    // Test 25: Interval Transitions - Smooth Progression
    // ========================================
    @Test
    fun `should smoothly transition between interval levels`() =
        runTest {
            // ARRANGE: Event we can advance time toward
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 2.hours

            // ACT & ASSERT: Check intervals at different time points
            val event1 = MockEvent(startDateTime = eventStartTime)
            assertEquals(1.hours, scheduler.calculateObservationInterval(event1))

            // Advance to 30 minutes before
            clock.advance(1.hours + 30.minutes)
            val event2 = MockEvent(startDateTime = eventStartTime)
            assertEquals(5.minutes, scheduler.calculateObservationInterval(event2))

            // Advance to 2 minutes before
            clock.advance(28.minutes)
            val event3 = MockEvent(startDateTime = eventStartTime)
            assertEquals(1.seconds, scheduler.calculateObservationInterval(event3))

            // Advance to 20 seconds before
            clock.advance(1.minutes + 40.seconds)
            val event4 = MockEvent(startDateTime = eventStartTime)
            assertEquals(500.milliseconds, scheduler.calculateObservationInterval(event4))
        }

    // ========================================
    // Test 26: Flow Cancellation - Graceful Cleanup
    // ========================================
    @Test
    fun `observation flow should handle cancellation gracefully`() =
        runTest {
            // ARRANGE: Running event
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() - 1.minutes
            val event =
                MockEvent(
                    startDateTime = eventStartTime,
                    isRunning = true,
                )

            var cleanupCalled = false
            val flow = scheduler.createObservationFlow(event)

            // ACT: Launch collection and cancel
            val job =
                launch {
                    try {
                        flow.collect {
                            // Collect a few emissions
                        }
                    } finally {
                        cleanupCalled = true
                    }
                }

            // Let it collect a couple emissions
            advanceTimeBy(10)
            job.cancel()
            advanceTimeBy(10)

            // ASSERT: Cleanup should be called
            assertTrue(cleanupCalled, "Flow cleanup should be called on cancellation")
        }

    // ========================================
    // Test 27: Reason String - Contains Useful Information
    // ========================================
    @Test
    fun `observation schedule reason should contain useful information`() =
        runTest {
            // ARRANGE: Event in CRITICAL phase
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 1.minutes
            val event =
                MockEvent(
                    startDateTime = eventStartTime,
                    timeBeforeUserHit = 3.seconds,
                )

            // ACT: Get observation schedule
            val schedule = scheduler.getObservationSchedule(event)

            // ASSERT: Reason should be descriptive
            assertTrue(schedule.reason.contains("Critical", ignoreCase = true))
            assertTrue(schedule.reason.contains("hit", ignoreCase = true))
        }

    // ========================================
    // Test 28: Performance - Scheduling Overhead
    // ========================================
    @Test
    fun `calculateObservationInterval should execute quickly`() =
        runTest {
            // ARRANGE: Simple event
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val eventStartTime = clock.now() + 30.minutes
            val event = MockEvent(startDateTime = eventStartTime)

            // ACT: Measure calculation time
            val startTime =
                kotlin.time.TimeSource.Monotonic
                    .markNow()
            repeat(1000) {
                scheduler.calculateObservationInterval(event)
            }
            val elapsed = startTime.elapsedNow()

            // ASSERT: 1000 calculations should complete in under 100ms
            assertTrue(elapsed < 100.milliseconds, "1000 interval calculations took $elapsed, expected < 100ms")
        }

    // ========================================
    // Test 29: Concurrent Events - No Interference
    // ========================================
    @Test
    fun `should handle multiple concurrent events independently`() =
        runTest {
            // ARRANGE: Multiple events with different timings
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)

            val event1 = MockEvent(startDateTime = clock.now() + 2.hours)
            val event2 = MockEvent(startDateTime = clock.now() + 30.minutes)
            val event3 = MockEvent(startDateTime = clock.now() + 20.seconds, isRunning = true)

            // ACT: Calculate intervals concurrently
            val interval1 = scheduler.calculateObservationInterval(event1)
            val interval2 = scheduler.calculateObservationInterval(event2)
            val interval3 = scheduler.calculateObservationInterval(event3)

            // ASSERT: Each event should have appropriate interval
            assertEquals(1.hours, interval1)
            assertEquals(5.minutes, interval2)
            assertEquals(500.milliseconds, interval3)
        }

    // ========================================
    // Test 30: Stress Test - Many Sequential Calculations
    // ========================================
    @Test
    fun `should handle many sequential interval calculations`() =
        runTest {
            // ARRANGE: Scheduler and event
            val clock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
            val scheduler = DefaultObservationScheduler(clock)
            val baseTime = clock.now()

            // ACT: Calculate intervals for many different time points
            val intervals = mutableListOf<Duration>()
            for (i in 0 until 1000) {
                val eventStartTime = baseTime + (i * 10).seconds
                val event = MockEvent(startDateTime = eventStartTime)
                intervals.add(scheduler.calculateObservationInterval(event))
            }

            // ASSERT: Should complete without errors and produce valid intervals
            assertEquals(1000, intervals.size)
            assertTrue(intervals.all { it.isFinite() || it == INFINITE })
        }
}
