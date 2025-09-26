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

// Debug-only utilities -------------------------------------------------------
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.WWWGlobals.Event
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.ui.screens.UserWaveStatusText
import com.worldwidewaves.shared.ui.screens.WaveHitCounter
import com.worldwidewaves.shared.ui.screens.WaveProgressionBar
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WaveActivity : AbstractEventWaveActivity() {
    private val clock: IClock by inject()
    private val platform: WWWPlatform by inject()

    // ------------------------------------------------------------------------

    @Composable
    override fun Screen(
        modifier: Modifier,
        event: IWWWEvent,
    ) {
        val context = LocalContext.current

        // States
        var hasPlayedHitSound = false

        // Calculate height based on aspect ratio and available width
        val windowInfo = LocalWindowInfo.current
        val density = LocalDensity.current
        val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
        val calculatedHeight = screenWidthDp / Event.MAP_RATIO

        // Get choreography-related states
        val isWarmingInProgress by event.observer.isUserWarmingInProgress.collectAsState(false)
        val hitDateTime by event.observer.hitDateTime.collectAsState()
        val isGoingToBeHit by event.observer.userIsGoingToBeHit.collectAsState(false)
        val hasBeenHit by event.observer.userHasBeenHit.collectAsState(false)

        // Derive choreography active state
        val isChoreographyActive =
            remember(isWarmingInProgress, isGoingToBeHit, hasBeenHit, hitDateTime) {
                isWarmingInProgress ||
                    isGoingToBeHit ||
                    run {
                        if (hasBeenHit) {
                            val secondsSinceHit = (clock.now() - hitDateTime).inWholeSeconds
                            secondsSinceHit in 0..com.worldwidewaves.shared.WWWGlobals.WaveTiming.SHOW_HIT_SEQUENCE_SECONDS.inWholeSeconds
                        } else {
                            false
                        }
                    }
            }

        // Construct the event Map
        val eventMap =
            remember(event.id) {
                AndroidEventMap(
                    event,
                    activityContext = context, // Pass Activity context for UI thread operations
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

        // Play the hit sound when the user has been hit - FIXME: move in WaveProgressionObserver
        LaunchedEffect(isWarmingInProgress, isGoingToBeHit, hasBeenHit, hitDateTime) {
            val secondsSinceHit = (clock.now() - hitDateTime).inWholeSeconds
            if (hasBeenHit && secondsSinceHit in 0..1 && !hasPlayedHitSound) {
                event.warming.playCurrentSoundChoreographyTone()
                hasPlayedHitSound = true
            }
        }

        // Always target the closest view to have user and wave in the same view
        MapZoomAndLocationUpdate(event, eventMap)

        // EXACT historical screen composition
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(30.dp),
            ) {
                UserWaveStatusText(event)
                eventMap.Screen(
                    autoMapDownload = true,
                    Modifier
                        .fillMaxWidth()
                        .height(calculatedHeight),
                )
                WaveProgressionBar(event)

                // Always show counter in the proper position with spacing
                Spacer(modifier = Modifier.weight(1f))
                WaveHitCounter(event)
                Spacer(modifier = Modifier.height(30.dp))
            }

            com.worldwidewaves.shared.ui.components.choreographies.WaveChoreographies(event, clock, Modifier.zIndex(10f))
        }
    }
}

// ------------------------------------------------------------------------

@Composable
fun MapZoomAndLocationUpdate(
    event: IWWWEvent,
    eventMap: AndroidEventMap,
) {
    val scope = rememberCoroutineScope()
    val progression by event.observer.progression.collectAsState()
    val isInArea by event.observer.userIsInArea.collectAsState()

    LaunchedEffect(progression, isInArea) {
        if (isInArea) {
            scope.launch {
                eventMap.targetUserAndWave()
            }
        }
    }
}

