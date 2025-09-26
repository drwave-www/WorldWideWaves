package com.worldwidewaves.activities.event

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

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.play.core.splitcompat.SplitCompat
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.map.EventMapConfig
import com.worldwidewaves.shared.map.MapCameraPosition
import com.worldwidewaves.shared.ui.screens.FullMapScreen
import org.koin.android.ext.android.inject
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class EventFullMapActivity : AbstractEventWaveActivity(activateInfiniteScroll = false) {
    private val clock: IClock by inject()

    // Ensure dynamic-feature splits are available without restarting the app
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        SplitCompat.installActivity(this)
    }

    // ------------------------------------------------------------------------

    @Composable
    override fun Screen(
        modifier: Modifier,
        event: IWWWEvent,
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current

        // Construct the event map
        val eventMap =
            remember(event.id) {
                AndroidEventMap(
                    event,
                    context = context as AppCompatActivity, // Pass Activity context for wave layer UI thread operations
                    mapConfig =
                        EventMapConfig(
                            initialCameraPosition = MapCameraPosition.WINDOW,
                            autoTargetUserOnFirstLocation = true,
                        ),
                )
            }

        // Start event/map coordination and map zoom/location updates
        ObserveEventMapProgression(event, eventMap)

        // Use the shared full map screen implementation
        FullMapScreen(
            event = event,
            eventMap = eventMap,
            modifier = modifier,
            onNavigateToWave = { eventId ->
                context.startActivity(
                    Intent(context, WaveActivity::class.java).apply {
                        putExtra("eventId", eventId)
                    }
                )
            },
            mapContent = { mapModifier ->
                eventMap.Screen(modifier = mapModifier, autoMapDownload = true)
            }
        )
    }
}

