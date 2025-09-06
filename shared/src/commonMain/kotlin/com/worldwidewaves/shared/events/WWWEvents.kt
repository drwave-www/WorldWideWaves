package com.worldwidewaves.shared.events

/*
 * Copyright 2025 DrWave
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

import androidx.annotation.VisibleForTesting
import com.worldwidewaves.shared.data.InitFavoriteEvent
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.EventsConfigurationProvider
import com.worldwidewaves.shared.events.utils.EventsDecoder
import com.worldwidewaves.shared.events.utils.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.jvm.JvmOverloads

// ---------------------------

/**
 * Central, injectable repository that owns the **list of World-Wide-Waves
 * events** for the whole application.
 *
 * Responsibilities:
 * • Load and decode the JSON configuration shipped with the app (or downloaded)  
 * • Validate every [IWWWEvent] and expose only the sound ones via
 *   [flow] / [list] accessors  
 * • Persist & restore user-specific flags such as “favorite” through
 *   [InitFavoriteEvent]  
 * • Provide simple callback registration helpers so view-models / screens know
 *   when the catalogue is ready or if loading failed  
 * • Cache the load job behind a mutex to guarantee a single concurrent fetch
 *
 * All UI view-models (e.g. `EventsViewModel`) and Compose screens observe the
 * StateFlow exposed by this service to drive their lists or selectors.
 */
class WWWEvents : KoinComponent {

    private val loadingMutex = Mutex()

    private val initFavoriteEvent: InitFavoriteEvent by inject()
    private val eventsConfigurationProvider: EventsConfigurationProvider by inject()
    private val coroutineScopeProvider: CoroutineScopeProvider by inject()
    private val eventsDecoder : EventsDecoder by inject()

    // ---------------------------

    private var currentLoadJob: Job? = null
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

        // Launch a coroutine to handle the mutex-protected loading
        coroutineScopeProvider.launchIO {
            loadingMutex.withLock {
                // Double-check if events are already loaded after acquiring the lock
                if (!eventsLoaded && loadingError == null) {
                    currentLoadJob = loadEventsJob()
                    currentLoadJob?.join() // Wait for the loading job to complete
                }
            }
        }
    }

    /**
     * Launches a coroutine to load events from the configuration provider.
     * The coroutine runs on the IO dispatcher.
     */
    private fun loadEventsJob() = coroutineScopeProvider.launchIO {
        try {
            val eventsJsonString = eventsConfigurationProvider.geoEventsConfiguration()
            val events = eventsDecoder.decodeFromJson(eventsJsonString)
            val validatedEvents = confValidationErrors(events)

            validatedEvents.filterValues { it?.isNotEmpty() == true } // Log validation errors
                .forEach { (event, errors) ->
                    Log.e(::WWWEvents.name, "Validation Errors for Event ID: ${event.id}")
                    errors?.forEach { errorMessage ->
                        Log.e(::WWWEvents.name, errorMessage)
                    }
                    validationErrors.add(event to errors!!)
                }

            // Filter out invalid events
            val validEvents = validatedEvents.filterValues { it.isNullOrEmpty() }
                .keys.onEach { initFavoriteEvent.call(it) } // Initialize favorite status
                .toList()

            // Update the _eventsFlow in the main dispatcher to ensure thread safety
            withContext(Dispatchers.Main) {
                _eventsFlow.value = validEvents
            }

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
        if (eventsLoaded) callback()
        else if (!pendingLoadedCallbacks.contains(callback))
            pendingLoadedCallbacks.add(callback)
    }

    fun addOnEventsErrorListener(callback: (Exception) -> Unit){
        if (loadingError != null) callback(loadingError!!)
        else if (!pendingErrorCallbacks.contains(callback))
            pendingErrorCallbacks.add(callback)
    }

    fun addOnTerminationListener(callback: (Exception?) -> Unit) {
        if (eventsLoaded || loadingError != null)
            callback(loadingError)
        else {
            addOnEventsLoadedListener { callback(null) }
            addOnEventsErrorListener { callback(it) }
        }
    }

    // --------------------------------------------------------------------
    //  Simulation helpers
    // --------------------------------------------------------------------
    /**
     * Restarts observers for all events that should be actively observed when the
     * simulation context (running simulation or simulation-mode) changes.
     */
    fun restartObserversOnSimulationChange() {
        coroutineScopeProvider.launchDefault {
            list().forEach { event ->
                try {
                    event.observer.stopObservation()
                    event.observer.startObservation()
                } catch (e: Exception) {
                    Log.e("WWWEvents", "Error restarting observer for ${event.id}: $e", e)
                }
            }
        }
    }

}