/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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
package com.worldwidewaves.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldwidewaves.shared.AndroidPlatform
import com.worldwidewaves.shared.events.WWWEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventsViewModel : ViewModel() {

    private var originalEvents : List<WWWEvent> = emptyList()

    private val _hasFavorites = MutableStateFlow(false)
    val hasFavorites: StateFlow<Boolean> = _hasFavorites.asStateFlow()

    private val _events = MutableStateFlow<List<WWWEvent>>(emptyList())
    val events: StateFlow<List<WWWEvent>> = _events.asStateFlow()

    init {
        loadEvents()
    }

    // ---------------------------

    private fun loadEvents() {
        viewModelScope.launch {
            AndroidPlatform.getEvents().eventsFlow.collect { eventsList ->
                originalEvents = eventsList
                _events.value = eventsList
                _hasFavorites.value = eventsList.any { it.favorite }
            }
        }
    }

    // ---------------------------

    fun filterFavoriteEvents() {
        _events.value = originalEvents.filter { it.favorite }
    }

    fun filterAllEvents() {
        _events.value = originalEvents
    }

    fun filterEvents(starredSelected: Boolean) {
        if (starredSelected) {
            filterFavoriteEvents()
        } else {
            filterAllEvents()
        }
    }

}
