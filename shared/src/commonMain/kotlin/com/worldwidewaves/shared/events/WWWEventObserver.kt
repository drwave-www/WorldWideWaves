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

@OptIn(ExperimentalTime::class)
/**
 * Observes a single [IWWWEvent] and the user’s current position to expose
 * UI-friendly reactive state.
 *
 * The observer runs periodic sampling loops to:
 * • Track overall wave progression & event [Status]  
 * • Predict if/when the user will be hit and the remaining time-to-hit  
 * • Detect warming phases (global & per-user) and completion of the hit  
 * • Mirror helper booleans used by choreography layers (start-warming, hit, …)  
 * • Cache latest computations to minimise expensive geo / time calculations
 *
 * All values are surfaced as cold, hot [StateFlow]s so Composables can observe
 * them without manual cancellation.  Internally the work is coalesced through
 * an adaptive observation interval to avoid unnecessary CPU wake-ups when the
 * event is far in the future.
 */
class WWWEventObserver(private val event: IWWWEvent) : KoinComponent {

    data class EventObservation(
        val progression: Double,
        val status: Status
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

    // --

    private var observationJob: Job? = null

    // ---------------------------

    init {
        startObservation()
    }

    /**
     * Starts observing the wave event if not already started.
     *
     * Launches a coroutine to initialize the last observed status and progression,
     * then calls `observeWave` to begin periodic checks.
     */
    fun startObservation() {
        if (observationJob == null) {
            coroutineScopeProvider.launchDefault {
                Log.v("startObservation", "Starting observation for event $event.id")

                // Initialize state with current values
                _eventStatus.value = event.getStatus()

                try {
                    _progression.value = event.wave.getProgression()
                } catch (e: Throwable) {
                    Log.e(
                        tag = WWWEventWave::class.simpleName!!,
                        message = "Error initializing progression: $e"
                    )
                }

                // Start observation if event is running or about to start
                // Accepted as the application will not be let running for days (isSoon includes a delay)
                if (event.isRunning() || (event.isSoon() && event.isNearTime())) {
                    observationJob = createObservationFlow()
                        .flowOn(Dispatchers.IO)
                        .catch { e ->
                            Log.e("observeWave", "Error in observation flow: $e")
                        }
                        .onEach { (progressionValue, status) ->
                            updateStates(progressionValue, status)
                        }
                        .launchIn(coroutineScopeProvider.scopeDefault())
                }
            }
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
    fun createObservationFlow() = callbackFlow {
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
     */
    private suspend fun updateStates(progression: Double, status: Status) {
        // Update the main state flows
        _progression.updateIfChanged(progression)
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

        // Update additional state flows
        _timeBeforeHit.updateIfChanged(timeBeforeHit)
        _userPositionRatio.updateIfChanged(event.wave.userPositionToWaveRatio() ?: 0.0)
        _hitDateTime.updateIfChanged(event.wave.userHitDateTime() ?: DISTANT_FUTURE)

        // Warming start (between event start and wave start)
        val now = clock.now()
        _isStartWarmingInProgress.updateIfChanged(now > event.getStartDateTime() && now < event.getWaveStartDateTime())

        // User in area
        val userPosition = event.wave.getUserPosition()
        if (userPosition != null) {
            _userIsInArea.updateIfChanged(event.area.isPositionWithin(userPosition))
        } else {
            _userIsInArea.updateIfChanged(false)
        }

        _isUserWarmingInProgress.updateIfChanged(warmingInProgress)
        _userIsGoingToBeHit.updateIfChanged(userIsGoingToBeHit)
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


