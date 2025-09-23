package com.worldwidewaves.shared.position

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

import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.events.utils.Position
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Centralized position management system that provides a single source of truth for user position.
 *
 * Features:
 * - Unified position state management
 * - Source priority and conflict resolution
 * - Debouncing to prevent excessive updates
 * - Position deduplication using epsilon comparison
 * - Thread-safe reactive position updates
 */
class PositionManager(
    private val coroutineScopeProvider: CoroutineScopeProvider,
    private val debounceDelay: Duration = 100.milliseconds,
    private val positionEpsilon: Double = 0.0001 // ~10 meters
) {

    /**
     * Position source types with implicit priority ordering (lower ordinal = higher priority)
     */
    enum class PositionSource {
        SIMULATION,     // Highest priority - debug/testing
        GPS            // Standard priority - real device location
    }

    /**
     * Internal position state with source tracking
     */
    private data class PositionState(
        val position: Position?,
        val source: PositionSource?,
        val timestamp: Long = System.currentTimeMillis()
    )

    // Internal state management
    private val _currentState = MutableStateFlow(PositionState(null, null))
    private var debounceJob: Job? = null
    private var pendingUpdate: PositionState? = null

    // Public reactive API
    private val _position = MutableStateFlow<Position?>(null)
    val position: StateFlow<Position?> = _position.asStateFlow()

    /**
     * Updates position from a specific source with conflict resolution and debouncing.
     *
     * @param source The source of the position update
     * @param newPosition The new position (null to clear)
     */
    fun updatePosition(source: PositionSource, newPosition: Position?) {
        Log.v("PositionManager", "Position update from $source: $newPosition")

        val newState = PositionState(newPosition, if (newPosition == null) null else source)

        // Check if this update should be applied based on source priority
        val currentState = _currentState.value
        if (!shouldAcceptUpdate(currentState, newState)) {
            Log.v("PositionManager", "Rejected position update from $source (lower priority than ${currentState.source})")
            return
        }

        // Check for position deduplication
        if (isPositionDuplicate(currentState.position, newPosition)) {
            Log.v("PositionManager", "Skipped duplicate position update")
            return
        }

        // Store pending update and start/restart debounce
        pendingUpdate = newState

        debounceJob?.cancel()
        debounceJob = coroutineScopeProvider.launchDefault {
            delay(debounceDelay)

            // Apply the debounced update
            pendingUpdate?.let { finalState ->
                _currentState.value = finalState
                _position.value = finalState.position
                Log.v("PositionManager", "Applied debounced position: ${finalState.position} from ${finalState.source}")
            }
            pendingUpdate = null
        }
    }

    /**
     * Gets the current position synchronously
     */
    fun getCurrentPosition(): Position? = _position.value

    /**
     * Gets the current position source
     */
    fun getCurrentSource(): PositionSource? = _currentState.value.source

    /**
     * Clears position from a specific source
     */
    fun clearPosition(source: PositionSource) {
        updatePosition(source, null)
    }

    /**
     * Clears all position data
     */
    fun clearAll() {
        debounceJob?.cancel()
        pendingUpdate = null
        _currentState.value = PositionState(null, null)
        _position.value = null
        Log.v("PositionManager", "Cleared all position data")
    }

    /**
     * Determines if a new position update should be accepted based on source priority
     */
    private fun shouldAcceptUpdate(current: PositionState, new: PositionState): Boolean {
        // Always accept if no current position
        if (current.position == null) return true

        // Always accept clearing operations (null position)
        if (new.position == null) return true

        // Always accept if new source has higher priority (lower ordinal)
        return when {
            current.source == null -> true
            new.source == null -> false
            new.source.ordinal <= current.source.ordinal -> true
            else -> false
        }
    }

    /**
     * Checks if a position is effectively duplicate using epsilon comparison
     */
    private fun isPositionDuplicate(current: Position?, new: Position?): Boolean {
        if (current == null || new == null) return false

        val latDiff = abs(current.lat - new.lat)
        val lngDiff = abs(current.lng - new.lng)

        return latDiff < positionEpsilon && lngDiff < positionEpsilon
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        debounceJob?.cancel()
        debounceJob = null
        pendingUpdate = null
        Log.v("PositionManager", "Cleaned up resources")
    }
}