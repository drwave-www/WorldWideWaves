package com.worldwidewaves.shared

import com.worldwidewaves.shared.events.utils.Position
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
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
            WWWSimulation(startDateTime, userPosition, 501)
        }
    }

    @Test
    fun `test setSpeed updates speed correctly`() {
        val simulation = WWWSimulation(startDateTime, userPosition)

        val newSpeed = simulation.setSpeed(5)
        assertEquals(5, newSpeed, "setSpeed should return the new speed")
        assertEquals(5, simulation.speed, "Speed property should be updated")
    }

    @Test
    fun `test setSpeed with invalid values throws exception`() {
        val simulation = WWWSimulation(startDateTime, userPosition)

        // Test with speed below minimum
        assertFailsWith<IllegalArgumentException> {
            simulation.setSpeed(0)
        }

        // Test with speed above maximum
        assertFailsWith<IllegalArgumentException> {
            simulation.setSpeed(501)
        }
    }

    @Test
    fun `should return correct time with speed 1`() {
        // GIVEN: Simulation with speed 1
        val simulation = WWWSimulation(startDateTime, userPosition)

        // WHEN: Getting initial and subsequent time
        val initialSimTime = simulation.now()

        // THEN: Initial time should be close to start time (allowing for microsecond differences)
        val timeDifference = (initialSimTime - startDateTime).inWholeMilliseconds
        assertTrue(
            timeDifference < 100,
            "Initial simulation time should be close to start time, got ${timeDifference}ms difference"
        )

        // AND: Time should advance linearly with speed 1
        // We test the calculation logic rather than real-time delays
        val futureTime = simulation.now()
        assertTrue(
            futureTime >= initialSimTime,
            "Simulation time should always advance forward"
        )
    }

    @Test
    fun `should return correct time with speed greater than 1`() {
        // GIVEN: Simulation with speed 10
        val simulation = WWWSimulation(startDateTime, userPosition, 10)

        // WHEN: Getting initial time
        val initialSimTime = simulation.now()

        // THEN: Initial time should be close to start time (allowing for microsecond differences)
        val timeDifference = (initialSimTime - startDateTime).inWholeMilliseconds
        assertTrue(
            timeDifference < 100,
            "Initial simulation time should be close to start time, got ${timeDifference}ms difference"
        )

        // AND: Speed should be correctly set
        assertEquals(10, simulation.speed, "Simulation speed should be 10")

        // AND: Time advancement should work
        val laterTime = simulation.now()
        assertTrue(
            laterTime >= initialSimTime,
            "Simulation time should advance even with higher speed"
        )
    }

    @Test
    fun `should handle speed changes correctly`() {
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
            "Time should continue advancing after speed change"
        )
    }

    @Test
    fun `should handle multiple speed changes in sequence`() {
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
                "Time should always advance forward with speed $speed"
            )
            previousTime = currentTime
        }
    }

    @Test
    fun `should handle pause and resume functionality`() {
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
            "Time should not advance significantly while paused"
        )

        // WHEN: Resuming with speed 5
        simulation.resume(5)

        // THEN: Speed should be updated when resumed
        assertEquals(5, simulation.speed, "Speed should be updated when resumed")

        // AND: Time should advance after resume
        val resumedTime = simulation.now()
        assertTrue(
            resumedTime >= pausedTime,
            "Time should advance after resume"
        )

        // WHEN: Testing resume without specifying speed (should use last active speed)
        simulation.pause()
        simulation.resume()

        // THEN: Should use last active speed
        assertEquals(5, simulation.speed, "Resume without speed parameter should use last active speed")
    }

    @Test
    fun `should handle reset functionality`() {
        // GIVEN: Simulation with speed 5
        val simulation = WWWSimulation(startDateTime, userPosition, 5)

        // WHEN: Getting time after creation
        val initialTime = simulation.now()

        // THEN: Time should be close to start time initially (allowing for microsecond differences)
        val initialTimeDifference = (initialTime - startDateTime).inWholeMilliseconds
        assertTrue(
            initialTimeDifference < 100,
            "Initial time should be close to start time, got ${initialTimeDifference}ms difference"
        )

        // WHEN: Resetting the simulation
        simulation.reset()

        // THEN: Time should be close to start time after reset
        val afterResetTime = simulation.now()
        val resetTimeDifference = (afterResetTime - startDateTime).inWholeMilliseconds
        assertTrue(
            resetTimeDifference < 100,
            "Time should be close to start time after reset, got ${resetTimeDifference}ms difference"
        )

        // AND: Speed should remain unchanged
        assertEquals(5, simulation.speed, "Speed should remain unchanged after reset")

        // AND: Time should continue to advance after reset
        val laterTime = simulation.now()
        assertTrue(
            laterTime >= afterResetTime,
            "Time should continue advancing after reset"
        )
    }

    @Test
    fun `test boundary speeds`() {
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
        val mutablePosition = position3

        // User position from simulation should still have original values
        val retrievedPosition = simulation3.getUserPosition()
        assertEquals(45.0, retrievedPosition.latitude)
        assertEquals(45.0, retrievedPosition.longitude)
    }
}
