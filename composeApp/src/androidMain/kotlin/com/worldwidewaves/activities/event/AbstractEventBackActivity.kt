package com.worldwidewaves.activities.event

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

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.worldwidewaves.activities.MainActivity
import com.worldwidewaves.activities.utils.setStatusBarColor
import com.worldwidewaves.compose.DownloadProgressIndicator
import com.worldwidewaves.compose.ErrorMessage
import com.worldwidewaves.compose.LoadingIndicator
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_BACK_EVENT_LOCATION_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_BACK_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_BACK_PADDING
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.generated.resources.back
import com.worldwidewaves.shared.generated.resources.map_checking_state
import com.worldwidewaves.shared.generated.resources.map_downloading
import com.worldwidewaves.shared.generated.resources.map_error_download
import com.worldwidewaves.shared.generated.resources.map_loading
import com.worldwidewaves.shared.generated.resources.map_starting_download
import com.worldwidewaves.theme.AppTheme
import com.worldwidewaves.theme.primaryColoredTextStyle
import com.worldwidewaves.theme.quinaryColoredBoldTextStyle
import com.worldwidewaves.viewmodels.MapFeatureState
import com.worldwidewaves.viewmodels.MapViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.android.ext.android.inject
import com.worldwidewaves.shared.generated.resources.Res as ShRes

abstract class AbstractEventBackActivity(
    private val activateInfiniteScroll : Boolean = true
) : MainActivity() {

    private val mapViewModel by viewModels<MapViewModel>()
    private val wwwEvents: WWWEvents by inject()
    private var selectedEvent by mutableStateOf<IWWWEvent?>(null)

    // ----------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent the screen from turning off when on event screens
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setStatusBarColor(window)

        val eventId = intent.getStringExtra("eventId")

        // Download or Load the map as app feature
        if (eventId != null) {
            lifecycleScope.launch {
                // Check if map is available
                mapViewModel.checkIfMapIsAvailable(eventId)

                // Then manage the state
                mapViewModel.featureState.collect { state ->
                    when (state) {
                        is MapFeatureState.Available, is MapFeatureState.Installed -> {
                            // Load event when map is available
                            trackEventLoading(eventId) { event -> selectedEvent = event }
                        }
                        is MapFeatureState.NotAvailable -> {
                            // Download map if not available
                            mapViewModel.downloadMap(eventId) {
                                trackEventLoading(eventId) { event -> selectedEvent = event }
                            }
                        }
                        else -> { /* Do nothing, managed by BackwardScreen() for consistency */ }
                    }
                }
            }
        }

        setContent {
            AppTheme {
                Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxSize()) {
                    tabManager.TabView(startScreen = {
                        BackwardScreen(eventId, selectedEvent, mapViewModel)
                    })
                }
            }
        }
    }

    // ----------------------------

    private fun trackEventLoading(eventId: String, onEventLoaded: (IWWWEvent?) -> Unit) {
        wwwEvents.addOnEventsLoadedListener {
            lifecycleScope.launch {
                onEventLoaded(wwwEvents.getEventById(eventId))
            }
        }
    }

    // ----------------------------

    @Composable
    private fun BackwardScreen(eventId: String?, event: IWWWEvent?, mapViewModel: MapViewModel) {
        val scrollState = rememberScrollState()
        val mapState by mapViewModel.featureState.collectAsState()

        Column(modifier = Modifier.fillMaxSize()) {

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
                    if (event != null) {
                        Text(
                            modifier = Modifier.fillMaxWidth().align(Center),
                            text = event.location.uppercase(),
                            style = quinaryColoredBoldTextStyle(DIM_BACK_EVENT_LOCATION_FONTSIZE).copy(
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }

            // Default page to manage initializations, download process and errors
            if (event != null) { // Event has been loaded

                    // Content Event screen
                    var screenModifier = Modifier.fillMaxSize()
                    if (activateInfiniteScroll)
                        screenModifier = screenModifier.verticalScroll(scrollState)

                    Box(modifier = screenModifier) {
                        Screen(modifier = Modifier.fillMaxSize(), event)
                    }

            } else {
                if (eventId == null) { // Should not occur, activity has been called with empty data

                    Text(
                        text = "Event not found",
                        style = primaryColoredTextStyle()
                    )

                } else { // Map is not loaded yet, handle map states to show appropriate UI

                    fun cancelDownload() {
                        mapViewModel.cancelDownload()
                        finish()
                    }

                    Box(modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Center
                    ) {

                        when (val state = mapState) {

                            is MapFeatureState.NotChecked -> {
                                // Initial state, waiting for check
                                LoadingIndicator(message = stringResource(ShRes.string.map_checking_state))
                            }

                            is MapFeatureState.Downloading -> {
                                // Show download progress
                                DownloadProgressIndicator(
                                    progress = state.progress,
                                    message = stringResource(
                                        ShRes.string.map_downloading,
                                        state.progress
                                    ),
                                    ::cancelDownload
                                )
                            }

                            is MapFeatureState.Pending -> {
                                // Waiting for download to start
                                LoadingIndicator(message = stringResource(ShRes.string.map_starting_download))
                            }

                            is MapFeatureState.Retrying -> {
                                // Show retry progress
                                DownloadProgressIndicator(
                                    message = "Retrying download (${state.attempt}/${state.maxAttempts})...",
                                    onCancel = ::cancelDownload
                                )
                            }

                            is MapFeatureState.Failed -> {
                                // Show error message with retry button
                                ErrorMessage(
                                    message = state.errorMessage
                                        ?: stringResource(ShRes.string.map_error_download),
                                    onRetry = { mapViewModel.downloadMap(eventId) {
                                        trackEventLoading(eventId) { event -> selectedEvent = event }
                                    } }
                                )
                            }

                            else -> {
                                // For other states, show a generic loading indicator
                                LoadingIndicator(message = stringResource(ShRes.string.map_loading))
                            }
                        }
                    }
                }
            }
        }
    }

    // Main activity UI building methode to be implemented --------------------
    @Composable
    abstract fun Screen(modifier: Modifier, event: IWWWEvent)

}