package com.worldwidewaves.viewmodels

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_SHOW_HIT_SEQUENCE_SECONDS
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEventWave.WaveMode
import com.worldwidewaves.shared.events.WWWEventWave.WaveNumbersLiterals
import com.worldwidewaves.shared.events.WWWEventWave.WavePolygons
import com.worldwidewaves.shared.toMapLibrePolygon
import com.worldwidewaves.shared.toPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.maplibre.android.geometry.LatLng
import org.maplibre.geojson.Polygon
import kotlin.time.Duration

class WaveViewModel(private val platform: WWWPlatform) : ViewModel() {

    companion object {
        private const val MAX_POLY_RECOMPOSE = 100
    }

    private var event : IWWWEvent? = null
    @Volatile private var observationStarted = false

    private var progressionListenerKey: Int? = null
    private var statusListenerKey: Int? = null
    private var userWarmingStartedListenerKey: Int? = null
    private var userGoingToBeHitListenerKey: Int? = null
    private var userHasBeenHitListenerKey: Int? = null

    private val _waveNumbers = MutableStateFlow<WaveNumbersLiterals?>(null)
    val waveNumbers: StateFlow<WaveNumbersLiterals?> = _waveNumbers.asStateFlow()

    private val _eventState = MutableStateFlow(IWWWEvent.Status.UNDEFINED)
    val eventStatus: StateFlow<IWWWEvent.Status> = _eventState.asStateFlow()

    private val _progression = MutableStateFlow(0.0)
    val progression: StateFlow<Double> = _progression.asStateFlow()

    private val _userPositionRatio = MutableStateFlow(0.0)
    val userPositionRatio: StateFlow<Double> = _userPositionRatio.asStateFlow()

    private val _isInArea = MutableStateFlow(false)
    val isInArea: StateFlow<Boolean> = _isInArea.asStateFlow()

    private val _isWarmingInProgress = MutableStateFlow(false)
    val isWarmingInProgress: StateFlow<Boolean> = _isWarmingInProgress.asStateFlow()

    private val _isGoingToBeHit = MutableStateFlow(false)
    val isGoingToBeHit: StateFlow<Boolean> = _isGoingToBeHit.asStateFlow()

    private val _hasBeenHit = MutableStateFlow(false)
    val hasBeenHit: StateFlow<Boolean> = _hasBeenHit.asStateFlow()

    private val _hitDateTime = MutableStateFlow(Instant.DISTANT_FUTURE)
    val hitDateTime: StateFlow<Instant> = _hitDateTime.asStateFlow()

    private val _timeBeforeHit = MutableStateFlow(Duration.INFINITE)
    val timeBeforeHit: StateFlow<Duration> = _timeBeforeHit.asStateFlow()

    private var lastWaveState : WavePolygons? = null

    var clearPolygonsBeforeAdd = false

    // ----------------------------

    fun startObservation(
        event: IWWWEvent,
        polygonsHandler: ((wavePolygons: List<Polygon>, clearPolygons: Boolean) -> Unit)? = null
    ) {
        if (!observationStarted) {
            this.event = event
            viewModelScope.launch(Dispatchers.Default) {

                _progression.value = event.wave.getProgression()
                _userPositionRatio.value = event.wave.userPositionToWaveRatio() ?: 0.0
                _timeBeforeHit.value = event.wave.timeBeforeUserHit() ?: Duration.INFINITE
                _waveNumbers.value = event.getAllNumbers()
                _hitDateTime.value = event.wave.userHitDateTime() ?: Instant.DISTANT_FUTURE

                _eventState.value = event.getStatus()
                _isWarmingInProgress.value = event.warming.isUserWarmingStarted()
                _isGoingToBeHit.value = event.wave.userIsGoingToBeHit()
                _hasBeenHit.value = event.wave.hasUserBeenHitInCurrentPosition()

            }.invokeOnCompletion {

                progressionListenerKey = event.addOnWaveProgressionChangedListener {
                    viewModelScope.launch(Dispatchers.Default) {
                        _progression.value = event.wave.getProgression()
                        _userPositionRatio.value = event.wave.userPositionToWaveRatio() ?: 0.0
                        _timeBeforeHit.value = event.wave.timeBeforeUserHit() ?: Duration.INFINITE
                        _hitDateTime.value = event.wave.userHitDateTime() ?: Instant.DISTANT_FUTURE

                        if (_waveNumbers.value == null) {
                            _waveNumbers.value = event.getAllNumbers()
                        } else {
                            _waveNumbers.value = waveNumbers.value?.copy(
                                waveProgression = event.wave.getLiteralProgression()
                            )
                        }
                    }
                    viewModelScope.launch(Dispatchers.Default) {
                        updateWavePolygons(polygonsHandler)
                    }
                }

                statusListenerKey = event.addOnStatusChangedListener {
                    viewModelScope.launch(Dispatchers.Default) {
                        _eventState.value = event.getStatus()
                    }
                }

                userWarmingStartedListenerKey = event.addOnWarmingStartedListener {
                    platform.disableSimulation() // Disable simulation during the wave warming and hit sequence
                    _isWarmingInProgress.value = true
                }
                userGoingToBeHitListenerKey = event.addOnUserIsGoingToBeHitListener {
                    _isGoingToBeHit.value = true
                    _isWarmingInProgress.value = false
                }
                userHasBeenHitListenerKey = event.addOnUserHasBeenHitListener {
                    _hasBeenHit.value = true
                    _isWarmingInProgress.value = false
                    _isGoingToBeHit.value = false
                    viewModelScope.launch(Dispatchers.Default) {
                        delay(WAVE_SHOW_HIT_SEQUENCE_SECONDS.inWholeSeconds)
                        platform.restartSimulation() // Reactivate simulation after the hit sequence
                    }
                }

            }
            observationStarted = true
        }
    }

    internal fun stopObservation() {
        if (observationStarted) {
            event?.stopListeners(
                listOfNotNull(
                    progressionListenerKey,
                    statusListenerKey,
                    userWarmingStartedListenerKey,
                    userGoingToBeHitListenerKey ,
                    userHasBeenHitListenerKey
                )
            )

            observationStarted = false

            progressionListenerKey = null
            statusListenerKey = null
            userWarmingStartedListenerKey = null
            userGoingToBeHitListenerKey = null
            userHasBeenHitListenerKey = null
        }
    }

    // ----------------------------

    /**
     * Updates the geolocation text based on the new location provided.
     */
    fun updateGeolocation(newLocation: LatLng) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentEvent = event
            if (currentEvent != null) {
                val currentPosition = newLocation.toPosition()
                if (currentEvent.area.isPositionWithin(currentPosition)) {
                    _isInArea.value = true
                }
            }
        }
    }

    // ----------------------------

    /**
     * Updates the wave polygons based on the current wave state.
     *
     * This function determines the mode (ADD or RECOMPOSE) based on the number of polygons
     * and updates the `lastWaveState` with the new wave polygons. It then converts the polygons
     * to MapLibre polygons and updates the state flows.
     *
     */
    private suspend fun updateWavePolygons(polygonsHandler: ((wavePolygons: List<Polygon>, clearPolygons: Boolean) -> Unit)?) {
        event?.let { event ->
            if (event.isRunning()) try { // FIXME: right setup vs perf to be found
                val mode = WaveMode.ADD

                // lastWaveState = event.wave.getWavePolygons(lastWaveState, mode)
                lastWaveState = event.wave.getWavePolygons(null, mode)

                val polygons =  lastWaveState?.traversedPolygons
                val newPolygons = polygons?.map { it.toMapLibrePolygon() } ?: listOf()

                viewModelScope.launch(Dispatchers.Main) {
                    polygonsHandler?.invoke(newPolygons, mode != WaveMode.ADD)
                }

            } catch (e: Exception) {
                Log.e(WaveViewModel::class.simpleName, "updateWavePolygons: ${e.message}")
            }
        }
    }

    // ----------------------------

    override fun onCleared() {
        super.onCleared()
        stopObservation()
    }
}