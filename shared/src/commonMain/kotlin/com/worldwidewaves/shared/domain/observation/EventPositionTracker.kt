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

import com.worldwidewaves.shared.domain.progression.WaveProgressionTracker
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.position.PositionManager
import com.worldwidewaves.shared.utils.Log
import kotlin.coroutines.cancellation.CancellationException

/**
 * Position tracking and area detection for events.
 *
 * This class is responsible for:
 * - Detecting if the user is within the event area
 * - Handling polygon data loading
 * - Coordinating with PositionManager for current position
 * - Providing immediate area detection updates
 *
 * ## Area Detection Strategy
 * The class provides immediate area detection alongside the PositionObserver flow:
 * 1. Checks if polygon data is available first
 * 2. Calls WaveProgressionTracker for geometric containment check
 * 3. Handles errors gracefully (assumes not in area on error)
 * 4. Updates state immediately for UI responsiveness
 *
 * ## Performance Considerations
 * - Only performs area checks when polygon data is loaded
 * - Caches position data from PositionManager
 * - Minimal CPU usage when user is not near event
 *
 * ## Thread Safety
 * All methods are thread-safe and can be called from any coroutine context.
 *
 * ## iOS Safety
 * This class follows iOS safety patterns:
 * - No KoinComponent dependency (dependencies injected)
 * - No coroutine launches in initialization
 * - Stateless operations (no mutable state)
 */
class EventPositionTracker(
    private val positionManager: PositionManager,
    private val waveProgressionTracker: WaveProgressionTracker,
) {
    /**
     * Updates area detection state for the given event.
     *
     * This method:
     * 1. Gets current user position from PositionManager
     * 2. Checks if event polygon data is available
     * 3. Calls WaveProgressionTracker to check if user is in wave area
     * 4. Returns the area detection result
     *
     * @param event The event to check area detection for
     * @return True if user is in event area, false otherwise (including errors)
     */
    suspend fun isUserInArea(event: IWWWEvent): Boolean {
        val userPosition = positionManager.getCurrentPosition() ?: return false

        return try {
            // Check if polygon data is available first
            val polygons = event.area.getPolygons()

            if (polygons.isNotEmpty()) {
                // Now call the actual area detection with pre-fetched polygons (performance optimization)
                waveProgressionTracker.isUserInWaveArea(userPosition, event.area, polygons)
            } else {
                false
            }
        } catch (e: IllegalStateException) {
            Log.e("EventPositionTracker", "isUserInArea: State exception $e, eventId=${event.id}")
            false // On error, assume user is not in area for safety
        } catch (e: CancellationException) {
            throw e // Re-throw cancellation
        } catch (e: Exception) {
            Log.e("EventPositionTracker", "isUserInArea: Unexpected exception $e, eventId=${event.id}")
            false // On error, assume user is not in area for safety
        }
    }

    /**
     * Gets the current user position from PositionManager.
     *
     * @return The current position, or null if not available
     */
    fun getCurrentPosition() = positionManager.getCurrentPosition()
}
