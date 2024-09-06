package com.worldwidewaves.models

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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.WWWEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing event data.
 *
 * This ViewModel fetches events from a data source (`WWWEvents`), exposes them as a
 * `StateFlow`, and provides filtering functionality for displaying all events or only
 * favorite events.
 *
 * @param wwwEvents The data source for retrieving events.
 */
class EventsViewModel(private val wwwEvents: WWWEvents) : ViewModel() {

    private var originalEvents: List<WWWEvent> = emptyList()

    private val _hasFavorites = MutableStateFlow(false)
    val hasFavorites: StateFlow<Boolean> = _hasFavorites.asStateFlow()

    private val _events = MutableStateFlow<List<WWWEvent>>(emptyList())
    val events: StateFlow<List<WWWEvent>> = _events.asStateFlow()

    private val loadingError = MutableStateFlow(false)
    val hasLoadingError: StateFlow<Boolean> = loadingError.asStateFlow()

    init {
        loadEvents {
            loadingError.value = true
        }
    }

    // ---------------------------

    private fun loadEvents(onLoadingError: ((Exception) -> Unit)? = null) =
        wwwEvents.loadEvents(onLoadingError = onLoadingError).also {
            viewModelScope.launch(Dispatchers.IO) {
                wwwEvents.flow().collect { eventsList ->
                    originalEvents = eventsList
                    _events.value = eventsList
                    _hasFavorites.value = eventsList.any(WWWEvent::favorite)
                }
            }
        }

    // ---------------------------

    fun filterEvents(onlyFavorites: Boolean) {
        _events.value = originalEvents.filter { !onlyFavorites || it.favorite }
    }

}
