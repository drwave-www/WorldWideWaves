package com.worldwidewaves.activities

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.worldwidewaves.activities.utils.setStatusBarColor
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_BACK_EVENT_LOCATION_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_BACK_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_BACK_PADDING
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.generated.resources.back
import com.worldwidewaves.shared.generated.resources.map_cancel_download
import com.worldwidewaves.shared.generated.resources.map_checking_state
import com.worldwidewaves.shared.generated.resources.map_downloading
import com.worldwidewaves.shared.generated.resources.map_error_download
import com.worldwidewaves.shared.generated.resources.map_loading
import com.worldwidewaves.shared.generated.resources.map_retry_download
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
    protected val platform: WWWPlatform by inject()

    // ----------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent the screen from turning off
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setStatusBarColor(window)

        var selectedEvent by mutableStateOf<IWWWEvent?>(null)
        val eventId = intent.getStringExtra("eventId")

        if (eventId != null) {
            lifecycleScope.launch {
                // Check if map is available
                mapViewModel.checkIfMapIsAvailable(eventId)

                // Then manage the state
                mapViewModel.featureState.collect { state ->
                    when (state) {
                        is MapFeatureState.Available, is MapFeatureState.Installed -> {
                            // Load event when map is available
                            loadEvent(eventId) { event -> selectedEvent = event }
                        }
                        is MapFeatureState.NotAvailable -> {
                            // Download map if not available
                            mapViewModel.downloadMap(eventId)
                        }
                        else -> { /* Do nothing, managed by BackwardScreen() */ }
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

    private fun loadEvent(eventId: String, onEventLoaded: (IWWWEvent?) -> Unit) {
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
                                    )
                                )
                            }

                            is MapFeatureState.Pending -> {
                                // Waiting for download to start
                                LoadingIndicator(message = stringResource(ShRes.string.map_starting_download))
                            }

                            is MapFeatureState.Retrying -> {
                                // Show retry progress
                                DownloadProgressIndicator(
                                    message = "Retrying download (${state.attempt}/${state.maxAttempts})..."
                                )
                            }

                            is MapFeatureState.Failed -> {
                                // Show error message with retry button
                                ErrorMessage(
                                    message = state.errorMessage
                                        ?: stringResource(ShRes.string.map_error_download),
                                    onRetry = { mapViewModel.downloadMap(eventId) }
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

    // ----------------------------

    // Reusable composable for showing loading state
    @Composable
    private fun LoadingIndicator(message: String) {
        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }

    // Reusable composable for showing download progress
    @Composable
    private fun DownloadProgressIndicator(
        progress: Int = 0,
        message: String
    ) {
        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // Show progress percentage
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Linear progress indicator
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Cancel button
            Button(
                onClick = {
                            mapViewModel.cancelDownload()
                            finish()
                          },
                modifier = Modifier
            ) {
                Text(text = stringResource(ShRes.string.map_cancel_download))
            }
        }
    }

    // Reusable composable for showing error messages
    @Composable
    private fun ErrorMessage(
        message: String,
        onRetry: () -> Unit
    ) {
        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Error",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(ShRes.string.map_retry_download))
            }
        }
    }

    // ----------------------------

    @Composable
    abstract fun Screen(modifier: Modifier, event: IWWWEvent)

}