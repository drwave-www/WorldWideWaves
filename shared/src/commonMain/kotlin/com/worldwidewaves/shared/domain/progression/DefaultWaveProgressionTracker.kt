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
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.events.utils.Position
import kotlin.math.roundToInt

/**
 * Default implementation of WaveProgressionTracker.
 *
 * This implementation:
 * - Calculates progression based on elapsed time vs total event duration
 * - Uses area polygon containment for position validation
 * - Maintains a circular buffer of progression snapshots
 * - Handles edge cases and provides error tolerance
 */
class DefaultWaveProgressionTracker(
    private val clock: IClock
) : WaveProgressionTracker {

    private val progressionHistory = mutableListOf<ProgressionSnapshot>()
    private val maxHistorySize = 100

    override suspend fun calculateProgression(event: IWWWEvent): Double {
        return try {
            when {
                event.isDone() -> 100.0
                !event.isRunning() -> 0.0
                else -> {
                    val elapsedTime = clock.now().epochSeconds - event.getWaveStartDateTime().epochSeconds
                    val totalTime = event.wave.getWaveDuration().inWholeSeconds

                    if (totalTime <= 0) {
                        Log.w("WaveProgressionTracker", "Invalid total time: $totalTime for event ${event.id}")
                        return 0.0
                    }

                    val progression = (elapsedTime.toDouble() / totalTime * 100).coerceIn(0.0, 100.0)

                    Log.v("WaveProgressionTracker",
                        "Calculated progression: $progression% for event ${event.id} " +
                        "(elapsed: ${elapsedTime}s, total: ${totalTime}s)"
                    )

                    progression
                }
            }
        } catch (e: Exception) {
            Log.e("WaveProgressionTracker", "Error calculating progression for event ${event.id}: $e")
            0.0
        }
    }

    override suspend fun isUserInWaveArea(userPosition: Position, waveArea: WWWEventArea): Boolean {
        return try {
            // Check if polygon data is available first
            val polygons = waveArea.getPolygons()

            if (polygons.isEmpty()) {
                Log.v("WaveProgressionTracker", "No polygons available for area detection")
                return false
            }

            // Use the area's optimized position checking
            val isInArea = waveArea.isPositionWithin(userPosition)

            Log.v("WaveProgressionTracker",
                "Position ${userPosition.lat}, ${userPosition.lng} " +
                "${if (isInArea) "is" else "is not"} within wave area"
            )

            isInArea
        } catch (e: Exception) {
            Log.e("WaveProgressionTracker", "Error checking position in wave area: $e")
            // On error, assume user is not in area for safety
            false
        }
    }

    override fun getProgressionHistory(): List<ProgressionSnapshot> {
        return progressionHistory.toList() // Return defensive copy
    }

    override suspend fun recordProgressionSnapshot(event: IWWWEvent, userPosition: Position?) {
        try {
            val progression = calculateProgression(event)
            val isInArea = userPosition?.let {
                isUserInWaveArea(it, event.area)
            } ?: false

            val snapshot = ProgressionSnapshot(
                timestamp = clock.now(),
                progression = progression,
                userPosition = userPosition,
                isInWaveArea = isInArea
            )

            // Add to history with circular buffer behavior
            progressionHistory.add(snapshot)
            if (progressionHistory.size > maxHistorySize) {
                progressionHistory.removeAt(0)
            }

            Log.v("WaveProgressionTracker",
                "Recorded progression snapshot: ${progression}% at ${snapshot.timestamp}"
            )
        } catch (e: Exception) {
            Log.e("WaveProgressionTracker", "Error recording progression snapshot: $e")
        }
    }

    override fun clearProgressionHistory() {
        progressionHistory.clear()
        Log.v("WaveProgressionTracker", "Cleared progression history")
    }
}