package com.worldwidewaves.shared

import com.worldwidewaves.shared.events.utils.Position
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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

@OptIn(ExperimentalTime::class)
class WWWSimulation(
    private val startDateTime: Instant,
    private val userPosition: Position,
    private var initialSpeed: Int = 1
) {
    companion object {
        const val MIN_SPEED = 1
        const val MAX_SPEED = 500
    }

    private var _speed: Int = validateSpeed(initialSpeed)
    val speed: Int get() = _speed

    // Track time checkpoints for accurate time calculation
    private data class TimeCheckpoint(
        val realTime: Instant,
        val simulatedTime: Instant
    )

    private var lastCheckpoint: TimeCheckpoint

    init {
        // Initialize with the first checkpoint at creation time
        lastCheckpoint = TimeCheckpoint(
            realTime = Clock.System.now(),
            simulatedTime = startDateTime
        )
    }

    /**
     * Change the simulation speed.
     * This creates a new time checkpoint to ensure accurate time calculations.
     * @param newSpeed The new simulation speed (1-500)
     * @return The updated speed value
     */
    fun setSpeed(newSpeed: Int): Int {
        // Calculate simulated time at this moment before changing speed
        val currentSimulatedTime = calculateCurrentTime()

        // Update speed
        _speed = validateSpeed(newSpeed)

        // Create a new checkpoint with current values
        lastCheckpoint = TimeCheckpoint(
            realTime = Clock.System.now(),
            simulatedTime = currentSimulatedTime
        )

        initialSpeed = _speed
        return _speed
    }

    /**
     * Get the current simulated time.
     * @return The current instant in the simulation timeline
     */
    fun now(): Instant {
        return calculateCurrentTime()
    }

    /**
     * Get the user's position.
     * @return The user's position
     */
    fun getUserPosition() = userPosition

    /**
     * Calculate the current simulated time based on elapsed real time
     * and the current simulation speed.
     */
    private fun calculateCurrentTime(): Instant {
        // Calculate real time elapsed since the last checkpoint
        val elapsedRealTimeSinceCheckpoint = Clock.System.now() - lastCheckpoint.realTime

        // Apply current speed factor to the elapsed time
        val simulatedElapsedTime = elapsedRealTimeSinceCheckpoint * _speed

        // Add the simulated elapsed time to the last checkpoint's simulated time
        return lastCheckpoint.simulatedTime + simulatedElapsedTime
    }

    /**
     * Validate that the speed is within acceptable bounds.
     * @param speed The speed to validate
     * @return The validated speed
     * @throws IllegalArgumentException if the speed is outside acceptable bounds
     */
    private fun validateSpeed(speed: Int): Int {
        return speed.also {
            require(it in MIN_SPEED..MAX_SPEED) {
                "Speed must be between $MIN_SPEED and $MAX_SPEED"
            }
        }
    }

    /**
     * Reset the simulation to start from the current moment.
     * Useful if you want to "pause" the simulation and restart it.
     */
    fun reset() {
        lastCheckpoint = TimeCheckpoint(
            realTime = Clock.System.now(),
            simulatedTime = startDateTime
        )
    }

    /**
     * Pause the simulation by creating a checkpoint at the current time.
     * To resume, call resume().
     */
    fun pause() {
        lastCheckpoint = TimeCheckpoint(
            realTime = Clock.System.now(),
            simulatedTime = calculateCurrentTime()
        )
        _speed = 0 // Set speed to 0 to effectively pause
    }

    /**
     * Resume the simulation with the specified speed.
     * @param resumeSpeed The speed to resume at (defaults to last active speed)
     */
    fun resume(resumeSpeed: Int = initialSpeed.takeIf { it > 0 } ?: 1) {
        // Only update the real time in the checkpoint, keep simulated time as is
        lastCheckpoint = TimeCheckpoint(
            realTime = Clock.System.now(),
            simulatedTime = lastCheckpoint.simulatedTime
        )
        _speed = validateSpeed(resumeSpeed)
        initialSpeed = _speed
    }

}