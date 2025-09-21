package com.worldwidewaves.shared.testing

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

import com.worldwidewaves.shared.events.utils.IClock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A controllable clock implementation for testing time-dependent functionality.
 *
 * This mock clock allows tests to:
 * - Set specific times for predictable testing
 * - Advance time manually to test time-based state transitions
 * - Record delay calls for verification
 * - Simulate time-based scenarios without real time delays
 */
@OptIn(ExperimentalTime::class)
class MockClock(
    private var currentTime: Instant = TestHelpers.TestTimes.BASE_TIME
) : IClock {

    private val _delayCalls = mutableListOf<Duration>()
    val delayCalls: List<Duration> get() = _delayCalls.toList()

    private var _totalDelayTime = Duration.ZERO
    val totalDelayTime: Duration get() = _totalDelayTime

    override fun now(): Instant = currentTime

    override suspend fun delay(duration: Duration) {
        _delayCalls.add(duration)
        _totalDelayTime += duration
        // Optionally advance time by the delay duration
        advanceTimeBy(duration)
    }

    /**
     * Manually advance the clock by the specified duration.
     */
    fun advanceTimeBy(duration: Duration) {
        currentTime += duration
    }

    /**
     * Set the clock to a specific time.
     */
    fun setTime(instant: Instant) {
        currentTime = instant
    }

    /**
     * Reset the clock to the base time and clear all recorded calls.
     */
    fun reset(newTime: Instant = TestHelpers.TestTimes.BASE_TIME) {
        currentTime = newTime
        _delayCalls.clear()
        _totalDelayTime = Duration.ZERO
    }

    /**
     * Advance time to simulate the passage of time during an event.
     */
    fun simulateEventProgress(
        eventDuration: Duration,
        steps: Int = 10
    ): List<Instant> {
        val timePoints = mutableListOf<Instant>()
        val stepDuration = eventDuration / steps

        repeat(steps) {
            timePoints.add(now())
            advanceTimeBy(stepDuration)
        }
        timePoints.add(now()) // Final time point

        return timePoints
    }

    /**
     * Verify that delay was called with specific durations.
     */
    fun verifyDelaysCalled(expectedDelays: List<Duration>) {
        if (delayCalls != expectedDelays) {
            throw AssertionError(
                "Expected delays: $expectedDelays, but got: $delayCalls"
            )
        }
    }

    /**
     * Verify that delay was called at least once.
     */
    fun verifyDelayWasCalled() {
        if (delayCalls.isEmpty()) {
            throw AssertionError("Expected delay to be called, but it wasn't")
        }
    }

    /**
     * Verify that delay was never called.
     */
    fun verifyNoDelaysCalled() {
        if (delayCalls.isNotEmpty()) {
            throw AssertionError("Expected no delays, but got: $delayCalls")
        }
    }

    /**
     * Get the number of delay calls made.
     */
    fun getDelayCallCount(): Int = delayCalls.size

    /**
     * Check if a specific delay duration was called.
     */
    fun wasDelayCalledWith(duration: Duration): Boolean = duration in delayCalls

    companion object {
        /**
         * Create a mock clock set to a specific event start time.
         */
        fun forEventStartTime(
            date: String = "2022-01-01",
            time: String = "18:00"
        ): MockClock {
            // Parse the date and time to create an Instant
            // This is a simplified implementation for testing
            val baseTime = TestHelpers.TestTimes.BASE_TIME
            return MockClock(baseTime)
        }

        /**
         * Create a mock clock for testing event transitions.
         */
        fun forStatusTesting(): MockClock = MockClock(TestHelpers.TestTimes.BASE_TIME)
    }
}