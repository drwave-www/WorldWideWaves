package com.worldwidewaves.models

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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEventWave.WaveNumbersLiterals
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.generated.resources.geoloc_undone
import com.worldwidewaves.shared.generated.resources.geoloc_warm_in
import com.worldwidewaves.shared.generated.resources.geoloc_yourein
import com.worldwidewaves.shared.generated.resources.geoloc_yourenotin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.maplibre.android.geometry.LatLng
import com.worldwidewaves.shared.generated.resources.Res as ShRes

class WaveViewModel : ViewModel() {

    private var event : IWWWEvent? = null
    private var observationStarted = false

    private var progressionListenerKey: Int? = null
    private var statusListenerKey: Int? = null

    private val _waveNumbers = MutableStateFlow<WaveNumbersLiterals?>(null)
    val waveNumbers: StateFlow<WaveNumbersLiterals?> = _waveNumbers.asStateFlow()

    private val _eventState = MutableStateFlow(IWWWEvent.Status.UNDEFINED)
    val eventStatus: StateFlow<IWWWEvent.Status> = _eventState.asStateFlow()

    private val _geolocText = MutableStateFlow(ShRes.string.geoloc_undone)
    val geolocText: StateFlow<StringResource> = _geolocText.asStateFlow()

    // ----------------------------

    fun startObservation(event : IWWWEvent) {
        if (!observationStarted) {
            this.event = event
            viewModelScope.launch(Dispatchers.Default) {
                _waveNumbers.value = event.wave.getAllNumbers()
                _eventState.value = event.getStatus()
                progressionListenerKey = event.wave.addOnWaveProgressionChangedListener {
                    viewModelScope.launch(Dispatchers.Default) {
                        if (_waveNumbers.value == null) {
                            _waveNumbers.value = event.wave.getAllNumbers()
                        } else {
                            _waveNumbers.value = waveNumbers.value?.copy(
                                waveProgression = event.wave.getLiteralProgression()
                            )
                        }
                    }
                }
                statusListenerKey = event.wave.addOnWaveStatusChangedListener {
                    viewModelScope.launch(Dispatchers.Default) {
                        _eventState.value = event.getStatus()
                    }
                }
            }
            observationStarted = true
        }
    }

    fun stopObservation() {
        if (observationStarted) {
            event?.wave?.stopListeners(listOfNotNull(
                progressionListenerKey,
                statusListenerKey
            ))
            observationStarted = false
        }
    }

    // ----------------------------

    fun updateGeolocationText(newLocation: LatLng) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentEvent = event
            if (currentEvent != null) {
                val currentPosition = Position(newLocation.latitude, newLocation.longitude)
                val newText = when {
                    currentEvent.wave.warming.area.isPositionWithin(currentPosition) -> ShRes.string.geoloc_warm_in
                    currentEvent.area.isPositionWithin(currentPosition) -> ShRes.string.geoloc_yourein
                    else -> ShRes.string.geoloc_yourenotin
                }
                _geolocText.value = newText
            }
        }
    }

    // ----------------------------

    override fun onCleared() {
        super.onCleared()
        stopObservation()
    }
}