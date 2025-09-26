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
@file:OptIn(ExperimentalTime::class)

package com.worldwidewaves.activities.event

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.ui.screens.WaveScreen
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WaveActivity : AbstractEventWaveActivity() {

    @Composable
    override fun Screen(
        modifier: Modifier,
        event: IWWWEvent,
    ) {
        val context = LocalContext.current

        // Construct the event Map
        val eventMap =
            remember(event.id) {
                AndroidEventMap(
                    event,
                    context = this@WaveActivity as AppCompatActivity,
                    onMapClick = {
                        context.startActivity(
                            Intent(context, EventFullMapActivity::class.java).apply {
                                putExtra("eventId", event.id)
                            },
                        )
                    },
                )
            }

        // Start event/map coordination
        ObserveEventMapProgression(event, eventMap)

        // Use the complete shared wave screen with exact working behavior
        WaveScreen(
            event = event,
            eventMap = eventMap,
            modifier = modifier,
            mapContent = { mapModifier ->
                eventMap.Screen(
                    autoMapDownload = true,
                    modifier = mapModifier,
                )
            }
        )
    }
}


