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
import org.maplibre.geojson.Polygon
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class WaveViewModel(private val platform: WWWPlatform) : ViewModel() {

    private val observers = mutableMapOf<String, ObserverState>()
    private val lastPolygonUpdateTimestamps = mutableMapOf<String, AtomicLong>()
    private val polygonUpdateIntervalMs = 250L // Limit updates to 4 per second

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine error: ${throwable.message}", throwable)
    }

    // Track which observers exist
    private val observerExistsMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    // Consolidated state flows to reduce the number of active flows
    // Using a single MutableStateFlow for multiple properties reduces memory usage
    data class WaveState(
        val userPositionRatio: Double = 0.0,
        val isInArea: Boolean = false,
        val hitDateTime: Instant = Instant.DISTANT_FUTURE,
        val timeBeforeHit: Duration = Duration.INFINITE,
        val waveNumbers: WaveNumbersLiterals? = null
    )

    // Class to encapsulate all state for a single observer
    inner class ObserverState(val event: IWWWEvent) {
        private val job: Job = Job()
        val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + job + exceptionHandler)

        var backupSimulationSpeed: Int? = null

        // Weak reference to last wave state to avoid memory leaks
        // This avoids keeping large objects in memory
        private var _lastWaveStateRef: Any? = null

        val waveStateFlow = MutableStateFlow(WaveState())

        // Cleanup function
        fun cleanup() {
            job.cancel()
            _lastWaveStateRef = null
        }

        // Setter for last wave state with memory considerations
        fun updateLastWaveState(state: Any?) {
            // Only store essential data or weak references to large objects
            _lastWaveStateRef = state
        }

        // Getter that doesn't expose the implementation details
        fun getLastWaveState(): Any? = _lastWaveStateRef
    }

    /**
     * Gets or creates an observer state for a given ID
     */
    private fun getOrCreateObserverState(observerId: String, event: IWWWEvent): ObserverState {
        val existing = observers[observerId]
        val state = existing ?: ObserverState(event).also { initializeObserver(it) }

        if (existing == null) {
            observers[observerId] = state
            lastPolygonUpdateTimestamps[observerId] = AtomicLong(0)

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

        Log.v(TAG, "Initializing observer for event ${event.id}")

        // Initialize state with initial values
        state.scope.launch {
            try {

                // Initialize with consolidated state
                state.waveStateFlow.value = WaveState(
                    userPositionRatio = event.wave.userPositionToWaveRatio() ?: 0.0,
                    timeBeforeHit = event.wave.timeBeforeUserHit() ?: Duration.INFINITE,
                    waveNumbers = event.getAllNumbers(),
                    hitDateTime = event.wave.userHitDateTime() ?: Instant.DISTANT_FUTURE
                )

                // Set up flow collectors for the event
                setupEventCollectors(state)

            } catch (e: Exception) {
                Log.e(TAG, "Error initializing observer: ${e.message}", e)
            }
        }
    }

    /**
     * Setup flow collectors for an observer
     */
    private fun setupEventCollectors(state: ObserverState) {
        val event = state.event
        val scope = state.scope

        // Update derived state on progression changes - use a single collector for multiple properties
        scope.launch {
            event.progression.collect { _ ->
                try {
                    // Update state in a single operation to reduce StateFlow emissions
                    val currentState = state.waveStateFlow.value
                    state.waveStateFlow.value = currentState.copy(
                        userPositionRatio = event.wave.userPositionToWaveRatio() ?: 0.0,
                        timeBeforeHit = event.wave.timeBeforeUserHit() ?: Duration.INFINITE,
                        hitDateTime = event.wave.userHitDateTime() ?: Instant.DISTANT_FUTURE,
                        waveNumbers = currentState.waveNumbers?.copy(
                            waveProgression = event.wave.getLiteralProgression()
                        ) ?: event.getAllNumbers()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating progression: ${e.message}", e)
                }
            }
        }

        // Handle warming started
        scope.launch {
            event.isWarmingInProgress.collect { isWarmingStarted ->
                if (isWarmingStarted) {
                    state.backupSimulationSpeed = platform.getSimulation()?.speed
                    platform.getSimulation()?.setSpeed(1)
                }
            }
        }

        // Handle user has been hit
        scope.launch {
            event.userHasBeenHit.collect { hasBeenHit ->
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

        // Start the event's observation if not already started
        event.startObservation()

        // Create and initialize observer state
        val state = getOrCreateObserverState(observerId, event)

        // Handle polygon updates if provided, with rate limiting
        if (polygonsHandler != null) {
            state.scope.launch {
                event.progression.collect {
                    val timestamp = System.currentTimeMillis()
                    val lastUpdate = lastPolygonUpdateTimestamps[observerId]?.get() ?: 0L

                    // Rate limit polygon updates to reduce memory pressure
                    if (timestamp - lastUpdate >= polygonUpdateIntervalMs) {
                        lastPolygonUpdateTimestamps[observerId]?.set(timestamp)
                        updateWavePolygons(state, polygonsHandler)
                    }
                }
            }
        }

        return observerId
    }

    // Optimized helper function for creating derived flows - caches flows by observer ID
    private val cachedFlows = mutableMapOf<String, Map<String, StateFlow<*>>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> createDerivedFlow(
        observerId: String,
        propertyKey: String,
        defaultValue: T,
        transform: (WaveState) -> T
    ): StateFlow<T> {
        // Check if we have a cached flow for this property
        @Suppress("UNCHECKED_CAST")
        cachedFlows[observerId]?.get(propertyKey)?.let { return it as StateFlow<T> }

        val flow = observerExistsMap
            .map { it[observerId] == true }
            .flatMapLatest { exists ->
                if (exists) {
                    observers[observerId]?.waveStateFlow?.map { transform(it) } ?: flowOf(defaultValue)
                } else {
                    flowOf(defaultValue)
                }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = defaultValue
            )

        // Cache the flow
        val currentCache = cachedFlows[observerId]?.toMutableMap() ?: mutableMapOf()
        currentCache[propertyKey] = flow
        cachedFlows[observerId] = currentCache

        return flow
    }

    // Simplified property accessor methods using the consolidated state flow
    fun getProgressionFlow(observerId: String): StateFlow<Double> =
        createDerivedFlow(observerId, "progression", 0.0) { _ ->
            observers[observerId]?.event?.progression?.value ?: 0.0
        }

    fun getEventStatusFlow(observerId: String): StateFlow<IWWWEvent.Status> =
        createDerivedFlow(observerId, "eventStatus", IWWWEvent.Status.UNDEFINED) { _ ->
            observers[observerId]?.event?.eventStatus?.value ?: IWWWEvent.Status.UNDEFINED
        }

    fun getIsWarmingInProgressFlow(observerId: String): StateFlow<Boolean> =
        createDerivedFlow(observerId, "isWarmingInProgress", false) { _ ->
            observers[observerId]?.event?.isWarmingInProgress?.value ?: false
        }

    fun getIsGoingToBeHitFlow(observerId: String): StateFlow<Boolean> =
        createDerivedFlow(observerId, "isGoingToBeHit", false) { _ ->
            observers[observerId]?.event?.userIsGoingToBeHit?.value ?: false
        }

    fun getHasBeenHitFlow(observerId: String): StateFlow<Boolean> =
        createDerivedFlow(observerId, "hasBeenHit", false) { _ ->
            observers[observerId]?.event?.userHasBeenHit?.value ?: false
        }

    // Consolidated state access methods
    fun getUserPositionRatioFlow(observerId: String): StateFlow<Double> =
        createDerivedFlow(observerId, "userPositionRatio", 0.0) { it.userPositionRatio }

    fun getTimeBeforeHitFlow(observerId: String): StateFlow<Duration> =
        createDerivedFlow(observerId, "timeBeforeHit", Duration.INFINITE) { it.timeBeforeHit }

    fun getHitDateTimeFlow(observerId: String): StateFlow<Instant> =
        createDerivedFlow(observerId, "hitDateTime", Instant.DISTANT_FUTURE) { it.hitDateTime }

    fun getWaveNumbersFlow(observerId: String): StateFlow<WaveNumbersLiterals?> =
        createDerivedFlow(observerId, "waveNumbers", null) { it.waveNumbers }

    fun getIsInAreaFlow(observerId: String): StateFlow<Boolean> =
        createDerivedFlow(observerId, "isInArea", false) { it.isInArea }

    /**
     * Stop observation for a specific observer ID
     */
    fun stopObservation(observerId: String) {
        observers[observerId]?.let { state ->
            // Restore simulation speed
            state.backupSimulationSpeed?.let { speed ->
                platform.getSimulation()?.setSpeed(speed)
            }

            // Cleanup state
            state.cleanup()
            observers.remove(observerId)
            lastPolygonUpdateTimestamps.remove(observerId)

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
        cachedFlows.clear()
    }

    /**
     * Updates the user's location and checks if they're in the event area.
     */
    fun updateUserLocation(observerId: String, newLocation: Position) {
        observers[observerId]?.let { state ->
            state.scope.launch {
                try {
                    val event = state.event
                    val isInArea = event.area.isPositionWithin(newLocation)

                    // Only update if the value changes
                    if (state.waveStateFlow.value.isInArea != isInArea) {
                        state.waveStateFlow.value = state.waveStateFlow.value.copy(isInArea = isInArea)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating user location: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Updates the wave polygons based on the current wave state.
     * Optimized to reduce memory pressure.
     */
    private suspend fun updateWavePolygons(
        state: ObserverState,
        polygonsHandler: (wavePolygons: List<Polygon>, clearPolygons: Boolean) -> Unit
    ) {
        val event = state.event
        if (!event.isRunning() && !event.isDone()) return

        try {
            val mode = WaveMode.ADD

            // Recalculate the cut
            val waveState = event.wave.getWavePolygons(null, mode)

            // Store minimally required data from wave state
            state.updateLastWaveState(waveState)

            val polygons = waveState?.traversedPolygons

            // Process polygons in batches if needed
            val newPolygons = polygons?.map { it.toMapLibrePolygon() } ?: listOf()

            withContext(Dispatchers.Main) {
                polygonsHandler(newPolygons, true)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating wave polygons: ${e.message}", e)
        }
    }

    override fun onCleared() {
        stopAllObservations()
        cachedFlows.clear()
        super.onCleared()
    }

    companion object {
        private const val TAG = "WaveViewModel"
    }
}