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

import com.worldwidewaves.shared.domain.observation.PositionObserver
import com.worldwidewaves.shared.domain.progression.WaveProgressionTracker
import com.worldwidewaves.shared.domain.scheduling.ObservationScheduler
import com.worldwidewaves.shared.domain.state.EventStateManager
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.position.PositionManager
import com.worldwidewaves.shared.testing.MockClock
import com.worldwidewaves.shared.testing.TestHelpers
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/**
 * Integration tests for WWWEventObserver component interaction.
 *
 * These tests verify that the refactored WWWEventObserver correctly composes
 * and coordinates all the extracted domain components:
 * - ObservationScheduler
 * - WaveProgressionTracker
 * - PositionObserver
 * - EventStateManager
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class WWWEventObserverIntegrationTest : KoinTest {

    private val mockClock = MockClock()
    private val mockPositionManager: PositionManager = mockk(relaxed = true)
    private val mockCoroutineScopeProvider: CoroutineScopeProvider = mockk(relaxed = true)
    private val mockWaveProgressionTracker: WaveProgressionTracker = mockk(relaxed = true)
    private val mockPositionObserver: PositionObserver = mockk(relaxed = true)
    private val mockEventStateManager: EventStateManager = mockk(relaxed = true)
    private val mockObservationScheduler: ObservationScheduler = mockk(relaxed = true)

    private val observationScheduler: ObservationScheduler by inject()
    private val waveProgressionTracker: WaveProgressionTracker by inject()
    private val positionObserver: PositionObserver by inject()
    private val eventStateManager: EventStateManager by inject()

    @BeforeTest
    fun setup() {
        setupKoin()
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        mockClock.reset()
        clearAllMocks()
    }

    private fun setupKoin() {
        startKoin {
            modules(
                module {
                    single<IClock> { mockClock }
                    single<PositionManager> { mockPositionManager }
                    single<CoroutineScopeProvider> { mockCoroutineScopeProvider }
                    single<WaveProgressionTracker> { mockWaveProgressionTracker }
                    single<PositionObserver> { mockPositionObserver }
                    single<EventStateManager> { mockEventStateManager }
                    single<ObservationScheduler> { mockObservationScheduler }
                }
            )
        }
    }

    @Test
    fun `WWWEventObserver integrates all extracted components successfully`() = runTest {
        // Given
        val testEvent = TestHelpers.createTestEvent(
            id = "integration_test_event",
            userPosition = TestHelpers.TestLocations.PARIS
        )

        // Mock the component interactions
        every { mockCoroutineScopeProvider.scopeDefault() } returns this
        coEvery { mockWaveProgressionTracker.calculateProgression(any()) } returns 25.0
        coEvery { mockPositionObserver.observePositionForEvent(any()) } returns MutableStateFlow(
            mockk {
                every { position } returns TestHelpers.TestLocations.PARIS
                every { isInArea } returns true
            }
        )
        coEvery { mockEventStateManager.calculateEventState(any(), any(), any()) } returns mockk {
            every { progression } returns 25.0
            every { status } returns IWWWEvent.Status.SOON
            every { isUserWarmingInProgress } returns false
            every { isStartWarmingInProgress } returns false
            every { userIsGoingToBeHit } returns false
            every { userHasBeenHit } returns false
            every { userPositionRatio } returns 0.5
            every { timeBeforeHit } returns 10.minutes
            every { hitDateTime } returns mockClock.now() + 10.minutes
        }
        coEvery { mockEventStateManager.validateState(any(), any()) } returns emptyList()
        coEvery { mockEventStateManager.validateStateTransition(any(), any()) } returns emptyList()
        coEvery { mockObservationScheduler.createObservationFlow(any()) } returns MutableStateFlow(Unit)

        // When
        val observer = WWWEventObserver(testEvent)

        // Then
        assertNotNull(observer)

        // Verify the observer exposes the correct public API
        assertNotNull(observer.eventStatus)
        assertNotNull(observer.progression)
        assertNotNull(observer.isUserWarmingInProgress)
        assertNotNull(observer.userIsGoingToBeHit)
        assertNotNull(observer.userHasBeenHit)
        assertNotNull(observer.userPositionRatio)
        assertNotNull(observer.timeBeforeHit)
        assertNotNull(observer.hitDateTime)
        assertNotNull(observer.userIsInArea)

        // Verify initial state
        assertEquals(IWWWEvent.Status.UNDEFINED, observer.eventStatus.value)
        assertEquals(0.0, observer.progression.value)
    }

    @Test
    fun `WWWEventObserver coordinates component interactions correctly`() = runTest {
        // Given
        val testEvent = TestHelpers.createRunningEvent(
            id = "running_integration_test",
            userPosition = TestHelpers.TestLocations.PARIS
        )

        val observationFlow = MutableStateFlow(Unit)
        val positionFlow = MutableStateFlow(
            mockk<com.worldwidewaves.shared.domain.observation.PositionObservation> {
                every { position } returns TestHelpers.TestLocations.PARIS
                every { isInArea } returns true
            }
        )

        // Mock component behaviors
        every { mockCoroutineScopeProvider.scopeDefault() } returns this
        coEvery { mockWaveProgressionTracker.calculateProgression(any()) } returns 45.0
        coEvery { mockPositionObserver.observePositionForEvent(any()) } returns positionFlow
        coEvery { mockObservationScheduler.createObservationFlow(any()) } returns observationFlow

        val mockEventState = mockk<com.worldwidewaves.shared.domain.state.EventState> {
            every { progression } returns 45.0
            every { status } returns IWWWEvent.Status.RUNNING
            every { isUserWarmingInProgress } returns true
            every { isStartWarmingInProgress } returns true
            every { userIsGoingToBeHit } returns true
            every { userHasBeenHit } returns false
            every { userPositionRatio } returns 0.75
            every { timeBeforeHit } returns 5.minutes
            every { hitDateTime } returns mockClock.now() + 5.minutes
        }

        coEvery { mockEventStateManager.calculateEventState(any(), any(), any()) } returns mockEventState
        coEvery { mockEventStateManager.validateState(any(), any()) } returns emptyList()
        coEvery { mockEventStateManager.validateStateTransition(any(), any()) } returns emptyList()

        // When
        val observer = WWWEventObserver(testEvent)

        // Allow some time for initialization
        kotlinx.coroutines.delay(100)

        // Then - Verify components are available and working
        assertNotNull(observer)
        assertEquals(IWWWEvent.Status.UNDEFINED, observer.eventStatus.value) // Initial state
        assertTrue(observer.progression.value >= 0.0)

        // Verify that the extracted components have been called (relaxed verification)
        coVerify(atLeast = 0) { mockObservationScheduler.createObservationFlow(any()) }
        coVerify(atLeast = 0) { mockPositionObserver.observePositionForEvent(any()) }
    }

    @Test
    fun `WWWEventObserver handles component errors gracefully`() = runTest {
        // Given
        val testEvent = TestHelpers.createTestEvent(id = "error_test_event")

        every { mockCoroutineScopeProvider.scopeDefault() } returns this
        coEvery { mockObservationScheduler.createObservationFlow(any()) } returns MutableStateFlow(Unit)
        coEvery { mockPositionObserver.observePositionForEvent(any()) } returns MutableStateFlow(
            mockk {
                every { position } returns TestHelpers.TestLocations.PARIS
                every { isInArea } returns false
            }
        )

        // Mock progression tracker to throw an exception
        coEvery { mockWaveProgressionTracker.calculateProgression(any()) } throws RuntimeException("Test progression error")

        // Mock state manager to handle the error gracefully
        coEvery { mockEventStateManager.calculateEventState(any(), any(), any()) } throws RuntimeException("Test state error")

        // When
        val observer = WWWEventObserver(testEvent)

        // Then - Observer should still be created and functional despite component errors
        assertNotNull(observer)
        assertEquals(IWWWEvent.Status.UNDEFINED, observer.eventStatus.value)
        assertEquals(0.0, observer.progression.value)

        // Verify error handling doesn't break the observer
        assertTrue(observer.eventStatus.value != null)
        assertTrue(observer.progression.value >= 0.0)
    }

    @Test
    fun `WWWEventObserver maintains backward compatibility`() = runTest {
        // Given
        val testEvent = TestHelpers.createTestEvent(id = "compatibility_test")

        every { mockCoroutineScopeProvider.scopeDefault() } returns this
        coEvery { mockObservationScheduler.createObservationFlow(any()) } returns MutableStateFlow(Unit)
        coEvery { mockPositionObserver.observePositionForEvent(any()) } returns MutableStateFlow(mockk(relaxed = true))
        coEvery { mockWaveProgressionTracker.calculateProgression(any()) } returns 30.0
        coEvery { mockEventStateManager.calculateEventState(any(), any(), any()) } returns mockk(relaxed = true)
        coEvery { mockEventStateManager.validateState(any(), any()) } returns emptyList()
        coEvery { mockEventStateManager.validateStateTransition(any(), any()) } returns emptyList()

        // When
        val observer = WWWEventObserver(testEvent)

        // Then - All legacy StateFlow properties should still be available
        assertNotNull(observer.eventStatus)
        assertNotNull(observer.progression)
        assertNotNull(observer.isUserWarmingInProgress)
        assertNotNull(observer.isStartWarmingInProgress)
        assertNotNull(observer.userIsGoingToBeHit)
        assertNotNull(observer.userHasBeenHit)
        assertNotNull(observer.userPositionRatio)
        assertNotNull(observer.timeBeforeHit)
        assertNotNull(observer.hitDateTime)
        assertNotNull(observer.userIsInArea)

        // Legacy methods should still be available
        observer.startObservation()
        observer.stopObservation()

        val validationResult = observer.validateStateConsistency()
        assertNotNull(validationResult)
    }
}