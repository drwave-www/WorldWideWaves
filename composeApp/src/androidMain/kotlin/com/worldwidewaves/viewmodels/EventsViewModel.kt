package com.worldwidewaves.viewmodels

/*
 * Copyright 2025 DrWave
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
import com.worldwidewaves.BuildConfig
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_SHOW_HIT_SEQUENCE_SECONDS
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.utils.MapAvailabilityChecker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.ExperimentalTime

/**
 * ViewModel for managing event data.
 *
 * This ViewModel fetches events from a data source (`WWWEvents`), exposes them as a
 * `StateFlow`, and provides filtering functionality for displaying all events or only
 * favorite events.
 */
@OptIn(ExperimentalTime::class)
class EventsViewModel(
    private val wwwEvents: WWWEvents,
    private val mapChecker: MapAvailabilityChecker,
    private val platform: WWWPlatform
) : ViewModel() {

    private val originalEventsMutex = Mutex()
    var originalEvents: List<IWWWEvent> = emptyList()

    // State flows
    private val _hasFavorites = MutableStateFlow(false)
    val hasFavorites: StateFlow<Boolean> = _hasFavorites.asStateFlow()

    private val _events = MutableStateFlow<List<IWWWEvent>>(emptyList())
    val events: StateFlow<List<IWWWEvent>> = _events.asStateFlow()

    private val _loadingError = MutableStateFlow(false)
    val hasLoadingError: StateFlow<Boolean> = _loadingError.asStateFlow()

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

                // Collect the events flow and process events list
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
        sortedEvents.forEach { event ->

            // Start event observation
            event.observer.startObservation()

            // Setup simulation speed listeners on DEBUG mode
            monitorSimulatedSpeed(event)
        }

    }

    /**
     * Monitor simulation speed during event phases (DEBUG mode only)
     */
    private fun monitorSimulatedSpeed(event: IWWWEvent) {
        if (BuildConfig.DEBUG) {
            val scope = CoroutineScope(Dispatchers.Default)
            var backupSimulationSpeed = 1

            // Handle warming started
            scope.launch {
                event.observer.isWarmingInProgress.collect { isWarmingStarted ->
                    if (isWarmingStarted) {
                        backupSimulationSpeed = platform.getSimulation()?.speed ?: 1
                        platform.getSimulation()?.setSpeed(1)
                    }
                }
            }

            // Handle user has been hit
            scope.launch {
                event.observer.userHasBeenHit.collect { hasBeenHit ->
                    if (hasBeenHit) {
                        // Restore simulation speed after a delay
                        launch {
                            delay(WAVE_SHOW_HIT_SEQUENCE_SECONDS.inWholeSeconds * 1000)
                            platform.getSimulation()?.setSpeed(backupSimulationSpeed)
                        }
                    }
                }
            }
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
        mapChecker.refreshAvailability()
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

}