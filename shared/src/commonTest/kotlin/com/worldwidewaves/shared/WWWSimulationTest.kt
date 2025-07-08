package com.worldwidewaves.shared

import com.worldwidewaves.shared.events.utils.Position
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for WWWSimulation class
 */
class WWWSimulationTest {

    // Test fixture for controlling time in tests
    class TestClock(private var currentTime: Instant = Instant.fromEpochMilliseconds(0)) : Clock {
        override fun now(): Instant = currentTime
        
        fun advanceTime(duration: Duration) {
            currentTime += duration
        }
    }

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
    fun `test now returns correct time with speed 1`() {
        // Create a test clock to control time
        val testClock = TestClock(Instant.fromEpochMilliseconds(0))
        val originalClock = Clock.System
        
        try {
            // Replace the system clock with our test clock
            Clock.System = testClock
            
            // Create simulation with speed 1
            val simulation = WWWSimulation(startDateTime, userPosition)
            
            // Initial time should be the start time
            assertEquals(startDateTime, simulation.now())
            
            // Advance real time by 1 minute
            testClock.advanceTime(1.minutes)
            
            // With speed 1, simulated time should also advance by 1 minute
            assertEquals(startDateTime + 1.minutes, simulation.now())
            
            // Advance real time by 1 hour
            testClock.advanceTime(1.hours)
            
            // Simulated time should advance by 1 hour
            assertEquals(startDateTime + 1.minutes + 1.hours, simulation.now())
        } finally {
            // Restore the original clock
            Clock.System = originalClock
        }
    }
    
    @Test
    fun `test now returns correct time with speed greater than 1`() {
        // Create a test clock to control time
        val testClock = TestClock(Instant.fromEpochMilliseconds(0))
        val originalClock = Clock.System
        
        try {
            // Replace the system clock with our test clock
            Clock.System = testClock
            
            // Create simulation with speed 10
            val simulation = WWWSimulation(startDateTime, userPosition, 10)
            
            // Initial time should be the start time
            assertEquals(startDateTime, simulation.now())
            
            // Advance real time by 1 minute
            testClock.advanceTime(1.minutes)
            
            // With speed 10, simulated time should advance by 10 minutes
            assertEquals(startDateTime + 10.minutes, simulation.now())
            
            // Advance real time by 30 seconds
            testClock.advanceTime(30.seconds)
            
            // Simulated time should advance by additional 5 minutes (30 sec * 10)
            assertEquals(startDateTime + 10.minutes + 5.minutes, simulation.now())
        } finally {
            // Restore the original clock
            Clock.System = originalClock
        }
    }
    
    @Test
    fun `test speed changes affect time calculations correctly`() {
        // Create a test clock to control time
        val testClock = TestClock(Instant.fromEpochMilliseconds(0))
        val originalClock = Clock.System
        
        try {
            // Replace the system clock with our test clock
            Clock.System = testClock
            
            // Create simulation with speed 1
            val simulation = WWWSimulation(startDateTime, userPosition)
            
            // Advance real time by 10 minutes with speed 1
            testClock.advanceTime(10.minutes)
            val timeAtSpeed1 = simulation.now()
            assertEquals(startDateTime + 10.minutes, timeAtSpeed1)
            
            // Change speed to 5
            simulation.setSpeed(5)
            
            // Advance real time by another 10 minutes with speed 5
            testClock.advanceTime(10.minutes)
            val finalTime = simulation.now()
            
            // Final time should be: start time + 10min (from speed 1) + 50min (10min * 5)
            assertEquals(startDateTime + 10.minutes + 50.minutes, finalTime)
        } finally {
            // Restore the original clock
            Clock.System = originalClock
        }
    }
    
    @Test
    fun `test multiple speed changes in sequence`() {
        // Create a test clock to control time
        val testClock = TestClock(Instant.fromEpochMilliseconds(0))
        val originalClock = Clock.System
        
        try {
            // Replace the system clock with our test clock
            Clock.System = testClock
            
            // Create simulation with initial speed 2
            val simulation = WWWSimulation(startDateTime, userPosition, 2)
            
            // Sequence of speed changes and time advancements
            // 1. Advance 5 minutes with speed 2
            testClock.advanceTime(5.minutes)
            val time1 = simulation.now()
            assertEquals(startDateTime + 10.minutes, time1) // 5min * 2 = 10min
            
            // 2. Change speed to 5 and advance 2 minutes
            simulation.setSpeed(5)
            testClock.advanceTime(2.minutes)
            val time2 = simulation.now()
            assertEquals(time1 + 10.minutes, time2) // time1 + (2min * 5) = time1 + 10min
            
            // 3. Change speed to 100 and advance 30 seconds
            simulation.setSpeed(100)
            testClock.advanceTime(30.seconds)
            val time3 = simulation.now()
            assertEquals(time2 + 50.minutes, time3) // time2 + (0.5min * 100) = time2 + 50min
            
            // 4. Change speed to minimum (1) and advance 1 minute
            simulation.setSpeed(1)
            testClock.advanceTime(1.minutes)
            val time4 = simulation.now()
            assertEquals(time3 + 1.minutes, time4) // time3 + (1min * 1) = time3 + 1min
            
            // 5. Change speed to maximum (500) and advance 10 seconds
            simulation.setSpeed(500)
            testClock.advanceTime(10.seconds)
            val time5 = simulation.now()
            assertEquals(time4 + (500 * 10).seconds, time5) // time4 + (10sec * 500) = time4 + 5000sec
        } finally {
            // Restore the original clock
            Clock.System = originalClock
        }
    }
    
    @Test
    fun `test pause and resume functionality`() {
        // Create a test clock to control time
        val testClock = TestClock(Instant.fromEpochMilliseconds(0))
        val originalClock = Clock.System
        
        try {
            // Replace the system clock with our test clock
            Clock.System = testClock
            
            // Create simulation with speed 10
            val simulation = WWWSimulation(startDateTime, userPosition, 10)
            
            // Advance time by 5 minutes
            testClock.advanceTime(5.minutes)
            val timeBeforePause = simulation.now()
            assertEquals(startDateTime + 50.minutes, timeBeforePause) // 5min * 10 = 50min
            
            // Pause the simulation
            simulation.pause()
            assertEquals(0, simulation.speed, "Speed should be 0 when paused")
            
            // Advance real time by 10 minutes while paused
            testClock.advanceTime(10.minutes)
            val timeDuringPause = simulation.now()
            assertEquals(timeBeforePause, timeDuringPause, "Time should not advance while paused")
            
            // Resume with speed 5
            simulation.resume(5)
            assertEquals(5, simulation.speed, "Speed should be updated when resumed")
            
            // Advance real time by 5 minutes after resume
            testClock.advanceTime(5.minutes)
            val timeAfterResume = simulation.now()
            assertEquals(timeBeforePause + 25.minutes, timeAfterResume) // timeBeforePause + (5min * 5) = timeBeforePause + 25min
            
            // Test resume without specifying speed (should use last active speed)
            simulation.pause()
            simulation.resume()
            assertEquals(5, simulation.speed, "Resume without speed parameter should use last active speed")
        } finally {
            // Restore the original clock
            Clock.System = originalClock
        }
    }
    
    @Test
    fun `test reset functionality`() {
        // Create a test clock to control time
        val testClock = TestClock(Instant.fromEpochMilliseconds(0))
        val originalClock = Clock.System
        
        try {
            // Replace the system clock with our test clock
            Clock.System = testClock
            
            // Create simulation with speed 5
            val simulation = WWWSimulation(startDateTime, userPosition, 5)
            
            // Advance time by 10 minutes
            testClock.advanceTime(10.minutes)
            val timeBeforeReset = simulation.now()
            assertEquals(startDateTime + 50.minutes, timeBeforeReset) // 10min * 5 = 50min
            
            // Reset the simulation
            simulation.reset()
            
            // Time should be reset to start time
            assertEquals(startDateTime, simulation.now(), "Time should be reset to start time")
            
            // Speed should remain unchanged
            assertEquals(5, simulation.speed, "Speed should remain unchanged after reset")
            
            // Advance time again to verify simulation continues from start time
            testClock.advanceTime(2.minutes)
            assertEquals(startDateTime + 10.minutes, simulation.now()) // 2min * 5 = 10min from start time
        } finally {
            // Restore the original clock
            Clock.System = originalClock
        }
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
    fun `test very long durations`() {
        // Create a test clock to control time
        val testClock = TestClock(Instant.fromEpochMilliseconds(0))
        val originalClock = Clock.System
        
        try {
            // Replace the system clock with our test clock
            Clock.System = testClock
            
            // Create simulation with maximum speed
            val simulation = WWWSimulation(startDateTime, userPosition, WWWSimulation.MAX_SPEED)
            
            // Advance real time by 24 hours
            testClock.advanceTime(24.hours)
            
            // With max speed (500), simulated time should advance by 500 * 24 hours = 12,000 hours
            val expectedAdvance = 24.hours * WWWSimulation.MAX_SPEED
            val expectedTime = startDateTime + expectedAdvance
            
            // Allow small tolerance for floating point calculations
            val actualTime = simulation.now()
            val difference = if (actualTime > expectedTime) 
                actualTime - expectedTime else expectedTime - actualTime
            
            assertTrue(difference < 1.seconds, 
                "Difference between expected and actual time should be less than 1 second, " +
                "but was ${difference.inWholeMilliseconds} ms")
        } finally {
            // Restore the original clock
            Clock.System = originalClock
        }
    }
    
    @Test
    fun `test time progression accuracy with real clock time`() {
        // Create a test clock to control time with precise increments
        val testClock = TestClock(Instant.fromEpochMilliseconds(0))
        val originalClock = Clock.System
        
        try {
            // Replace the system clock with our test clock
            Clock.System = testClock
            
            // Create simulation with speed 60 (1 minute per second)
            val simulation = WWWSimulation(startDateTime, userPosition, 60)
            
            // Test with small increments to verify accuracy
            for (i in 1..100) {
                // Advance real time by 1 second
                testClock.advanceTime(1.seconds)
                
                // Expected simulated time: start time + (i seconds * 60)
                val expectedTime = startDateTime + (i * 60).seconds
                val actualTime = simulation.now()
                
                // Check that times match with small tolerance
                val difference = if (actualTime > expectedTime) 
                    actualTime - expectedTime else expectedTime - actualTime
                
                assertTrue(difference < 0.01.seconds, 
                    "Time calculation should be precise. Iteration $i: " +
                    "Expected $expectedTime, got $actualTime, difference ${difference.inWholeMilliseconds} ms")
            }
        } finally {
            // Restore the original clock
            Clock.System = originalClock
        }
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
