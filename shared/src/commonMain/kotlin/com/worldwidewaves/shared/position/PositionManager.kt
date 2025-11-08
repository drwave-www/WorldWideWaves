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

import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.SystemClock
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
@OptIn(ExperimentalTime::class)
class PositionManager(
    private val coroutineScopeProvider: CoroutineScopeProvider,
    private val debounceDelay: Duration = 100.milliseconds,
    private val positionEpsilon: Double = 0.0001, // ~10 meters
    clock: IClock? = null, // Injectable for testing, simulation-aware
) {
    private val clock: IClock = clock ?: SystemClock()

    private companion object {
        private const val TAG = "WWW.Position.Manager"

        /**
         * Default staleness threshold for GPS positions.
         * After this duration without updates, GPS position is considered stale.
         */
        val DEFAULT_GPS_STALENESS_THRESHOLD = 5.minutes
    }

    /**
     * Position source types with implicit priority ordering (lower ordinal = higher priority)
     */
    enum class PositionSource {
        SIMULATION, // Highest priority - debug/testing
        GPS, // Standard priority - real device location
    }

    /**
     * Internal position state with source tracking
     */
    private data class PositionState(
        val position: Position?,
        val source: PositionSource?,
    )

    // Internal state management
    private val _currentState = MutableStateFlow(PositionState(null, null))
    private var debounceJob: Job? = null
    private var pendingUpdate: PositionState? = null

    // GPS position storage - always keeps latest GPS position regardless of simulation priority
    private var lastGPSPosition: Position? = null
    private var pendingGPSPosition: Position? = null
    private var lastGPSUpdateTime: Instant? = null

    // Public reactive API with performance optimization
    private val _position = MutableStateFlow<Position?>(null)
    val position: StateFlow<Position?> = _position.asStateFlow()

    /**
     * Updates position from a specific source with conflict resolution and debouncing.
     *
     * @param source The source of the position update
     * @param newPosition The new position (null to clear)
     *
     * Note: For production with log sampling (to avoid overwhelming logs), consider using:
     * ```kotlin
     * // Log only 1% of position updates (1 out of 100)
     * Log.vSampled(TAG, "Position update from $source: $newPosition", sampleRate = 100)
     * ```
     * This is useful when ENABLE_POSITION_TRACKING_LOGGING is true in production.
     */
    fun updatePosition(
        source: PositionSource,
        newPosition: Position?,
    ) {
        if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
            Log.v(TAG, "[DEBUG] Position update from $source: $newPosition")
        }

        // Always store GPS position separately, regardless of priority system
        // This ensures GPS position is available even when simulation is active
        if (source == PositionSource.GPS) {
            pendingGPSPosition = newPosition
            // Immediately commit GPS position (no debouncing for GPS storage)
            lastGPSPosition = newPosition
            // Update timestamp when GPS position is received (or cleared)
            lastGPSUpdateTime = if (newPosition != null) clock.now() else null
            if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
                Log.v(TAG, "[DEBUG] Stored GPS position: $newPosition (timestamp=$lastGPSUpdateTime)")
            }
        }

        val newState = PositionState(newPosition, if (newPosition == null) null else source)

        // Check if this update should be applied based on source priority
        val currentState = _currentState.value
        if (!shouldAcceptUpdate(currentState, newState)) {
            if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
                Log.v(TAG, "[DEBUG] Rejected position update from $source (lower priority than ${currentState.source})")
            }
            return
        }

        // Check for position deduplication
        if (isPositionDuplicate(currentState.position, newPosition)) {
            if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
                Log.v(TAG, "[DEBUG] Skipped duplicate position update")
            }
            return
        }

        // Store pending update and start/restart debounce
        pendingUpdate = newState
        if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
            Log.v(TAG, "[DEBUG] Stored pending update: $newState, debounceDelay=$debounceDelay")
        }

        debounceJob?.cancel()
        debounceJob =
            coroutineScopeProvider.launchDefault {
                delay(debounceDelay)

                // Apply the debounced update
                pendingUpdate?.let { finalState ->
                    _currentState.value = finalState
                    _position.value = finalState.position
                    if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
                        Log.v(TAG, "[DEBUG] Applied debounced position: ${finalState.position} from ${finalState.source}")
                    }
                }
                pendingUpdate = null
            }
    }

    /**
     * Gets the current position synchronously.
     * Returns pending position (if available) to ensure consistency with reactive flows
     * during the debounce window.
     */
    fun getCurrentPosition(): Position? = pendingUpdate?.position ?: _position.value

    /**
     * Gets the GPS position specifically, ignoring simulation priority.
     * This always returns the latest GPS position received, even when simulation is active.
     * Used by SimulationButton to check if user's actual GPS position is in event area.
     *
     * Note: Does NOT check staleness - use isGPSStale() separately if needed.
     */
    fun getGPSPosition(): Position? = pendingGPSPosition ?: lastGPSPosition

    /**
     * Gets the age of the last GPS position update.
     *
     * @return Duration since last GPS update, or null if no GPS position has been received
     */
    fun getGPSAge(): Duration? {
        val timestamp = lastGPSUpdateTime ?: return null
        return clock.now() - timestamp
    }

    /**
     * Checks if the GPS position is stale (too old to be reliable).
     *
     * @param threshold The maximum age before GPS is considered stale (default: 5 minutes)
     * @return true if GPS position exists but is older than threshold, false otherwise
     */
    fun isGPSStale(threshold: Duration = DEFAULT_GPS_STALENESS_THRESHOLD): Boolean {
        val position = getGPSPosition()
        if (position == null) {
            // No GPS position - not "stale", just absent
            return false
        }

        val age = getGPSAge() ?: return false
        return age > threshold
    }

    /**
     * Gets diagnostic information about GPS state.
     * Useful for logging and debugging GPS issues.
     */
    fun getGPSInfo(): String {
        val position = getGPSPosition()
        val age = getGPSAge()
        val isStale = isGPSStale()

        return buildString {
            append("GPS Position: ")
            if (position != null) {
                append("$position")
                if (age != null) {
                    append(", age=${age.inWholeSeconds}s")
                }
                if (isStale) {
                    append(" (STALE)")
                }
            } else {
                append("null")
            }
        }
    }

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
        pendingGPSPosition = null
        lastGPSPosition = null
        lastGPSUpdateTime = null
        _currentState.value = PositionState(null, null)
        _position.value = null
        Log.v("PositionManager", "Cleared all position data")
    }

    /**
     * Determines if a new position update should be accepted based on source priority
     */
    @Suppress("ReturnCount") // Early returns for guard clauses improve readability
    private fun shouldAcceptUpdate(
        current: PositionState,
        new: PositionState,
    ): Boolean {
        // Always accept if no current position
        val noCurrentPosition = current.position == null
        if (noCurrentPosition) {
            return true
        }

        // Always accept clearing operations (null position)
        val isClearingOperation = new.position == null
        if (isClearingOperation) {
            return true
        }

        // Always accept if new source has higher priority (lower ordinal)
        val shouldAccept =
            when {
                current.source == null -> true
                new.source == null -> false
                new.source.ordinal <= current.source.ordinal -> true
                else -> false
            }
        return shouldAccept
    }

    /**
     * Checks if a position is effectively duplicate using epsilon comparison
     */
    private fun isPositionDuplicate(
        current: Position?,
        new: Position?,
    ): Boolean {
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
        pendingGPSPosition = null
        lastGPSUpdateTime = null
        Log.v("PositionManager", "Cleaned up resources")
    }
}
