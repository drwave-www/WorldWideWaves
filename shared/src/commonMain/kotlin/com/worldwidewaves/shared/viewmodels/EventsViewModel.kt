package com.worldwidewaves.shared.viewmodels

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.WWWGlobals.WaveTiming
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.domain.repository.EventsRepository
import com.worldwidewaves.shared.domain.usecases.CheckEventFavoritesUseCase
import com.worldwidewaves.shared.domain.usecases.EventFilterCriteria
import com.worldwidewaves.shared.domain.usecases.FilterEventsUseCase
import com.worldwidewaves.shared.domain.usecases.GetSortedEventsUseCase
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.ui.BaseViewModel
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

/**
 * Central ViewModel that drives the **Events** tab.
 *
 * Following Clean Architecture principles, this ViewModel focuses solely on UI state
 * management and delegates business logic to use cases.
 *
 * Responsibilities:
 * • Manage UI state through reactive `StateFlow`s for events list, loading/error flags
 * • Delegate data loading to [EventsRepository]
 * • Use [GetSortedEventsUseCase] for event sorting business logic
 * • Use [FilterEventsUseCase] for event filtering logic
 * • Use [CheckEventFavoritesUseCase] for favorites checking logic
 * • Handle UI-specific simulation speed adjustments in debug builds
 * • Provide event observation coordination for UI updates
 * • Catch uncaught coroutine exceptions and surface them to UI
 */
@OptIn(ExperimentalTime::class)
class EventsViewModel(
    private val eventsRepository: EventsRepository,
    private val getSortedEventsUseCase: GetSortedEventsUseCase,
    private val filterEventsUseCase: FilterEventsUseCase,
    private val checkEventFavoritesUseCase: CheckEventFavoritesUseCase,
    private val platform: WWWPlatform,
) : BaseViewModel() {
    companion object {
        private const val MILLIS_PER_SECOND = 1000L
    }

    // UI State flows
    private val _hasFavorites = MutableStateFlow(false)
    val hasFavorites: StateFlow<Boolean> = _hasFavorites.asStateFlow()

    private val _events = MutableStateFlow<List<IWWWEvent>>(emptyList())
    val events: StateFlow<List<IWWWEvent>> = _events.asStateFlow()

    private val _loadingError = MutableStateFlow(false)
    val hasLoadingError: StateFlow<Boolean> = _loadingError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Exception handler removed - unused in the codebase

    // ---------------------------

    // ---------------------------
    // iOS FIX: No init{} block to prevent deadlocks
    // Events loading triggered from LaunchedEffect in EventsListScreen
    // ---------------------------

    /**
     * Load events from the data source through repository and use cases
     * ⚠️ Called from LaunchedEffect for iOS safety
     */
    suspend fun loadEvents() {
        try {
            // Start loading events through repository
            eventsRepository.loadEvents { exception ->
                Log.e("EventsViewModel", "Error loading events", throwable = exception)
                _loadingError.value = true
            }

            // Observe loading state from repository
            eventsRepository
                .isLoading()
                .onEach { isLoading -> _isLoading.value = isLoading }
                .launchIn(scope)

            // Observe errors from repository
            eventsRepository
                .getLastError()
                .onEach { error ->
                    _loadingError.value = error != null
                    error?.let {
                        Log.e("EventsViewModel", "Repository error: ${it.message}", throwable = it)
                    }
                }.launchIn(scope)

            // Get sorted events through use case and process them
            getSortedEventsUseCase
                .invoke()
                .onEach { sortedEvents: List<IWWWEvent> ->
                    processEventsList(sortedEvents)
                }.flowOn(Dispatchers.Default)
                .launchIn(scope)
        } catch (e: Exception) {
            Log.e("EventsViewModel", "Error in loadEvents", throwable = e)
            _loadingError.value = true
        }
    }

    /**
     * Process a new list of sorted events for UI updates
     */
    private suspend fun processEventsList(sortedEvents: List<IWWWEvent>) {
        // Update UI state - events are already sorted by use case
        _events.value = sortedEvents

        // Check for favorites using use case
        _hasFavorites.value = checkEventFavoritesUseCase.hasFavoriteEvents(sortedEvents)

        // Start observing all events - multiple events can be active simultaneously
        // The user has a single position that needs to be checked against all event areas
        sortedEvents.forEach { event ->
            Log.d("EventsViewModel", "Starting observation for event ${event.id}")

            // Start event observation for all events
            event.observer.startObservation()

            // Setup simulation speed listeners on DEBUG mode
            monitorSimulatedSpeed(event)
        }
    }

    /**
     * Monitor simulation speed during event phases (DEBUG mode only).
     * Uses viewModelScope for automatic lifecycle management and memory leak prevention.
     */
    private fun monitorSimulatedSpeed(event: IWWWEvent) {
        var backupSimulationSpeed = 1

        // Handle warming started - using viewModelScope for automatic cleanup
        scope.launch {
            event.observer.isUserWarmingInProgress.collect { isWarmingStarted ->
                if (isWarmingStarted) {
                    backupSimulationSpeed = platform.getSimulation()?.speed ?: 1
                    platform.getSimulation()?.setSpeed(1)
                }
            }
        }

        // Handle user has been hit - using viewModelScope for automatic cleanup
        scope.launch {
            event.observer.userHasBeenHit.collect { hasBeenHit ->
                if (hasBeenHit) {
                    // Restore simulation speed after a delay
                    launch {
                        delay(WaveTiming.SHOW_HIT_SEQUENCE_SECONDS.inWholeSeconds * MILLIS_PER_SECOND)
                        platform.getSimulation()?.setSpeed(backupSimulationSpeed)
                    }
                }
            }
        }
    }

    // ---------------------------

    /**
     * Filter events using business logic from FilterEventsUseCase
     */
    fun filterEvents(
        onlyFavorites: Boolean = false,
        onlyDownloaded: Boolean = false,
    ) {
        scope.launch {
            try {
                // Get all events from the use case first
                getSortedEventsUseCase
                    .invoke()
                    .onEach { allEvents: List<IWWWEvent> ->
                        // Use FilterEventsUseCase for business logic
                        val filterCriteria =
                            EventFilterCriteria(
                                onlyFavorites = onlyFavorites,
                                onlyDownloaded = onlyDownloaded,
                                onlyRunning = false, // Not used in this UI context
                                onlyUpcoming = false, // Not used in this UI context
                                onlyCompleted = false, // Not used in this UI context
                            )

                        val filteredEvents = filterEventsUseCase.invoke(allEvents, filterCriteria)
                        _events.value = filteredEvents
                    }.launchIn(scope)
            } catch (e: Exception) {
                Log.e("EventsViewModel", "Error filtering events", throwable = e)
                _loadingError.value = true
            }
        }
    }
}
