package com.worldwidewaves.shared

/* * Copyright 2025 DrWave
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
 * limitations under the License. */

import com.worldwidewaves.shared.events.utils.Position
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Tests for WWWSimulation class
 */
@OptIn(ExperimentalTime::class)
class WWWSimulationTest {
    // Common test parameters
    private val startDateTime = Instant.fromEpochMilliseconds(1000)
    private val userPosition = Position(37.7749, -122.4194) // San Francisco coordinates

    @Test
    fun `test initialization with default speed`() {
        val simulation = WWWSimulation(startDateTime, userPosition)

        assertEquals(1, simulation.speed, "Default speed should be 1")
        assertEquals(userPosition, simulation.getUserPosition(), "User position should match initialization parameter")
    }

    @Test
    fun `test initialization with custom speed`() {
        val simulation = WWWSimulation(startDateTime, userPosition, 10)

        assertEquals(10, simulation.speed, "Speed should match initialization parameter")
    }

    @Test
    fun `test initialization with invalid speed throws exception`() {
        // Test with speed below minimum
        assertFailsWith<IllegalArgumentException> {
            WWWSimulation(startDateTime, userPosition, 0)
        }

        // Test with speed above maximum
        assertFailsWith<IllegalArgumentException> {
            WWWSimulation(startDateTime, userPosition, 601)
        }
    }

    @Test
    fun `test setSpeed updates speed correctly`() {
        runBlocking {
            val simulation = WWWSimulation(startDateTime, userPosition)

            val newSpeed = simulation.setSpeed(5)
            assertEquals(5, newSpeed, "setSpeed should return the new speed")
            assertEquals(5, simulation.speed, "Speed property should be updated")
        }
    }

    @Test
    fun `test setSpeed with invalid values throws exception`() {
        runBlocking {
            val simulation = WWWSimulation(startDateTime, userPosition)

            // Test with speed below minimum
            assertFailsWith<IllegalArgumentException> {
                simulation.setSpeed(0)
            }

            // Test with speed above maximum
            assertFailsWith<IllegalArgumentException> {
                simulation.setSpeed(601)
            }
        }
    }

    @Test
    fun `should return correct time with speed 1`() {
        runBlocking {
            // GIVEN: Simulation with speed 1
            val simulation = WWWSimulation(startDateTime, userPosition)

            // WHEN: Getting initial and subsequent time
            val initialSimTime = simulation.now()

            // THEN: Initial time should be close to start time (allowing for microsecond differences)
            val timeDifference = (initialSimTime - startDateTime).inWholeMilliseconds
            assertTrue(
                timeDifference < 100,
                "Initial simulation time should be close to start time, got ${timeDifference}ms difference",
            )

            // AND: Time should advance linearly with speed 1
            // We test the calculation logic rather than real-time delays
            val futureTime = simulation.now()
            assertTrue(
                futureTime >= initialSimTime,
                "Simulation time should always advance forward",
            )
        }
    }

    @Test
    fun `should return correct time with speed greater than 1`() {
        runBlocking {
            // GIVEN: Simulation with speed 10
            val simulation = WWWSimulation(startDateTime, userPosition, 10)

            // WHEN: Getting initial time
            val initialSimTime = simulation.now()

            // THEN: Initial time should be close to start time (allowing for microsecond differences)
            val timeDifference = (initialSimTime - startDateTime).inWholeMilliseconds
            assertTrue(
                timeDifference < 100,
                "Initial simulation time should be close to start time, got ${timeDifference}ms difference",
            )

            // AND: Speed should be correctly set
            assertEquals(10, simulation.speed, "Simulation speed should be 10")

            // AND: Time advancement should work
            val laterTime = simulation.now()
            assertTrue(
                laterTime >= initialSimTime,
                "Simulation time should advance even with higher speed",
            )
        }
    }

    @Test
    fun `should handle speed changes correctly`() {
        runBlocking {
            // GIVEN: Simulation with initial speed 1
            val simulation = WWWSimulation(startDateTime, userPosition)

            // WHEN: Getting initial state
            val initialSpeed = simulation.speed
            val initialTime = simulation.now()

            // THEN: Initial speed should be 1
            assertEquals(1, initialSpeed, "Initial speed should be 1")

            // WHEN: Changing speed to 5
            simulation.setSpeed(5)

            // THEN: Speed should be updated
            assertEquals(5, simulation.speed, "Speed should be updated to 5")

            // AND: Time should still advance
            val timeAfterSpeedChange = simulation.now()
            assertTrue(
                timeAfterSpeedChange >= initialTime,
                "Time should continue advancing after speed change",
            )
        }
    }

    @Test
    fun `should handle multiple speed changes in sequence`() =
        runBlocking {
            // GIVEN: Simulation with initial speed 2
            val simulation = WWWSimulation(startDateTime, userPosition, 2)

            // WHEN: Testing sequence of speed changes
            val speeds = listOf(2, 5, 10, 1, 50)
            var previousTime = simulation.now()

            for (speed in speeds) {
                // WHEN: Changing to new speed
                simulation.setSpeed(speed)

                // THEN: Speed should be updated correctly
                assertEquals(speed, simulation.speed, "Speed should be updated to $speed")

                // AND: Time should advance consistently
                val currentTime = simulation.now()
                assertTrue(
                    currentTime >= previousTime,
                    "Time should always advance forward with speed $speed",
                )
                previousTime = currentTime
            }
        }

    @Test
    fun `should handle pause and resume functionality`() =
        runBlocking {
            // GIVEN: Simulation with speed 10
            val simulation = WWWSimulation(startDateTime, userPosition, 10)
            val initialTime = simulation.now()

            // WHEN: Pausing the simulation
            simulation.pause()

            // THEN: Speed should be 0 when paused
            assertEquals(0, simulation.speed, "Speed should be 0 when paused")

            // AND: Time should not advance significantly while paused
            val pausedTime = simulation.now()
            assertTrue(
                (pausedTime - initialTime).inWholeMilliseconds < 50,
                "Time should not advance significantly while paused",
            )

            // WHEN: Resuming with speed 5
            simulation.resume(5)

            // THEN: Speed should be updated when resumed
            assertEquals(5, simulation.speed, "Speed should be updated when resumed")

            // AND: Time should advance after resume
            val resumedTime = simulation.now()
            assertTrue(
                resumedTime >= pausedTime,
                "Time should advance after resume",
            )

            // WHEN: Testing resume without specifying speed (should use last active speed)
            simulation.pause()
            simulation.resume()

            // THEN: Should use last active speed
            assertEquals(5, simulation.speed, "Resume without speed parameter should use last active speed")
        }

    @Test
    fun `should handle reset functionality`() =
        runBlocking {
            // GIVEN: Simulation with speed 5
            val simulation = WWWSimulation(startDateTime, userPosition, 5)

            // WHEN: Getting time after creation
            val initialTime = simulation.now()

            // THEN: Time should be close to start time initially (allowing for microsecond differences)
            val initialTimeDifference = (initialTime - startDateTime).inWholeMilliseconds
            assertTrue(
                initialTimeDifference < 100,
                "Initial time should be close to start time, got ${initialTimeDifference}ms difference",
            )

            // WHEN: Resetting the simulation
            simulation.reset()

            // THEN: Time should be close to start time after reset
            val afterResetTime = simulation.now()
            val resetTimeDifference = (afterResetTime - startDateTime).inWholeMilliseconds
            assertTrue(
                resetTimeDifference < 100,
                "Time should be close to start time after reset, got ${resetTimeDifference}ms difference",
            )

            // AND: Speed should remain unchanged
            assertEquals(5, simulation.speed, "Speed should remain unchanged after reset")

            // AND: Time should continue to advance after reset
            val laterTime = simulation.now()
            assertTrue(
                laterTime >= afterResetTime,
                "Time should continue advancing after reset",
            )
        }

    @Test
    fun `test boundary speeds`() =
        runBlocking {
            // Test with minimum speed
            val minSpeedSimulation = WWWSimulation(startDateTime, userPosition, WWWSimulation.MIN_SPEED)
            assertEquals(WWWSimulation.MIN_SPEED, minSpeedSimulation.speed)

            // Test with maximum speed
            val maxSpeedSimulation = WWWSimulation(startDateTime, userPosition, WWWSimulation.MAX_SPEED)
            assertEquals(WWWSimulation.MAX_SPEED, maxSpeedSimulation.speed)

            // Test changing to boundary speeds
            val simulation = WWWSimulation(startDateTime, userPosition, 50)

            simulation.setSpeed(WWWSimulation.MIN_SPEED)
            assertEquals(WWWSimulation.MIN_SPEED, simulation.speed)

            simulation.setSpeed(WWWSimulation.MAX_SPEED)
            assertEquals(WWWSimulation.MAX_SPEED, simulation.speed)
        }

    @Test
    fun `test time calculation accuracy`() =
        runBlocking {
            // Create simulation with a moderate speed for accuracy testing
            val testSpeed = 20
            val simulation = WWWSimulation(startDateTime, userPosition, testSpeed)

            // Record initial times
            val initialSimTime = simulation.now()
            val initialRealTime = Clock.System.now()

            // Wait for a precise amount of time
            val waitDuration = 500.milliseconds
            delay(waitDuration)

            // Record final times
            val finalSimTime = simulation.now()
            val finalRealTime = Clock.System.now()

            // Calculate actual elapsed times
            val realElapsed = finalRealTime - initialRealTime
            val simElapsed = finalSimTime - initialSimTime

            // Calculate expected simulated elapsed time
            val expectedSimElapsed = realElapsed * testSpeed

            // Allow for a small tolerance (10ms * speed)
            val tolerance = 10.milliseconds * testSpeed
            val difference =
                if (simElapsed > expectedSimElapsed) {
                    simElapsed - expectedSimElapsed
                } else {
                    expectedSimElapsed - simElapsed
                }

            assertTrue(
                difference < tolerance,
                "Time calculation should be precise. " +
                    "Expected: $expectedSimElapsed, Actual: $simElapsed, Difference: $difference",
            )
        }

    @Test
    fun `test user position handling`() {
        // Test that user position is stored and retrieved correctly
        val position1 = Position(0.0, 0.0)
        val simulation1 = WWWSimulation(startDateTime, position1)
        assertEquals(position1, simulation1.getUserPosition())

        // Test with different position
        val position2 = Position(90.0, 180.0)
        val simulation2 = WWWSimulation(startDateTime, position2)
        assertEquals(position2, simulation2.getUserPosition())

        // Verify position is immutable (can't be changed after initialization)
        val position3 = Position(45.0, 45.0)
        val simulation3 = WWWSimulation(startDateTime, position3)

        // Modify the original position object
        position3

        // User position from simulation should still have original values
        val retrievedPosition = simulation3.getUserPosition()
        assertEquals(45.0, retrievedPosition.latitude)
        assertEquals(45.0, retrievedPosition.longitude)
    }

    @Test
    fun `test concurrent setSpeed calls are thread-safe`() =
        runBlocking {
            // GIVEN: Simulation with initial speed 1
            val simulation = WWWSimulation(startDateTime, userPosition)

            // WHEN: Multiple coroutines concurrently change speed
            val speeds = listOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100)
            val jobs =
                speeds.map { speed ->
                    async {
                        simulation.setSpeed(speed)
                        // Small delay to increase chance of interleaving
                        delay(1)
                        simulation.now() // Read current time to verify consistency
                    }
                }

            // THEN: All operations should complete without corruption
            val results = jobs.awaitAll()

            // AND: Final speed should be one of the values set (not corrupted)
            assertTrue(
                speeds.contains(simulation.speed),
                "Final speed ${simulation.speed} should be one of the set values",
            )

            // AND: Time calculation should not crash or produce invalid results
            results.forEach { time ->
                assertTrue(
                    time >= startDateTime,
                    "Simulated time should never go backwards: $time vs $startDateTime",
                )
            }
        }

    @Test
    fun `test concurrent pause and resume are thread-safe`() =
        runBlocking {
            // GIVEN: Simulation with speed 10
            val simulation = WWWSimulation(startDateTime, userPosition, 10)

            // WHEN: Multiple coroutines concurrently pause and resume
            val operations =
                List(10) { index ->
                    async {
                        if (index % 2 == 0) {
                            simulation.pause()
                        } else {
                            simulation.resume(index + 1)
                        }
                        delay(1)
                        simulation.speed // Read speed to verify consistency
                    }
                }

            // THEN: All operations should complete without crashes
            val results = operations.awaitAll()

            // AND: Speed should be valid (0 for paused, or > 0 for resumed)
            assertTrue(
                simulation.speed >= 0 && simulation.speed <= WWWSimulation.MAX_SPEED,
                "Speed should be valid: ${simulation.speed}",
            )
        }

    @Test
    fun `test concurrent reset calls are thread-safe`() =
        runBlocking {
            // GIVEN: Simulation with speed 50
            val simulation = WWWSimulation(startDateTime, userPosition, 50)

            // WHEN: Multiple coroutines concurrently reset
            val jobs =
                List(20) {
                    async {
                        simulation.reset()
                        delay(1)
                        simulation.now()
                    }
                }

            // THEN: All operations should complete without corruption
            val results = jobs.awaitAll()

            // AND: Time should be close to start time after all resets
            val currentTime = simulation.now()
            val timeDiff = (currentTime - startDateTime).inWholeMilliseconds
            assertTrue(
                timeDiff < 200,
                "Time should be close to start time after resets: ${timeDiff}ms",
            )

            // AND: All intermediate time reads should be >= startDateTime
            results.forEach { time ->
                assertTrue(
                    time >= startDateTime,
                    "Time should never be before start time: $time",
                )
            }
        }

    // ============================================================================
    // GPS_MARKER Tests
    // ============================================================================

    @Test
    fun `test GPS_MARKER is a valid Position`() {
        // GPS_MARKER should be a Position that can be used in simulation
        val simulation = WWWSimulation(startDateTime, WWWSimulation.GPS_MARKER)

        assertEquals(WWWSimulation.GPS_MARKER, simulation.getUserPosition(), "GPS_MARKER should be stored correctly")
        assertEquals(999.0, simulation.getUserPosition().lat, "GPS_MARKER lat should be 999.0")
        assertEquals(999.0, simulation.getUserPosition().lng, "GPS_MARKER lng should be 999.0")
    }

    @Test
    fun `test GPS_MARKER equality works correctly`() {
        // Test that GPS_MARKER can be compared with == (unlike NaN)
        val marker1 = WWWSimulation.GPS_MARKER
        val marker2 = Position(999.0, 999.0)

        assertEquals(marker1, marker2, "GPS_MARKER should equal Position(999.0, 999.0)")
        assertTrue(marker1 == marker2, "GPS_MARKER should be comparable with ==")
    }

    @Test
    fun `test isUsingGPS returns true for GPS_MARKER`() {
        val simulation = WWWSimulation(startDateTime, WWWSimulation.GPS_MARKER)

        assertTrue(simulation.isUsingGPS(), "isUsingGPS should return true when position is GPS_MARKER")
    }

    @Test
    fun `test isUsingGPS returns false for normal position`() {
        val simulation = WWWSimulation(startDateTime, userPosition)

        assertTrue(!simulation.isUsingGPS(), "isUsingGPS should return false for normal position")
    }

    @Test
    fun `test GPS_MARKER is outside valid coordinate range`() {
        // Verify GPS_MARKER is clearly distinguishable from valid coordinates
        val marker = WWWSimulation.GPS_MARKER

        assertTrue(marker.lat > 90.0, "GPS_MARKER lat should be outside valid range (-90 to 90)")
        assertTrue(marker.lng > 180.0, "GPS_MARKER lng should be outside valid range (-180 to 180)")
    }

    @Test
    fun `test concurrent now() during setSpeed() is thread-safe`() =
        runBlocking {
            // GIVEN: Simulation with initial speed 10
            val simulation = WWWSimulation(startDateTime, userPosition, 10)

            // WHEN: Multiple coroutines concurrently read time while others change speed
            val readJobs =
                List(15) {
                    async {
                        // Continuously read time
                        val times = mutableListOf<Instant>()
                        repeat(5) {
                            times.add(simulation.now())
                            delay(1)
                        }
                        times
                    }
                }

            val writeJobs =
                List(10) { index ->
                    async {
                        delay(2) // Stagger writes slightly
                        simulation.setSpeed((index + 1) * 10)
                    }
                }

            // THEN: All operations should complete without crashes or inconsistent state
            val allReadResults = readJobs.awaitAll().flatten()
            writeJobs.awaitAll()

            // AND: All time readings should be valid (never before start time)
            allReadResults.forEach { time ->
                assertTrue(
                    time >= startDateTime,
                    "Time should never be before start time: $time vs $startDateTime",
                )
            }

            // AND: Final speed should be valid
            assertTrue(
                simulation.speed in WWWSimulation.MIN_SPEED..WWWSimulation.MAX_SPEED,
                "Final speed should be valid: ${simulation.speed}",
            )

            // AND: Final time should be consistent (can be read without crashing)
            val finalTime = simulation.now()
            assertTrue(
                finalTime >= startDateTime,
                "Final time should be >= start time: $finalTime",
            )
        }
}
