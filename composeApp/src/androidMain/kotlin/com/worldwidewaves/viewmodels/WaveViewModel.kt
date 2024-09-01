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
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEventWave.WaveMode
import com.worldwidewaves.shared.events.WWWEventWave.WaveNumbersLiterals
import com.worldwidewaves.shared.events.WWWEventWave.WavePolygons
import com.worldwidewaves.shared.generated.resources.geoloc_undone
import com.worldwidewaves.shared.generated.resources.geoloc_warm_in
import com.worldwidewaves.shared.generated.resources.geoloc_yourein
import com.worldwidewaves.shared.generated.resources.geoloc_yourenotin
import com.worldwidewaves.shared.toMapLibrePolygon
import com.worldwidewaves.shared.toPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.maplibre.android.geometry.LatLng
import org.maplibre.geojson.Polygon
import com.worldwidewaves.shared.generated.resources.Res as ShRes

class WaveViewModel : ViewModel() {

    companion object {
        private const val MAX_POLY_RECOMPOSE = 100
    }

    private var event : IWWWEvent? = null
    @Volatile private var observationStarted = false

    private var progressionListenerKey: Int? = null
    private var statusListenerKey: Int? = null

    private val _waveNumbers = MutableStateFlow<WaveNumbersLiterals?>(null)
    val waveNumbers: StateFlow<WaveNumbersLiterals?> = _waveNumbers.asStateFlow()

    private val _eventState = MutableStateFlow(IWWWEvent.Status.UNDEFINED)
    val eventStatus: StateFlow<IWWWEvent.Status> = _eventState.asStateFlow()

    private val _geolocText = MutableStateFlow(ShRes.string.geoloc_undone)
    val geolocText: StateFlow<StringResource> = _geolocText.asStateFlow()

    private var lastWaveState : WavePolygons? = null

    var clearPolygonsBeforeAdd = false

    // ----------------------------

    fun startObservation(
        event: IWWWEvent,
        polygonsHandler: (wavePolygons: List<Polygon>, clearPolygons: Boolean) -> Unit
    ) {
        if (!observationStarted) {
            this.event = event
            viewModelScope.launch(Dispatchers.Default) {
                _waveNumbers.value = event.getAllNumbers()
                _eventState.value = event.getStatus()
                progressionListenerKey = event.addOnWaveProgressionChangedListener {
                    viewModelScope.launch(Dispatchers.Default) {
                        if (_waveNumbers.value == null) {
                            _waveNumbers.value = event.getAllNumbers()
                        } else {
                            _waveNumbers.value = waveNumbers.value?.copy(
                                waveProgression = event.wave.getLiteralProgression()
                            )
                        }
                        updateWavePolygons(polygonsHandler)
                    }
                }
                statusListenerKey = event.addOnStatusChangedListener {
                    viewModelScope.launch(Dispatchers.Default) {
                        _eventState.value = event.getStatus()
                    }
                }
            }
            observationStarted = true
        }
    }

    private fun stopObservation() {
        if (observationStarted) {
            event?.stopListeners(
                listOfNotNull(
                    progressionListenerKey,
                    statusListenerKey
                )
            )
            observationStarted = false
            progressionListenerKey = null
            statusListenerKey = null
        }
    }

    // ----------------------------

    /**
     * Updates the geolocation text based on the new location provided.
     */
    fun updateGeolocationText(newLocation: LatLng) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentEvent = event
            if (currentEvent != null) {
                val currentPosition = newLocation.toPosition()
                val newText = when {
                    currentEvent.warming.area.isPositionWithin(currentPosition) -> ShRes.string.geoloc_warm_in
                    currentEvent.area.isPositionWithin(currentPosition) -> ShRes.string.geoloc_yourein
                    else -> ShRes.string.geoloc_yourenotin
                }
                _geolocText.value = newText
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
    private suspend fun updateWavePolygons(polygonsHandler: (wavePolygons: List<Polygon>, clearPolygons: Boolean) -> Unit) {
        event?.let { event ->
            if (event.isRunning()) try { // FIXME: right setup vs perf to be found
                val mode = WaveMode.ADD

                // lastWaveState = event.wave.getWavePolygons(lastWaveState, mode)
                lastWaveState = event.wave.getWavePolygons(null, mode)

                val polygons =  lastWaveState?.traversedPolygons
                val newPolygons = polygons?.map { it.toMapLibrePolygon() } ?: listOf()

                viewModelScope.launch(Dispatchers.Main) {
                    polygonsHandler(newPolygons, mode != WaveMode.ADD)
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