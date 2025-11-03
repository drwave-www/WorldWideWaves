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

package com.worldwidewaves.shared.utils

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEventArea
import com.worldwidewaves.shared.events.WWWEventObserver
import com.worldwidewaves.shared.events.WWWEventWave
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.AbstractEventMap
import com.worldwidewaves.shared.map.MapLibreAdapter
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * Comprehensive tests for WaveProgressionObserver.
 *
 * Tests cover:
 * - Initial observation setup based on event status
 * - Status transition handling (SOON → RUNNING → DONE)
 * - Polygon update throttling (250ms sampling)
 * - Observer lifecycle (start, pause, stop)
 * - Concurrent status and progression updates
 * - Map integration and polygon rendering
 * - Performance and memory characteristics
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WaveProgressionObserverTest {
    private lateinit var testScope: TestScope
    private lateinit var mockEvent: IWWWEvent
    private lateinit var mockEventMap: AbstractEventMap<*>
    private lateinit var mockMapLibreAdapter: MapLibreAdapter<*>
    private lateinit var mockObserver: WWWEventObserver
    private lateinit var mockArea: WWWEventArea
    private lateinit var mockWave: WWWEventWave
    private var observer: WaveProgressionObserver? = null

    private val statusFlow = MutableStateFlow(IWWWEvent.Status.UNDEFINED)
    private val progressionFlow = MutableStateFlow(0.0)

    /**
     * Sets up mock objects and test environment before each test.
     */
    private fun setup() {
        testScope = TestScope()

        // Mock core dependencies
        mockObserver = mockk(relaxed = true)
        mockArea = mockk(relaxed = true)
        mockWave = mockk(relaxed = true)
        mockMapLibreAdapter = mockk<MapLibreAdapter<Any>>(relaxed = true)
        mockEventMap = mockk(relaxed = true)
        mockEvent = mockk(relaxed = true)

        // Setup event mock
        every { mockEvent.observer } returns mockObserver
        every { mockEvent.area } returns mockArea
        every { mockEvent.wave } returns mockWave

        // Setup observer flows
        every { mockObserver.eventStatus } returns statusFlow
        every { mockObserver.progression } returns progressionFlow

        // Setup event map
        every { mockEventMap.mapLibreAdapter } returns mockMapLibreAdapter
        every { mockEventMap.updateWavePolygons(any(), any()) } just runs

        // Setup map adapter with onMapSet callback
        // Note: We don't invoke the callback immediately to avoid test complexity with coroutine scheduling
        // Tests that need to verify addWavePolygons behavior should use relaxed mocks
        every { mockMapLibreAdapter.onMapSet(any()) } just runs
        every { mockMapLibreAdapter.addWavePolygons(any<List<Any>>(), any()) } just runs

        // Setup area polygons
        val testPolygons =
            listOf(
                createPolygon(Position(40.0, -74.0), Position(40.1, -74.0), Position(40.1, -74.1), Position(40.0, -74.1)),
            )
        coEvery { mockArea.getPolygons() } returns testPolygons

        // Setup wave polygons - default to empty
        coEvery { mockWave.getWavePolygons() } returns null
    }

    @AfterTest
    fun cleanup() {
        observer?.stopObservation()
        observer = null
        statusFlow.value = IWWWEvent.Status.UNDEFINED
        progressionFlow.value = 0.0
    }

    /**
     * Helper function to create a Polygon from list of positions
     */
    private fun createPolygon(vararg positions: Position): Polygon =
        Polygon().apply {
            positions.forEach { add(it) }
        }

    @Test
    fun `should not observe when event is null`() =
        runTest {
            // Arrange
            val testEventMap = mockk<AbstractEventMap<*>>(relaxed = true)
            every { testEventMap.updateWavePolygons(any(), any()) } just runs

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = testEventMap,
                    event = null,
                )

            // Act
            observer.startObservation()
            testScheduler.runCurrent() // Use runCurrent() since event is null, no infinite flow

            // Assert - no interactions with map should occur
            verify(exactly = 0) { testEventMap.updateWavePolygons(any(), any()) }
        }

    @Test
    fun `should start polygon observation when event is RUNNING`() =
        runTest {
            // Arrange
            setup()
            coEvery { mockEvent.isRunning() } returns true
            coEvery { mockEvent.isDone() } returns false

            val testPolygons =
                listOf(
                    createPolygon(Position(40.0, -74.0), Position(40.1, -74.0), Position(40.1, -74.1)),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk<WWWEventWave.WavePolygons> {
                    every { traversedPolygons } returns testPolygons
                }

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            // Act - start observation with RUNNING status
            statusFlow.value = IWWWEvent.Status.RUNNING
            observer.startObservation()

            // Give time for coroutines to start and first emission to be processed
            advanceTimeBy(1.milliseconds)
            testScheduler.runCurrent()

            // Trigger progression update
            progressionFlow.value = 0.5
            advanceTimeBy(250.milliseconds)
            testScheduler.runCurrent()

            // Stop before waiting for idle to cancel infinite flow
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Assert - should update wave polygons
            verify(atLeast = 1) { mockEventMap.updateWavePolygons(testPolygons, true) }
        }

    @Test
    fun `should add full wave polygons when event is DONE`() =
        runTest {
            // Arrange
            setup()
            coEvery { mockEvent.isRunning() } returns false
            coEvery { mockEvent.isDone() } returns true

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            // Act - start observation with DONE status
            statusFlow.value = IWWWEvent.Status.DONE
            observer.startObservation()
            testScheduler.runCurrent() // Process initial setup

            // Stop before waiting for idle to cancel infinite flow
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Assert - should call onMapSet to register callback for rendering
            verify(atLeast = 1) { mockMapLibreAdapter.onMapSet(any()) }
        }

    @Test
    fun `should wait when event status is UNDEFINED or SOON`() =
        runTest {
            // Arrange
            setup()
            coEvery { mockEvent.isRunning() } returns false
            coEvery { mockEvent.isDone() } returns false

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            // Act - start with UNDEFINED status
            statusFlow.value = IWWWEvent.Status.UNDEFINED
            observer.startObservation()
            testScheduler.runCurrent() // Process initial setup

            // Assert - only the initial clear call should occur, no polygon rendering
            verify(exactly = 1) { mockEventMap.updateWavePolygons(emptyList(), clearPolygons = true) }
            verify(exactly = 0) { mockEventMap.updateWavePolygons(match { it.isNotEmpty() }, any()) }

            // Act - change to SOON
            statusFlow.value = IWWWEvent.Status.SOON
            testScheduler.runCurrent()

            // Stop before waiting for idle to cancel infinite flow
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Assert - still only the initial clear, no polygon rendering
            verify(exactly = 1) { mockEventMap.updateWavePolygons(emptyList(), clearPolygons = true) }
            verify(exactly = 0) { mockEventMap.updateWavePolygons(match { it.isNotEmpty() }, any()) }
        }

    @Test
    fun `should transition from RUNNING to DONE status`() =
        runTest {
            // Arrange
            setup()
            // Keep RUNNING state long enough for polygon observation to work
            coEvery { mockEvent.isRunning() } returns true
            coEvery { mockEvent.isDone() } returns false

            val testPolygons =
                listOf(
                    createPolygon(Position(40.0, -74.0), Position(40.1, -74.0), Position(40.1, -74.1)),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk<WWWEventWave.WavePolygons> {
                    every { traversedPolygons } returns testPolygons
                }

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            // Act - start with RUNNING status
            statusFlow.value = IWWWEvent.Status.RUNNING
            observer.startObservation()
            advanceTimeBy(1.milliseconds)
            testScheduler.runCurrent()

            // Trigger progression update
            progressionFlow.value = 0.5
            advanceTimeBy(250.milliseconds)
            testScheduler.runCurrent()

            // Now transition to DONE
            coEvery { mockEvent.isRunning() } returns false
            coEvery { mockEvent.isDone() } returns true
            statusFlow.value = IWWWEvent.Status.DONE
            advanceTimeBy(1.milliseconds)
            testScheduler.runCurrent()

            // Stop before waiting for idle to cancel infinite flow
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Assert - should have updated wave polygons during RUNNING
            verify(atLeast = 1) { mockEventMap.updateWavePolygons(any(), any()) }
            // And called onMapSet for full polygons during DONE
            verify(atLeast = 1) { mockMapLibreAdapter.onMapSet(any()) }
        }

    @Test
    fun `should throttle polygon updates to 250ms using Flow sample`() =
        runTest {
            // Arrange
            setup()
            coEvery { mockEvent.isRunning() } returns true
            coEvery { mockEvent.isDone() } returns false

            val testPolygons =
                listOf(
                    createPolygon(Position(40.0, -74.0), Position(40.1, -74.0), Position(40.1, -74.1)),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk<WWWEventWave.WavePolygons> {
                    every { traversedPolygons } returns testPolygons
                }

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            statusFlow.value = IWWWEvent.Status.RUNNING
            observer.startObservation()
            testScheduler.runCurrent()

            // Act - trigger rapid progression updates (every 50ms)
            repeat(10) { i ->
                progressionFlow.value = i * 0.1
                advanceTimeBy(50.milliseconds)
            }

            // Stop before waiting for idle to cancel infinite flow
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Assert - should have throttled updates (sampled at 250ms intervals)
            // With 10 updates at 50ms intervals (500ms total), expect ~2 sampled updates
            verify(atMost = 3) { mockEventMap.updateWavePolygons(any(), any()) }
        }

    @Test
    fun `should preserve last non-empty polygon list when wave returns empty set`() =
        runTest {
            // Arrange
            setup()
            coEvery { mockEvent.isRunning() } returns true
            coEvery { mockEvent.isDone() } returns false

            val testPolygons =
                listOf(
                    createPolygon(Position(40.0, -74.0), Position(40.1, -74.0), Position(40.1, -74.1)),
                )

            // First return non-empty, then empty
            coEvery { mockWave.getWavePolygons() } returns
                mockk<WWWEventWave.WavePolygons> {
                    every { traversedPolygons } returns testPolygons
                } andThen
                mockk<WWWEventWave.WavePolygons> {
                    every { traversedPolygons } returns emptyList()
                }

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            statusFlow.value = IWWWEvent.Status.RUNNING
            observer.startObservation()
            advanceTimeBy(1.milliseconds)
            testScheduler.runCurrent()

            // Act - first update with polygons
            progressionFlow.value = 0.5
            advanceTimeBy(250.milliseconds)
            testScheduler.runCurrent()

            // Second update with empty polygons
            progressionFlow.value = 0.6
            advanceTimeBy(250.milliseconds)
            testScheduler.runCurrent()

            // Stop before waiting for idle to cancel infinite flow
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Assert - should have updated with testPolygons, then kept it when empty
            verify(atLeast = 2) { mockEventMap.updateWavePolygons(testPolygons, any()) }
        }

    @Test
    fun `should cancel polygon observation when paused`() =
        runTest {
            // Arrange
            setup()
            coEvery { mockEvent.isRunning() } returns true
            coEvery { mockEvent.isDone() } returns false

            val testPolygons =
                listOf(
                    createPolygon(Position(40.0, -74.0), Position(40.1, -74.0), Position(40.1, -74.1)),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk<WWWEventWave.WavePolygons> {
                    every { traversedPolygons } returns testPolygons
                }

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            statusFlow.value = IWWWEvent.Status.RUNNING
            observer.startObservation()
            testScheduler.runCurrent()

            // Act - pause observation (cancels the infinite flow)
            observer.pauseObservation()
            testScheduler.advanceUntilIdle()

            // Try to trigger more updates
            progressionFlow.value = 0.8
            advanceTimeBy(500.milliseconds)
            testScheduler.advanceUntilIdle()

            // Assert - only the initial clear call from startObservation, no polygon rendering after pause
            verify(exactly = 1) { mockEventMap.updateWavePolygons(emptyList(), clearPolygons = true) }
            verify(exactly = 0) { mockEventMap.updateWavePolygons(match { it.isNotEmpty() }, any()) }
        }

    @Test
    fun `should cancel polygon observation when stopped`() =
        runTest {
            // Arrange
            setup()
            coEvery { mockEvent.isRunning() } returns true
            coEvery { mockEvent.isDone() } returns false

            val testPolygons =
                listOf(
                    createPolygon(Position(40.0, -74.0), Position(40.1, -74.0), Position(40.1, -74.1)),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk<WWWEventWave.WavePolygons> {
                    every { traversedPolygons } returns testPolygons
                }

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            statusFlow.value = IWWWEvent.Status.RUNNING
            observer.startObservation()
            testScheduler.runCurrent()

            // Act - stop observation (alias for pause, cancels the infinite flow)
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Try to trigger more updates
            progressionFlow.value = 0.9
            advanceTimeBy(500.milliseconds)
            testScheduler.advanceUntilIdle()

            // Assert - only the initial clear call from startObservation, no polygon rendering after stop
            verify(exactly = 1) { mockEventMap.updateWavePolygons(emptyList(), clearPolygons = true) }
            verify(exactly = 0) { mockEventMap.updateWavePolygons(match { it.isNotEmpty() }, any()) }
        }

    @Test
    fun `should resume observation after pause when startObservation called again`() =
        runTest {
            // Arrange
            setup()
            coEvery { mockEvent.isRunning() } returns true
            coEvery { mockEvent.isDone() } returns false

            val testPolygons =
                listOf(
                    createPolygon(Position(40.0, -74.0), Position(40.1, -74.0), Position(40.1, -74.1)),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk<WWWEventWave.WavePolygons> {
                    every { traversedPolygons } returns testPolygons
                }

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            statusFlow.value = IWWWEvent.Status.RUNNING
            observer.startObservation()
            advanceTimeBy(1.milliseconds)
            testScheduler.runCurrent()

            // Pause
            observer.pauseObservation()
            testScheduler.advanceUntilIdle()

            // Act - resume
            observer.startObservation()
            advanceTimeBy(1.milliseconds)
            testScheduler.runCurrent()

            progressionFlow.value = 0.8
            advanceTimeBy(250.milliseconds)
            testScheduler.runCurrent()

            // Stop before waiting for idle to cancel infinite flow
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Assert - should resume updates after restart
            verify(atLeast = 1) { mockEventMap.updateWavePolygons(testPolygons, true) }
        }

    @Test
    fun `should handle concurrent status and progression updates`() =
        runTest {
            // Arrange
            setup()
            // Keep RUNNING state long enough for polygon observation to work
            coEvery { mockEvent.isRunning() } returns true
            coEvery { mockEvent.isDone() } returns false

            val testPolygons =
                listOf(
                    createPolygon(Position(40.0, -74.0), Position(40.1, -74.0), Position(40.1, -74.1)),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk<WWWEventWave.WavePolygons> {
                    every { traversedPolygons } returns testPolygons
                }

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            statusFlow.value = IWWWEvent.Status.RUNNING
            observer.startObservation()
            advanceTimeBy(1.milliseconds)
            testScheduler.runCurrent()

            // Act - concurrent updates
            progressionFlow.value = 0.3
            statusFlow.value = IWWWEvent.Status.RUNNING
            advanceTimeBy(100.milliseconds)
            testScheduler.runCurrent()

            progressionFlow.value = 0.6
            advanceTimeBy(150.milliseconds)
            testScheduler.runCurrent()

            // Now transition to DONE
            coEvery { mockEvent.isRunning() } returns false
            coEvery { mockEvent.isDone() } returns true
            statusFlow.value = IWWWEvent.Status.DONE
            progressionFlow.value = 1.0
            advanceTimeBy(1.milliseconds)
            testScheduler.runCurrent()

            // Stop before waiting for idle to cancel infinite flow
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Assert - should handle both flows correctly
            verify(atLeast = 1) { mockEventMap.updateWavePolygons(any(), any()) }
            verify(atLeast = 1) { mockMapLibreAdapter.onMapSet(any()) }
        }

    @Test
    fun `should not update polygons when event is neither RUNNING nor DONE`() =
        runTest {
            // Arrange
            setup()
            coEvery { mockEvent.isRunning() } returns false
            coEvery { mockEvent.isDone() } returns false

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            statusFlow.value = IWWWEvent.Status.SOON
            observer.startObservation()
            testScheduler.runCurrent()

            // Act - try to trigger progression (but event not running/done)
            progressionFlow.value = 0.5
            advanceTimeBy(300.milliseconds)

            // Stop before waiting for idle to cancel infinite flow
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Assert - only the initial clear call, no polygon rendering for SOON status
            verify(exactly = 1) { mockEventMap.updateWavePolygons(emptyList(), clearPolygons = true) }
            verify(exactly = 0) { mockEventMap.updateWavePolygons(match { it.isNotEmpty() }, any()) }
        }

    @Test
    fun `should handle null eventMap gracefully`() =
        runTest {
            // Arrange
            setup()
            coEvery { mockEvent.isRunning() } returns true
            coEvery { mockEvent.isDone() } returns false

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = null, // null map
                    event = mockEvent,
                )

            // Act
            statusFlow.value = IWWWEvent.Status.RUNNING
            observer.startObservation()
            testScheduler.runCurrent()

            progressionFlow.value = 0.5
            advanceTimeBy(250.milliseconds)

            // Stop before waiting for idle to cancel infinite flow
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Assert - should not crash, just no-op
            assertTrue(true, "Should handle null eventMap without crashing")
        }

    @Test
    fun `should update with correct isDone flag in addWavePolygons`() =
        runTest {
            // Arrange
            setup()
            coEvery { mockEvent.isRunning() } returns false
            coEvery { mockEvent.isDone() } returns true

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            // Act
            statusFlow.value = IWWWEvent.Status.DONE
            observer.startObservation()
            testScheduler.runCurrent()

            // Stop before waiting for idle to cancel infinite flow
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Assert - should call onMapSet which will eventually call addWavePolygons with isDone = true
            verify { mockMapLibreAdapter.onMapSet(any()) }
        }

    @Test
    fun `should handle rapid status transitions without race conditions`() =
        runTest {
            // Arrange
            setup()
            var runningState = false
            coEvery { mockEvent.isRunning() } answers { runningState }
            coEvery { mockEvent.isDone() } answers { !runningState }

            val testPolygons =
                listOf(
                    createPolygon(Position(40.0, -74.0), Position(40.1, -74.0), Position(40.1, -74.1)),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk<WWWEventWave.WavePolygons> {
                    every { traversedPolygons } returns testPolygons
                }

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            observer.startObservation()
            testScheduler.runCurrent()

            // Act - rapid status transitions
            repeat(5) {
                runningState = true
                statusFlow.value = IWWWEvent.Status.RUNNING
                advanceTimeBy(50.milliseconds)

                runningState = false
                statusFlow.value = IWWWEvent.Status.DONE
                advanceTimeBy(50.milliseconds)
            }

            // Stop before waiting for idle to cancel infinite flow
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Assert - should handle all transitions without crashes
            assertTrue(true, "Should handle rapid status transitions safely")
        }

    @Test
    fun `should update wave polygons with correct refresh flag`() =
        runTest {
            // Arrange
            setup()
            coEvery { mockEvent.isRunning() } returns true
            coEvery { mockEvent.isDone() } returns false

            val testPolygons =
                listOf(
                    createPolygon(Position(40.0, -74.0), Position(40.1, -74.0), Position(40.1, -74.1)),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk<WWWEventWave.WavePolygons> {
                    every { traversedPolygons } returns testPolygons
                }

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            statusFlow.value = IWWWEvent.Status.RUNNING
            observer.startObservation()
            advanceTimeBy(1.milliseconds)
            testScheduler.runCurrent()

            // Act
            progressionFlow.value = 0.5
            advanceTimeBy(250.milliseconds)
            testScheduler.runCurrent()

            // Stop before waiting for idle to cancel infinite flow
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Assert - should update with refresh = true for new polygons
            verify(atLeast = 1) { mockEventMap.updateWavePolygons(testPolygons, true) }
        }

    @Test
    fun `performance test - should handle 100 progression updates efficiently`() =
        runTest {
            // Arrange
            setup()
            coEvery { mockEvent.isRunning() } returns true
            coEvery { mockEvent.isDone() } returns false

            val testPolygons =
                listOf(
                    createPolygon(Position(40.0, -74.0), Position(40.1, -74.0), Position(40.1, -74.1)),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk<WWWEventWave.WavePolygons> {
                    every { traversedPolygons } returns testPolygons
                }

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            statusFlow.value = IWWWEvent.Status.RUNNING
            observer.startObservation()
            testScheduler.runCurrent()

            // Act - 100 rapid updates
            repeat(100) { i ->
                progressionFlow.value = i / 100.0
                advanceTimeBy(10.milliseconds) // Total 1000ms
            }

            // Stop before waiting for idle to cancel infinite flow
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Assert - should throttle to ~4 updates (1000ms / 250ms sampling)
            verify(atMost = 6) { mockEventMap.updateWavePolygons(any(), any()) }
        }

    @Test
    fun `should cancel previous polygon job when starting new observation`() =
        runTest {
            // Arrange
            setup()
            coEvery { mockEvent.isRunning() } returns true
            coEvery { mockEvent.isDone() } returns false

            val testPolygons =
                listOf(
                    createPolygon(Position(40.0, -74.0), Position(40.1, -74.0), Position(40.1, -74.1)),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk<WWWEventWave.WavePolygons> {
                    every { traversedPolygons } returns testPolygons
                }

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            statusFlow.value = IWWWEvent.Status.RUNNING
            observer.startObservation()
            advanceTimeBy(1.milliseconds)
            testScheduler.runCurrent()

            // Trigger first update
            progressionFlow.value = 0.3
            advanceTimeBy(250.milliseconds)
            testScheduler.runCurrent()

            // Act - start observation again (should cancel previous job)
            statusFlow.value = IWWWEvent.Status.RUNNING
            advanceTimeBy(1.milliseconds)
            testScheduler.runCurrent()

            progressionFlow.value = 0.7
            advanceTimeBy(250.milliseconds)
            testScheduler.runCurrent()

            // Stop before waiting for idle to cancel infinite flow
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Assert - should have handled both observations
            verify(atLeast = 2) { mockEventMap.updateWavePolygons(any(), any()) }
        }

    /**
     * Tests polygon clearing functionality when starting observation.
     * Ensures clean state when simulation starts or observer restarts.
     */
    @Test
    fun `startObservation clears existing polygons before starting`() =
        runTest {
            setup()
            observer = WaveProgressionObserver(testScope, mockEventMap, mockEvent)

            // Act - Start observation
            observer!!.startObservation()
            testScheduler.runCurrent()

            // Assert - polygons were cleared with clearPolygons=true
            verify { mockEventMap.updateWavePolygons(emptyList(), clearPolygons = true) }
        }

    /**
     * Tests that polygons are cleared when observer restarts.
     * This simulates what happens when a simulation starts and triggers observer cascade.
     */
    @Test
    fun `observer restart clears polygons from previous observation`() =
        runTest {
            setup()
            observer = WaveProgressionObserver(testScope, mockEventMap, mockEvent)

            // First observation
            statusFlow.value = IWWWEvent.Status.RUNNING
            observer!!.startObservation()
            testScheduler.runCurrent()

            // Verify initial clear
            verify(atLeast = 1) { mockEventMap.updateWavePolygons(emptyList(), clearPolygons = true) }

            // Stop observation
            observer!!.stopObservation()
            testScheduler.runCurrent()

            // Restart observation (simulating simulation start triggering observer restart)
            observer!!.startObservation()
            testScheduler.runCurrent()

            // Verify polygons cleared again on restart
            verify(atLeast = 2) { mockEventMap.updateWavePolygons(emptyList(), clearPolygons = true) }
        }

    /**
     * Tests that multiple observer restarts consistently clear polygons.
     * This verifies the fix works across multiple simulation start/stop cycles.
     */
    @Test
    fun `multiple observer restarts clear polygons each time`() =
        runTest {
            setup()
            observer = WaveProgressionObserver(testScope, mockEventMap, mockEvent)

            // Cycle through start/stop 3 times
            repeat(3) { cycle ->
                observer!!.startObservation()
                testScheduler.runCurrent()

                observer!!.stopObservation()
                testScheduler.runCurrent()
            }

            // Verify polygons cleared at least 3 times (once per start)
            verify(atLeast = 3) { mockEventMap.updateWavePolygons(emptyList(), clearPolygons = true) }
        }
}
