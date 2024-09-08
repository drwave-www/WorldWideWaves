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
import kotlin.jvm.JvmOverloads

// ---------------------------

class WWWEvents : KoinComponent {

    private val initFavoriteEvent: InitFavoriteEvent by inject()
    private val eventsConfigurationProvider: EventsConfigurationProvider by inject()
    private val coroutineScopeProvider: CoroutineScopeProvider by inject()
    private val eventsDecoder : EventsDecoder by inject()

    // ---------------------------

    private var eventsLoaded: Boolean = false
    private var loadingError: Exception? = null
    private val validationErrors = mutableListOf<Pair<IWWWEvent, List<String>>>()

    private val pendingLoadedCallbacks = mutableListOf<() -> Unit>()
    private val pendingErrorCallbacks = mutableListOf<(Exception) -> Unit>()

    private val _eventsFlow = MutableStateFlow<List<IWWWEvent>>(emptyList())
    private val eventsFlow = _eventsFlow.asStateFlow()

    // ---------------------------

    /**
     * Initiates the loading of events if not already started.
     */
    @JvmOverloads
    fun loadEvents(
        onLoaded: (() -> Unit)? = null,
        onLoadingError: ((Exception) -> Unit)? = null,
        onTermination: ((Exception?) -> Unit)? = null
    ): WWWEvents = apply {

        onLoaded?.let { addOnEventsLoadedListener(it) }
        onLoadingError?.let { addOnEventsErrorListener(it) }
        onTermination?.let { addOnTerminationListener(it) }

        if (!eventsLoaded) {
            loadEventsJob()
        }
    }

    /**
     * Launches a coroutine to load events from the configuration provider.
     * The coroutine runs on the IO dispatcher.
     */
    private fun loadEventsJob() = coroutineScopeProvider.scopeIO.launch {
        try {
            val eventsJsonString = eventsConfigurationProvider.geoEventsConfiguration()
            val events = eventsDecoder.decodeFromJson(eventsJsonString)
            val validatedEvents = confValidationErrors(events)

            validatedEvents.filterValues { it?.isEmpty() == false } // Log validation errors
                .onEach { (event, errors) ->
                    validationErrors.add(event to errors!!)
                }
                .values
                .forEach { errorMessage ->
                    Log.e(::WWWEvents.name, "Validation Error: $errorMessage")
                }


            // Filter out invalid events
            _eventsFlow.value = validatedEvents.filterValues { it.isNullOrEmpty() }
                .keys.onEach { initFavoriteEvent.call(it) } // Initialize favorite status
                .toList()

            // The events have been loaded, so we can now call any pending callbacks
            onEventsLoaded()

        } catch (e: Exception) {
            Log.e(::WWWEvents.name, "Unexpected error loading events: ${e.message}", e)
            onLoadingError(e)
        }
    }

    @VisibleForTesting
    fun confValidationErrors(events: List<IWWWEvent>) =
        events.associateWith(IWWWEvent::validationErrors)

    // ---------------------------

    fun flow(): StateFlow<List<IWWWEvent>> = eventsFlow
    fun list(): List<IWWWEvent> = eventsFlow.value
    fun getEventById(id: String): IWWWEvent? = eventsFlow.value.find { it.id == id }

    fun isLoaded(): Boolean = eventsLoaded
    fun getLoadingError(): Exception? = loadingError
    fun getValidationErrors(): List<Pair<IWWWEvent, List<String>>> = validationErrors

    // ---------------------------

    @VisibleForTesting
    fun onEventsLoaded() {
        eventsLoaded = true
        pendingLoadedCallbacks.onEach { callback -> callback.invoke() }.clear()
    }

    @VisibleForTesting
    fun onLoadingError(exception: Exception) {
        loadingError = exception
        pendingErrorCallbacks.onEach { callback -> callback.invoke(exception) }.clear()
    }

    fun addOnEventsLoadedListener(callback: () -> Unit) {
        if (eventsLoaded) callback() else pendingLoadedCallbacks.add(callback)
    }

    fun addOnEventsErrorListener(callback: (Exception) -> Unit){
        if (loadingError != null) callback(loadingError!!)
        else pendingErrorCallbacks.add(callback)
    }

    fun addOnTerminationListener(callback: (Exception?) -> Unit) {
        if (eventsLoaded || loadingError != null)
            callback(loadingError)
        else {
            addOnEventsLoadedListener { callback(null) }
            addOnEventsErrorListener { callback(it) }
        }
    }

}