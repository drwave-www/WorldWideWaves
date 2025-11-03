package com.worldwidewaves.shared.ui.activities

/* * Copyright 2025 DrWave
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
 * limitations under the License. */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.worldwidewaves.shared.PlatformEnabler
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.map.AbstractEventMap
import com.worldwidewaves.shared.utils.CloseableCoroutineScope
import com.worldwidewaves.shared.utils.WaveProgressionObserver
import org.koin.core.component.inject
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
abstract class BaseWaveActivityScreen(
    eventId: String,
    platformEnabler: PlatformEnabler,
    showSplash: Boolean = false,
) : BaseEventBackgroundScreen(eventId, platformEnabler, showSplash) {
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

    private var waveProgressionObserver: WaveProgressionObserver? = null

    protected var eventMap: AbstractEventMap<*>? = null

    // ------------------------------------------------------------------------

    @Composable fun asComponent(
        eventMapBuilder: (IWWWEvent) -> AbstractEventMap<*>,
        onFinish: () -> Unit,
    ) {
        var eventMap by remember { mutableStateOf<AbstractEventMap<*>?>(null) }
        var event by remember { mutableStateOf<IWWWEvent?>(null) }

        this.Load()
        this.onEventLoaded { loadedEvent ->
            event = loadedEvent
            // Only create eventMap if it doesn't exist yet (prevent multiple instances)
            if (eventMap == null) {
                eventMap = eventMapBuilder(loadedEvent)
            }
        }

        event?.let { evt ->
            eventMap?.let {
                val currentEventMap = eventMap
                this.Draw(
                    event = evt,
                    eventMap = currentEventMap!!,
                    onFinish = onFinish,
                )
            }
        }
    }

    // ------------------------------------------------------------------------

    override fun onResume() {
        // Call parent to handle global sound choreography
        super.onResume()
        // Restart observation when activity is visible
        waveProgressionObserver?.startObservation()
    }

    override fun onPause() {
        // Call parent to handle global sound choreography
        super.onPause()
        // Stop observation when activity is not visible
        // Only pause to keep the same StateFlow instances alive for UI collectors
        waveProgressionObserver?.pauseObservation()
    }

    // ------------------------------------------------------------------------

    @Composable
    fun Draw(
        event: IWWWEvent,
        eventMap: AbstractEventMap<*>,
        onFinish: (() -> Unit)? = null,
    ) {
        this.eventMap = eventMap

        // Start event/map coordination
        this.ObserveEventMapProgression(event)
        super.Draw(onFinish)
    }

    // -------------------------------------------------------------------------

    @Composable
    protected fun ObserveEventMapProgression(event: IWWWEvent) {
        // Restart observation whenever simulation context changes
        val simulationChanged by platform.simulationChanged.collectAsState()
        val simMode by platform.simulationModeEnabled.collectAsState()

        // Only create the observer once per activity instance
        LaunchedEffect(Unit) {
            if (waveProgressionObserver == null) {
                waveProgressionObserver =
                    WaveProgressionObserver(
                        scope = appScope,
                        eventMap = eventMap,
                        event = event,
                    )
                waveProgressionObserver!!.startObservation()
            }
        }

        // Propagate simulation changes to both observer layers:
        // 1. EventObserver (business logic) - handles event state, progression, etc.
        // 2. WaveProgressionObserver (UI layer) - renders polygons on map
        // Both need restart because WaveProgressionObserver's polygon clearing logic
        // only executes in startObservation(), not automatically when flows change
        LaunchedEffect(simulationChanged) {
            // Restart EventObserver (business logic layer) for all events
            events.restartObserversOnSimulationChange()
            // Also restart WaveProgressionObserver (UI layer) to clear polygons and reset state
            waveProgressionObserver?.stopObservation()
            waveProgressionObserver?.startObservation()
        }

        // Also react when simulation *mode* is toggled; only restart if a
        // simulation is actually running to avoid unnecessary work.
        LaunchedEffect(simMode) {
            if (platform.isOnSimulation()) {
                events.restartObserversOnSimulationChange()
                // Also restart WaveProgressionObserver to clear polygons
                waveProgressionObserver?.stopObservation()
                waveProgressionObserver?.startObservation()
            }
        }
    }

    // ------------------------------------------------------------------------

    override fun onDestroy() {
        // Call parent to handle global sound choreography cleanup
        super.onDestroy()
        waveProgressionObserver?.stopObservation()
        waveProgressionObserver = null
    }
}
