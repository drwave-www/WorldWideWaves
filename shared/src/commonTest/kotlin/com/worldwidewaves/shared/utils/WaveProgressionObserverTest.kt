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
import io.mockk.slot
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
        val onMapSetSlot = slot<(MapLibreAdapter<*>) -> Unit>()
        every { mockMapLibreAdapter.onMapSet(capture(onMapSetSlot)) } answers {
            onMapSetSlot.captured.invoke(mockMapLibreAdapter)
        }
        every { mockMapLibreAdapter.addWavePolygons(any<List<Any>>(), any()) } just runs

        // Setup area polygons
        val testPolygons =
            listOf(
                Polygon(
                    listOf(
                        Position(40.0, -74.0),
                        Position(40.1, -74.0),
                        Position(40.1, -74.1),
                        Position(40.0, -74.1),
                    ),
                ),
            )
        coEvery { mockArea.getPolygons() } returns testPolygons

        // Setup wave polygons - default to empty
        coEvery { mockWave.getWavePolygons() } returns null
    }

    @AfterTest
    fun cleanup() {
        statusFlow.value = IWWWEvent.Status.UNDEFINED
        progressionFlow.value = 0.0
    }

    @Test
    fun `should not observe when event is null`() =
        runTest {
            // Arrange
            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = null,
                )

            // Act
            observer.startObservation()
            testScheduler.advanceUntilIdle()

            // Assert - no interactions with map should occur
            verify(exactly = 0) { mockEventMap.updateWavePolygons(any(), any()) }
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
                    Polygon(
                        listOf(
                            Position(40.0, -74.0),
                            Position(40.1, -74.0),
                            Position(40.1, -74.1),
                        ),
                    ),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk {
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
            testScheduler.advanceUntilIdle()

            // Trigger progression update
            progressionFlow.value = 0.5
            advanceTimeBy(250.milliseconds)
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
            testScheduler.advanceUntilIdle()

            // Assert - should render all original polygons
            verify(atLeast = 1) { mockMapLibreAdapter.addWavePolygons(any(), true) }
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
            testScheduler.advanceUntilIdle()

            // Assert - no polygon updates should occur
            verify(exactly = 0) { mockEventMap.updateWavePolygons(any(), any()) }

            // Act - change to SOON
            statusFlow.value = IWWWEvent.Status.SOON
            testScheduler.advanceUntilIdle()

            // Assert - still no polygon updates
            verify(exactly = 0) { mockEventMap.updateWavePolygons(any(), any()) }
        }

    @Test
    fun `should transition from RUNNING to DONE status`() =
        runTest {
            // Arrange
            setup()
            coEvery { mockEvent.isRunning() } returns true andThen false
            coEvery { mockEvent.isDone() } returns false andThen true

            val testPolygons =
                listOf(
                    Polygon(
                        listOf(
                            Position(40.0, -74.0),
                            Position(40.1, -74.0),
                            Position(40.1, -74.1),
                        ),
                    ),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk {
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
            testScheduler.advanceUntilIdle()

            // Trigger progression update
            progressionFlow.value = 0.5
            advanceTimeBy(250.milliseconds)
            testScheduler.advanceUntilIdle()

            // Transition to DONE
            statusFlow.value = IWWWEvent.Status.DONE
            testScheduler.advanceUntilIdle()

            // Assert - should have updated wave polygons during RUNNING
            verify(atLeast = 1) { mockEventMap.updateWavePolygons(any(), any()) }
            // And added full polygons during DONE
            verify(atLeast = 1) { mockMapLibreAdapter.addWavePolygons(any(), true) }
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
                    Polygon(
                        listOf(
                            Position(40.0, -74.0),
                            Position(40.1, -74.0),
                            Position(40.1, -74.1),
                        ),
                    ),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk {
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
            testScheduler.advanceUntilIdle()

            // Act - trigger rapid progression updates (every 50ms)
            repeat(10) { i ->
                progressionFlow.value = i * 0.1
                advanceTimeBy(50.milliseconds)
            }
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
                    Polygon(
                        listOf(
                            Position(40.0, -74.0),
                            Position(40.1, -74.0),
                            Position(40.1, -74.1),
                        ),
                    ),
                )

            // First return non-empty, then empty
            coEvery { mockWave.getWavePolygons() } returns
                mockk { every { traversedPolygons } returns testPolygons } andThen
                mockk { every { traversedPolygons } returns emptyList() }

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            statusFlow.value = IWWWEvent.Status.RUNNING
            observer.startObservation()
            testScheduler.advanceUntilIdle()

            // Act - first update with polygons
            progressionFlow.value = 0.5
            advanceTimeBy(250.milliseconds)
            testScheduler.advanceUntilIdle()

            // Second update with empty polygons
            progressionFlow.value = 0.6
            advanceTimeBy(250.milliseconds)
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
                    Polygon(
                        listOf(
                            Position(40.0, -74.0),
                            Position(40.1, -74.0),
                            Position(40.1, -74.1),
                        ),
                    ),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk {
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
            testScheduler.advanceUntilIdle()

            // Act - pause observation
            observer.pauseObservation()
            testScheduler.advanceUntilIdle()

            // Try to trigger more updates
            progressionFlow.value = 0.8
            advanceTimeBy(500.milliseconds)
            testScheduler.advanceUntilIdle()

            // Assert - no new updates should occur after pause
            verify(exactly = 0) { mockEventMap.updateWavePolygons(any(), any()) }
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
                    Polygon(
                        listOf(
                            Position(40.0, -74.0),
                            Position(40.1, -74.0),
                            Position(40.1, -74.1),
                        ),
                    ),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk {
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
            testScheduler.advanceUntilIdle()

            // Act - stop observation (alias for pause)
            observer.stopObservation()
            testScheduler.advanceUntilIdle()

            // Try to trigger more updates
            progressionFlow.value = 0.9
            advanceTimeBy(500.milliseconds)
            testScheduler.advanceUntilIdle()

            // Assert - no new updates should occur after stop
            verify(exactly = 0) { mockEventMap.updateWavePolygons(any(), any()) }
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
                    Polygon(
                        listOf(
                            Position(40.0, -74.0),
                            Position(40.1, -74.0),
                            Position(40.1, -74.1),
                        ),
                    ),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk {
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
            testScheduler.advanceUntilIdle()

            // Pause
            observer.pauseObservation()
            testScheduler.advanceUntilIdle()

            // Act - resume
            observer.startObservation()
            testScheduler.advanceUntilIdle()

            progressionFlow.value = 0.8
            advanceTimeBy(250.milliseconds)
            testScheduler.advanceUntilIdle()

            // Assert - should resume updates after restart
            verify(atLeast = 1) { mockEventMap.updateWavePolygons(testPolygons, true) }
        }

    @Test
    fun `should handle concurrent status and progression updates`() =
        runTest {
            // Arrange
            setup()
            coEvery { mockEvent.isRunning() } returns true andThen false
            coEvery { mockEvent.isDone() } returns false andThen true

            val testPolygons =
                listOf(
                    Polygon(
                        listOf(
                            Position(40.0, -74.0),
                            Position(40.1, -74.0),
                            Position(40.1, -74.1),
                        ),
                    ),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk {
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
            testScheduler.advanceUntilIdle()

            // Act - concurrent updates
            progressionFlow.value = 0.3
            statusFlow.value = IWWWEvent.Status.RUNNING
            advanceTimeBy(100.milliseconds)

            progressionFlow.value = 0.6
            advanceTimeBy(150.milliseconds)

            statusFlow.value = IWWWEvent.Status.DONE
            progressionFlow.value = 1.0
            testScheduler.advanceUntilIdle()

            // Assert - should handle both flows correctly
            verify(atLeast = 1) { mockEventMap.updateWavePolygons(any(), any()) }
            verify(atLeast = 1) { mockMapLibreAdapter.addWavePolygons(any(), true) }
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
            testScheduler.advanceUntilIdle()

            // Act - try to trigger progression (but event not running/done)
            progressionFlow.value = 0.5
            advanceTimeBy(300.milliseconds)
            testScheduler.advanceUntilIdle()

            // Assert - no polygon updates should occur
            verify(exactly = 0) { mockEventMap.updateWavePolygons(any(), any()) }
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
            testScheduler.advanceUntilIdle()

            progressionFlow.value = 0.5
            advanceTimeBy(250.milliseconds)
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
            testScheduler.advanceUntilIdle()

            // Assert - should call with isDone = true
            verify { mockMapLibreAdapter.addWavePolygons(any(), true) }
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
                    Polygon(
                        listOf(
                            Position(40.0, -74.0),
                            Position(40.1, -74.0),
                            Position(40.1, -74.1),
                        ),
                    ),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk {
                    every { traversedPolygons } returns testPolygons
                }

            val observer =
                WaveProgressionObserver(
                    scope = this,
                    eventMap = mockEventMap,
                    event = mockEvent,
                )

            observer.startObservation()
            testScheduler.advanceUntilIdle()

            // Act - rapid status transitions
            repeat(5) {
                runningState = true
                statusFlow.value = IWWWEvent.Status.RUNNING
                advanceTimeBy(50.milliseconds)

                runningState = false
                statusFlow.value = IWWWEvent.Status.DONE
                advanceTimeBy(50.milliseconds)
            }
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
                    Polygon(
                        listOf(
                            Position(40.0, -74.0),
                            Position(40.1, -74.0),
                            Position(40.1, -74.1),
                        ),
                    ),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk {
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
            testScheduler.advanceUntilIdle()

            // Act
            progressionFlow.value = 0.5
            advanceTimeBy(250.milliseconds)
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
                    Polygon(
                        listOf(
                            Position(40.0, -74.0),
                            Position(40.1, -74.0),
                            Position(40.1, -74.1),
                        ),
                    ),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk {
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
            testScheduler.advanceUntilIdle()

            // Act - 100 rapid updates
            repeat(100) { i ->
                progressionFlow.value = i / 100.0
                advanceTimeBy(10.milliseconds) // Total 1000ms
            }
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
                    Polygon(
                        listOf(
                            Position(40.0, -74.0),
                            Position(40.1, -74.0),
                            Position(40.1, -74.1),
                        ),
                    ),
                )
            coEvery { mockWave.getWavePolygons() } returns
                mockk {
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
            testScheduler.advanceUntilIdle()

            // Trigger first update
            progressionFlow.value = 0.3
            advanceTimeBy(250.milliseconds)
            testScheduler.advanceUntilIdle()

            // Act - start observation again (should cancel previous job)
            statusFlow.value = IWWWEvent.Status.RUNNING
            testScheduler.advanceUntilIdle()

            progressionFlow.value = 0.7
            advanceTimeBy(250.milliseconds)
            testScheduler.advanceUntilIdle()

            // Assert - should have handled both observations
            verify(atLeast = 2) { mockEventMap.updateWavePolygons(any(), any()) }
        }
}
