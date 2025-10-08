@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.worldwidewaves.shared.domain.progression

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
import com.worldwidewaves.shared.events.WWWEventArea
import com.worldwidewaves.shared.events.utils.Position
import kotlin.time.Instant

/**
 * Data class representing a snapshot of wave progression at a specific time.
 */
data class ProgressionSnapshot(
    val timestamp: Instant,
    val progression: Double,
    val userPosition: Position?,
    val isInWaveArea: Boolean,
)

/**
 * Interface for tracking wave progression and user position relative to wave events.
 *
 * This component is responsible for:
 * - Calculating wave progression based on time and event state
 * - Determining if user is within wave area boundaries
 * - Maintaining progression history for analysis
 * - Providing optimized calculations for area containment
 */
interface WaveProgressionTracker {
    /**
     * Calculates the current wave progression as a percentage (0.0 to 100.0).
     *
     * @param event The wave event to calculate progression for
     * @return Progression percentage where:
     *         - 0.0 = event not started
     *         - 0.0-100.0 = running event progression
     *         - 100.0 = event completed
     */
    suspend fun calculateProgression(event: IWWWEvent): Double

    /**
     * Determines if the user's current position is within the wave area.
     *
     * @param userPosition The user's current position
     * @param waveArea The wave area to check against
     * @return true if user is within the wave area, false otherwise
     */
    suspend fun isUserInWaveArea(
        userPosition: Position,
        waveArea: WWWEventArea,
    ): Boolean

    /**
     * Gets the history of progression snapshots for analysis.
     * Limited to prevent memory issues.
     *
     * @return List of recent progression snapshots (max 100 entries)
     */
    fun getProgressionHistory(): List<ProgressionSnapshot>

    /**
     * Records a progression snapshot for history tracking.
     *
     * @param event The event being tracked
     * @param userPosition The user's position at time of snapshot
     */
    suspend fun recordProgressionSnapshot(
        event: IWWWEvent,
        userPosition: Position?,
    )

    /**
     * Clears progression history to free memory.
     */
    suspend fun clearProgressionHistory()
}
