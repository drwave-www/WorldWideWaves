package com.worldwidewaves.activities

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.worldwidewaves.MainApplication
import com.worldwidewaves.activities.utils.setStatusBarColor
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_BACK_EVENT_LOCATION_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_BACK_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_BACK_PADDING
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.generated.resources.back
import com.worldwidewaves.theme.AppTheme
import com.worldwidewaves.theme.primaryColoredTextStyle
import com.worldwidewaves.theme.quinaryColoredBoldTextStyle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.android.ext.android.inject
import com.worldwidewaves.shared.generated.resources.Res as ShRes

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

abstract class AbstractEventBackActivity(
    private val activateInfiniteScroll : Boolean = true
) : MainActivity() {

    private val wwwEvents: WWWEvents by inject()

    var platform : WWWPlatform? = null

    // ----------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        platform = (applicationContext as MainApplication).platform

        // Prevent the screen from turning off
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setStatusBarColor(window)

        var selectedEvent by mutableStateOf<IWWWEvent?>(null)
        val eventId = intent.getStringExtra("eventId")

        if (eventId != null) {
            loadEvent(eventId) { event -> selectedEvent = event }
        }

        setContent {
            AppTheme {
                Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    tabManager.TabView(startScreen = { BackwardScreen(selectedEvent) })
                }
            }
        }
    }

    private fun loadEvent(eventId: String, onEventLoaded: (IWWWEvent?) -> Unit) {
        wwwEvents.addOnEventsLoadedListener {
            lifecycleScope.launch {
                onEventLoaded(wwwEvents.getEventById(eventId))
            }
        }
    }

    // ----------------------------

    @Composable
    private fun BackwardScreen(event: IWWWEvent?) {
        val scrollState = rememberScrollState()

        if (event != null) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // Back layer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = DIM_BACK_PADDING[0].dp,
                            end = DIM_BACK_PADDING[1].dp,
                            top = DIM_BACK_PADDING[2].dp,
                            bottom = DIM_BACK_PADDING[3].dp
                        )
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .clickable(onClick = { finish() }),
                            text = "< " + stringResource(ShRes.string.back),
                            style = primaryColoredTextStyle(DIM_BACK_FONTSIZE)
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth().align(Center),
                            text = event.location.uppercase(),
                            style = quinaryColoredBoldTextStyle(DIM_BACK_EVENT_LOCATION_FONTSIZE).copy(
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }

                // Content Event screen
                var screenModifier = Modifier.fillMaxSize()
                if (activateInfiniteScroll)
                    screenModifier = screenModifier.verticalScroll(scrollState)

                Box(modifier = screenModifier) { Screen(modifier = Modifier, event) }

            }
        } else { // Error, should not occur
            Text(
                text = "Event not found",
                style = primaryColoredTextStyle()
            )
        }
    }

    // ----------------------------

    @Composable
    abstract fun Screen(modifier: Modifier, event: IWWWEvent)

}