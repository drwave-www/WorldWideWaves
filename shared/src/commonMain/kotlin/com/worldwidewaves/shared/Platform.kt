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

import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.Log
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WWWPlatform(
    val name: String,
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
     */
    private val _simulationChanged = MutableStateFlow(0)
    val simulationChanged: StateFlow<Int> = _simulationChanged.asStateFlow()

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
        // Notify observers that the simulation context has changed
        _simulationChanged.value = _simulationChanged.value + 1
    }

    fun setSimulation(simulation: WWWSimulation) {
        Log.i(::setSimulation.name, "Set simulation to ${simulation.now()} and ${simulation.getUserPosition()}")
        _simulation = simulation
        // Notify observers that the simulation context has changed
        _simulationChanged.value = _simulationChanged.value + 1
    }

    fun getSimulation(): WWWSimulation? = _simulation

    fun isOnSimulation(): Boolean = _simulation != null
}

class WWWShutdownHandler(
    private val coroutineScopeProvider: CoroutineScopeProvider,
) {
    fun onAppShutdown() {
        coroutineScopeProvider.cancelAllCoroutines()
    }
}

// ---------------------------

expect fun localizeString(resource: StringResource): String

// ---------------------------

expect suspend fun readGeoJson(eventId: String): String?

expect suspend fun getMapFileAbsolutePath(
    eventId: String,
    extension: String,
): String?

expect fun cachedFileExists(fileName: String): Boolean

expect fun cachedFilePath(fileName: String): String?

expect fun cacheStringToFile(
    fileName: String,
    content: String,
): String

expect suspend fun cacheDeepFile(fileName: String)

expect fun getCacheDir(): String

// ---------------------------------------------------------------------------
//  Cache maintenance helpers (platform-specific actual implementations)
// ---------------------------------------------------------------------------

expect fun clearEventCache(eventId: String)

expect fun isCachedFileStale(fileName: String): Boolean

expect fun updateCacheMetadata(fileName: String)
