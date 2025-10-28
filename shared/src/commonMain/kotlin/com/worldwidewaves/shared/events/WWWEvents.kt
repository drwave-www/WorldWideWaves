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

import com.worldwidewaves.shared.data.InitFavoriteEvent
import com.worldwidewaves.shared.events.config.EventsConfigurationProvider
import com.worldwidewaves.shared.events.decoding.EventsDecoder
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.utils.PerformanceTracer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.jvm.JvmOverloads
import kotlin.time.ExperimentalTime

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
    companion object {
        private const val JSON_PREVIEW_LENGTH = 200
    }

    private val loadingMutex = Mutex()

    private val initFavoriteEvent: InitFavoriteEvent by inject()
    private val eventsConfigurationProvider: EventsConfigurationProvider by inject()
    private val coroutineScopeProvider: CoroutineScopeProvider by inject()
    private val eventsDecoder: EventsDecoder by inject()

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
        onTermination: ((Exception?) -> Unit)? = null,
    ): WWWEvents =
        apply {
            onLoaded?.let { addOnEventsLoadedListener(it) }
            onLoadingError?.let { addOnEventsErrorListener(it) }
            onTermination?.let { addOnTerminationListener(it) }

            // Launch a coroutine to handle the mutex-protected loading
            coroutineScopeProvider.launchIO {
                // Check if we need to start loading (outside mutex for quick check)
                val needsLoading = !eventsLoaded && loadingError == null && currentLoadJob == null

                if (needsLoading) {
                    loadingMutex.withLock {
                        // Double-check if events are already loaded after acquiring the lock
                        if (!eventsLoaded && loadingError == null && currentLoadJob == null) {
                            currentLoadJob = loadEventsJob()
                        }
                    }

                    // Wait for the job to complete outside the mutex to prevent deadlock
                    currentLoadJob?.join()
                }
            }
        }

    /**
     * Launches a coroutine to load events from the configuration provider.
     * The coroutine runs on the IO dispatcher.
     */
    private fun loadEventsJob() =
        coroutineScopeProvider.launchIO {
            val trace = PerformanceTracer.startTrace("event_loading")
            try {
                Log.i("WWWEvents.loadEventsJob", "=== STARTING loadEventsJob() ===")
                Log.i("WWWEvents.loadEventsJob", "Calling eventsConfigurationProvider.geoEventsConfiguration()...")

                val eventsJsonString: String = eventsConfigurationProvider.geoEventsConfiguration()
                Log.i("WWWEvents.loadEventsJob", "Received JSON string: ${eventsJsonString.length} characters")
                Log.i("WWWEvents.loadEventsJob", "JSON preview: ${eventsJsonString.take(JSON_PREVIEW_LENGTH)}")
                trace.putMetric("json_size_bytes", eventsJsonString.length.toLong())

                Log.i("WWWEvents.loadEventsJob", "Decoding JSON to events...")
                val events: List<IWWWEvent> = eventsDecoder.decodeFromJson(eventsJsonString)
                Log.i("WWWEvents.loadEventsJob", "Successfully decoded ${events.size} events")
                trace.putMetric("events_decoded", events.size.toLong())
                Log.i("WWWEvents.loadEventsJob", "Running validation on decoded events...")

                // Restore proper validation but with error handling
                val validatedEvents =
                    try {
                        confValidationErrors(events)
                    } catch (e: Exception) {
                        Log.e("WWWEvents.loadEventsJob", "Error during validation: ${e.message}")
                        // Fall back to no validation if validation itself crashes
                        events.associateWith { null }
                    }

                validatedEvents
                    .filterValues { it?.isNotEmpty() == true } // Log validation errors
                    .forEach { (event, errors) ->
                        // errors is guaranteed non-null and non-empty by filterValues above
                        val errorList = errors ?: return@forEach
                        Log.e("WWWEvents.loadEventsJob", "Validation Errors for Event ID: ${event.id}")
                        errorList.forEach { errorMessage ->
                            Log.e("WWWEvents.loadEventsJob", errorMessage)
                        }
                        validationErrors.add(event to errorList)
                    }

                // Filter out invalid events and initialize favorites
                val validEvents =
                    validatedEvents
                        .filterValues { it.isNullOrEmpty() }
                        .keys
                        .onEach {
                            try {
                                initFavoriteEvent.call(it)
                                Log.d("WWWEvents.loadEventsJob", "Initialized favorite for event: ${it.id}")
                            } catch (e: Exception) {
                                Log.e("WWWEvents.loadEventsJob", "Error initializing favorite for event ${it.id}: ${e.message}")
                            }
                        }.toList()

                Log.i("WWWEvents.loadEventsJob", "After validation: ${validEvents.size} valid events out of ${events.size} total")
                trace.putMetric("events_valid", validEvents.size.toLong())
                trace.putMetric("events_invalid", (events.size - validEvents.size).toLong())

                Log.i("WWWEvents.loadEventsJob", "About to update events flow with ${validEvents.size} events")

                // Update the _eventsFlow directly (StateFlow is thread-safe)
                try {
                    _eventsFlow.value = validEvents
                    Log.i("WWWEvents.loadEventsJob", "Successfully updated events flow")
                } catch (e: Exception) {
                    Log.e("WWWEvents.loadEventsJob", "Error updating events flow: ${e.message}")
                    throw e
                }

                Log.i("WWWEvents.loadEventsJob", "About to call onEventsLoaded()")
                // The events have been loaded, so we can now call any pending callbacks
                try {
                    onEventsLoaded()
                    Log.i("WWWEvents.loadEventsJob", "Successfully called onEventsLoaded()")
                } catch (e: Exception) {
                    Log.e("WWWEvents.loadEventsJob", "Error in onEventsLoaded(): ${e.message}")
                    throw e
                }
            } catch (e: Exception) {
                Log.e(::WWWEvents.name, "Unexpected error loading events: ${e.message}", e)
                trace.putMetric("loading_error", 1)
                onLoadingError(e)
            } finally {
                trace.stop()
            }
        }

    fun confValidationErrors(events: List<IWWWEvent>) = events.associateWith(IWWWEvent::validationErrors)

    // ---------------------------

    fun flow(): StateFlow<List<IWWWEvent>> = eventsFlow

    fun list(): List<IWWWEvent> = eventsFlow.value

    fun getEventById(id: String): IWWWEvent? = eventsFlow.value.find { it.id == id }

    fun isLoaded(): Boolean = eventsLoaded

    fun getLoadingError(): Exception? = loadingError

    fun getValidationErrors(): List<Pair<IWWWEvent, List<String>>> = validationErrors

    // ---------------------------

    fun onEventsLoaded() {
        Log.i(::WWWEvents.name, "Events loaded successfully. Calling ${pendingLoadedCallbacks.size} pending callbacks")
        eventsLoaded = true
        pendingLoadedCallbacks
            .onEach { callback ->
                try {
                    callback.invoke()
                    Log.d(::WWWEvents.name, "Successfully called events loaded callback")
                } catch (e: Exception) {
                    Log.e(::WWWEvents.name, "Error calling events loaded callback: ${e.message}", e)
                }
            }.clear()
    }

    fun onLoadingError(exception: Exception) {
        loadingError = exception
        pendingErrorCallbacks.onEach { callback -> callback.invoke(exception) }.clear()
    }

    fun addOnEventsLoadedListener(callback: () -> Unit) {
        if (eventsLoaded) {
            callback()
        } else if (!pendingLoadedCallbacks.contains(callback)) {
            pendingLoadedCallbacks.add(callback)
        }
    }

    fun addOnEventsErrorListener(callback: (Exception) -> Unit) {
        loadingError?.let { error ->
            callback(error)
        } ?: run {
            if (!pendingErrorCallbacks.contains(callback)) {
                pendingErrorCallbacks.add(callback)
            }
        }
    }

    fun addOnTerminationListener(callback: (Exception?) -> Unit) {
        if (eventsLoaded || loadingError != null) {
            callback(loadingError)
        } else {
            addOnEventsLoadedListener { callback(null) }
            addOnEventsErrorListener { callback(it) }
        }
    }

    // --------------------------------------------------------------------
    //  Lifecycle Management
    // --------------------------------------------------------------------

    /**
     * Cleanup method to prevent memory leaks.
     * Cancels the current load job and clears internal state.
     *
     * ## When to Call
     * This method should be called during app shutdown lifecycle:
     * - **Android**: In `MainApplication.onTerminate()` via WWWShutdownHandler
     * - **iOS**: In `AppDelegate.applicationWillTerminate()` or SceneDelegate cleanup
     * - **Tests**: In `@AfterTest` tearDown methods
     *
     * ## What it Does
     * - Cancels the ongoing `currentLoadJob` if still running
     * - Prevents memory leaks from long-lived coroutines
     * - Ensures clean shutdown of event loading infrastructure
     *
     * ## Thread Safety
     * This method is thread-safe and can be called from any thread.
     */
    fun cleanup() {
        Log.v("WWWEvents.cleanup", "Cancelling current load job if running")
        currentLoadJob?.cancel()
        currentLoadJob = null
        Log.v("WWWEvents.cleanup", "Cleanup complete")
    }

    // --------------------------------------------------------------------
    //  Simulation helpers
    // --------------------------------------------------------------------

    /**
     * Restarts observers for all events when the simulation context changes.
     * Multiple events can be active simultaneously for the same user position.
     */
    @OptIn(ExperimentalTime::class)
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
