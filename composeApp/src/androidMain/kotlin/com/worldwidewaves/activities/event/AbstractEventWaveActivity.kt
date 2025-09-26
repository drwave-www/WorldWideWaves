package com.worldwidewaves.activities.event

/*
 * Copyright 2024 DrWave
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.map.AbstractEventMap
import com.worldwidewaves.shared.utils.CloseableCoroutineScope
import com.worldwidewaves.shared.utils.WaveProgressionObserver
import org.koin.android.ext.android.inject

/**
 * Abstract base activity for event wave features.
 * Handles lifecycle management and observation of wave progression,
 * providing a stable observer and coroutine scope for subclasses.
 */
abstract class AbstractEventWaveActivity(
    activateInfiniteScroll: Boolean = true,
) : AbstractEventBackActivity(activateInfiniteScroll) {
    /**
     * Application-level CoroutineScope (stays alive as long as the process lives)
     * Injected from DI to avoid cancelling jobs when the Composable scope disappears.
     */
    private val appScope: CloseableCoroutineScope by inject()

    /**
     * Platform single instance – used to observe simulation mode / simulation
     * changes so we can restart observers accordingly.
     */
    private val platform: WWWPlatform by inject()

    /**
     * Central events repository – provides helper to restart observers globally.
     */
    private val events: WWWEvents by inject()

    private var waveProgressionObserver: WaveProgressionObserver? = null

    private var _eventMap: AbstractEventMap<*>? = null

    // ------------------------------------------------------------------------

    override fun onResume() {
        super.onResume()

        // Restart observation when activity is visible
        waveProgressionObserver?.startObservation()
    }

    override fun onPause() {
        // Stop observation when activity is not visible
        // Only pause to keep the same StateFlow instances alive for UI collectors
        waveProgressionObserver?.pauseObservation()
        super.onPause()
    }

    // ------------------------------------------------------------------------

    @Composable
    protected fun <T> ObserveEventMapProgression(
        event: IWWWEvent,
        eventMap: AbstractEventMap<T>,
    ) {
        // Restart observation whenever simulation context changes
        val simulationChanged by platform.simulationChanged.collectAsState()
        val simMode by platform.simulationModeEnabled.collectAsState()

        _eventMap = eventMap

        // Only create the observer once per activity instance
        LaunchedEffect(Unit) {
            if (waveProgressionObserver == null) {
                android.util.Log.i("AbstractEventWaveActivity", "Creating WaveProgressionObserver for event ${event.id}")
                waveProgressionObserver =
                    WaveProgressionObserver(
                        scope = appScope,
                        eventMap = eventMap,
                        event = event,
                    )
                android.util.Log.i("AbstractEventWaveActivity", "Starting WaveProgressionObserver for event ${event.id}")
                waveProgressionObserver!!.startObservation()
            } else {
                android.util.Log.i("AbstractEventWaveActivity", "WaveProgressionObserver already exists for event ${event.id}")
            }
        }

        // Propagate simulation changes to all relevant events via central helper
        LaunchedEffect(simulationChanged) {
            events.restartObserversOnSimulationChange()
        }

        // Also react when simulation *mode* is toggled; only restart if a
        // simulation is actually running to avoid unnecessary work.
        LaunchedEffect(simMode) {
            if (platform.isOnSimulation()) {
                events.restartObserversOnSimulationChange()
            }
        }
    }

    // ------------------------------------------------------------------------

    override fun onDestroy() {
        waveProgressionObserver?.stopObservation()
        waveProgressionObserver = null
        super.onDestroy()
    }
}
