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
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_SHOW_HIT_SEQUENCE_SECONDS
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEventWave.WaveMode
import com.worldwidewaves.shared.events.WWWEventWave.WaveNumbersLiterals
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.toMapLibrePolygon
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import org.maplibre.geojson.Polygon
import java.util.UUID
import kotlin.time.Duration

class WaveViewModel(private val platform: WWWPlatform) : ViewModel() {

    private val observers = mutableMapOf<String, ObserverState>()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(::WaveViewModel.name, "Coroutine error: ${throwable.message}", throwable)
    }

    // Track which observers exist
    private val observerExistsMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    // Class to encapsulate all state for a single observer
    inner class ObserverState(val event: IWWWEvent) {
        private val job: Job = Job()
        val scope: CoroutineScope = CoroutineScope(viewModelScope.coroutineContext + job)

        var backupSimulationSpeed: Int? = null
        val lastWaveState = MutableStateFlow<Any?>(null)

        // Event source flow
        val eventFlow = MutableStateFlow(event)

        // Derived state flows that need special computation
        val userPositionRatio = MutableStateFlow(0.0)
        val isInArea = MutableStateFlow(false)
        val hitDateTime = MutableStateFlow(Instant.DISTANT_FUTURE)
        val timeBeforeHit = MutableStateFlow(Duration.INFINITE)
        val waveNumbers = MutableStateFlow<WaveNumbersLiterals?>(null)

        // Cleanup function
        fun cleanup() {
            job.cancel()
        }
    }

    /**
     * Creates a proxy StateFlow that maps from an event property
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> ObserverState.proxyEventFlow(
        defaultValue: T,
        flowSelector: (IWWWEvent) -> StateFlow<T>
    ): StateFlow<T> {
        return eventFlow
            .flatMapLatest { event -> flowSelector(event) }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = defaultValue
            )
    }

    /**
     * Gets or creates an observer state for a given ID
     */
    private fun getOrCreateObserver(observerId: String, event: IWWWEvent): ObserverState {
        val existing = observers[observerId]
        val state = existing ?: ObserverState(event).also { initializeObserver(it) }

        if (existing == null) {
            observers[observerId] = state

            // Update observer existence map
            val currentMap = observerExistsMap.value.toMutableMap()
            currentMap[observerId] = true
            observerExistsMap.value = currentMap
        }

        return state
    }

    /**
     * Initialize a new observer with event data
     */
    private fun initializeObserver(state: ObserverState) {
        val event = state.event

        Log.v(::WaveViewModel.name, "Initializing observer for event ${event.id}")

        // Initialize state with initial values
        state.scope.launch(Dispatchers.Default + exceptionHandler) {
            try {
                state.userPositionRatio.value = event.wave.userPositionToWaveRatio() ?: 0.0
                state.timeBeforeHit.value = event.wave.timeBeforeUserHit() ?: Duration.INFINITE
                state.waveNumbers.value = event.getAllNumbers()
                state.hitDateTime.value = event.wave.userHitDateTime() ?: Instant.DISTANT_FUTURE

                // Set up flow collectors for the event
                setupEventCollectors(state)
            } catch (e: Exception) {
                Log.e(::WaveViewModel.name, "Error initializing observer: ${e.message}", e)
            }
        }
    }

    /**
     * Setup flow collectors for an observer
     */
    private fun setupEventCollectors(state: ObserverState) {
        val event = state.event

        // Handle progression updates
        event.progression
            .map { event.wave.getProgression() }
            .flowOn(Dispatchers.Default)
            .stateIn(state.scope, SharingStarted.WhileSubscribed(), 0.0)

        // Update derived state on progression changes
        state.scope.launch(Dispatchers.Default) {
            event.progression.collect {
                state.userPositionRatio.value = event.wave.userPositionToWaveRatio() ?: 0.0
                state.timeBeforeHit.value = event.wave.timeBeforeUserHit() ?: Duration.INFINITE
                state.hitDateTime.value = event.wave.userHitDateTime() ?: Instant.DISTANT_FUTURE

                // Update wave numbers
                if (state.waveNumbers.value == null) {
                    state.waveNumbers.value = event.getAllNumbers()
                } else {
                    state.waveNumbers.value = state.waveNumbers.value?.copy(
                        waveProgression = event.wave.getLiteralProgression()
                    )
                }
            }
        }

        // Handle warming started
        state.scope.launch(Dispatchers.Default) {
            event.isWarmingInProgress
                .collect { isStarted ->
                    if (isStarted) {
                        state.backupSimulationSpeed = platform.getSimulation()?.speed
                        platform.getSimulation()?.setSpeed(1)
                    }
                }
        }

        // Handle user has been hit
        state.scope.launch(Dispatchers.Default) {
            event.userHasBeenHit
                .collect { hasBeenHit ->
                    if (hasBeenHit) {
                        // Restore simulation speed after a delay
                        state.backupSimulationSpeed?.let { speed ->
                            launch {
                                delay(WAVE_SHOW_HIT_SEQUENCE_SECONDS.inWholeSeconds * 1000)
                                platform.getSimulation()?.setSpeed(speed)
                            }
                        }
                    }
                }
        }
    }

    /**
     * Start observation for a specific observer ID
     */
    fun startObservation(
        observerId: String = UUID.randomUUID().toString(),
        event: IWWWEvent,
        polygonsHandler: ((wavePolygons: List<Polygon>, clearPolygons: Boolean) -> Unit)? = null
    ): String {
        // First stop any existing observation for this ID
        stopObservation(observerId)

        // Create and initialize observer state
        val state = getOrCreateObserver(observerId, event)

        // Handle polygon updates if provided
        if (polygonsHandler != null) {
            state.scope.launch(Dispatchers.Default) {
                event.progression.collect {
                    updateWavePolygons(state, polygonsHandler)
                }
            }
        }

        // Start the event's observation
        event.startObservation()

        return observerId
    }

    // Helper function to create reactive property flows
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> flowWithDefaults(
        observerId: String,
        defaultValue: T,
        propertySelector: (ObserverState) -> StateFlow<T>
    ): StateFlow<T> = observerExistsMap
            .map { it[observerId] == true }
            .flatMapLatest { exists ->
                if (exists) {
                    observers[observerId]?.let { propertySelector(it) } ?: flowOf(defaultValue)
                } else {
                    flowOf(defaultValue)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = defaultValue
            )

    // Simplified property accessor methods
    fun getProgressionFlow(observerId: String): StateFlow<Double> =
        flowWithDefaults(observerId, 0.0) {
            it.proxyEventFlow(0.0) { event -> event.progression }
        }

    fun getEventStatusFlow(observerId: String): StateFlow<IWWWEvent.Status> =
        flowWithDefaults(observerId, IWWWEvent.Status.UNDEFINED) {
            it.proxyEventFlow(IWWWEvent.Status.UNDEFINED) { event -> event.eventStatus }
        }

    fun getIsWarmingInProgressFlow(observerId: String): StateFlow<Boolean> =
        flowWithDefaults(observerId, false) {
            it.proxyEventFlow(false) { event -> event.isWarmingInProgress }
        }

    fun getIsGoingToBeHitFlow(observerId: String): StateFlow<Boolean> =
        flowWithDefaults(observerId, false) {
            it.proxyEventFlow(false) { event -> event.userIsGoingToBeHit }
        }

    fun getHasBeenHitFlow(observerId: String): StateFlow<Boolean> =
        flowWithDefaults(observerId, false) {
            it.proxyEventFlow(false) { event -> event.userHasBeenHit }
        }

    // -----

    fun getUserPositionRatioFlow(observerId: String): StateFlow<Double> =
        flowWithDefaults(observerId, 0.0) { it.userPositionRatio }

    fun getTimeBeforeHitFlow(observerId: String): StateFlow<Duration> =
        flowWithDefaults(observerId, Duration.INFINITE) { it.timeBeforeHit }

    fun getHitDateTimeFlow(observerId: String): StateFlow<Instant> =
        flowWithDefaults(observerId, Instant.DISTANT_FUTURE) { it.hitDateTime }

    fun getWaveNumbersFlow(observerId: String): StateFlow<WaveNumbersLiterals?> =
        flowWithDefaults(observerId, null) { it.waveNumbers }

    fun getIsInAreaFlow(observerId: String): StateFlow<Boolean> =
        flowWithDefaults(observerId, false) { it.isInArea }

    /**
     * Stop observation for a specific observer ID
     */
    fun stopObservation(observerId: String) {
        observers[observerId]?.let { state ->
            state.event.stopObservation()
            state.cleanup()
            observers.remove(observerId)

            // Update observer existence map
            val currentMap = observerExistsMap.value.toMutableMap()
            currentMap.remove(observerId)
            observerExistsMap.value = currentMap
        }
    }

    /**
     * Stop all observations
     */
    private fun stopAllObservations() {
        observers.keys.toList().forEach { stopObservation(it) }
    }

    /**
     * Updates the user's location and checks if they're in the event area.
     */
    fun updateUserLocation(observerId: String, newLocation: Position) {
        observers[observerId]?.let { state ->
            state.scope.launch(Dispatchers.Default + exceptionHandler) {
                try {
                    val event = state.event
                    if (event.area.isPositionWithin(newLocation)) {
                        state.isInArea.value = true
                    }
                } catch (e: Exception) {
                    Log.e(::WaveViewModel.name, "Error updating user location: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Updates the wave polygons based on the current wave state.
     */
    private suspend fun updateWavePolygons(
        state: ObserverState,
        polygonsHandler: (wavePolygons: List<Polygon>, clearPolygons: Boolean) -> Unit
    ) {
        val event = state.event
        if (!event.isRunning()) return

        try {
            val mode = WaveMode.ADD

            // Recalculate the cut
            val waveState = event.wave.getWavePolygons(null, mode)
            state.lastWaveState.value = waveState

            val polygons = waveState?.traversedPolygons
            val newPolygons = polygons?.map { it.toMapLibrePolygon() } ?: listOf()

            withContext(Dispatchers.Main) {
                polygonsHandler(newPolygons, false) // mode != WaveMode.ADD)
            }

        } catch (e: Exception) {
            Log.e(::WaveViewModel.name, "Error updating wave polygons: ${e.message}", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAllObservations()
    }
}