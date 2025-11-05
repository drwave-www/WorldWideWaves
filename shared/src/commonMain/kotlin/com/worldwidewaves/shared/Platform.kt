package com.worldwidewaves.shared

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

import androidx.compose.runtime.Composable
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.position.PositionManager
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WWWPlatform(
    val name: String,
    private val positionManager: PositionManager? = null,
) {
    private var _simulation: WWWSimulation? = null

    // -------------------------------------------------------------------- //
    //  Reactive simulation change tracking
    // -------------------------------------------------------------------- //

    /**
     * A simple counter that is incremented every time the simulation changes
     * (created, reset, or disabled).  UI or ViewModel layers can observe
     * [simulationChanged] to react and restart any time-sensitive observation
     * flows that depend on the current simulation context.
     *
     * Thread-safe: Uses StateFlow.update() for atomic, non-blocking increments.
     */
    private val _simulationChanged = MutableStateFlow(0)
    val simulationChanged: StateFlow<Int> = _simulationChanged.asStateFlow()

    /**
     * Thread-safe increment of the simulation changed counter.
     * Uses StateFlow.update() for atomic, non-blocking updates that are
     * safe to call from any thread including the main thread.
     */
    private fun incrementSimulationChanged() {
        _simulationChanged.update { it + 1 }
    }
    // -------------------------------------------------------------------- //
    //  Simulation *mode* flag (enables in-app testing UI)
    // -------------------------------------------------------------------- //

    /**
     * Indicates whether the global "simulation mode" is enabled.
     * When true, UI layers may reveal extra controls to start/stop a
     * per-event simulation.  This flag is orthogonal to an actual running
     * [WWWSimulation] (held in [_simulation]) so that the mode can be active
     * even when no simulation is currently running.
     */
    private val _simulationModeEnabled = MutableStateFlow(false)
    val simulationModeEnabled: StateFlow<Boolean> = _simulationModeEnabled.asStateFlow()

    fun enableSimulationMode() {
        _simulationModeEnabled.value = true
    }

    fun disableSimulationMode() {
        _simulationModeEnabled.value = false
    }

    fun disableSimulation() {
        _simulation = null
        // Clear simulation position from PositionManager
        positionManager?.clearPosition(PositionManager.PositionSource.SIMULATION)
        // Notify observers that the simulation context has changed
        incrementSimulationChanged()
    }

    fun setSimulation(simulation: WWWSimulation) {
        Log.i(::setSimulation.name, "Set simulation at position ${simulation.getUserPosition()}")
        _simulation = simulation
        // Update PositionManager with simulation position
        positionManager?.updatePosition(PositionManager.PositionSource.SIMULATION, simulation.getUserPosition())
        // Notify observers that the simulation context has changed
        incrementSimulationChanged()
    }

    /**
     * Atomically resets any existing simulation and sets a new one.
     * Emits only a single simulationChanged notification instead of two,
     * preventing observer restart cascade when switching simulations.
     */
    fun resetAndSetSimulation(simulation: WWWSimulation) {
        Log.i(::resetAndSetSimulation.name, "Resetting and setting simulation at position ${simulation.getUserPosition()}")

        // Clear previous simulation position if any
        if (_simulation != null) {
            positionManager?.clearPosition(PositionManager.PositionSource.SIMULATION)
        }

        // Set new simulation
        _simulation = simulation
        positionManager?.updatePosition(PositionManager.PositionSource.SIMULATION, simulation.getUserPosition())

        // Single notification for both operations
        incrementSimulationChanged()
    }

    fun getSimulation(): WWWSimulation? = _simulation

    fun isOnSimulation(): Boolean = _simulation != null

    // Current GPS-based position (PositionManager) without simulation override
    // Returns actual GPS position even when simulation is active
    fun getCurrentPosition(): Position? = positionManager?.getGPSPosition()

    fun getCurrentPositionSource(): PositionManager.PositionSource? = positionManager?.getCurrentSource()
}

/**
 * Handles application shutdown lifecycle to prevent memory leaks.
 *
 * ## Purpose
 * Coordinates cleanup of all long-lived components during app termination:
 * - Cancels ongoing coroutines
 * - Cancels event loading jobs
 * - Ensures clean resource release
 *
 * ## Usage
 * - **Android**: Called in `MainApplication.onTerminate()`
 * - **iOS**: Should be called in `AppDelegate.applicationWillTerminate()` or scene cleanup
 * - **Tests**: Called in test tearDown methods
 *
 * @property coroutineScopeProvider Provides access to coroutine scopes for cancellation
 * @property wwwEvents Events repository that needs cleanup
 */
class WWWShutdownHandler(
    private val coroutineScopeProvider: CoroutineScopeProvider,
    private val wwwEvents: WWWEvents,
) {
    /**
     * Performs app shutdown cleanup to prevent memory leaks.
     *
     * ## Cleanup Operations
     * 1. Cancels all running coroutines via CoroutineScopeProvider
     * 2. Cancels WWWEvents load job if still running
     *
     * ## Thread Safety
     * Safe to call from any thread. All cleanup operations are thread-safe.
     */
    fun onAppShutdown() {
        Log.i("WWWShutdownHandler", "Starting app shutdown cleanup")
        wwwEvents.cleanup()
        coroutineScopeProvider.cancelAllCoroutines()
        Log.i("WWWShutdownHandler", "App shutdown cleanup complete")
    }
}

// ---------------------------

expect fun localizeString(resource: StringResource): String

// ---------------------------

interface PlatformEnabler {
    /**
     * Indicates whether this is a debug build.
     * Determined at compile time by the platform-specific build configuration:
     * - Android: BuildConfig.DEBUG
     * - iOS: #if DEBUG compilation condition
     *
     * Used to control debug-only features like simulation mode.
     */
    val isDebugBuild: Boolean

    // Open an event activity / screen
    fun openEventActivity(eventId: String)

    fun openWaveActivity(eventId: String)

    fun openFullMapActivity(eventId: String)

    // Close the current activity (used for back navigation)
    fun finishActivity()

    fun toast(message: String)

    @Composable fun OpenUrl(url: String)

    fun openUrl(url: String)
}
