package com.worldwidewaves.activities

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

import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.worldwidewaves.activities.utils.WaveObserver
import com.worldwidewaves.compose.EventMap
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_MAP_RATIO
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_WAVE_BEREADY_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_WAVE_BEREADY_PADDING
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.generated.resources.wave_be_ready
import com.worldwidewaves.theme.quinaryColoredBoldTextStyle
import com.worldwidewaves.viewmodels.WaveViewModel
import org.jetbrains.compose.resources.stringResource
import org.maplibre.android.geometry.LatLng
import com.worldwidewaves.shared.generated.resources.Res as ShRes

class WaveActivity : AbstractEventBackActivity() {

    private val waveViewModel: WaveViewModel by viewModels()
    private var waveObserver: WaveObserver? = null

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

    @Composable
    override fun Screen(modifier: Modifier, event: IWWWEvent) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var lastKnownLocation by remember { mutableStateOf<LatLng?>(null) }

        // Calculate height based on aspect ratio and available width
        val configuration = LocalConfiguration.current
        val calculatedHeight = configuration.screenWidthDp.dp / DIM_EVENT_MAP_RATIO

        val eventMap = remember(event.id) {
            EventMap(
                platform, event,
                onLocationUpdate = { newLocation ->
                    if (lastKnownLocation == null || lastKnownLocation != newLocation) {
                        waveViewModel.updateGeolocation(newLocation)
                        lastKnownLocation = newLocation
                    }
                },
                onMapClick = { _, _ ->
                    context.startActivity(Intent(context, EventFullMapActivity::class.java).apply {
                        putExtra("eventId", event.id)
                    })
                }
            ).also {
                waveObserver = WaveObserver(context, scope, it, event, waveViewModel)
            }
        }

        LaunchedEffect(true) { // Start wave observation
            waveObserver?.startObservation()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            BeReady()
            eventMap.Screen(modifier = Modifier.fillMaxWidth().height(calculatedHeight))
        }
    }

    override fun onDestroy() {
        waveObserver?.stopObservation()
        super.onDestroy()
    }

}

// ----------------------------

@Composable
fun BeReady(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(vertical = DIM_WAVE_BEREADY_PADDING.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(ShRes.string.wave_be_ready),
            style = quinaryColoredBoldTextStyle(DIM_WAVE_BEREADY_FONTSIZE)
        )
    }
}
