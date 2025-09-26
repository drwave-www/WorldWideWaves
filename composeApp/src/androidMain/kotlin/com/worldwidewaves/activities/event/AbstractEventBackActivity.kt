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

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.worldwidewaves.R
import com.worldwidewaves.activities.MainActivity
import com.worldwidewaves.activities.utils.hideStatusBar
import com.worldwidewaves.activities.utils.setStatusBarColor
import com.worldwidewaves.shared.ui.components.SimulationModeChip
import com.worldwidewaves.shared.ui.components.navigation.BackwardScreen as SharedBackwardScreen
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.BackNav
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.ui.theme.SharedWorldWideWavesTheme
import com.worldwidewaves.shared.ui.theme.sharedPrimaryColoredTextStyle
import com.worldwidewaves.shared.ui.theme.sharedQuinaryColoredBoldTextStyle
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Abstract base activity for event-related screens with back navigation.
 * Handles event loading, status bar color, and provides a composable back screen.
 * Subclasses must implement the [Screen] composable to display event-specific content.
 */
abstract class AbstractEventBackActivity(
    private val activateInfiniteScroll: Boolean = true,
) : MainActivity() {
    private val wwwEvents: WWWEvents by inject()
    private val platform: WWWPlatform by inject()
    private var selectedEvent by mutableStateOf<IWWWEvent?>(null)

    // ----------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val eventId = intent.getStringExtra("eventId")

        // Prevent the screen from turning off when on event screens
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setStatusBarColor(window)
        hideStatusBar(this)

        // Download or Load the map as app feature
        if (eventId != null) {
            lifecycleScope.launch {
                trackEventLoading(eventId) { event -> selectedEvent = event }
            }
        }

        setContent {
            SharedWorldWideWavesTheme {
                Surface(
                    modifier =
                        Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .fillMaxSize(),
                ) {
                    // Stack content & global simulation-mode chip
                    Box(modifier = Modifier.fillMaxSize()) {
                        tabManager.TabView(startScreen = { BackwardScreen() })

                        // Global Simulation-Mode overlay
                        SimulationModeChip(platform)
                    }
                }
            }
        }
    }

    // ----------------------------

    private fun trackEventLoading(
        eventId: String,
        onEventLoaded: (IWWWEvent?) -> Unit,
    ) {
        wwwEvents.addOnEventsLoadedListener {
            lifecycleScope.launch {
                onEventLoaded(wwwEvents.getEventById(eventId))
            }
        }
    }

    // ----------------------------

    @Composable
    private fun BackwardScreen() {
        val scrollState = rememberScrollState()

        Column(modifier = Modifier.fillMaxSize()) {
            // Back layer
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = BackNav.PADDING[0].dp,
                            end = BackNav.PADDING[1].dp,
                            top = BackNav.PADDING[2].dp,
                            bottom = BackNav.PADDING[3].dp,
                        ),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .clickable { finish() },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(MokoRes.strings.back),
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .padding(end = 4.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(MokoRes.strings.back),
                            style = sharedPrimaryColoredTextStyle(BackNav.FONTSIZE),
                        )
                    }
                    if (selectedEvent != null) {
                        Text(
                            modifier = Modifier.fillMaxWidth().align(Center),
                            text = stringResource(selectedEvent!!.getLocation()),
                            style =
                                quinaryColoredBoldTextStyle(BackNav.EVENT_LOCATION_FONTSIZE).copy(
                                    textAlign = TextAlign.Center,
                                ),
                        )
                    }
                }
            }

            // Default page to manage initializations, download process and errors
            if (selectedEvent != null) { // Event has been loaded

                // Content Event screen
                var screenModifier = Modifier.fillMaxSize()
                if (activateInfiniteScroll) {
                    screenModifier = screenModifier.verticalScroll(scrollState)
                }

                Box(modifier = screenModifier) {
                    Screen(modifier = Modifier.fillMaxSize(), selectedEvent!!)
                }
            } else {
                Text(
                    text = stringResource(MokoRes.strings.events_not_found_loading),
                    style = primaryColoredTextStyle(),
                )
            }
        }
    }

    // Main activity UI building method to implement ----------------------------
    @Composable
    abstract fun Screen(
        modifier: Modifier,
        event: IWWWEvent,
    )
}
