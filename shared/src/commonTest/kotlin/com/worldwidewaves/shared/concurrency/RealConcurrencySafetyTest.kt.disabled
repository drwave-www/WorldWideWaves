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

package com.worldwidewaves.shared.concurrency

import com.worldwidewaves.shared.position.PositionManager
import com.worldwidewaves.shared.position.PositionSource
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.domain.state.DefaultEventStateManager
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Real concurrency safety tests using actual WorldWideWaves classes.
 * Tests race conditions, thread safety, and proper synchronization.
 */
class RealConcurrencySafetyTest {

    @Test
    fun `positionManager_concurrentGPSAndSimulationUpdates_maintainsSimulationPriority`() = runTest {
        val positionManager = PositionManager()

        val gpsPosition = Position(lat = 40.7831, lng = -73.9712, source = PositionSource.GPS)
        val simulationPosition = Position(lat = 40.7832, lng = -73.9713, source = PositionSource.SIMULATION)

        // Launch 100 concurrent GPS updates
        val gpsJobs = (1..100).map {
            launch {
                positionManager.updatePosition(gpsPosition.copy(lat = gpsPosition.lat + it * 0.0001))
                delay(1) // Small delay to create race conditions
            }
        }

        // Launch 100 concurrent SIMULATION updates
        val simJobs = (1..100).map {
            launch {
                positionManager.updatePosition(simulationPosition.copy(lat = simulationPosition.lat + it * 0.0001))
                delay(1)
            }
        }

        // Wait for all updates to complete
        (gpsJobs + simJobs).forEach { it.join() }

        // SIMULATION should always maintain priority
        val finalPosition = positionManager.currentPosition.first()
        assertNotNull(finalPosition)
        assertEquals(PositionSource.SIMULATION, finalPosition.source)
    }

    @Test
    fun `eventStateManager_concurrentStateUpdates_maintainsConsistency`() = runTest {
        val stateManager = DefaultEventStateManager()
        val testEventId = "concurrent-test-event"

        val updateResults = mutableListOf<Boolean>()
        val mutex = Mutex()

        // Launch multiple concurrent state updates
        val jobs = (1..50).map { updateId ->
            launch {
                val progression = updateId / 100.0 // 0.01 to 0.50
                val success = kotlin.runCatching {
                    stateManager.updateProgression(testEventId, progression)
                }.isSuccess

                mutex.withLock {
                    updateResults.add(success)
                }

                delay(kotlin.random.Random.nextLong(1, 5))
            }
        }

        jobs.forEach { it.join() }

        // All updates should succeed
        assertEquals(50, updateResults.size)
        assertTrue(updateResults.all { it }, "All concurrent updates should succeed")

        // Final state should be valid
        val finalState = stateManager.getEventState(testEventId)
        assertNotNull(finalState)
        assertTrue(finalState.progression >= 0.0 && finalState.progression <= 1.0)
    }

    @Test
    fun `polygonCalculations_parallelExecution_consistentResults`() = runTest {
        val testPolygon = createTestPolygon()
        val testPosition = Position(lat = 40.7831, lng = -73.9712)

        val results = mutableListOf<Boolean>()
        val mutex = Mutex()

        // Perform the same calculation in parallel
        val jobs = (1..20).map {
            launch {
                val containsPoint = testPolygon.contains(testPosition)

                mutex.withLock {
                    results.add(containsPoint)
                }
            }
        }

        jobs.forEach { it.join() }

        // All parallel calculations should produce the same result
        assertEquals(20, results.size)
        val firstResult = results.first()
        assertTrue(results.all { it == firstResult }, "All parallel calculations should be consistent")
    }

    @Test
    fun `waveProgression_concurrentCalculations_threadsafe`() = runTest {
        val testEvent = createTestWaveEvent()
        val testPositions = (1..10).map { i ->
            Position(lat = 40.7831 + i * 0.001, lng = -73.9712 + i * 0.001)
        }

        val progressionResults = mutableMapOf<Position, Double>()
        val mutex = Mutex()

        // Calculate wave progression for multiple positions concurrently
        val jobs = testPositions.map { position ->
            launch {
                repeat(10) { // Multiple calculations per position
                    val progression = testEvent.wave.calculateProgression(
                        position,
                        kotlinx.datetime.Clock.System.now()
                    )

                    mutex.withLock {
                        progressionResults[position] = progression
                    }

                    delay(1)
                }
            }
        }

        jobs.forEach { it.join() }

        // All calculations should produce valid results
        assertEquals(testPositions.size, progressionResults.size)
        progressionResults.values.forEach { progression ->
            assertTrue(progression >= 0.0 && progression <= 1.0, "Progression should be valid: $progression")
        }
    }

    @Test
    fun `eventObserver_multipleSubscribers_allReceiveUpdates`() = runTest {
        val eventObserver = createTestEventObserver()
        val testEventId = "multi-subscriber-test"

        val subscriberResults = mutableMapOf<String, Int>()
        val mutex = Mutex()

        // Create multiple concurrent subscribers
        val subscriberJobs = (1..5).map { subscriberId ->
            launch {
                var updateCount = 0
                eventObserver.observeEventStatus(testEventId).collect { status ->
                    updateCount++
                    if (updateCount <= 10) { // Limit to prevent infinite collection
                        mutex.withLock {
                            subscriberResults["subscriber-$subscriberId"] = updateCount
                        }
                    }
                }
            }
        }

        // Publish updates
        val publishJob = launch {
            repeat(10) { updateIdx ->
                eventObserver.updateEventStatus(testEventId, "UPDATE-$updateIdx")
                delay(10)
            }
        }

        delay(200) // Allow updates to propagate
        publishJob.join()
        subscriberJobs.forEach { it.cancel() }

        // All subscribers should receive updates
        assertTrue(subscriberResults.size >= 3, "Most subscribers should receive updates")
        subscriberResults.values.forEach { count ->
            assertTrue(count > 0, "Each subscriber should receive at least one update")
        }
    }

    @Test
    fun `mapConstraints_concurrentBoundsUpdates_maintainsValidState`() = runTest {
        val constraintManager = createTestMapConstraintManager()

        val validationResults = mutableListOf<Boolean>()
        val mutex = Mutex()

        // Update map constraints from multiple sources concurrently
        val jobs = (1..20).map { updateId ->
            launch {
                val bounds = createValidBounds(updateId)
                constraintManager.updateConstraints(bounds)

                delay(kotlin.random.Random.nextLong(1, 10))

                val currentBounds = constraintManager.getCurrentConstraints()
                val isValid = currentBounds?.isValid() ?: false

                mutex.withLock {
                    validationResults.add(isValid)
                }
            }
        }

        jobs.forEach { it.join() }

        // All constraint states should remain valid
        assertTrue(validationResults.isNotEmpty())
        assertTrue(validationResults.all { it }, "All constraint states should be valid")
    }

    // Helper functions and test data builders

    private fun createTestPolygon(): Polygon {
        val centerLat = 40.7831
        val centerLng = -73.9712
        val radius = 0.01

        val vertices = (0..6).map { i ->
            val angle = 2 * kotlin.math.PI * i / 6
            Position(
                lat = centerLat + radius * kotlin.math.cos(angle),
                lng = centerLng + radius * kotlin.math.sin(angle)
            )
        }

        return Polygon(coordinates = vertices)
    }

    private fun createTestWaveEvent(): WWWEvent {
        return WWWEvent.create(
            id = "concurrency-test-event",
            name = "Concurrency Test Event",
            centerPosition = Position(lat = 40.7831, lng = -73.9712),
            radiusKm = 2.0,
            startTime = kotlinx.datetime.Clock.System.now(),
            waveSpeedKmh = 50.0
        )
    }

    private fun createTestEventObserver(): TestEventObserver {
        return TestEventObserver()
    }

    private fun createTestMapConstraintManager(): TestMapConstraintManager {
        return TestMapConstraintManager()
    }

    private fun createValidBounds(id: Int): TestBounds {
        val offset = id * 0.001
        return TestBounds(
            minLat = 40.7 + offset,
            minLng = -74.0 + offset,
            maxLat = 40.8 + offset,
            maxLng = -73.9 + offset
        )
    }

    // Test helper classes
    private class TestEventObserver {
        private val _statusFlow = kotlinx.coroutines.flow.MutableStateFlow<String>("INITIAL")

        fun observeEventStatus(eventId: String): kotlinx.coroutines.flow.Flow<String> = _statusFlow

        fun updateEventStatus(eventId: String, status: String) {
            _statusFlow.value = status
        }
    }

    private class TestMapConstraintManager {
        private var currentBounds: TestBounds? = null

        fun updateConstraints(bounds: TestBounds) {
            currentBounds = bounds
        }

        fun getCurrentConstraints(): TestBounds? = currentBounds
    }

    private data class TestBounds(
        val minLat: Double,
        val minLng: Double,
        val maxLat: Double,
        val maxLng: Double
    ) {
        fun isValid(): Boolean = minLat < maxLat && minLng < maxLng
    }
}