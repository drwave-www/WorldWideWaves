package com.worldwidewaves.activities

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldwidewaves.compose.ButtonWave
import com.worldwidewaves.compose.EventMap
import com.worldwidewaves.shared.events.IWWWEvent

class EventFullMapActivity : AbstractEventBackActivity(activateInfiniteScroll = false) {

    @Composable
    override fun Screen(modifier: Modifier, event: IWWWEvent) {
        Box(modifier = modifier.fillMaxWidth()) {
            EventMap(event, mapConfig = EventMap.EventMapConfig(
                initialCameraPosition = EventMap.MapCameraPosition.WINDOW
            )).Screen(modifier = Modifier.fillMaxSize())

            ButtonWave(event = event, modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp))
        }
    }

}