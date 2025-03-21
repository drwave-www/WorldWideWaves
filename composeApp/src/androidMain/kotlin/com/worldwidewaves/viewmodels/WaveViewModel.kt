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
import com.worldwidewaves.shared.events.WWWEventWave.WavePolygons
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.toMapLibrePolygon
import com.worldwidewaves.shared.utils.updateIfChanged
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import org.maplibre.geojson.Polygon
import kotlin.time.Duration

class WaveViewModel(private val platform: WWWPlatform) : ViewModel() {

    private var event: IWWWEvent? = null
    private var backupSimulationSpeed: Int? = null
    private var observationJob: Job? = null
    private var lastWaveState: WavePolygons? = null

    // Event-specific StateFlows that need local computation
    private val _waveNumbers = MutableStateFlow<WaveNumbersLiterals?>(null)
    val waveNumbers: StateFlow<WaveNumbersLiterals?> = _waveNumbers.asStateFlow()

    private val _userPositionRatio = MutableStateFlow(0.0)
    val userPositionRatio: StateFlow<Double> = _userPositionRatio.asStateFlow()

    private val _isInArea = MutableStateFlow(false)
    val isInArea: StateFlow<Boolean> = _isInArea.asStateFlow()

    private val _hitDateTime = MutableStateFlow(Instant.DISTANT_FUTURE)
    val hitDateTime: StateFlow<Instant> = _hitDateTime.asStateFlow()

    private val _timeBeforeHit = MutableStateFlow(Duration.INFINITE)
    val timeBeforeHit: StateFlow<Duration> = _timeBeforeHit.asStateFlow()

    // Proxy flows that directly map to event states
    private val _currentEvent = MutableStateFlow<IWWWEvent?>(null)

    // Dynamically update the viewmodel when the event changes
    val eventStatus = createEventFlow(IWWWEvent.Status.UNDEFINED) { it.eventStatus }
    val progression = createEventFlow(0.0) { it.progression }
    val isWarmingInProgress = createEventFlow(false) { it.isWarmingInProgress }
    val isGoingToBeHit = createEventFlow(false) { it.userIsGoingToBeHit }
    val hasBeenHit = createEventFlow(false) { it.userHasBeenHit }

    // Exception handler for coroutines
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(::WaveViewModel.name, "Coroutine error: ${throwable.message}", throwable)
    }

    // ------------------------------------------------------------------------

    // Proxy event states that directly map to event states
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> createEventFlow(defaultValue: T, flowSelector: (IWWWEvent) -> StateFlow<T>): StateFlow<T> =
        _currentEvent
            .flatMapLatest { event -> event?.let { flowSelector(it) } ?: flowOf(defaultValue) }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = defaultValue
            )

    // ------------------------------------------------------------------------

    /**
     * Starts observing an event and updates the UI accordingly.
     */
    fun startObservation(
        event: IWWWEvent,
        polygonsHandler: ((wavePolygons: List<Polygon>, clearPolygons: Boolean) -> Unit)? = null
    ) {
        // Stop any existing observation
        stopObservation()

        this.event = event

        // Start event observation
        event.startObservation()

        // Update the current event to trigger flow updates
        _currentEvent.value = event

        // Create a parent job for observation coroutines
        observationJob = viewModelScope.launch(Dispatchers.Default + exceptionHandler) {
            try {
                // Initialize the derived state values
                initializeState(event)

                // Update additional state values and wave polygons
                setupStatesAndWaveUpdates(event, polygonsHandler)

                // Change simulation speed if required
                setupSimulationSpeedAdaptation(event)

            } catch (e: CancellationException) {
                Log.d(::WaveViewModel.name, "Observation cancelled")
            } catch (e: Exception) {
                Log.e(::WaveViewModel.name, "Error in startObservation: ${e.message}", e)
            }
        }
    }

    /**
     * Initializes derived state values from the event.
     */
    private suspend fun initializeState(event: IWWWEvent) {
        _userPositionRatio.value = event.wave.userPositionToWaveRatio() ?: 0.0
        _timeBeforeHit.value = event.wave.timeBeforeUserHit() ?: Duration.INFINITE
        _waveNumbers.value = event.getAllNumbers()
        _hitDateTime.value = event.wave.userHitDateTime() ?: Instant.DISTANT_FUTURE
    }

    /**
     * Sets up collectors for derived state that needs computation.
     */
    private fun setupStatesAndWaveUpdates(
        event: IWWWEvent,
        polygonsHandler: ((wavePolygons: List<Polygon>, clearPolygons: Boolean) -> Unit)?
    ) {
        // Update derived state when progression changes
        event.progression
            .onEach { updateDerivedState(event) }
            .catch { e -> Log.e(::WaveViewModel.name, "Error updating derived state: ${e.message}", e) }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)

        // Update wave polygons when progression changes
        event.progression
            .onEach { updateWavePolygons(polygonsHandler) }
            .catch { e -> Log.e(::WaveViewModel.name, "Error updating polygons: ${e.message}", e) }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }

    /**
     * Sets up handlers for specific state transitions.
     */
    private fun setupSimulationSpeedAdaptation(event: IWWWEvent) {

        // Handle warming started
        event.isWarmingInProgress
            .filter { it }
            .onEach {
                // Disable simulation speed during warming
                backupSimulationSpeed = platform.getSimulation()?.speed
                platform.getSimulation()?.setSpeed(1)
            }
            .catch { e -> Log.e(::WaveViewModel.name, "Error handling warming start: ${e.message}", e) }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)

        // Handle user has been hit
        event.userHasBeenHit
            .filter { it }
            .onEach {
                // Restore simulation speed after a delay
                backupSimulationSpeed?.let { speed ->
                    viewModelScope.launch(Dispatchers.Default + exceptionHandler) {
                        try {
                            delay(WAVE_SHOW_HIT_SEQUENCE_SECONDS.inWholeSeconds * 1000)
                            platform.getSimulation()?.setSpeed(speed)
                        } catch (e: Exception) {
                            Log.e(::WaveViewModel.name, "Error restoring simulation speed: ${e.message}", e)
                        }
                    }
                }
            }
            .catch { e -> Log.e(::WaveViewModel.name, "Error handling hit: ${e.message}", e) }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }

    /**
     * Updates derived state that needs computation.
     */
    private suspend fun updateDerivedState(event: IWWWEvent) {
        _userPositionRatio.updateIfChanged(event.wave.userPositionToWaveRatio() ?: 0.0)
        _timeBeforeHit.updateIfChanged(event.wave.timeBeforeUserHit() ?: Duration.INFINITE)
        _hitDateTime.updateIfChanged(event.wave.userHitDateTime() ?: Instant.DISTANT_FUTURE)

        // Update wave numbers
        if (_waveNumbers.value == null) {
            _waveNumbers.value = event.getAllNumbers()
        } else {
            _waveNumbers.value = _waveNumbers.value?.copy(
                waveProgression = event.wave.getLiteralProgression()
            )
        }
    }

    /**
     * Stops observing the event.
     */
    fun stopObservation() {
        // Cancel all observation coroutines
        observationJob?.cancelChildren()
        observationJob?.cancel()
        observationJob = null

        // Stop event observation
        event?.stopObservation()

        // Clear the current event to stop flow updates
        _currentEvent.value = null
        event = null
    }

    // ----------------------------

    /**
     * Updates the user's location and checks if they're in the event area.
     */
    fun updateUserLocation(newLocation: Position) {
        viewModelScope.launch(Dispatchers.Default + exceptionHandler) {
            try {
                event?.let { currentEvent ->
                    if (currentEvent.area.isPositionWithin(newLocation)) {
                        _isInArea.updateIfChanged(true)
                    }
                }
            } catch (e: Exception) {
                Log.e(::WaveViewModel.name, "Error updating user location: ${e.message}", e)
            }
        }
    }

    // ----------------------------

    /**
     * Updates the wave polygons based on the current wave state.
     */
    private suspend fun updateWavePolygons(
        polygonsHandler: ((wavePolygons: List<Polygon>, clearPolygons: Boolean) -> Unit)?
    ) {
        if (polygonsHandler == null) return

        event?.let { currentEvent ->
            if (!currentEvent.isRunning()) return

            try {
                val mode = WaveMode.ADD

                // Recalculate the cut
                lastWaveState = currentEvent.wave.getWavePolygons(null, mode)

                val polygons = lastWaveState?.traversedPolygons
                val newPolygons = polygons?.map { it.toMapLibrePolygon() } ?: listOf()

                withContext(Dispatchers.Main) {
                    polygonsHandler.invoke(newPolygons, mode != WaveMode.ADD)
                }
            } catch (e: Exception) {
                Log.e(::WaveViewModel.name, "Error updating wave polygons: ${e.message}", e)
            }
        }
    }

    // ----------------------------

    override fun onCleared() {
        super.onCleared()
        stopObservation()
    }
}
