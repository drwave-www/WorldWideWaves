@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.worldwidewaves.shared.domain.observation

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

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.Position
import kotlinx.coroutines.flow.Flow

/**
 * Data class representing a position observation event.
 */
data class PositionObservation(
    val position: Position?,
    val isInArea: Boolean,
    val timestamp: kotlinx.datetime.Instant
)

/**
 * Interface for observing user position changes and their relationship to wave events.
 *
 * This component is responsible for:
 * - Monitoring user position changes in real-time
 * - Detecting when user enters/exits wave areas
 * - Providing optimized position update streams
 * - Coordinating with polygon loading events
 * - Validating position data integrity
 */
interface PositionObserver {

    /**
     * Starts observing position changes for a specific event.
     *
     * @param event The event to observe position changes for
     * @return Flow of position observations
     */
    fun observePositionForEvent(event: IWWWEvent): Flow<PositionObservation>

    /**
     * Gets the current position synchronously.
     *
     * @return Current position or null if unavailable
     */
    fun getCurrentPosition(): Position?

    /**
     * Validates if a position is reasonable and within expected bounds.
     *
     * @param position The position to validate
     * @return true if position is valid, false otherwise
     */
    fun isValidPosition(position: Position): Boolean

    /**
     * Calculates the distance between two positions in meters.
     *
     * @param from Starting position
     * @param to Ending position
     * @return Distance in meters
     */
    fun calculateDistance(from: Position, to: Position): Double

    /**
     * Stops position observation and cleans up resources.
     */
    fun stopObservation()

    /**
     * Checks if position observation is currently active.
     *
     * @return true if observing, false otherwise
     */
    fun isObserving(): Boolean
}