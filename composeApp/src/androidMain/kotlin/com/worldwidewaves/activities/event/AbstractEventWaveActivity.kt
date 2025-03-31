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
import com.worldwidewaves.activities.utils.WaveObserver
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.viewmodels.WaveViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

abstract class AbstractEventWaveActivity(
    activateInfiniteScroll : Boolean = true
) : AbstractEventBackActivity(activateInfiniteScroll) {

    protected val waveViewModel: WaveViewModel by viewModel()
    private var waveObserver: WaveObserver? = null

    // ------------------------------------------------------------------------

    override fun onResume() {
        super.onResume()
        // Restart observation when activity is visible
        waveObserver?.startObservation()
    }

    override fun onPause() {
        // Stop observation when activity is not visible
        waveObserver?.stopObservation()
        super.onPause()
    }

    // ------------------------------------------------------------------------

    @Composable
    protected fun ObserveEventMap(event: IWWWEvent, eventMap: AndroidEventMap) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        waveObserver = WaveObserver(context, scope, eventMap, event, waveViewModel)

        LaunchedEffect(true) { // Start wave observation
            waveObserver!!.startObservation()
        }
    }

    // ------------------------------------------------------------------------

    override fun onDestroy() {
        waveObserver?.stopObservation()
        super.onDestroy()
    }

}