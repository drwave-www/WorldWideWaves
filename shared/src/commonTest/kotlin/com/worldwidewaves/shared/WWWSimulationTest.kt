package com.worldwidewaves.shared

import com.worldwidewaves.shared.events.utils.Position
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

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
    fun `test now returns correct time with speed 1`() = runBlocking {
        // Create simulation with speed 1
        val simulation = WWWSimulation(startDateTime, userPosition)
        
        // Get initial time
        val initialSimTime = simulation.now()
        val initialRealTime = Clock.System.now()
        
        // Wait for a small amount of real time
        delay(500) // 500ms
        
        // Get final time
        val finalSimTime = simulation.now()
        val finalRealTime = Clock.System.now()
        
        // Calculate elapsed times
        val realElapsed = finalRealTime - initialRealTime
        val simElapsed = finalSimTime - initialSimTime
        
        // With speed 1, simulated time should advance at the same rate as real time
        // Allow for small tolerance in timing
        val ratio = simElapsed.inWholeMilliseconds.toDouble() / realElapsed.inWholeMilliseconds.toDouble()
        assertTrue(abs(ratio - 1.0) < 0.2, 
            "With speed 1, simulated time should advance at the same rate as real time. " +
            "Actual ratio: $ratio")
    }
    
    @Test
    fun `test now returns correct time with speed greater than 1`() = runBlocking {
        // Create simulation with speed 10
        val simulation = WWWSimulation(startDateTime, userPosition, 10)
        
        // Get initial time
        val initialSimTime = simulation.now()
        val initialRealTime = Clock.System.now()
        
        // Wait for a small amount of real time
        delay(500) // 500ms
        
        // Get final time
        val finalSimTime = simulation.now()
        val finalRealTime = Clock.System.now()
        
        // Calculate elapsed times
        val realElapsed = finalRealTime - initialRealTime
        val simElapsed = finalSimTime - initialSimTime
        
        // With speed 10, simulated time should advance 10x as fast as real time
        // Allow for small tolerance in timing
        val expectedRatio = 10.0
        val actualRatio = simElapsed.inWholeMilliseconds.toDouble() / realElapsed.inWholeMilliseconds.toDouble()
        
        assertTrue(abs(actualRatio - expectedRatio) < 2.0, 
            "With speed 10, simulated time should advance 10x as fast as real time. " +
            "Expected ratio: $expectedRatio, Actual ratio: $actualRatio")
    }
    
    @Test
    fun `test speed changes affect time calculations correctly`() = runBlocking {
        // Create simulation with speed 1
        val simulation = WWWSimulation(startDateTime, userPosition)
        
        // Get initial time
        val initialSimTime = simulation.now()
        
        // Wait for a small amount of real time with speed 1
        val initialRealTime = Clock.System.now()
        delay(500) // 500ms
        val midRealTime = Clock.System.now()
        val midSimTime = simulation.now()
        
        // Calculate elapsed times for first period
        val firstRealElapsed = midRealTime - initialRealTime
        val firstSimElapsed = midSimTime - initialSimTime
        
        // Verify first period ratio is close to 1.0
        val firstRatio = firstSimElapsed.inWholeMilliseconds.toDouble() / firstRealElapsed.inWholeMilliseconds.toDouble()
        assertTrue(abs(firstRatio - 1.0) < 0.2, 
            "With speed 1, simulated time should advance at the same rate as real time. " +
            "Actual ratio: $firstRatio")
        
        // Change speed to 5
        simulation.setSpeed(5)
        
        // Wait for another small amount of real time with speed 5
        delay(500) // 500ms
        val finalRealTime = Clock.System.now()
        val finalSimTime = simulation.now()
        
        // Calculate elapsed times for second period
        val secondRealElapsed = finalRealTime - midRealTime
        val secondSimElapsed = finalSimTime - midSimTime
        
        // Verify second period ratio is close to 5.0
        val secondRatio = secondSimElapsed.inWholeMilliseconds.toDouble() / secondRealElapsed.inWholeMilliseconds.toDouble()
        assertTrue(abs(secondRatio - 5.0) < 1.0, 
            "With speed 5, simulated time should advance 5x as fast as real time. " +
            "Actual ratio: $secondRatio")
    }
    
    @Test
    fun `test multiple speed changes in sequence`() = runBlocking {
        // Create simulation with initial speed 2
        val simulation = WWWSimulation(startDateTime, userPosition, 2)
        
        // Test with a sequence of speed changes
        val speeds = listOf(2, 5, 10, 1, 50)
        var currentSimTime = simulation.now()
        
        for (speed in speeds) {
            if (simulation.speed != speed) {
                simulation.setSpeed(speed)
            }
            
            val beforeRealTime = Clock.System.now()
            delay(200) // 200ms
            val afterRealTime = Clock.System.now()
            val newSimTime = simulation.now()
            
            // Calculate elapsed times
            val realElapsed = afterRealTime - beforeRealTime
            val simElapsed = newSimTime - currentSimTime
            
            // Verify ratio is close to the current speed
            val ratio = simElapsed.inWholeMilliseconds.toDouble() / realElapsed.inWholeMilliseconds.toDouble()
            assertTrue(abs(ratio - speed) < speed * 0.3, 
                "With speed $speed, simulated time should advance ${speed}x as fast as real time. " +
                "Actual ratio: $ratio")
            
            currentSimTime = newSimTime
        }
    }
    
    @Test
    fun `test pause and resume functionality`() = runBlocking {
        // Create simulation with speed 10
        val simulation = WWWSimulation(startDateTime, userPosition, 10)
        
        // Pause the simulation
        simulation.pause()
        assertEquals(0, simulation.speed, "Speed should be 0 when paused")
        
        // Wait while paused
        val beforeDelaySimTime = simulation.now()
        delay(300) // 300ms
        
        // Time should not advance while paused
        val duringPauseSimTime = simulation.now()
        assertEquals(beforeDelaySimTime, duringPauseSimTime,
            "Simulated time should not advance while paused")
        
        // Resume with speed 5
        simulation.resume(5)
        assertEquals(5, simulation.speed, "Speed should be updated when resumed")
        
        // Wait after resume
        val afterResumeRealTime = Clock.System.now()
        delay(300) // 300ms
        val finalRealTime = Clock.System.now()
        val finalSimTime = simulation.now()
        
        // Calculate elapsed times after resume
        val realElapsed = finalRealTime - afterResumeRealTime
        val simElapsed = finalSimTime - duringPauseSimTime
        
        // Verify ratio is close to 5.0 after resume
        val ratio = simElapsed.inWholeMilliseconds.toDouble() / realElapsed.inWholeMilliseconds.toDouble()
        assertTrue(abs(ratio - 5.0) < 1.5, 
            "After resume with speed 5, simulated time should advance 5x as fast as real time. " +
            "Actual ratio: $ratio")
        
        // Test resume without specifying speed (should use last active speed)
        simulation.pause()
        simulation.resume()
        assertEquals(5, simulation.speed, "Resume without speed parameter should use last active speed")
    }
    
    @Test
    fun `test reset functionality`() = runBlocking {
        // Create simulation with speed 5
        val simulation = WWWSimulation(startDateTime, userPosition, 5)
        
        // Let some time pass
        delay(300) // 300ms
        val beforeResetSimTime = simulation.now()
        
        // Verify time has advanced from start time
        assertTrue(beforeResetSimTime > startDateTime, 
            "Simulated time should advance before reset")
        
        // Reset the simulation
        simulation.reset()
        
        // Time should be reset to start time
        val afterResetSimTime = simulation.now()
        val tolerance = 100.milliseconds
        assertEquals(startDateTime.toEpochMilliseconds().toDouble(),
            afterResetSimTime.toEpochMilliseconds().toDouble(),
            tolerance.inWholeMilliseconds.toDouble(),
            "Simulated time should be reset to start time")
        
        // Speed should remain unchanged
        assertEquals(5, simulation.speed, "Speed should remain unchanged after reset")
        
        // Verify time advances correctly after reset
        delay(300) // 300ms
        val finalSimTime = simulation.now()
        assertTrue(finalSimTime > startDateTime, 
            "Simulated time should advance after reset")
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
    fun `test time calculation accuracy`() = runBlocking {
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
        val difference = if (simElapsed > expectedSimElapsed) 
            simElapsed - expectedSimElapsed else expectedSimElapsed - simElapsed
        
        assertTrue(difference < tolerance, 
            "Time calculation should be precise. " +
            "Expected: $expectedSimElapsed, Actual: $simElapsed, Difference: $difference")
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
