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

import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.domain.progression.WaveProgressionTracker
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.position.PositionManager
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Default implementation of PositionObserver.
 *
 * This implementation:
 * - Combines position changes with polygon loading events
 * - Optimizes position updates with debouncing and validation
 * - Calculates area containment using the WaveProgressionTracker
 * - Provides distance calculations using Haversine formula
 * - Handles edge cases and provides robust error tolerance
 */
class DefaultPositionObserver(
    private val positionManager: PositionManager,
    private val waveProgressionTracker: WaveProgressionTracker,
    private val clock: IClock,
) : PositionObserver {
    private var observationJob: Job? = null
    private var isCurrentlyObserving = false

    companion object {
        // Earth's radius in meters for distance calculations
        private const val EARTH_RADIUS_METERS = 6371000.0

        // Minimum significant position change (meters) to avoid GPS noise
        private const val MIN_POSITION_CHANGE_METERS = 1.0
    }

    override fun observePositionForEvent(event: IWWWEvent): Flow<PositionObservation> {
        Log.v("DefaultPositionObserver", "Starting position observation for event ${event.id}")
        isCurrentlyObserving = true

        return combine(
            positionManager.position
                .onEach { position ->
                    Log.performance("DefaultPositionObserver", "Position update: $position for event ${event.id}")
                },
            event.area.polygonsLoaded
                .onEach { loaded ->
                    Log.v("DefaultPositionObserver", "Polygons loaded state: $loaded for event ${event.id}")
                },
        ) { position, polygonsLoaded ->

            val isInArea =
                if (position != null && polygonsLoaded) {
                    try {
                        waveProgressionTracker.isUserInWaveArea(position, event.area)
                    } catch (e: Exception) {
                        Log.e("DefaultPositionObserver", "Error checking if user in area: $e")
                        false
                    }
                } else {
                    false
                }

            PositionObservation(
                position = position,
                isInArea = isInArea,
                timestamp = clock.now(),
            )
        }.distinctUntilChanged { old, new ->
            // Only emit if position changed significantly or area status changed
            if (old.isInArea != new.isInArea) {
                false // Always emit if area status changed
            } else if (old.position == null || new.position == null) {
                old.position == new.position // Only emit if position availability changed
            } else {
                calculateDistance(old.position!!, new.position!!) < MIN_POSITION_CHANGE_METERS
            }
        }.flowOn(Dispatchers.Default)
    }

    override fun getCurrentPosition(): Position? = positionManager.getCurrentPosition()

    override fun isValidPosition(position: Position): Boolean =
        position.lat in WWWGlobals.Geodetic.MIN_LATITUDE..WWWGlobals.Geodetic.MAX_LATITUDE &&
            position.lng in WWWGlobals.Geodetic.MIN_LONGITUDE..WWWGlobals.Geodetic.MAX_LONGITUDE &&
            !position.lat.isNaN() &&
            !position.lng.isNaN() &&
            position.lat.isFinite() &&
            position.lng.isFinite()

    override fun calculateDistance(
        from: Position,
        to: Position,
    ): Double {
        // Validate positions
        val arePositionsValid = isValidPosition(from) && isValidPosition(to)
        if (!arePositionsValid) {
            Log.w("DefaultPositionObserver", "Invalid positions for distance calculation: $from, $to")
            return Double.POSITIVE_INFINITY
        }

        // Handle same position case
        val isSamePosition = from.lat == to.lat && from.lng == to.lng
        if (isSamePosition) {
            return 0.0
        }

        // Haversine formula for calculating great-circle distance
        val lat1Rad = from.lat * PI / 180.0
        val lat2Rad = to.lat * PI / 180.0
        val deltaLatRad = (to.lat - from.lat) * PI / 180.0
        val deltaLngRad = (to.lng - from.lng) * PI / 180.0

        val a =
            sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLngRad / 2) * sin(deltaLngRad / 2)

        // Ensure a is within valid range [0, 1] to avoid NaN from acos
        val clampedA = a.coerceIn(0.0, 1.0)
        val c = 2 * acos(sqrt(clampedA))

        val distance = EARTH_RADIUS_METERS * c
        return distance
    }

    override fun stopObservation() {
        Log.v("DefaultPositionObserver", "Stopping position observation")
        observationJob?.cancel()
        observationJob = null
        isCurrentlyObserving = false
    }

    override fun isObserving(): Boolean = isCurrentlyObserving && observationJob?.isActive == true
}
