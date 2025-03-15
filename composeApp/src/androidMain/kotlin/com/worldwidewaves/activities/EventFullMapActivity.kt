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

import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.worldwidewaves.compose.ButtonWave
import com.worldwidewaves.compose.EventMap
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_INT_PADDING
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_FOLLOW_ME_IMAGE_SIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_FOLLOW_WAVE_IMAGE_SIZE
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.event_follow_me_off
import com.worldwidewaves.shared.generated.resources.event_follow_me_on
import com.worldwidewaves.shared.generated.resources.event_follow_wave_off
import com.worldwidewaves.shared.generated.resources.event_follow_wave_on
import com.worldwidewaves.shared.generated.resources.follow_me_active
import com.worldwidewaves.shared.generated.resources.follow_me_inactive
import com.worldwidewaves.shared.generated.resources.follow_wave_active
import com.worldwidewaves.shared.generated.resources.follow_wave_inactive
import com.worldwidewaves.viewmodels.WaveViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.maplibre.android.geometry.LatLng
import com.worldwidewaves.shared.generated.resources.Res as ShRes

class EventFullMapActivity : AbstractEventBackActivity(activateInfiniteScroll = false) {

    private val waveViewModel: WaveViewModel by viewModels()

    @Composable
    override fun Screen(modifier: Modifier, event: IWWWEvent) {
        val context = LocalContext.current
        var lastKnownLocation by remember { mutableStateOf<LatLng?>(null) }

        val eventMap = EventMap(platform, event,
            onLocationUpdate = { newLocation ->
                if (lastKnownLocation == null || lastKnownLocation != newLocation) {
                    waveViewModel.updateGeolocation(newLocation)
                    lastKnownLocation = newLocation
                }
            },
            mapConfig = EventMap.EventMapConfig(
                initialCameraPosition = EventMap.MapCameraPosition.WINDOW
            )
        )

        waveViewModel.startObservation(event) { wavePolygons, clearPolygons ->
            eventMap.updateWavePolygons(context, wavePolygons, clearPolygons)
        }

        Box(modifier = modifier.fillMaxWidth()) {
            ButtonWave(event = event, modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp))
            eventMap.Screen(modifier = Modifier.fillMaxSize())
            MapActions(eventMap, waveViewModel)
        }
    }

    @Composable
    fun MapActions(eventMap: EventMap, waveViewModel: WaveViewModel, modifier: Modifier = Modifier) {
        val scope = rememberCoroutineScope()
        val eventStatus by waveViewModel.eventStatus.collectAsState()
        val isInArea by waveViewModel.isInArea.collectAsState()

        val isRunning = eventStatus == Status.RUNNING

        Box(
            modifier = modifier.fillMaxSize()
                .padding(end = DIM_DEFAULT_INT_PADDING.dp, bottom = DIM_DEFAULT_INT_PADDING.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(DIM_DEFAULT_INT_PADDING.dp)) {
                Image(
                    modifier = Modifier
                        .size(DIM_EVENT_FOLLOW_WAVE_IMAGE_SIZE.dp)
                        .clickable {
                            scope.launch {
                                // eventMap.followWave()
                            }
                        },
                    painter = painterResource(if (isRunning) ShRes.drawable.follow_wave_active else ShRes.drawable.follow_wave_inactive),
                    contentDescription = stringResource(if (isRunning) ShRes.string.event_follow_wave_on else Res.string.event_follow_wave_off)
                )
                Image(
                    modifier = Modifier
                        .size(DIM_EVENT_FOLLOW_ME_IMAGE_SIZE.dp)
                        .clickable {
                            scope.launch {
                                // eventMap.followUser()
                            }
                        },
                    painter = painterResource(if (isInArea) ShRes.drawable.follow_me_active else ShRes.drawable.follow_me_inactive),
                    contentDescription = stringResource(if (isInArea) ShRes.string.event_follow_me_on else ShRes.string.event_follow_me_off)
                )
            }
        }
    }

}