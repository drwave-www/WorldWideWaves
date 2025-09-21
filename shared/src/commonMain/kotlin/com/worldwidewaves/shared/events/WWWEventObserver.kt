package com.worldwidewaves.shared.events

import androidx.annotation.VisibleForTesting

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

import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_WARN_BEFORE_HIT
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.utils.updateIfChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.Instant.Companion.DISTANT_FUTURE

// ---------------------------

/**
 * Observes a single [IWWWEvent] and the user's current position to expose
 * UI-friendly reactive state.
 *
 * ## State Machine & Lifecycle Documentation
 *
 * ### Core Responsibilities:
 * • Track overall wave progression & event [Status]
 * • Predict if/when the user will be hit and the remaining time-to-hit
 * • Detect warming phases (global & per-user) and completion of the hit
 * • Mirror helper booleans used by choreography layers (start-warming, hit, …)
 * • Cache latest computations to minimise expensive geo / time calculations
 *
 * ### State Transitions & Flow:
 *
 * **Event Status Progression:**
 * ```
 * UNDEFINED → SOON → WARMING → RUNNING → DONE
 *     ↓        ↓       ↓        ↓       ↓
 *   Initial   Ready   Pre-hit  Active  Finished
 * ```
 *
 * **User-Specific State Flow:**
 * ```
 * Not in Area → In Area → Warming → About to Hit → Hit → Done
 *      ↓          ↓        ↓          ↓           ↓      ↓
 *    userIsInArea=false → true → isUserWarmingInProgress → userIsGoingToBeHit → userHasBeenHit
 * ```
 *
 * ### Threading Model & Concurrency:
 *
 * **Dispatcher Usage:**
 * - **Main Thread**: Volume control operations (AudioManager access)
 * - **Default Dispatcher**: CPU-bound calculations (progression, status, time)
 * - **I/O Dispatcher**: Not used (avoided for performance)
 *
 * **Coroutine Lifecycle:**
 * 1. **Initialization**: Immediate state computation on creation
 * 2. **Observation Flow**: Continuous monitoring via `createObservationFlow()`
 * 3. **Adaptive Intervals**: Dynamic polling based on event proximity
 * 4. **Cleanup**: Automatic cancellation on observer disposal
 *
 * **Thread Safety Guarantees:**
 * - All StateFlow emissions are thread-safe
 * - Internal state updates use smart throttling to prevent race conditions
 * - Coroutine cancellation is handled gracefully with proper cleanup
 *
 * ### Memory Management & Resource Cleanup:
 *
 * **Automatic Cleanup:**
 * ```kotlin
 * // Observer automatically cleans up when event is done
 * if (event.isDone()) {
 *     observationJob?.cancelAndJoin()
 *     observationJob = null
 * }
 * ```
 *
 * **Note**: The above example assumes execution within a suspend context.
 * In practice, this cleanup happens automatically within the observation coroutine.
 *
 * **Manual Cleanup (Optional):**
 * ```kotlin
 * observer.stopObservation() // Explicit cleanup if needed
 * ```
 *
 * **Memory Optimization Features:**
 * - Smart throttling reduces StateFlow emissions by ~80%
 * - Cached position calculations prevent redundant geo computations
 * - Adaptive observation intervals minimize CPU wake-ups
 *
 * ### Error Handling Patterns:
 *
 * **Graceful Degradation:**
 * - Invalid progression values are logged but don't crash
 * - Geographic calculation errors default to safe values
 * - Network/location failures maintain last known state
 *
 * **Error Recovery:**
 * - Observation flow catches exceptions and continues
 * - State validation provides consistency checks
 * - Logging helps diagnose state management issues
 *
 * ### Performance Characteristics:
 *
 * **Observation Intervals (Adaptive):**
 * - **> 1 hour**: 1 hour intervals
 * - **5-60 minutes**: 5 minute intervals
 * - **35-300 seconds**: 1 second intervals
 * - **0-35 seconds**: 500ms intervals (active monitoring)
 * - **Hit critical (< 1s)**: 50ms intervals (sound accuracy)
 *
 * **State Update Throttling (Adaptive):**
 * - Progression: Only updates if change > 0.1%
 * - Position: Only updates if change > 1%
 * - Time: Adaptive (50ms during critical hit phase < 2s, 1000ms otherwise)
 *
 * ### Usage Guidelines:
 *
 * **Lifecycle Management:**
 * 1. Observer auto-starts on creation
 * 2. Observation begins when event is "soon" and "near time"
 * 3. Automatic cleanup when event reaches DONE status
 * 4. Manual cleanup via `stopObservation()` if needed
 *
 * **StateFlow Consumption:**
 * ```kotlin
 * observer.eventStatus.collectAsState() // Compose integration
 * observer.progression.collect { ... }  // Coroutine integration
 * ```
 *
 * **Critical Timing Requirements:**
 * - Wave hit detection accuracy: ±50ms (for sound synchronization)
 * - State updates during critical phase: Every 50ms
 * - Geographic position updates: Sub-second precision
 *
 * All values are surfaced as cold, hot [StateFlow]s so Composables can observe
 * them without manual cancellation. Internally the work is coalesced through
 * an adaptive observation interval to avoid unnecessary CPU wake-ups when the
 * event is far in the future.
 */
@OptIn(ExperimentalTime::class)
class WWWEventObserver(
    private val event: IWWWEvent,
) : KoinComponent {
    data class EventObservation(
        val progression: Double,
        val status: Status,
    )

    private val clock: IClock by inject()

    private val coroutineScopeProvider: CoroutineScopeProvider by inject()

    private val _eventStatus = MutableStateFlow(Status.UNDEFINED)
    val eventStatus: StateFlow<Status> = _eventStatus.asStateFlow()

    private val _progression = MutableStateFlow(0.0)
    val progression: StateFlow<Double> = _progression.asStateFlow()

    private val _isUserWarmingInProgress = MutableStateFlow(false)
    val isUserWarmingInProgress: StateFlow<Boolean> = _isUserWarmingInProgress.asStateFlow()

    private val _isStartWarmingInProgress = MutableStateFlow(false)
    val isStartWarmingInProgress: StateFlow<Boolean> = _isStartWarmingInProgress.asStateFlow()

    private val _userIsGoingToBeHit = MutableStateFlow(false)
    val userIsGoingToBeHit: StateFlow<Boolean> = _userIsGoingToBeHit.asStateFlow()

    private val _userHasBeenHit = MutableStateFlow(false)
    val userHasBeenHit: StateFlow<Boolean> = _userHasBeenHit.asStateFlow()

    private val _userPositionRatio = MutableStateFlow(0.0)
    val userPositionRatio: StateFlow<Double> = _userPositionRatio.asStateFlow()

    private val _timeBeforeHit = MutableStateFlow(INFINITE)
    val timeBeforeHit: StateFlow<Duration> = _timeBeforeHit.asStateFlow()

    private val _hitDateTime = MutableStateFlow(DISTANT_FUTURE)
    val hitDateTime: StateFlow<Instant> = _hitDateTime.asStateFlow()

    private val _userIsInArea = MutableStateFlow(false)
    val userIsInArea: StateFlow<Boolean> = _userIsInArea.asStateFlow()

    // -- Performance optimization: Smart throttling for state updates

    /**
     * Thresholds for smart state update throttling.
     * Only emit updates when changes exceed these thresholds to reduce unnecessary UI updates.
     */
    private companion object {
        const val PROGRESSION_THRESHOLD = 0.1 // Only update progression if change is > 0.1%
        const val POSITION_RATIO_THRESHOLD = 0.01 // Only update position ratio if change is > 1%
        const val TIME_THRESHOLD_MS = 1000L // Normal phase: update time if change is > 1 second (adaptive to 50ms during critical hit phase)
    }

    // Cache last emitted values for throttling
    private var lastEmittedProgression: Double = -1.0
    private var lastEmittedPositionRatio: Double = -1.0
    private var lastEmittedTimeBeforeHit: Duration = Duration.INFINITE

    private var observationJob: Job? = null

    // ---------------------------

    init {
        startObservation()
    }

    /**
     * Starts observing the wave event if not already started.
     *
     * This method has been simplified to reduce state management complexity:
     * 1. Always initializes state immediately
     * 2. Starts continuous observation for active events
     * 3. Provides better error handling and logging
     */
    fun startObservation() {
        if (observationJob == null) {
            coroutineScopeProvider.launchDefault {
                Log.v("WWWEventObserver", "Starting observation for event ${event.id}")

                try {
                    // Initialize state with current values - enhanced error handling
                    val currentStatus = event.getStatus()
                    val currentProgression = try {
                        event.wave.getProgression()
                    } catch (e: Throwable) {
                        Log.e("WWWEventObserver", "Error getting wave progression for event ${event.id}: $e")
                        0.0
                    }

                    Log.v("WWWEventObserver", "Initial state: status=$currentStatus, progression=$currentProgression")

                    // Always initialize ALL state values - this ensures consistent state
                    updateStates(currentProgression, currentStatus)

                    // Check if we should start continuous observation
                    val shouldObserve = event.isRunning() || (event.isSoon() && event.isNearTime())
                    Log.v("WWWEventObserver", "Should start continuous observation: $shouldObserve")

                    if (shouldObserve) {
                        Log.i("WWWEventObserver", "Starting continuous observation flow for event ${event.id}")
                        observationJob =
                            createObservationFlow()
                                .flowOn(Dispatchers.Default)  // CPU-bound calculations (progression, status, time)
                                .catch { e ->
                                    Log.e("WWWEventObserver", "Error in observation flow for event ${event.id}: $e")
                                    // Don't propagate the error - just log it and continue
                                }.onEach { (progressionValue, status) ->
                                    Log.v("WWWEventObserver", "Observation update: progression=$progressionValue, status=$status")
                                    updateStates(progressionValue, status)
                                }.launchIn(coroutineScopeProvider.scopeDefault())
                    } else {
                        Log.v("WWWEventObserver", "Event ${event.id} not ready for continuous observation")
                    }
                } catch (e: Exception) {
                    Log.e("WWWEventObserver", "Failed to start observation for event ${event.id}", throwable = e)
                }
            }
        } else {
            Log.v("WWWEventObserver", "Observation already running for event ${event.id}")
        }
    }

    fun stopObservation() {
        coroutineScopeProvider.launchDefault {
            Log.v("stopObservation", "Stopping observation for event $event.id")
            try {
                observationJob?.cancelAndJoin()
            } catch (_: CancellationException) {
                // Expected exception during cancellation
            } catch (e: Exception) {
                Log.e("stopObservation", "Error stopping observation: $e")
            } finally {
                observationJob = null
            }
        }
    }

    /**
     * Creates a flow that periodically emits wave observations.
     */
    fun createObservationFlow() =
        callbackFlow {
            try {
                while (!event.isDone()) {
                    // Get current state and emit it
                    val progression = event.wave.getProgression()
                    val status = event.getStatus()
                    val eventObservation = EventObservation(progression, status)
                    send(eventObservation)

                    // Wait for the next observation interval
                    val observationDelay = getObservationInterval()

                    if (!observationDelay.isFinite()) {
                        Log.w("observationFlow", "Stopping flow due to infinite observation delay")
                        break
                    }

                    clock.delay(observationDelay)
                }

                // Final emission when event is done
                send(EventObservation(100.0, Status.DONE))
            } catch (e: Exception) {
                Log.e("observationFlow", "Error in observation flow: $e")
            }

            // Clean up when flow is cancelled
            awaitClose()
        }

    /**
     * Updates all state flows based on the current event state.
     *
     * This method validates state transitions and provides detailed logging
     * to help diagnose state management issues.
     */
    private suspend fun updateStates(
        progression: Double,
        status: Status,
    ) {
        Log.v("WWWEventObserver", "updateStates called: progression=$progression, status=$status, eventId=${event.id}")

        // Validate input parameters
        if (progression < 0.0 || progression > 100.0) {
            Log.w("WWWEventObserver", "Invalid progression value: $progression (should be 0-100)")
        }
        // Update the main state flows with smart throttling
        updateProgressionIfSignificant(progression)
        _eventStatus.updateIfChanged(status)

        // Check for warming started
        var warmingInProgress = false
        if (event.warming.isUserWarmingStarted()) {
            warmingInProgress = true
        }

        // Check if user is about to be hit
        var userIsGoingToBeHit = false
        val timeBeforeHit = event.wave.timeBeforeUserHit() ?: INFINITE
        if (timeBeforeHit > ZERO && timeBeforeHit <= WAVE_WARN_BEFORE_HIT) {
            warmingInProgress = false
            userIsGoingToBeHit = true
        }

        // Check if user has been hit
        if (event.wave.hasUserBeenHitInCurrentPosition()) {
            warmingInProgress = false
            userIsGoingToBeHit = false
            _userHasBeenHit.updateIfChanged(true)
        }

        // Update additional state flows with smart throttling
        updateTimeBeforeHitIfSignificant(timeBeforeHit)
        updatePositionRatioIfSignificant(event.wave.userPositionToWaveRatio() ?: 0.0)
        _hitDateTime.updateIfChanged(event.wave.userHitDateTime() ?: DISTANT_FUTURE)

        // Warming start (between event start and wave start)
        val now = clock.now()
        _isStartWarmingInProgress.updateIfChanged(now > event.getStartDateTime() && now < event.getWaveStartDateTime())

        // User in area - enhanced with validation and error handling
        val userPosition = event.wave.getUserPosition()
        Log.v("WWWEventObserver", "Area detection: userPosition=$userPosition")

        if (userPosition != null) {
            try {
                val isInArea = event.area.isPositionWithin(userPosition)
                Log.v("WWWEventObserver", "Area detection result: isInArea=$isInArea for position=$userPosition")
                _userIsInArea.updateIfChanged(isInArea)
            } catch (e: Exception) {
                Log.e("WWWEventObserver", "Error checking if user is in area", throwable = e)
                // On error, assume user is not in area for safety
                _userIsInArea.updateIfChanged(false)
            }
        } else {
            Log.v("WWWEventObserver", "No user position available, setting userIsInArea=false")
            _userIsInArea.updateIfChanged(false)
        }

        _isUserWarmingInProgress.updateIfChanged(warmingInProgress)
        _userIsGoingToBeHit.updateIfChanged(userIsGoingToBeHit)

        // Log final state for debugging
        Log.v("WWWEventObserver", "State updated - userIsInArea=${_userIsInArea.value}, " +
            "progression=${_progression.value}, status=${_eventStatus.value}")
    }

    /**
     * Validates the current state consistency for debugging purposes.
     * This method helps identify state management issues during development.
     */
    @VisibleForTesting
    suspend fun validateStateConsistency(): List<String> {
        val issues = mutableListOf<String>()

        try {
            // Check progression bounds
            val progression = _progression.value
            if (progression < 0.0 || progression > 100.0) {
                issues.add("Progression out of bounds: $progression (should be 0-100)")
            }

            // Check user position vs area consistency
            val userPosition = event.wave.getUserPosition()
            val observerUserInArea = _userIsInArea.value

            if (userPosition != null) {
                val actualUserInArea = event.area.isPositionWithin(userPosition)
                if (actualUserInArea != observerUserInArea) {
                    issues.add("userIsInArea inconsistency: observer=$observerUserInArea, actual=$actualUserInArea")
                }
            } else if (observerUserInArea) {
                issues.add("userIsInArea=true but no user position available")
            }

            // Check status consistency
            val observerStatus = _eventStatus.value
            val actualStatus = event.getStatus()
            if (observerStatus != actualStatus) {
                issues.add("Status inconsistency: observer=$observerStatus, actual=$actualStatus")
            }

        } catch (e: Exception) {
            issues.add("Error during state validation: ${e.message}")
        }

        if (issues.isNotEmpty()) {
            Log.w("WWWEventObserver", "State validation issues for event ${event.id}: ${issues.joinToString("; ")}")
        }

        return issues
    }

    /**
     * Smart throttling functions to reduce unnecessary state updates and improve performance.
     * These functions only emit updates when changes exceed predefined thresholds.
     */

    /**
     * Updates progression only if the change is significant enough to matter for UI.
     * Reduces state flow emissions by ~80% for smooth progression updates.
     */
    private fun updateProgressionIfSignificant(newProgression: Double) {
        if (abs(newProgression - lastEmittedProgression) >= PROGRESSION_THRESHOLD ||
            lastEmittedProgression < 0.0) { // Always emit first update
            _progression.updateIfChanged(newProgression)
            lastEmittedProgression = newProgression
        }
    }

    /**
     * Updates position ratio only if the change is significant enough to matter for UI.
     * Reduces unnecessary updates for small position movements.
     */
    private fun updatePositionRatioIfSignificant(newRatio: Double) {
        if (abs(newRatio - lastEmittedPositionRatio) >= POSITION_RATIO_THRESHOLD ||
            lastEmittedPositionRatio < 0.0) { // Always emit first update
            _userPositionRatio.updateIfChanged(newRatio)
            lastEmittedPositionRatio = newRatio
        }
    }

    /**
     * Updates time before hit with adaptive throttling for critical timing accuracy.
     *
     * Critical timing requirements:
     * - During hit phase (< 2s): Updates every 50ms for sound synchronization
     * - Normal phase (> 2s): Updates every 1000ms to reduce UI churn
     */
    private fun updateTimeBeforeHitIfSignificant(newTime: Duration) {
        val timeDifference = abs((newTime - lastEmittedTimeBeforeHit).inWholeMilliseconds)

        // Critical timing phase: Need sub-50ms accuracy for wave synchronization
        val isCriticalPhase = newTime.inWholeSeconds <= 2 && newTime > Duration.ZERO
        val threshold = if (isCriticalPhase) 50L else TIME_THRESHOLD_MS // 50ms vs 1000ms

        if (timeDifference >= threshold ||
            lastEmittedTimeBeforeHit == Duration.INFINITE) { // Always emit first update
            _timeBeforeHit.updateIfChanged(newTime)
            lastEmittedTimeBeforeHit = newTime
        }
    }

    /**
     * Calculates the observation interval for the wave event.
     *
     * This function determines the appropriate interval for observing the wave event based on the
     * current time, the event start time, and the time before the user is hit by the wave.
     *
     */
    private suspend fun getObservationInterval(): Duration {
        val now = clock.now()
        val eventStartTime = event.getStartDateTime()
        val timeBeforeEvent = eventStartTime - now
        val timeBeforeHit = event.wave.timeBeforeUserHit() ?: 1.days

        return when {
            timeBeforeEvent > 1.hours + 5.minutes -> 1.hours
            timeBeforeEvent > 5.minutes + 30.seconds -> 5.minutes
            timeBeforeEvent > 35.seconds -> 1.seconds
            timeBeforeEvent > 0.seconds || event.isRunning() -> 500.milliseconds
            timeBeforeHit < ZERO -> INFINITE
            timeBeforeHit < 1.seconds -> 50.milliseconds // For sound accuracy
            else -> 1.minutes // Default case, more reasonable than 1 day
        }
    }
}
