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
import com.worldwidewaves.shared.events.WWWEventWave
import com.worldwidewaves.shared.events.WWWEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

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

    private val originalEventsMutex = Mutex()
    private var originalEvents: List<IWWWEvent> = emptyList()

    private val observationStartedMutex = Mutex()
    private var observationStarted = AtomicBoolean(false)

    private val statusListenerKeysMutex = Mutex()
    private var statusListenerKeys = mutableMapOf<Int, IWWWEvent>()

    private val _hasFavorites = MutableStateFlow(false)
    val hasFavorites: StateFlow<Boolean> = _hasFavorites.asStateFlow()

    private val _events = MutableStateFlow<List<IWWWEvent>>(emptyList())
    val events: StateFlow<List<IWWWEvent>> = _events.asStateFlow()

    private val loadingError = MutableStateFlow(false)
    val hasLoadingError: StateFlow<Boolean> = loadingError.asStateFlow()

    private val _eventState = ConcurrentHashMap<String, MutableStateFlow<IWWWEvent.Status>>()
    val eventStatus: Map<String, StateFlow<IWWWEvent.Status>> get() = _eventState

    init {
        loadEvents { exception -> // Error management
            Log.e(EventsViewModel::class.simpleName, "Error loading events", exception)
            loadingError.value = true
        }
    }

    // ---------------------------

    private fun loadEvents(onLoadingError: ((Exception) -> Unit)? = null) =
        wwwEvents.loadEvents(onLoadingError = onLoadingError).also {
            viewModelScope.launch(Dispatchers.Default) {
                wwwEvents.flow().collect { eventsList ->
                    val sortedEvents = eventsList.sortedBy { it.getStartDateTime() }
                    originalEventsMutex.withLock {
                        originalEvents = sortedEvents
                    }
                    _events.value = sortedEvents
                    _hasFavorites.value = sortedEvents.any(IWWWEvent::favorite)
                    startObservation()
                }
            }
        }

    // ---------------------------

    fun filterEvents(onlyFavorites: Boolean) {
        viewModelScope.launch(Dispatchers.Default) {
            originalEventsMutex.withLock {
                _events.value = originalEvents.filter { !onlyFavorites || it.favorite }
            }
        }
    }

    // ---------------------------

    private suspend fun updateEventStatus(event: IWWWEvent) {
        val newStatus = event.getStatus()
        _eventState[event.id]?.emit(newStatus)
    }

    private fun startObservation() {
        viewModelScope.launch(Dispatchers.Default) {
            observationStartedMutex.withLock {
                if (observationStarted.compareAndSet(false, true)) {
                    originalEvents.forEach { event ->
                        viewModelScope.launch(Dispatchers.Default) {
                            val initialStatus = event.getStatus()
                            _eventState.getOrPut(event.id) { MutableStateFlow(initialStatus) }
                                .emit(initialStatus)

                            val key = event.addOnStatusChangedListener {
                                viewModelScope.launch(Dispatchers.Default) {
                                    updateEventStatus(event)
                                }
                            }
                            statusListenerKeysMutex.withLock {
                                statusListenerKeys[key] = event
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun stopObservation() {
        if (observationStarted.compareAndSet(true, false)) {
            statusListenerKeysMutex.withLock {
                statusListenerKeys.forEach { (key, event) ->
                    event.stopListeners(key)
                }
                statusListenerKeys.clear()
            }
        }
    }

    // ----------------------------

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.Default) {
            stopObservation()
        }
    }

}
