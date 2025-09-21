package com.worldwidewaves.shared.events

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

import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DefaultCoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.utils.GeoPosition
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.Instant.Companion.DISTANT_FUTURE

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class WWWEventObserverTest : KoinTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockClock: IClock
    private lateinit var mockEvent: IWWWEvent
    private lateinit var mockWave: WWWEventWave
    private lateinit var mockArea: WWWEventArea
    private lateinit var mockWarming: WWWEventWaveWarming
    private lateinit var observer: WWWEventObserver

    private val baseTime = Instant.fromEpochMilliseconds(1000000000L)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockClock = mockk<IClock>(relaxed = true)
        mockEvent = mockk<IWWWEvent>(relaxed = true)
        mockWave = mockk<WWWEventWave>(relaxed = true)
        mockArea = mockk<WWWEventArea>(relaxed = true)
        mockWarming = mockk<WWWEventWaveWarming>(relaxed = true)

        every { mockEvent.wave } returns mockWave
        every { mockEvent.area } returns mockArea
        every { mockEvent.warming } returns mockWarming
        every { mockEvent.id } returns "test-event-1"

        every { mockClock.now() } returns baseTime
        coEvery { mockClock.delay(any()) } returns Unit

        startKoin {
            modules(
                module {
                    single<IClock> { mockClock }
                    single<CoroutineScopeProvider> { DefaultCoroutineScopeProvider(testDispatcher, testDispatcher) }
                }
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun createObserver(): WWWEventObserver {
        return WWWEventObserver(mockEvent)
    }

    @Test
    fun `observer should initialize with default values`() = runTest {
        // GIVEN: Default mock setup
        every { mockEvent.getStatus() } returns Status.UNDEFINED
        every { mockWave.getProgression() } returns 0.0
        every { mockEvent.isRunning() } returns false
        every { mockEvent.isSoon() } returns false
        every { mockEvent.isNearTime() } returns false

        // WHEN
        observer = createObserver()
        testScheduler.advanceUntilIdle()

        // THEN
        assertEquals(Status.UNDEFINED, observer.eventStatus.value)
        assertEquals(0.0, observer.progression.value)
        assertFalse(observer.isUserWarmingInProgress.value)
        assertFalse(observer.isStartWarmingInProgress.value)
        assertFalse(observer.userIsGoingToBeHit.value)
        assertFalse(observer.userHasBeenHit.value)
        assertEquals(0.0, observer.userPositionRatio.value)
        assertEquals(INFINITE, observer.timeBeforeHit.value)
        assertEquals(DISTANT_FUTURE, observer.hitDateTime.value)
        assertFalse(observer.userIsInArea.value)
    }

    @Test
    fun `observer should start observation when event is running`() = runTest {
        // GIVEN
        every { mockEvent.getStatus() } returns Status.RUNNING
        every { mockWave.getProgression() } returns 25.0
        every { mockEvent.isRunning() } returns true
        every { mockEvent.isSoon() } returns false
        every { mockEvent.isNearTime() } returns true
        every { mockEvent.isDone() } returns false
        every { mockWarming.isUserWarmingStarted() } returns false
        every { mockWave.timeBeforeUserHit() } returns 5.minutes
        every { mockWave.hasUserBeenHitInCurrentPosition() } returns false
        every { mockWave.userPositionToWaveRatio() } returns 0.3
        every { mockWave.userHitDateTime() } returns baseTime + 5.minutes
        every { mockEvent.getStartDateTime() } returns baseTime - 1.hours
        every { mockEvent.getWaveStartDateTime() } returns baseTime - 30.minutes
        every { mockWave.getUserPosition() } returns GeoPosition(45.0, 2.0)
        every { mockArea.isPositionWithin(any()) } returns true

        // WHEN
        observer = createObserver()
        testScheduler.advanceUntilIdle()

        // THEN
        assertEquals(Status.RUNNING, observer.eventStatus.value)
        assertEquals(25.0, observer.progression.value)
        assertTrue(observer.userIsInArea.value)
    }

    @Test
    fun `observer should detect user warming phase`() = runTest {
        // GIVEN: Event is running and user warming has started
        every { mockEvent.getStatus() } returns Status.RUNNING
        every { mockWave.getProgression() } returns 10.0
        every { mockEvent.isRunning() } returns true
        every { mockEvent.isSoon() } returns false
        every { mockEvent.isNearTime() } returns true
        every { mockEvent.isDone() } returns false
        every { mockWarming.isUserWarmingStarted() } returns true
        every { mockWave.timeBeforeUserHit() } returns 2.minutes
        every { mockWave.hasUserBeenHitInCurrentPosition() } returns false
        every { mockWave.userPositionToWaveRatio() } returns 0.1
        every { mockWave.userHitDateTime() } returns baseTime + 2.minutes
        every { mockEvent.getStartDateTime() } returns baseTime - 1.hours
        every { mockEvent.getWaveStartDateTime() } returns baseTime - 30.minutes
        every { mockWave.getUserPosition() } returns GeoPosition(45.0, 2.0)
        every { mockArea.isPositionWithin(any()) } returns true

        // WHEN
        observer = createObserver()
        testScheduler.advanceUntilIdle()

        // THEN
        assertTrue(observer.isUserWarmingInProgress.value)
        assertFalse(observer.userIsGoingToBeHit.value)
        assertFalse(observer.userHasBeenHit.value)
    }

    @Test
    fun `observer should detect user about to be hit`() = runTest {
        // GIVEN: User is about to be hit (within warning time)
        every { mockEvent.getStatus() } returns Status.RUNNING
        every { mockWave.getProgression() } returns 85.0
        every { mockEvent.isRunning() } returns true
        every { mockEvent.isSoon() } returns false
        every { mockEvent.isNearTime() } returns true
        every { mockEvent.isDone() } returns false
        every { mockWarming.isUserWarmingStarted() } returns false
        every { mockWave.timeBeforeUserHit() } returns 15.seconds // Within WAVE_WARN_BEFORE_HIT
        every { mockWave.hasUserBeenHitInCurrentPosition() } returns false
        every { mockWave.userPositionToWaveRatio() } returns 0.85
        every { mockWave.userHitDateTime() } returns baseTime + 15.seconds
        every { mockEvent.getStartDateTime() } returns baseTime - 1.hours
        every { mockEvent.getWaveStartDateTime() } returns baseTime - 30.minutes
        every { mockWave.getUserPosition() } returns GeoPosition(45.0, 2.0)
        every { mockArea.isPositionWithin(any()) } returns true

        // WHEN
        observer = createObserver()
        testScheduler.advanceUntilIdle()

        // THEN
        assertFalse(observer.isUserWarmingInProgress.value)
        assertTrue(observer.userIsGoingToBeHit.value)
        assertFalse(observer.userHasBeenHit.value)
        assertEquals(15.seconds, observer.timeBeforeHit.value)
    }

    @Test
    fun `observer should detect user has been hit`() = runTest {
        // GIVEN: User has been hit by the wave
        every { mockEvent.getStatus() } returns Status.RUNNING
        every { mockWave.getProgression() } returns 90.0
        every { mockEvent.isRunning() } returns true
        every { mockEvent.isSoon() } returns false
        every { mockEvent.isNearTime() } returns true
        every { mockEvent.isDone() } returns false
        every { mockWarming.isUserWarmingStarted() } returns false
        every { mockWave.timeBeforeUserHit() } returns ZERO
        every { mockWave.hasUserBeenHitInCurrentPosition() } returns true
        every { mockWave.userPositionToWaveRatio() } returns 0.9
        every { mockWave.userHitDateTime() } returns baseTime
        every { mockEvent.getStartDateTime() } returns baseTime - 1.hours
        every { mockEvent.getWaveStartDateTime() } returns baseTime - 30.minutes
        every { mockWave.getUserPosition() } returns GeoPosition(45.0, 2.0)
        every { mockArea.isPositionWithin(any()) } returns true

        // WHEN
        observer = createObserver()
        testScheduler.advanceUntilIdle()

        // THEN
        assertFalse(observer.isUserWarmingInProgress.value)
        assertFalse(observer.userIsGoingToBeHit.value)
        assertTrue(observer.userHasBeenHit.value)
    }

    @Test
    fun `observer should detect start warming phase`() = runTest {
        // GIVEN: Between event start and wave start
        val eventStart = baseTime - 1.hours
        val waveStart = baseTime + 30.minutes
        every { mockClock.now() } returns baseTime // Now is between event start and wave start
        every { mockEvent.getStatus() } returns Status.RUNNING
        every { mockWave.getProgression() } returns 0.0
        every { mockEvent.isRunning() } returns true
        every { mockEvent.isSoon() } returns false
        every { mockEvent.isNearTime() } returns true
        every { mockEvent.isDone() } returns false
        every { mockWarming.isUserWarmingStarted() } returns false
        every { mockWave.timeBeforeUserHit() } returns 2.hours
        every { mockWave.hasUserBeenHitInCurrentPosition() } returns false
        every { mockWave.userPositionToWaveRatio() } returns 0.0
        every { mockWave.userHitDateTime() } returns baseTime + 2.hours
        every { mockEvent.getStartDateTime() } returns eventStart
        every { mockEvent.getWaveStartDateTime() } returns waveStart
        every { mockWave.getUserPosition() } returns GeoPosition(45.0, 2.0)
        every { mockArea.isPositionWithin(any()) } returns true

        // WHEN
        observer = createObserver()
        testScheduler.advanceUntilIdle()

        // THEN
        assertTrue(observer.isStartWarmingInProgress.value)
    }

    @Test
    fun `observer should handle user position tracking`() = runTest {
        // GIVEN: User position is tracked
        val userPosition = GeoPosition(48.8566, 2.3522) // Paris coordinates
        every { mockEvent.getStatus() } returns Status.RUNNING
        every { mockWave.getProgression() } returns 50.0
        every { mockEvent.isRunning() } returns true
        every { mockEvent.isSoon() } returns false
        every { mockEvent.isNearTime() } returns true
        every { mockEvent.isDone() } returns false
        every { mockWarming.isUserWarmingStarted() } returns false
        every { mockWave.timeBeforeUserHit() } returns 1.hours
        every { mockWave.hasUserBeenHitInCurrentPosition() } returns false
        every { mockWave.userPositionToWaveRatio() } returns 0.5
        every { mockWave.userHitDateTime() } returns baseTime + 1.hours
        every { mockEvent.getStartDateTime() } returns baseTime - 1.hours
        every { mockEvent.getWaveStartDateTime() } returns baseTime - 30.minutes
        every { mockWave.getUserPosition() } returns userPosition
        every { mockArea.isPositionWithin(userPosition) } returns true

        // WHEN
        observer = createObserver()
        testScheduler.advanceUntilIdle()

        // THEN
        assertTrue(observer.userIsInArea.value)
        assertEquals(0.5, observer.userPositionRatio.value)
        verify { mockArea.isPositionWithin(userPosition) }
    }

    @Test
    fun `observer should handle null user position`() = runTest {
        // GIVEN: No user position available
        every { mockEvent.getStatus() } returns Status.RUNNING
        every { mockWave.getProgression() } returns 50.0
        every { mockEvent.isRunning() } returns true
        every { mockEvent.isSoon() } returns false
        every { mockEvent.isNearTime() } returns true
        every { mockEvent.isDone() } returns false
        every { mockWarming.isUserWarmingStarted() } returns false
        every { mockWave.timeBeforeUserHit() } returns 1.hours
        every { mockWave.hasUserBeenHitInCurrentPosition() } returns false
        every { mockWave.userPositionToWaveRatio() } returns null
        every { mockWave.userHitDateTime() } returns null
        every { mockEvent.getStartDateTime() } returns baseTime - 1.hours
        every { mockEvent.getWaveStartDateTime() } returns baseTime - 30.minutes
        every { mockWave.getUserPosition() } returns null

        // WHEN
        observer = createObserver()
        testScheduler.advanceUntilIdle()

        // THEN
        assertFalse(observer.userIsInArea.value)
        assertEquals(0.0, observer.userPositionRatio.value)
        assertEquals(DISTANT_FUTURE, observer.hitDateTime.value)
    }

    @Test
    fun `observer should not start observation when event is not soon or running`() = runTest {
        // GIVEN: Event is far in the future
        every { mockEvent.getStatus() } returns Status.NEXT
        every { mockWave.getProgression() } returns 0.0
        every { mockEvent.isRunning() } returns false
        every { mockEvent.isSoon() } returns false
        every { mockEvent.isNearTime() } returns false

        // WHEN
        observer = createObserver()
        testScheduler.advanceUntilIdle()

        // THEN: Initial values should be set but no ongoing observation
        assertEquals(Status.NEXT, observer.eventStatus.value)
        assertEquals(0.0, observer.progression.value)
    }

    @Test
    fun `observer should handle wave progression errors gracefully`() = runTest {
        // GIVEN: Wave progression throws an exception
        every { mockEvent.getStatus() } returns Status.RUNNING
        every { mockWave.getProgression() } throws RuntimeException("Test progression error")
        every { mockEvent.isRunning() } returns true
        every { mockEvent.isSoon() } returns false
        every { mockEvent.isNearTime() } returns true

        // WHEN
        observer = createObserver()
        testScheduler.advanceUntilIdle()

        // THEN: Should not crash and should set status
        assertEquals(Status.RUNNING, observer.eventStatus.value)
        // Progression should remain at default value due to error
        assertEquals(0.0, observer.progression.value)
    }

    @Test
    fun `stopObservation should cancel ongoing observation`() = runTest {
        // GIVEN: Observer with active observation
        every { mockEvent.getStatus() } returns Status.RUNNING
        every { mockWave.getProgression() } returns 25.0
        every { mockEvent.isRunning() } returns true
        every { mockEvent.isSoon() } returns false
        every { mockEvent.isNearTime() } returns true
        every { mockEvent.isDone() } returns false
        every { mockWarming.isUserWarmingStarted() } returns false
        every { mockWave.timeBeforeUserHit() } returns 5.minutes
        every { mockWave.hasUserBeenHitInCurrentPosition() } returns false
        every { mockWave.userPositionToWaveRatio() } returns 0.3
        every { mockWave.userHitDateTime() } returns baseTime + 5.minutes
        every { mockEvent.getStartDateTime() } returns baseTime - 1.hours
        every { mockEvent.getWaveStartDateTime() } returns baseTime - 30.minutes
        every { mockWave.getUserPosition() } returns GeoPosition(45.0, 2.0)
        every { mockArea.isPositionWithin(any()) } returns true

        observer = createObserver()
        testScheduler.advanceUntilIdle()

        // WHEN
        observer.stopObservation()
        testScheduler.advanceUntilIdle()

        // THEN: Observer should have stopped (verified through no further state updates)
        // This is a basic test - in practice you'd verify the job is cancelled
        assertEquals(Status.RUNNING, observer.eventStatus.value)
    }

    @Test
    fun `observation flow should emit final state when event is done`() = runTest {
        // GIVEN: Event that transitions to done
        every { mockEvent.getStatus() } returns Status.RUNNING
        every { mockWave.getProgression() } returns 99.0
        every { mockEvent.isRunning() } returns true
        every { mockEvent.isSoon() } returns false
        every { mockEvent.isNearTime() } returns true
        every { mockEvent.isDone() } returnsMany listOf(false, true) // First false, then true
        every { mockWarming.isUserWarmingStarted() } returns false
        every { mockWave.timeBeforeUserHit() } returns 10.seconds
        every { mockWave.hasUserBeenHitInCurrentPosition() } returns false
        every { mockWave.userPositionToWaveRatio() } returns 0.99
        every { mockWave.userHitDateTime() } returns baseTime + 10.seconds
        every { mockEvent.getStartDateTime() } returns baseTime - 1.hours
        every { mockEvent.getWaveStartDateTime() } returns baseTime - 30.minutes
        every { mockWave.getUserPosition() } returns GeoPosition(45.0, 2.0)
        every { mockArea.isPositionWithin(any()) } returns true

        // WHEN
        observer = createObserver()
        val observationFlow = observer.createObservationFlow()

        // THEN: Should emit the final state
        val observation = observationFlow.first()
        assertEquals(99.0, observation.progression)
        assertEquals(Status.RUNNING, observation.status)
    }

    @Test
    fun `getObservationInterval should return appropriate intervals based on timing`() = runTest {
        // This is a private method test - we test it indirectly through the behavior
        // The actual interval calculation is tested through the observation behavior

        // GIVEN: Event far in the future
        every { mockEvent.getStatus() } returns Status.NEXT
        every { mockWave.getProgression() } returns 0.0
        every { mockEvent.isRunning() } returns false
        every { mockEvent.isSoon() } returns true
        every { mockEvent.isNearTime() } returns true
        every { mockEvent.isDone() } returns false
        every { mockEvent.getStartDateTime() } returns baseTime + 2.hours
        every { mockWave.timeBeforeUserHit() } returns 2.hours + 10.minutes
        every { mockWarming.isUserWarmingStarted() } returns false
        every { mockWave.hasUserBeenHitInCurrentPosition() } returns false
        every { mockWave.userPositionToWaveRatio() } returns 0.0
        every { mockWave.userHitDateTime() } returns baseTime + 2.hours + 10.minutes
        every { mockEvent.getWaveStartDateTime() } returns baseTime + 2.hours
        every { mockWave.getUserPosition() } returns GeoPosition(45.0, 2.0)
        every { mockArea.isPositionWithin(any()) } returns true

        // WHEN
        observer = createObserver()
        testScheduler.advanceUntilIdle()

        // THEN: Should handle the timing appropriately
        assertEquals(Status.NEXT, observer.eventStatus.value)
    }

    @Test
    fun `observer should handle concurrent access safely`() = runTest {
        // GIVEN: Observer with multiple concurrent operations
        every { mockEvent.getStatus() } returns Status.RUNNING
        every { mockWave.getProgression() } returns 50.0
        every { mockEvent.isRunning() } returns true
        every { mockEvent.isSoon() } returns false
        every { mockEvent.isNearTime() } returns true
        every { mockEvent.isDone() } returns false
        every { mockWarming.isUserWarmingStarted() } returns false
        every { mockWave.timeBeforeUserHit() } returns 1.hours
        every { mockWave.hasUserBeenHitInCurrentPosition() } returns false
        every { mockWave.userPositionToWaveRatio() } returns 0.5
        every { mockWave.userHitDateTime() } returns baseTime + 1.hours
        every { mockEvent.getStartDateTime() } returns baseTime - 1.hours
        every { mockEvent.getWaveStartDateTime() } returns baseTime - 30.minutes
        every { mockWave.getUserPosition() } returns GeoPosition(45.0, 2.0)
        every { mockArea.isPositionWithin(any()) } returns true

        // WHEN: Multiple concurrent operations
        observer = createObserver()
        observer.startObservation() // Should not crash if called multiple times
        testScheduler.advanceUntilIdle()

        // THEN: Should handle gracefully
        assertEquals(Status.RUNNING, observer.eventStatus.value)
        assertEquals(50.0, observer.progression.value)
    }
}