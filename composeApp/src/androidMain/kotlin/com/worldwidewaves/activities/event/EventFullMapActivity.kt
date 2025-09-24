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

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.play.core.splitcompat.SplitCompat
import com.worldwidewaves.BuildConfig
import com.worldwidewaves.compose.common.ButtonWave
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.WWWGlobals.Event
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.generated.resources.target_me_active
import com.worldwidewaves.shared.generated.resources.target_me_inactive
import com.worldwidewaves.shared.generated.resources.target_wave_active
import com.worldwidewaves.shared.generated.resources.target_wave_inactive
import com.worldwidewaves.shared.map.EventMapConfig
import com.worldwidewaves.shared.map.MapCameraPosition
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import com.worldwidewaves.shared.generated.resources.Res as ShRes

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
        val eventStatus by event.observer.eventStatus.collectAsState()
        val progression by event.observer.progression.collectAsState()
        val endDateTime by produceState<Instant?>(initialValue = null, key1 = event, key2 = progression) {
            value = event.getEndDateTime()
        }
        val isInArea by event.observer.userIsInArea.collectAsState()


        // Construct the event map
        val eventMap =
            remember(event.id) {
                AndroidEventMap(
                    event,
                    mapConfig =
                        EventMapConfig(
                            initialCameraPosition = MapCameraPosition.WINDOW,
                            autoTargetUserOnFirstLocation = true,
                        ),
                )
            }

        // Start event/map coordination
        ObserveEventMapProgression(event, eventMap)

        // Screen composition
        Box(modifier = modifier.fillMaxSize()) {
            eventMap.Screen(modifier = Modifier.fillMaxSize(), autoMapDownload = true)
            ButtonWave(event.id, eventStatus, endDateTime, clock, isInArea, Modifier.align(Alignment.TopCenter).padding(top = 40.dp))
            MapActions(event, eventMap, clock)
        }
    }
}

// ----------------------------------------------------------------------------

@OptIn(ExperimentalTime::class)
@Composable
fun MapActions(
    event: IWWWEvent,
    eventMap: AndroidEventMap,
    clock: IClock,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val eventStatus by event.observer.eventStatus.collectAsState(Status.UNDEFINED)
    val isInArea by event.observer.userIsInArea.collectAsState()

    val isRunning = eventStatus == Status.RUNNING

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(end = Dimensions.DEFAULT_INT_PADDING.dp, bottom = Dimensions.DEFAULT_INT_PADDING.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.DEFAULT_INT_PADDING.dp)) {
            Image(
                modifier =
                    Modifier
                        .size(Event.TARGET_WAVE_IMAGE_SIZE.dp)
                        .clickable {
                            if (isRunning && (clock.now() > event.getWaveStartDateTime())) {
                                eventMap.markUserInteracted()
                                scope.launch {
                                    eventMap.targetWave()
                                }
                            }
                        },
                painter = painterResource(if (isRunning) ShRes.drawable.target_wave_active else ShRes.drawable.target_wave_inactive),
                contentDescription =
                    stringResource(
                        if (isRunning) MokoRes.strings.event_target_wave_on else MokoRes.strings.event_target_wave_off,
                    ),
            )
            Image(
                modifier =
                    Modifier
                        .size(Event.TARGET_ME_IMAGE_SIZE.dp)
                        .clickable {
                            if (isInArea) {
                                eventMap.markUserInteracted()
                                scope.launch {
                                    eventMap.targetUser()
                                }
                            }
                        },
                painter = painterResource(if (isInArea) ShRes.drawable.target_me_active else ShRes.drawable.target_me_inactive),
                contentDescription =
                    stringResource(
                        if (isInArea) MokoRes.strings.event_target_me_on else MokoRes.strings.event_target_me_off,
                    ),
            )
        }
    }
}
