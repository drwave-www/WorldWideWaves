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
import com.worldwidewaves.shared.testing.MockClock
import com.worldwidewaves.shared.testing.TestHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant.Companion.DISTANT_FUTURE

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class WWWEventObserverTest : KoinTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockClock: MockClock

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockClock = MockClock(TestHelpers.TestTimes.BASE_TIME)

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

    @Test
    fun `observer should initialize with default values for future event`() = runTest {
        // GIVEN: A future event that's not near time
        val event = TestHelpers.createFutureEvent(startsIn = 5.hours)

        // WHEN
        val observer = WWWEventObserver(event)
        testScheduler.advanceUntilIdle()

        // THEN: Should have default values
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
    fun `observer should initialize correctly for running event`() = runTest {
        // GIVEN: A running event
        mockClock.setTime(TestHelpers.TestTimes.BASE_TIME + 1.hours)
        val event = TestHelpers.createRunningEvent(
            startedAgo = 30.minutes,
            totalDuration = 2.hours
        )

        // WHEN
        val observer = WWWEventObserver(event)
        testScheduler.advanceUntilIdle()

        // THEN: Should detect it as running and initialize state
        assertNotNull(observer.eventStatus.value)
        assertNotNull(observer.progression.value)
    }

    @Test
    fun `observer should handle completed event correctly`() = runTest {
        // GIVEN: A completed event
        mockClock.setTime(TestHelpers.TestTimes.BASE_TIME + 5.hours)
        val event = TestHelpers.createCompletedEvent(
            endedAgo = 1.hours,
            totalDuration = 30.minutes
        )

        // WHEN
        val observer = WWWEventObserver(event)
        testScheduler.advanceUntilIdle()

        // THEN: Should handle completed event gracefully
        assertNotNull(observer.eventStatus.value)
        assertNotNull(observer.progression.value)
    }

    @Test
    fun `observer should start and stop observation safely`() = runTest {
        // GIVEN: A future event
        val event = TestHelpers.createFutureEvent()

        val observer = WWWEventObserver(event)
        testScheduler.advanceUntilIdle()

        // WHEN: Start and stop observation
        observer.startObservation()
        testScheduler.advanceUntilIdle()

        observer.stopObservation()
        testScheduler.advanceUntilIdle()

        // THEN: Should complete without errors
        assertNotNull(observer)
    }

    @Test
    fun `observer should handle multiple start observation calls safely`() = runTest {
        // GIVEN: A future event
        val event = TestHelpers.createFutureEvent()

        val observer = WWWEventObserver(event)
        testScheduler.advanceUntilIdle()

        // WHEN: Call startObservation multiple times
        observer.startObservation()
        observer.startObservation()
        observer.startObservation()
        testScheduler.advanceUntilIdle()

        // THEN: Should handle gracefully without crashes
        assertNotNull(observer)
    }

    @Test
    fun `observer should handle events with different statuses`() = runTest {
        // GIVEN: Different types of events
        val events = TestHelpers.createTestEventSuite()

        // WHEN: Create observers for each event
        val observers = events.map { event ->
            WWWEventObserver(event)
        }
        testScheduler.advanceUntilIdle()

        // THEN: All observers should be created successfully
        assertEquals(4, observers.size)
        observers.forEach { observer ->
            assertNotNull(observer.eventStatus.value)
            assertNotNull(observer.progression.value)
        }
    }

    @Test
    fun `observer should create observation flow without errors`() = runTest {
        // GIVEN: A running event
        val event = TestHelpers.createRunningEvent()

        val observer = WWWEventObserver(event)
        testScheduler.advanceUntilIdle()

        // WHEN: Create observation flow
        val observationFlow = observer.createObservationFlow()

        // THEN: Flow should be created successfully
        assertNotNull(observationFlow)
    }

    @Test
    fun `observer should handle events with valid test data correctly`() = runTest {
        // GIVEN: Event with specific test configuration
        val event = TestHelpers.createTestEvent(
            id = "observer_test_event",
            type = "city",
            country = "france",
            userPosition = TestHelpers.TestLocations.PARIS
        )

        // WHEN
        val observer = WWWEventObserver(event)
        testScheduler.advanceUntilIdle()

        // THEN: Should initialize properly with test data
        assertNotNull(observer.eventStatus.value)
        // User should be in area based on our mock setup
        assertTrue(observer.userIsInArea.value)
    }

    @Test
    fun `observer should handle events without user position`() = runTest {
        // GIVEN: Event without user position
        val event = TestHelpers.createTestEvent(
            userPosition = null
        )

        // WHEN
        val observer = WWWEventObserver(event)
        testScheduler.advanceUntilIdle()

        // THEN: Should handle null user position gracefully
        assertNotNull(observer.eventStatus.value)
        assertFalse(observer.userIsInArea.value)
    }

    @Test
    fun `observer should handle time progression correctly`() = runTest {
        // GIVEN: Event and time advancement
        val event = TestHelpers.createFutureEvent(startsIn = 30.minutes)

        val observer = WWWEventObserver(event)
        testScheduler.advanceUntilIdle()

        // WHEN: Advance time
        mockClock.advanceTimeBy(1.hours)
        testScheduler.advanceUntilIdle()

        // THEN: Observer should continue to function
        assertNotNull(observer.eventStatus.value)
        assertNotNull(observer.progression.value)
    }
}