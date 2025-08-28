package com.worldwidewaves.activities.event

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.worldwidewaves.activities.utils.WaveProgressionObserver
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.events.IWWWEvent
import java.util.UUID

abstract class AbstractEventWaveActivity(
    activateInfiniteScroll : Boolean = true
) : AbstractEventBackActivity(activateInfiniteScroll) {

    private var waveProgressionObserver: WaveProgressionObserver? = null

    private var _eventMap : AndroidEventMap? = null

    // Create a stable observer ID based on activity instance
    protected val observerId = UUID.randomUUID().toString()

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
    protected fun ObserveEventMapProgression(event: IWWWEvent, eventMap: AndroidEventMap) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        _eventMap = eventMap

        // Only create the observer once per activity instance
        LaunchedEffect(Unit) {
            if (waveProgressionObserver == null) {
                waveProgressionObserver = WaveProgressionObserver(
                    context = context,
                    scope = scope,
                    eventMap = eventMap,
                    event = event,
                    observerId = observerId
                )
                waveProgressionObserver!!.startObservation()
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