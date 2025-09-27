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
import androidx.compose.runtime.LaunchedEffect
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
import com.worldwidewaves.shared.ui.activities.UIProperties
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.ic_arrow_back
import com.worldwidewaves.shared.ui.theme.sharedPrimaryColoredTextStyle
import com.worldwidewaves.shared.ui.theme.sharedQuinaryColoredBoldTextStyle
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.jetbrains.compose.resources.painterResource
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
abstract class WWWAbstractEventBackActivity(
    val eventId: String,
    platformEnabler: PlatformEnabler,
    showSplash: Boolean = false,
) : WWWMainActivity(platformEnabler, showSplash) {
    /**
     * Controls whether the event screen should be scrollable.
     * Set to false for full-screen content like maps that shouldn't scroll.
     */
    protected open val isScrollable: Boolean = true
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var selectedEvent by mutableStateOf<IWWWEvent?>(null)
    private var onFinish by mutableStateOf<(() -> Unit)?>(null)
    private val waitingHandlers: MutableList<(IWWWEvent) -> Unit> = mutableListOf()

    // iOS FIX: Removed init{} block to prevent Dispatchers.Main deadlock
    // Event tracking now must be triggered from @Composable LaunchedEffect

    /**
     * ⚠️ iOS CRITICAL: Start event tracking.
     * Must be called from @Composable LaunchedEffect, never from init{} or constructor.
     */
    protected suspend fun start() {
        initialize()
        trackEventLoading(eventId)
    }

    @Composable fun Load() {
        LaunchedEffect(Unit) {
            start()
        }
    }

    // ------------------------------------------------------------------------

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    @Composable
    fun Draw(onFinish: (() -> Unit)? = null) {
        this.onFinish = onFinish
        super.Draw(uiProperties = null)
    }

    @Composable
    override fun Draw(uiProperties: UIProperties?) {
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

    private fun setEvent(event: IWWWEvent?) {
        if (event != null) {
            selectedEvent = event
            waitingHandlers.forEach { it(event) }
            waitingHandlers.clear() // Clear the handlers after calling them
        }
    }

    private fun trackEventLoading(eventId: String) {
        events.addOnEventsLoadedListener {
            setEvent(events.getEventById(eventId))
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
                // Content Event screen - conditionally apply scrolling
                val screenModifier =
                    if (isScrollable) {
                        Modifier.fillMaxSize().verticalScroll(scrollState)
                    } else {
                        Modifier.fillMaxSize()
                    }
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
