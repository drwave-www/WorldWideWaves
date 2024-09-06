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

import androidx.annotation.VisibleForTesting
import com.worldwidewaves.shared.data.InitFavoriteEvent
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.EventsConfigurationProvider
import com.worldwidewaves.shared.events.utils.EventsDecoder
import com.worldwidewaves.shared.events.utils.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// ---------------------------

class WWWEvents : KoinComponent {

    private val initFavoriteEvent: InitFavoriteEvent by inject()
    private val eventsConfigurationProvider: EventsConfigurationProvider by inject()
    private val coroutineScopeProvider: CoroutineScopeProvider by inject()
    private val eventsDecoder : EventsDecoder by inject()

    // ---------------------------

    private var eventsLoaded: Boolean = false
    private val pendingCallbacks = mutableListOf<() -> Unit>()

    private val _eventsFlow = MutableStateFlow<List<WWWEvent>>(emptyList())
    private val eventsFlow = _eventsFlow.asStateFlow()

    // ---------------------------

    /**
     * Initiates the loading of events if not already started.
     */
    fun loadEvents(callback: (() -> Unit)? = null): WWWEvents = apply {
        callback?.let { addOnEventsLoadedListener(it) }
        if (!eventsLoaded) {
            loadEventsJob()
        }
    }

    /**
     * Launches a coroutine to load events from the configuration provider.
     * The coroutine runs on the IO dispatcher.
     */
    private fun loadEventsJob() = coroutineScopeProvider.scopeIO.launch {
        val eventsJsonString = eventsConfigurationProvider.geoEventsConfiguration()

        try {
            val events = eventsDecoder.decodeFromJson(eventsJsonString)
            val validationErrors = eventsConfValidationErrors(events)

            validationErrors.filterValues { it?.isEmpty() == false }
                .mapNotNull { it.value }
                .forEach { errorMessage ->
                    Log.e(::WWWEvents.name, "Validation Error: $errorMessage")
                }

            _eventsFlow.value = validationErrors.filterValues { it == null }
                .keys.onEach { initFavoriteEvent.call(it) }
                .toList()

            // The events have been loaded, so we can now call any pending callbacks
            onEventsLoaded()

        } catch (e: Exception) {
            Log.e(::WWWEvents.name, "Unexpected error loading events: ${e.message}", e)
        }
    }

    @VisibleForTesting
    fun eventsConfValidationErrors(events: List<WWWEvent>) =
        events.associateWith(WWWEvent::validationErrors)

    // ---------------------------

    fun flow(): StateFlow<List<WWWEvent>> = eventsFlow
    fun list(): List<WWWEvent> = eventsFlow.value
    fun getEventById(id: String): WWWEvent? = eventsFlow.value.find { it.id == id }

    // ---------------------------

    @VisibleForTesting
    fun onEventsLoaded() {
        eventsLoaded = true
        pendingCallbacks.onEach { callback -> callback.invoke() }.clear()
    }

    fun addOnEventsLoadedListener(callback: () -> Unit) {
        if (eventsLoaded) callback() else pendingCallbacks.add(callback)
    }

}