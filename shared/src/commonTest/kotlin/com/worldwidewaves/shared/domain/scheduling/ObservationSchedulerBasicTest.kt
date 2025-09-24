@file:OptIn(kotlin.time.ExperimentalTime::class)

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
import com.worldwidewaves.shared.events.WWWEventWave
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.testing.MockClock
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive tests for ObservationScheduler implementation.
 *
 * Tests cover adaptive interval calculation, observation phases,
 * continuous observation logic, and flow creation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObservationSchedulerTest {

    private val baseTime = Instant.parse("2025-01-01T12:00:00Z")
    private val mockClock = MockClock(baseTime)
    private val scheduler = DefaultObservationScheduler(mockClock)

    @Test
    fun `scheduler can be instantiated`() {
        assertNotNull(scheduler)
    }

    @Test
    fun `observation phases enum has expected values`() {
        val phases = ObservationPhase.values()
        assertTrue(phases.contains(ObservationPhase.DISTANT))
        assertTrue(phases.contains(ObservationPhase.APPROACHING))
        assertTrue(phases.contains(ObservationPhase.NEAR))
        assertTrue(phases.contains(ObservationPhase.ACTIVE))
        assertTrue(phases.contains(ObservationPhase.CRITICAL))
        assertTrue(phases.contains(ObservationPhase.INACTIVE))
    }

    @Test
    fun `observation schedule data class works`() {
        val schedule = ObservationSchedule(
            shouldObserve = true,
            interval = Duration.ZERO,
            phase = ObservationPhase.ACTIVE,
            nextObservationTime = baseTime,
            reason = "Test schedule"
        )

        assertNotNull(schedule)
        assertTrue(schedule.shouldObserve)
        assertEquals("Test schedule", schedule.reason)
    }

    @Test
    fun `calculateObservationInterval returns 1 hour for distant events`() = runTest {
        val eventStartTime = baseTime + 2.hours
        val mockEvent = createMockEvent(eventStartTime, isRunning = false)

        val interval = scheduler.calculateObservationInterval(mockEvent)

        assertEquals(1.hours, interval)
    }

    @Test
    fun `calculateObservationInterval returns 5 minutes for approaching events`() = runTest {
        val eventStartTime = baseTime + 30.minutes
        val mockEvent = createMockEvent(eventStartTime, isRunning = false)

        val interval = scheduler.calculateObservationInterval(mockEvent)

        assertEquals(5.minutes, interval)
    }

    @Test
    fun `calculateObservationInterval returns 1 second for near events`() = runTest {
        val eventStartTime = baseTime + 2.minutes
        val mockEvent = createMockEvent(eventStartTime, isRunning = false)

        val interval = scheduler.calculateObservationInterval(mockEvent)

        assertEquals(1.seconds, interval)
    }

    @Test
    fun `calculateObservationInterval returns 500ms for active events`() = runTest {
        val eventStartTime = baseTime + 10.seconds
        val mockEvent = createMockEvent(eventStartTime, isRunning = false)

        val interval = scheduler.calculateObservationInterval(mockEvent)

        assertEquals(500.milliseconds, interval)
    }

    @Test
    fun `calculateObservationInterval returns 500ms for running events`() = runTest {
        val eventStartTime = baseTime - 30.seconds // Event started 30 seconds ago
        val mockEvent = createMockEvent(eventStartTime, isRunning = true)

        val interval = scheduler.calculateObservationInterval(mockEvent)

        assertEquals(500.milliseconds, interval)
    }

    @Test
    fun `calculateObservationInterval returns 50ms for critical hit timing`() = runTest {
        val eventStartTime = baseTime - 1.minutes // Event started
        val mockEvent = createMockEvent(eventStartTime, isRunning = true, timeBeforeHit = 500.milliseconds)

        val interval = scheduler.calculateObservationInterval(mockEvent)

        assertEquals(50.milliseconds, interval)
    }

    @Test
    fun `calculateObservationInterval returns 200ms for near hit timing`() = runTest {
        val eventStartTime = baseTime - 1.minutes // Event started
        val mockEvent = createMockEvent(eventStartTime, isRunning = true, timeBeforeHit = 3.seconds)

        val interval = scheduler.calculateObservationInterval(mockEvent)

        assertEquals(200.milliseconds, interval)
    }

    @Test
    fun `calculateObservationInterval returns INFINITE for completed events`() = runTest {
        val eventStartTime = baseTime - 1.minutes // Event started
        val mockEvent = createMockEvent(eventStartTime, isRunning = true, timeBeforeHit = -10.seconds)

        val interval = scheduler.calculateObservationInterval(mockEvent)

        assertEquals(Duration.INFINITE, interval)
    }

    @Test
    fun `shouldObserveContinuously returns true for running events`() = runTest {
        val mockEvent = mockk<IWWWEvent> {
            coEvery { isRunning() } returns true
            every { isSoon() } returns false
            every { isNearTime() } returns false
        }

        val shouldObserve = scheduler.shouldObserveContinuously(mockEvent)

        assertTrue(shouldObserve)
    }

    @Test
    fun `shouldObserveContinuously returns true for soon and near time events`() = runTest {
        val mockEvent = mockk<IWWWEvent> {
            coEvery { isRunning() } returns false
            every { isSoon() } returns true
            every { isNearTime() } returns true
        }

        val shouldObserve = scheduler.shouldObserveContinuously(mockEvent)

        assertTrue(shouldObserve)
    }

    @Test
    fun `shouldObserveContinuously returns false for distant events`() = runTest {
        val mockEvent = mockk<IWWWEvent> {
            coEvery { isRunning() } returns false
            every { isSoon() } returns false
            every { isNearTime() } returns false
        }

        val shouldObserve = scheduler.shouldObserveContinuously(mockEvent)

        assertFalse(shouldObserve)
    }

    @Test
    fun `getObservationSchedule returns complete schedule information`() = runTest {
        val eventStartTime = baseTime + 30.minutes
        val mockEvent = createMockEvent(eventStartTime, isRunning = false)

        val schedule = scheduler.getObservationSchedule(mockEvent)

        assertFalse(schedule.shouldObserve) // Event is not soon or running
        assertEquals(5.minutes, schedule.interval)
        assertEquals(ObservationPhase.APPROACHING, schedule.phase)
        assertTrue(schedule.reason.contains("approaching"))
    }

    @Test
    fun `createObservationFlow emits once for non-continuous events`() = runTest {
        val mockEvent = mockk<IWWWEvent> {
            coEvery { isRunning() } returns false
            every { isSoon() } returns false
            every { isNearTime() } returns false
            every { id } returns "test_event"
        }

        val emissions = withTimeout(1000) {
            scheduler.createObservationFlow(mockEvent).take(1).toList()
        }

        assertEquals(1, emissions.size)
    }

    @Test
    fun `createObservationFlow handles continuous observation properly`() = runTest {
        val mockEvent = mockk<IWWWEvent> {
            coEvery { isRunning() } returns true
            every { isSoon() } returns true
            every { isNearTime() } returns true
            coEvery { isDone() } returnsMany listOf(false, false, true) // Will stop after 2 emissions
            every { id } returns "test_event"
            every { getStartDateTime() } returns baseTime + 10.seconds
            every { wave } returns mockk<WWWEventWave> {
                coEvery { timeBeforeUserHit() } returns 5.seconds
            }
        }

        // This test verifies the flow structure but doesn't wait for actual timing
        val firstEmission = scheduler.createObservationFlow(mockEvent).first()

        assertEquals(Unit, firstEmission)
    }

    private fun createMockEvent(
        startTime: Instant,
        isRunning: Boolean = false,
        timeBeforeHit: Duration? = 1.days
    ): IWWWEvent {
        return mockk {
            every { getStartDateTime() } returns startTime
            coEvery { isRunning() } returns isRunning
            every { isSoon() } returns false
            every { isNearTime() } returns false
            coEvery { isDone() } returns false
            every { id } returns "test_event"
            every { wave } returns mockk<WWWEventWave> {
                coEvery { timeBeforeUserHit() } returns timeBeforeHit
            }
        }
    }
}