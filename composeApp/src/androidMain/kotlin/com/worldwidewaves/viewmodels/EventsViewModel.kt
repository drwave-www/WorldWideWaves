package com.worldwidewaves.viewmodels

/*
 * Copyright 2024 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.utils.MapAvailabilityChecker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * ViewModel for managing event data.
 *
 * This ViewModel fetches events from a data source (`WWWEvents`), exposes them as a
 * `StateFlow`, and provides filtering functionality for displaying all events or only
 * favorite events.
 */
class EventsViewModel(
    private val wwwEvents: WWWEvents,
    private val mapChecker: MapAvailabilityChecker
) : ViewModel() {

    private val originalEventsMutex = Mutex()
    private var originalEvents: List<IWWWEvent> = emptyList()
    private val eventObservationJobs = mutableMapOf<String, Job>()
    private val eventStatusFlowCache = mutableMapOf<String, StateFlow<IWWWEvent.Status>>()

    // State flows
    private val _hasFavorites = MutableStateFlow(false)
    val hasFavorites: StateFlow<Boolean> = _hasFavorites.asStateFlow()

    private val _events = MutableStateFlow<List<IWWWEvent>>(emptyList())
    val events: StateFlow<List<IWWWEvent>> = _events.asStateFlow()

    private val _loadingError = MutableStateFlow(false)
    val hasLoadingError: StateFlow<Boolean> = _loadingError.asStateFlow()

    // Event status mapping
    private val _eventStatusMap = MutableStateFlow<Map<String, IWWWEvent.Status>>(emptyMap())

    // Exception handler for coroutines
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(::EventsViewModel.name, "Coroutine error: ${throwable.message}", throwable)
        if (throwable !is CancellationException) {
            _loadingError.value = true
        }
    }

    // ---------------------------

    init {
        loadEvents()
    }

    // ---------------------------

    /**
     * Load events from the data source
     */
    private fun loadEvents() {
        viewModelScope.launch(Dispatchers.Default + exceptionHandler) {
            try {
                wwwEvents.loadEvents(
                    onLoadingError = { exception ->
                        Log.e(::EventsViewModel.name, "Error loading events", exception)
                        _loadingError.value = true
                    }
                )

                // Collect the events flow
                wwwEvents.flow()
                    .onEach { eventsList ->
                        processEventsList(eventsList)
                    }
                    .flowOn(Dispatchers.Default)
                    .launchIn(viewModelScope)
            } catch (e: Exception) {
                Log.e(::EventsViewModel.name, "Failed to load events", e)
                _loadingError.value = true
            }
        }
    }

    /**
     * Process a new list of events
     */
    private suspend fun processEventsList(eventsList: List<IWWWEvent>) {
        val sortedEvents = eventsList.sortedBy { it.getStartDateTime() }

        // Update original events with mutex protection
        originalEventsMutex.withLock {
            originalEvents = sortedEvents
        }

        // Update UI state
        _events.value = sortedEvents
        _hasFavorites.value = sortedEvents.any(IWWWEvent::favorite)

        // Start observing all events
        startObservingEvents(sortedEvents)
    }

    // ---------------------------

    fun getEventStatusFlow(eventId: String): StateFlow<IWWWEvent.Status> {
        return eventStatusFlowCache.getOrPut(eventId) {
            _eventStatusMap
                .map { statusMap -> statusMap[eventId] ?: IWWWEvent.Status.UNDEFINED }
                .flowOn(Dispatchers.Default)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Lazily,
                    initialValue = _eventStatusMap.value[eventId] ?: IWWWEvent.Status.UNDEFINED
                )
        }
    }

    // ---------------------------

    /**
     * Filter events by favorite status
     */
    fun filterEvents(
        onlyFavorites: Boolean = false,
        onlyDownloaded: Boolean = false
    ) {
        viewModelScope.launch {
            _events.value = originalEvents.filter { event ->
                when {
                    onlyFavorites -> event.favorite
                    onlyDownloaded ->mapChecker.isMapDownloaded(event.id)
                    else -> true // All events
                }
            }
        }
    }

    // ---------------------------

    /**
     * Start observing status for a list of events
     */
    private suspend fun startObservingEvents(events: List<IWWWEvent>) {
        // Cancel any existing observation jobs first
        cancelEventObservations()

        // Build a new status map with initial values
        val initialStatusMap = events.associate { event ->
            event.id to (
                    try {
                        event.getStatus()
                    } catch (e: Exception) {
                        Log.e(::EventsViewModel.name, "Error getting status for event ${event.id}", e)
                        IWWWEvent.Status.UNDEFINED
                    }
                    )
        }.toMutableMap()

        // Update the status map
        _eventStatusMap.value = initialStatusMap

        // Clear the status flow cache as the events list changed
        eventStatusFlowCache.clear()

        // Start observation for each event
        events.forEach { event ->
            startEventObservation(event)
        }
    }

    /**
     * Start observation for a single event
     */
    private fun startEventObservation(event: IWWWEvent) {
        // Start the event's observation
        event.startObservation()

        // Create a job to collect the event's status changes
        val job = event.eventStatus
            .onEach { status ->
                try {
                    // Update our status map when the event's status changes
                    val updatedMap = _eventStatusMap.value.toMutableMap()
                    updatedMap[event.id] = status
                    _eventStatusMap.value = updatedMap
                } catch (e: Exception) {
                    Log.e(::EventsViewModel.name, "Error updating status for event ${event.id}: ${e.message}", e)
                }
            }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)

        // Store the job for cancellation
        eventObservationJobs[event.id] = job
    }

    /**
     * Cancel all event observations
     */
    private fun cancelEventObservations() {
        eventObservationJobs.forEach { (_, job) ->
            job.cancel()
        }
        eventObservationJobs.clear()

        originalEvents.forEach { event ->
            event.stopObservation()
        }
    }

    // ----------------------------

    override fun onCleared() {
        super.onCleared()
        cancelEventObservations()
    }
}