package com.worldwidewaves.shared.events

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

import com.worldwidewaves.shared.InitFavoriteEvent
import com.worldwidewaves.shared.events.utils.EventsConfigurationProvider
import com.worldwidewaves.shared.events.utils.ICoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// ---------------------------

class WWWEvents : KoinComponent {

    private val initFavoriteEvent: InitFavoriteEvent by inject()
    private val eventsConfigurationProvider: EventsConfigurationProvider by inject()
    private val coroutineScopeProvider: ICoroutineScopeProvider by inject()

    // ---------------------------

    private var loadJob: Job? = null
    private val jsonDecoder = Json { ignoreUnknownKeys = true }
    private val _eventsFlow = MutableStateFlow<List<WWWEvent>>(emptyList())
    private val eventsFlow = _eventsFlow.asStateFlow()

    fun resetEventsFlow() = _eventsFlow::value.set(emptyList())

    // ---------------------------

    init { loadEvents() }

    // ---------------------------

    /**
     * Initiates the loading of events if not already started.
     */
    private fun loadEvents() = loadJob ?: loadEventsJob().also { loadJob = it }

    /**
     * Launches a coroutine to load events from the configuration provider.
     * The coroutine runs on the IO dispatcher.
     */
    private fun loadEventsJob() = coroutineScopeProvider.scopeIO.launch {
        val eventsJsonString = eventsConfigurationProvider.geoEventsConfiguration()
        val events = jsonDecoder.decodeFromString<List<WWWEvent>>(eventsJsonString)

        val validationErrors = eventsConfValidationErrors(events)

        validationErrors.filterValues { it?.isEmpty() == false }
            .mapNotNull { it.value }
            .forEach { errorMessage ->
                Log.e(::WWWEvents.name, "Validation Error: $errorMessage")
            }

        _eventsFlow.value = validationErrors.filterValues { it == null }
            .keys.onEach { initFavoriteEvent.call(it) }
            .toList()
    }

    // ---------------------------

    fun flow(): StateFlow<List<WWWEvent>> = eventsFlow
    fun events(): List<WWWEvent> = eventsFlow.value
    fun getEventById(id: String): WWWEvent? = eventsFlow.value.find { it.id == id }
    fun onEventLoaded(function: () -> Job) = this.loadJob?.invokeOnCompletion { function() }

    // ---------------------------

    private fun eventsConfValidationErrors(events: List<WWWEvent>) = events.associateWith(WWWEvent::validationErrors)

}