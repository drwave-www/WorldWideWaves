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

import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.domain.progression.WaveProgressionTracker
import com.worldwidewaves.shared.domain.scheduling.ObservationScheduler
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.position.PositionManager
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.ExperimentalTime

/**
 * Core event observation logic and lifecycle management.
 *
 * This class is responsible for:
 * - Managing observation lifecycle (start/stop)
 * - Creating and managing observation flows
 * - Coordinating multiple observation streams (periodic, position, simulation)
 * - Handling errors and cancellation gracefully
 *
 * ## Thread Safety
 * All operations are thread-safe and use appropriate dispatchers:
 * - Default dispatcher for CPU-bound calculations
 * - Proper coroutine cancellation handling
 *
 * ## iOS Safety
 * This class follows iOS safety patterns:
 * - Class-level KoinComponent (safe pattern)
 * - Uses `by inject()` for dependency injection (safe)
 * - No `init{}` blocks with coroutine launches
 * - Proper exception handling for iOS compatibility
 */
@OptIn(ExperimentalTime::class)
class EventObserver(
    private val event: IWWWEvent,
    private val coroutineScopeProvider: CoroutineScopeProvider,
    private val positionManager: PositionManager,
    private val waveProgressionTracker: WaveProgressionTracker,
    private val positionObserver: PositionObserver,
    private val observationScheduler: ObservationScheduler,
) : KoinComponent {
    /**
     * Data class representing a single observation snapshot.
     */
    data class EventObservation(
        val progression: Double,
        val status: Status,
    )

    /**
     * Data class to hold all observation jobs for lifecycle management.
     */
    data class ObservationJobs(
        val mainObservationJob: Job,
        val positionObserverJob: Job,
        val directPositionObserverJob: Job,
        val polygonLoadingJob: Job,
    )

    private var unifiedObservationJob: Job? = null

    // ---------------------------
    // Lifecycle Management
    // ---------------------------

    /**
     * Starts observing the wave event if not already started.
     *
     * This method:
     * 1. Initializes state immediately
     * 2. Starts continuous observation for active events
     * 3. Provides error handling and logging
     *
     * @param onStateUpdate Callback for state updates
     * @param onAreaUpdate Callback for area detection updates
     * @param onPositionUpdate Callback for position updates
     */
    fun startObservation(
        onStateUpdate: suspend (EventObservation) -> Unit,
        onAreaUpdate: suspend (Boolean) -> Unit,
        onPositionUpdate: suspend () -> Unit,
        onPolygonLoad: suspend (Boolean) -> Unit,
    ) {
        if (unifiedObservationJob == null) {
            coroutineScopeProvider.launchDefault {
                Log.v("EventObserver", "Starting unified observation for event ${event.id}")

                try {
                    val observationJobs = startObservationJobs(onStateUpdate, onAreaUpdate, onPositionUpdate, onPolygonLoad)
                    setupParentObservationJob(observationJobs)
                } catch (e: IllegalStateException) {
                    handleInitializationStateError(e)
                } catch (e: CancellationException) {
                    handleCancellationException(e)
                }
            }
        }
    }

    /**
     * Stops the observation and cleans up resources.
     */
    fun stopObservation() {
        coroutineScopeProvider.launchDefault {
            try {
                unifiedObservationJob?.cancelAndJoin()
            } catch (_: CancellationException) {
                // Expected exception during cancellation
            } catch (e: IllegalStateException) {
                Log.e("EventObserver", "State error stopping unified observation: $e")
            } catch (e: Exception) {
                Log.e("EventObserver", "Unexpected error stopping unified observation: $e")
            } finally {
                unifiedObservationJob = null
            }
        }
    }

    // ---------------------------
    // Flow Creation
    // ---------------------------

    /**
     * Creates a unified observation flow that combines periodic ticks and simulation changes.
     */
    fun createUnifiedObservationFlow(onAreaDetection: suspend () -> Unit) =
        combine(
            createPeriodicObservationFlow(),
            createSimulationFlow(),
        ) { periodicObservation, _ ->
            periodicObservation
        }.onEach { _ ->
            // Ensure area detection is called on every unified observation update
            try {
                onAreaDetection()
            } catch (e: IllegalStateException) {
                Log.e("EventObserver", "State error in area detection from unified flow for event ${event.id}: $e")
            } catch (e: CancellationException) {
                handleCancellationException(e)
            } catch (e: Exception) {
                Log.e("EventObserver", "Unexpected error in area detection from unified flow for event ${event.id}: $e")
            }
        }

    /**
     * Creates a flow that periodically emits wave observations when the event should be observed.
     */
    private fun createPeriodicObservationFlow() =
        callbackFlow {
            try {
                Log.v("EventObserver", "Starting periodic observation flow for event ${event.id}")

                observationScheduler.createObservationFlow(event).collect { _ ->
                    val progression = calculateProgressionSafely(event)
                    val status = getEventStatusSafely(event)
                    val eventObservation = EventObservation(progression, status)
                    send(eventObservation)
                }
            } catch (e: IllegalStateException) {
                Log.e("EventObserver", "State error in periodic observation flow: $e")
            } catch (e: CancellationException) {
                handleCancellationException(e)
            } catch (e: Exception) {
                Log.e("EventObserver", "Unexpected error in periodic observation flow: $e")
            }

            awaitClose {
                Log.v("EventObserver", "Closing periodic observation flow for event ${event.id}")
            }
        }

    /**
     * Creates a flow that emits when simulation changes.
     */
    private fun createSimulationFlow() =
        callbackFlow {
            try {
                val platform =
                    try {
                        get<WWWPlatform>()
                    } catch (e: IllegalStateException) {
                        Log.w("EventObserver", "State error - Platform not available for simulation observation: $e")
                        send(Unit)
                        awaitClose()
                        return@callbackFlow
                    } catch (e: Exception) {
                        Log.w("EventObserver", "Unexpected error - Platform not available for simulation observation: $e")
                        send(Unit)
                        awaitClose()
                        return@callbackFlow
                    }

                send(Unit) // Emit initial value

                // Then collect simulation changes
                platform.simulationChanged.collect {
                    Log.v("EventObserver", "Simulation change detected for event ${event.id}")
                    send(Unit)
                }
            } catch (e: CancellationException) {
                Log.v("EventObserver", "Simulation observation cancelled for event ${event.id}")
                handleCancellationException(e)
            } catch (e: IllegalStateException) {
                Log.e("EventObserver", "State error in simulation observation for event ${event.id}: $e")
            } catch (e: Exception) {
                Log.e("EventObserver", "Unexpected error in simulation observation for event ${event.id}: $e")
            }

            awaitClose()
        }

    // ---------------------------
    // Private Helper Methods
    // ---------------------------

    /**
     * Starts all observation jobs and returns them for management.
     */
    private fun startObservationJobs(
        onStateUpdate: suspend (EventObservation) -> Unit,
        onAreaUpdate: suspend (Boolean) -> Unit,
        onPositionUpdate: suspend () -> Unit,
        onPolygonLoad: suspend (Boolean) -> Unit,
    ): ObservationJobs {
        val mainObservationJob = startMainObservationFlow(onStateUpdate)
        val positionObserverJob = startPositionObserverFlow(onAreaUpdate)
        val directPositionObserverJob = startDirectPositionObserverFlow(onPositionUpdate)
        val polygonLoadingJob = startPolygonLoadingFlow(onPolygonLoad)

        return ObservationJobs(
            mainObservationJob,
            positionObserverJob,
            directPositionObserverJob,
            polygonLoadingJob,
        )
    }

    /**
     * Starts the main unified observation flow.
     */
    private fun startMainObservationFlow(onStateUpdate: suspend (EventObservation) -> Unit): Job =
        createUnifiedObservationFlow {
            // Area detection callback handled in createUnifiedObservationFlow
        }.flowOn(Dispatchers.Default)
            .catch { e ->
                Log.e("EventObserver", "Error in unified observation flow for event ${event.id}: $e")
            }.onEach { observation ->
                handleMainObservationUpdate(observation, onStateUpdate)
            }.launchIn(coroutineScopeProvider.scopeDefault())

    /**
     * Starts the position observer flow.
     */
    private fun startPositionObserverFlow(onAreaUpdate: suspend (Boolean) -> Unit): Job =
        positionObserver
            .observePositionForEvent(event)
            .catch { e ->
                Log.e("EventObserver", "Error in position observation for event ${event.id}: $e")
            }.onEach { observation ->
                handlePositionObservationUpdate(observation, onAreaUpdate)
            }.launchIn(coroutineScopeProvider.scopeDefault())

    /**
     * Starts the direct position observer flow for immediate response.
     */
    private fun startDirectPositionObserverFlow(onPositionUpdate: suspend () -> Unit): Job =
        positionManager.position
            .onEach { _ ->
                handleDirectPositionUpdate(onPositionUpdate)
            }.launchIn(coroutineScopeProvider.scopeDefault())

    /**
     * Starts the polygon loading observer flow.
     */
    private fun startPolygonLoadingFlow(onPolygonLoad: suspend (Boolean) -> Unit): Job =
        event.area.polygonsLoaded
            .onEach { isLoaded ->
                handlePolygonLoadingUpdate(isLoaded, onPolygonLoad)
            }.launchIn(coroutineScopeProvider.scopeDefault())

    /**
     * Sets up the parent job that manages all child observation jobs.
     */
    private fun setupParentObservationJob(jobs: ObservationJobs) {
        unifiedObservationJob =
            coroutineScopeProvider.launchDefault {
                // Use invokeOnCompletion instead of blocking join (iOS deadlock prevention)
                jobs.mainObservationJob.invokeOnCompletion {
                    jobs.positionObserverJob.cancel()
                    jobs.directPositionObserverJob.cancel()
                    jobs.polygonLoadingJob.cancel()
                }
            }
    }

    /**
     * Handles updates from the main observation flow.
     */
    private suspend fun handleMainObservationUpdate(
        observation: EventObservation,
        onStateUpdate: suspend (EventObservation) -> Unit,
    ) {
        try {
            onStateUpdate(observation)
        } catch (e: IllegalStateException) {
            Log.e("EventObserver", "State error updating states for event ${event.id}: $e")
        } catch (e: CancellationException) {
            handleCancellationException(e)
        } catch (e: Exception) {
            Log.e("EventObserver", "Unexpected error updating states for event ${event.id}: $e")
        }
    }

    /**
     * Handles updates from the position observer flow.
     */
    private suspend fun handlePositionObservationUpdate(
        observation: PositionObservation,
        onAreaUpdate: suspend (Boolean) -> Unit,
    ) {
        try {
            onAreaUpdate(observation.isInArea)
        } catch (e: IllegalStateException) {
            Log.e("EventObserver", "State error updating user area status for event ${event.id}: $e")
        } catch (e: CancellationException) {
            handleCancellationException(e)
        } catch (e: Exception) {
            Log.e("EventObserver", "Unexpected error updating user area status for event ${event.id}: $e")
        }
    }

    /**
     * Handles direct position updates for immediate area detection.
     */
    private suspend fun handleDirectPositionUpdate(onPositionUpdate: suspend () -> Unit) {
        try {
            if (com.worldwidewaves.shared.WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
                Log.v("WWW.Domain.Observer", "Direct position changed, updating area detection for event ${event.id}")
            }
            onPositionUpdate()
        } catch (e: IllegalStateException) {
            Log.e("EventObserver", "State error in updateAreaDetection for event ${event.id}: $e")
        } catch (e: CancellationException) {
            handleCancellationException(e)
        } catch (e: Exception) {
            Log.e("EventObserver", "Unexpected error in updateAreaDetection for event ${event.id}: $e")
        }
    }

    /**
     * Handles polygon loading updates.
     */
    private suspend fun handlePolygonLoadingUpdate(
        isLoaded: Boolean,
        onPolygonLoad: suspend (Boolean) -> Unit,
    ) {
        try {
            onPolygonLoad(isLoaded)
        } catch (e: IllegalStateException) {
            Log.e("EventObserver", "State error in polygon loading handler for event ${event.id}: $e")
        } catch (e: CancellationException) {
            handleCancellationException(e)
        } catch (e: Exception) {
            Log.e("EventObserver", "Unexpected error in polygon loading handler for event ${event.id}: $e")
        }
    }

    /**
     * Handles initialization state errors.
     */
    private fun handleInitializationStateError(e: IllegalStateException) {
        if (e.message?.contains("Flow context cannot contain job") == true) {
            Log.i("EventObserver", "Feature map not yet downloaded for event ${event.id}")
        } else {
            Log.e("EventObserver", "State error starting observation for event ${event.id}: ${e.message}")
        }
    }

    /**
     * Helper function to safely calculate wave progression with error handling.
     */
    private suspend fun calculateProgressionSafely(event: IWWWEvent): Double =
        try {
            waveProgressionTracker.calculateProgression(event)
        } catch (e: IllegalArgumentException) {
            Log.e("EventObserver", "Invalid arguments for wave progression calculation for event ${event.id}: $e")
            0.0
        } catch (e: IllegalStateException) {
            Log.e("EventObserver", "Invalid state for wave progression calculation for event ${event.id}: $e")
            0.0
        } catch (e: ArithmeticException) {
            Log.e("EventObserver", "Arithmetic error in wave progression calculation for event ${event.id}: $e")
            0.0
        } catch (e: RuntimeException) {
            Log.e("EventObserver", "Runtime error getting wave progression for event ${event.id}: $e")
            0.0
        }

    /**
     * Helper function to safely get event status with error handling.
     */
    private suspend fun getEventStatusSafely(event: IWWWEvent): Status =
        try {
            event.getStatus()
        } catch (e: IllegalStateException) {
            Log.e("EventObserver", "State error getting event status for event ${event.id}: $e")
            Status.UNDEFINED
        } catch (e: Exception) {
            Log.e("EventObserver", "Unexpected error getting event status for event ${event.id}: $e")
            Status.UNDEFINED
        }

    /**
     * Helper function to handle cancellation exceptions with proper re-throwing.
     */
    private fun handleCancellationException(e: CancellationException): Nothing = throw e
}
