package com.worldwidewaves.shared.ui.activities

/* * Copyright 2025 DrWave
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
 * limitations under the License. */

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
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.PlatformEnabler
import com.worldwidewaves.shared.WWWGlobals.BackNav
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.ic_arrow_back
import com.worldwidewaves.shared.ui.theme.sharedPrimaryColoredTextStyle
import com.worldwidewaves.shared.ui.theme.sharedQuinaryColoredBoldTextStyle
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.core.component.inject
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
abstract class WWWAbstractEventBackActivity(
    val eventId: String,
    platformEnabler: PlatformEnabler,
    showSplash: Boolean = false,
) : WWWMainActivity(platformEnabler, showSplash) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val wwwEvents: WWWEvents by inject()
    private var selectedEvent by mutableStateOf<IWWWEvent?>(null)
    private var onFinish by mutableStateOf<(() -> Unit)?>(null)
    private val waitingHandlers: MutableList<(IWWWEvent) -> Unit> = mutableListOf()

    init {
        scope.launch {
            trackEventLoading(eventId)
        }
    }

    @Composable
    fun Draw(onFinish: (() -> Unit)? = null) {
        this.onFinish = onFinish
        super.Draw()
    }

    @Composable
    override fun Draw() {
        Draw(onFinish = null)
    }

    @Composable
    override fun Screen() {
        tabManager.TabView(startScreen = { BackwardScreen() })
    }

    fun onEventLoaded(handler: (IWWWEvent) -> Unit) {
        if (selectedEvent != null) {
            handler(selectedEvent!!)
        } else {
            waitingHandlers.add(handler)
        }
    }

    /**
     * Handle back button press - should be called from Android activity's onBackPressed()
     */
    fun handleBackPress(): Boolean =
        if (onFinish != null) {
            onFinish?.invoke()
            true // Back press was handled
        } else {
            false // Back press was not handled
        }

    private fun setEvent(event: IWWWEvent?) {
        if (event != null) {
            selectedEvent = event
            waitingHandlers.forEach { it(event) }
            waitingHandlers.clear() // Clear the handlers after calling them
        }
    }

    private fun trackEventLoading(eventId: String) {
        wwwEvents.addOnEventsLoadedListener {
            setEvent(wwwEvents.getEventById(eventId))
        }
    }

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
                                .clickable { onFinish?.invoke() },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
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
                                sharedQuinaryColoredBoldTextStyle(BackNav.EVENT_LOCATION_FONTSIZE).copy(
                                    textAlign = TextAlign.Center,
                                ),
                        )
                    }
                }
            }

            // Default page to manage initializations, download process and errors
            if (selectedEvent != null) { // Event has been loaded
                // Content Event screen
                val screenModifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                Box(modifier = screenModifier) {
                    Event(selectedEvent!!, modifier = Modifier.fillMaxSize())
                }
            } else {
                Text(
                    text = stringResource(MokoRes.strings.events_not_found_loading),
                    style = sharedPrimaryColoredTextStyle(),
                )
            }
        }
    }

    @Composable
    protected abstract fun Event(
        event: IWWWEvent,
        modifier: Modifier,
    )
}
