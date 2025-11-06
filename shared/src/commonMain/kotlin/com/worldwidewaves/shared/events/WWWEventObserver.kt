package com.worldwidewaves.shared.events

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

import com.worldwidewaves.shared.domain.detection.EventStateParams
import com.worldwidewaves.shared.domain.observation.PositionObserver
import com.worldwidewaves.shared.domain.progression.WaveProgressionTracker
import com.worldwidewaves.shared.domain.scheduling.ObservationScheduler
import com.worldwidewaves.shared.domain.state.EventState
import com.worldwidewaves.shared.domain.state.EventStateHolder
import com.worldwidewaves.shared.domain.state.EventStateInput
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.position.PositionManager
import com.worldwidewaves.shared.utils.Log
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
 * **Observation Intervals (Adaptive - via ObservationScheduler):**
 * - **> 1 hour**: 1 hour intervals (minimal battery usage)
 * - **5-60 minutes**: 5 minute intervals (moderate monitoring)
 * - **35-300 seconds**: 1 second intervals (active monitoring)
 * - **0-35 seconds**: 500ms intervals (real-time updates)
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
    private val positionManager: PositionManager by inject()
    private val waveProgressionTracker: WaveProgressionTracker by inject()
    private val positionObserver: PositionObserver by inject()
    private val eventStateHolder: EventStateHolder by inject()
    private val observationScheduler: ObservationScheduler by inject()
    private val favoriteEventsStore: com.worldwidewaves.shared.data.FavoriteEventsStore by inject()
    private val notificationManager: com.worldwidewaves.shared.notifications.NotificationManager? =
        try {
            get()
        } catch (_: Exception) {
            null // Gracefully handle missing notification manager (tests)
        }
    private val notificationContentProvider: com.worldwidewaves.shared.notifications.NotificationContentProvider? =
        try {
            get()
        } catch (_: Exception) {
            null // Gracefully handle missing content provider (tests)
        }

    // -- Specialized Components (Facade Pattern) --

    /**
     * Core observation lifecycle manager.
     * Handles observation flow creation and coordination.
     */
    private val eventObserver: com.worldwidewaves.shared.domain.observation.EventObserver by lazy {
        com.worldwidewaves.shared.domain.observation.EventObserver(
            event = event,
            coroutineScopeProvider = coroutineScopeProvider,
            positionManager = positionManager,
            waveProgressionTracker = waveProgressionTracker,
            positionObserver = positionObserver,
            observationScheduler = observationScheduler,
        )
    }

    /**
     * Wave hit detection and state calculation.
     * Uses EventStateHolder for all calculations.
     */
    private val waveHitDetector: com.worldwidewaves.shared.domain.detection.WaveHitDetector by lazy {
        com.worldwidewaves.shared.domain.detection.WaveHitDetector(
            eventStateHolder = eventStateHolder,
            clock = clock,
            positionManager = positionManager,
        )
    }

    /**
     * StateFlow management with smart throttling.
     * Reduces state updates by ~80%.
     */
    private val progressionState =
        com.worldwidewaves.shared.domain.state
            .EventProgressionState()

    /**
     * Position tracking for events.
     * Handles area detection logic.
     */
    private val positionTracker: com.worldwidewaves.shared.domain.observation.EventPositionTracker by lazy {
        com.worldwidewaves.shared.domain.observation.EventPositionTracker(
            positionManager = positionManager,
            waveProgressionTracker = waveProgressionTracker,
        )
    }

    // -- Public StateFlow API (unchanged) --

    val eventStatus: StateFlow<Status> = progressionState.eventStatus
    val progression: StateFlow<Double> = progressionState.progression
    val isUserWarmingInProgress: StateFlow<Boolean> = progressionState.isUserWarmingInProgress
    val isStartWarmingInProgress: StateFlow<Boolean> = progressionState.isStartWarmingInProgress
    val userIsGoingToBeHit: StateFlow<Boolean> = progressionState.userIsGoingToBeHit
    val userHasBeenHit: StateFlow<Boolean> = progressionState.userHasBeenHit
    val userPositionRatio: StateFlow<Double> = progressionState.userPositionRatio
    val timeBeforeHit: StateFlow<Duration> = progressionState.timeBeforeHit
    val hitDateTime: StateFlow<Instant> = progressionState.hitDateTime
    val userIsInArea: StateFlow<Boolean> = progressionState.userIsInArea

    private var unifiedObservationJob: Job? = null

    /**
     * Atomic flag to prevent concurrent observer restarts.
     * Fixes race condition where multiple rapid startObservation() calls
     * would cancel each other's observation flows before completion.
     */
    private val isObserving = atomic(false)

    /**
     * Tracks previous wave hit state to detect transitions.
     * Phase 4: Used to trigger wave hit notification on transition from false → true.
     */
    private var previousUserHasBeenHit = false

    /**
     * Starts observing the wave event if not already started.
     *
     * This method delegates to specialized components:
     * 1. EventObserver - manages observation lifecycle
     * 2. WaveHitDetector - calculates event state
     * 3. EventProgressionState - manages StateFlow with throttling
     * 4. EventPositionTracker - handles area detection
     *
     * Uses atomic compareAndSet to prevent race conditions where multiple
     * rapid calls would cancel observation flows before completion.
     */
    fun startObservation() {
        // Atomic guard: Only proceed if not already observing
        if (isObserving.compareAndSet(expect = false, update = true)) {
            unifiedObservationJob =
                coroutineScopeProvider.launchDefault {
                    Log.v("WWWEventObserver", "Starting unified observation for event ${event.id}")

                    try {
                        // Initialize state immediately
                        initializeEventState()

                        // Start observation using EventObserver component
                        eventObserver.startObservation(
                            onStateUpdate = { observation ->
                                updateStates(observation.progression, observation.status)
                            },
                            onAreaUpdate = { isInArea ->
                                progressionState.updateUserIsInArea(isInArea)
                            },
                            onPositionUpdate = {
                                updateAreaDetection()
                            },
                            onPolygonLoad = { isLoaded ->
                                if (isLoaded) {
                                    Log.v("WWWEventObserver", "Polygons loaded, updating area detection for event ${event.id}")
                                    updateAreaDetection()
                                }
                            },
                        )

                        // CRITICAL: Always update area detection on observer start
                        // The onPolygonLoad callback won't fire if polygonsLoaded is already true
                        // (no state change to trigger onEach). This ensures isInArea is calculated
                        // on every observer restart, especially important for simulation restarts.
                        updateAreaDetection()
                        Log.v("WWWEventObserver", "Initial area detection completed for event ${event.id}")
                    } catch (e: IllegalStateException) {
                        handleInitializationStateError(e)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        handleCancellationException(e)
                    } finally {
                        // Always reset flag when observation ends (cancelled or completed)
                        isObserving.value = false
                        unifiedObservationJob = null
                    }
                }
        } else {
            Log.v("WWWEventObserver", "Observation already in progress for event ${event.id}, ignoring duplicate start request")
        }
    }

    /**
     * Initializes event state with current values using WaveHitDetector
     */
    private suspend fun initializeEventState() {
        val currentStatus = getEventStatusSafely(event)
        val currentProgression = calculateProgressionSafely(event)
        Log.v("WWWEventObserver", "Initial state: status=$currentStatus, progression=$currentProgression")
        updateStates(currentProgression, currentStatus)
    }

    /**
     * Handles initialization state errors
     */
    private fun handleInitializationStateError(e: IllegalStateException) {
        if (e.message?.contains("Flow context cannot contain job") == true) {
            Log.i("WWWEventObserver", "Feature map not yet downloaded for event ${event.id}")
        } else {
            Log.e("WWWEventObserver", "State error starting observation for event ${event.id}: ${e.message}")
        }
    }

    /**
     * Stops active observation and resets state.
     * Ensures atomic flag is released for future restarts.
     */
    fun stopObservation() {
        eventObserver.stopObservation()
        unifiedObservationJob?.cancel()
        unifiedObservationJob = null
        isObserving.value = false
        Log.v("WWWEventObserver", "Stopped observation for event ${event.id}")
    }

    /**
     * Stops active observation and waits for cancellation to complete.
     * Use this when you need to ensure all async operations are fully cancelled
     * before starting a new observer (e.g., simulation restart).
     *
     * CRITICAL: Prevents race conditions where polygon loading from the cancelled
     * observer interferes with the new observer's initialization.
     */
    suspend fun stopObservationAndWait() {
        eventObserver.stopObservation()
        val job = unifiedObservationJob
        if (job != null) {
            try {
                job.cancelAndJoin() // Wait for cancellation to complete
                Log.v("WWWEventObserver", "Stopped and joined observation for event ${event.id}")
            } catch (e: Exception) {
                Log.v("WWWEventObserver", "Exception during observer cancellation for ${event.id}: ${e.message}")
            }
        }
        unifiedObservationJob = null
        isObserving.value = false
    }

    /**
     * Resets event state to initial values.
     * Used when starting simulation to ensure clean state transitions and avoid
     * validation errors like "DONE -> NEXT" or "userHasBeenHit cannot go to false".
     */
    fun resetState() {
        progressionState.reset()
        previousUserHasBeenHit = false // Reset wave hit tracking
        Log.v("WWWEventObserver", "Reset state for event ${event.id}")
    }

    /**
     * Updates all state flows based on the current event state.
     * Delegates to WaveHitDetector for state calculation and EventProgressionState for updates.
     */
    private suspend fun updateStates(
        progression: Double,
        status: Status,
    ) {
        // User in area - ensure immediate detection alongside PositionObserver
        updateAreaDetection()
        val userIsInArea = progressionState.userIsInArea.value

        try {
            // Calculate event state using WaveHitDetector
            val calculatedState =
                waveHitDetector.calculateEventState(
                    event = event,
                    progression = progression,
                    status = status,
                    userIsInArea = userIsInArea,
                )

            if (calculatedState != null) {
                // Validate the calculated state
                val stateInput =
                    EventStateInput(
                        progression = progression,
                        status = status,
                        userPosition = positionManager.getCurrentPosition(),
                        currentTime = clock.now(),
                    )
                val validationIssues = waveHitDetector.validateState(stateInput, calculatedState)
                if (validationIssues.isNotEmpty()) {
                    Log.w("WWWEventObserver", "State validation issues found: ${validationIssues.joinToString(", ")}")
                }

                // Validate state transitions
                val currentState = getCurrentEventState()
                val transitionIssues = waveHitDetector.validateStateTransition(currentState, calculatedState)
                if (transitionIssues.isNotEmpty()) {
                    Log.w("WWWEventObserver", "State transition issues found: ${transitionIssues.joinToString(", ")}")
                }

                // Update state flows with calculated values using smart throttling (via EventProgressionState)
                progressionState.updateFromEventState(calculatedState)

                // Phase 4: Detect wave hit transition and trigger immediate notification
                detectAndNotifyWaveHit(calculatedState)
            } else {
                // Fall back to basic state updates for safety
                progressionState.updateProgressionAndStatus(progression, status)
            }
        } catch (e: IllegalStateException) {
            Log.e("WWWEventObserver", "State error calculating event state: $e")
            // Fall back to basic state updates for safety
            progressionState.updateProgressionAndStatus(progression, status)
        } catch (e: kotlinx.coroutines.CancellationException) {
            handleCancellationException(e)
        } catch (e: Exception) {
            Log.e("WWWEventObserver", "Unexpected error calculating event state: $e")
            // Fall back to basic state updates for safety
            progressionState.updateProgressionAndStatus(progression, status)
        }
    }

    /**
     * Gets the current EventState from the StateFlow values.
     * Delegates to WaveHitDetector for state creation.
     */
    private fun getCurrentEventState(): EventState? =
        waveHitDetector.createEventState(
            EventStateParams(
                progression = progressionState.progression.value,
                status = progressionState.eventStatus.value,
                isUserWarmingInProgress = progressionState.isUserWarmingInProgress.value,
                isStartWarmingInProgress = progressionState.isStartWarmingInProgress.value,
                userIsGoingToBeHit = progressionState.userIsGoingToBeHit.value,
                userHasBeenHit = progressionState.userHasBeenHit.value,
                userPositionRatio = progressionState.userPositionRatio.value,
                timeBeforeHit = progressionState.timeBeforeHit.value,
                hitDateTime = progressionState.hitDateTime.value,
                userIsInArea = progressionState.userIsInArea.value,
            ),
        )

    /**
     * Updates area detection state (isInArea) based on current user position.
     * Delegates to EventPositionTracker for area detection logic.
     */
    private suspend fun updateAreaDetection() {
        try {
            val isInArea = positionTracker.isUserInArea(event)
            progressionState.updateUserIsInArea(isInArea)
        } catch (e: IllegalStateException) {
            Log.e("WWWEventObserver", "State error in updateAreaDetection for event ${event.id}: $e")
        } catch (e: kotlinx.coroutines.CancellationException) {
            handleCancellationException(e)
        } catch (e: Exception) {
            Log.e("WWWEventObserver", "Unexpected error in updateAreaDetection for event ${event.id}: $e")
        }
    }

    /**
     * Helper function to safely calculate wave progression with error handling.
     * Consolidates all progression calculation exceptions into one place.
     */
    private suspend fun calculateProgressionSafely(event: IWWWEvent): Double =
        try {
            waveProgressionTracker.calculateProgression(event)
        } catch (e: IllegalArgumentException) {
            Log.e("WWWEventObserver", "Invalid arguments for wave progression calculation for event ${event.id}: $e")
            0.0
        } catch (e: IllegalStateException) {
            Log.e("WWWEventObserver", "Invalid state for wave progression calculation for event ${event.id}: $e")
            0.0
        } catch (e: ArithmeticException) {
            Log.e("WWWEventObserver", "Arithmetic error in wave progression calculation for event ${event.id}: $e")
            0.0
        } catch (e: RuntimeException) {
            Log.e("WWWEventObserver", "Runtime error getting wave progression for event ${event.id}: $e")
            0.0
        }

    /**
     * Helper function to safely get event status with error handling.
     * Consolidates all status retrieval exceptions into one place.
     */
    private suspend fun getEventStatusSafely(event: IWWWEvent): Status =
        try {
            event.getStatus()
        } catch (e: IllegalStateException) {
            Log.e("WWWEventObserver", "State error getting event status for event ${event.id}: $e")
            Status.UNDEFINED
        } catch (e: Exception) {
            Log.e("WWWEventObserver", "Unexpected error getting event status for event ${event.id}: $e")
            Status.UNDEFINED
        }

    /**
     * Helper function to handle cancellation exceptions with proper re-throwing.
     * Consolidates cancellation handling to reduce throw statements.
     */
    private fun handleCancellationException(e: kotlinx.coroutines.CancellationException): Nothing {
        // Expected during cancellation, re-throw to maintain coroutine semantics
        throw e
    }

    /**
     * Phase 4: Detects wave hit transition and triggers immediate notification.
     *
     * ## Wave Hit Detection
     * Triggers notification when ALL conditions are met:
     * 1. User has just been hit (transition from false → true)
     * 2. Event is favorited by user
     * 3. Notification manager and content provider are available
     *
     * ## Notification Behavior
     * - Immediate delivery via NotificationManager.deliverNow()
     * - Only fires once per wave hit (prevents duplicate notifications)
     * - Works when app is open or backgrounded
     *
     * ## Thread Safety
     * Called from updateStates() which runs on coroutineScopeProvider.scopeDefault()
     * Safe to call suspend functions for notification delivery.
     *
     * @param calculatedState The newly calculated event state
     */
    private suspend fun detectAndNotifyWaveHit(calculatedState: EventState) {
        val currentHit = calculatedState.userHasBeenHit

        // Detect transition: not hit → hit
        if (currentHit && !previousUserHasBeenHit) {
            try {
                // Check if event is favorited
                val isFavorite = favoriteEventsStore.isFavorite(event.id)

                if (isFavorite) {
                    // Trigger wave hit notification
                    notificationManager?.let { manager ->
                        notificationContentProvider?.let { provider ->
                            // ContentProvider will resolve the location StringResource to a localized string
                            val content = provider.generateWaveHitNotification(event)
                            manager.deliverNow(
                                eventId = event.id,
                                trigger = com.worldwidewaves.shared.notifications.NotificationTrigger.WaveHit,
                                content = content,
                            )
                            Log.i(
                                "WWWEventObserver",
                                "Wave hit notification delivered for favorited event ${event.id}",
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WWWEventObserver", "Error delivering wave hit notification for event ${event.id}: $e")
            }
        }

        // Update previous state for next comparison
        previousUserHasBeenHit = currentHit
    }
}
