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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.PlatformEnabler
import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.sound.GlobalSoundChoreographyManager
import com.worldwidewaves.shared.ui.AboutTabScreen
import com.worldwidewaves.shared.ui.DebugTabScreen
import com.worldwidewaves.shared.ui.EventsListScreen
import com.worldwidewaves.shared.ui.TabManager
import com.worldwidewaves.shared.ui.components.global.SimulationModeChip
import com.worldwidewaves.shared.ui.components.global.SplashScreen
import com.worldwidewaves.shared.ui.components.navigation.ConfigurableTabBarItem
import com.worldwidewaves.shared.ui.screens.DebugScreen
import com.worldwidewaves.shared.ui.theme.WorldWideWavesTheme
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
open class WWWMainActivity(
    val platformEnabler: PlatformEnabler,
    showSplash: Boolean = true,
) : KoinComponent {
    private val platform: WWWPlatform by inject()
    private val events: WWWEvents by inject()
    private val globalSoundChoreography: GlobalSoundChoreographyManager by inject()

    private val eventsListScreen: EventsListScreen by inject()
    private val aboutTabScreen: AboutTabScreen by inject()
    private val debugTabScreen: DebugTabScreen? by inject()

    /** Flag updated when `events.loadEvents()` finishes. */
    private var isDataLoaded: Boolean = false

    /** Flow observed by Compose to know when we can display main content. */
    private val isSplashFinished = MutableStateFlow(!showSplash)

    // Record start time to enforce minimum duration
    val startTime = Clock.System.now().toEpochMilliseconds()

    init {
        Log.i("WWWMainActivity", "Initializing WWWMainActivity")

        // Begin loading events – when done, flag so splash can disappear
        events.loadEvents(onTermination = {
            Log.i("WWWMainActivity", "Events loading completed")
            isDataLoaded = true
            checkSplashFinished(startTime)
            // Start global sound choreography observation for all events
            startGlobalSoundChoreographyForAllEvents()
        })
    }

    protected val tabManager by lazy {
        val screens =
            mutableListOf(
                eventsListScreen,
                aboutTabScreen,
            )
        // Debug screen removed from tab bar - will be accessed via floating icon

        TabManager(
            platformEnabler,
            screens.toList(),
        ) { isSelected, tabIndex, contentDescription ->
            ConfigurableTabBarItem(isSelected, tabIndex, contentDescription, screens.size)
        }
    }

    @Composable
    open fun Draw() {
        // Enforce minimum duration for programmatic splash
        LaunchedEffect(Unit) {
            delay(WWWGlobals.Timing.SPLASH_MIN_DURATION)
            checkSplashFinished(startTime)
        }

        WorldWideWavesTheme {
            Surface(
                modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                // Box to stack main content and simulation-mode overlay
                Box(modifier = Modifier.fillMaxSize()) {
                    var showDebugScreen by remember { mutableStateOf(false) }

                    val ready by isSplashFinished.collectAsState()
                    if (ready) {
                        if (showDebugScreen) {
                            debugTabScreen?.Screen(platformEnabler, Modifier.fillMaxSize()) ?: run {
                                // Fallback debug screen if injection failed
                                DebugScreen(
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        } else {
                            Screen()
                        }
                    } else {
                        SplashScreen()
                    }

                    // -----------------------------------------------------------------
                    //  Global Simulation-Mode chip shown whenever the mode is enabled
                    // -----------------------------------------------------------------
                    SimulationModeChip(platform)

                    // -----------------------------------------------------------------
                    //  Floating Debug Icon (green) - bottom right corner
                    // -----------------------------------------------------------------
                    if (ready && debugTabScreen != null) {
                        // Calculate position at 15% from bottom
                        val windowInfo = LocalWindowInfo.current
                        val density = LocalDensity.current
                        val screenHeight = with(density) { windowInfo.containerSize.height.toDp() }
                        val bottomOffset = screenHeight * 0.15f

                        FloatingActionButton(
                            onClick = { showDebugScreen = !showDebugScreen },
                            modifier =
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = bottomOffset),
                            containerColor = Color(0xFF4CAF50), // Green color
                            shape = CircleShape,
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Debug Screen",
                                tint = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    protected open fun Screen() {
        tabManager.TabView()
    }

    /**
     * Start global sound choreography for all loaded events.
     * This enables sound to play throughout the app when user is in any event area.
     */
    private fun startGlobalSoundChoreographyForAllEvents() {
        try {
            Log.d("WWWMainActivity", "TEMPORARILY DISABLED: Starting global sound choreography for all events")
            // TEMPORARILY DISABLED to test if this causes the crashes
            // globalSoundChoreography.startObservingAllEvents()
            Log.d("WWWMainActivity", "Sound choreography disabled for crash testing")
        } catch (e: Exception) {
            Log.e("WWWMainActivity", "Error starting global sound choreography: ${e.message}")
            // Don't crash the app if sound choreography fails
        }
    }

    /**
     * Lifecycle methods for global sound choreography management.
     * These should be called from the Android activity lifecycle.
     */
    open fun onPause() {
        globalSoundChoreography.pause()
    }

    open fun onResume() {
        globalSoundChoreography.resume()
    }

    open fun onDestroy() {
        globalSoundChoreography.stopObserving()
    }

    /** Updates [isSplashFinished] once both data and min duration requirements are met. */
    private fun checkSplashFinished(startTime: Long) {
        val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
        Log.d("WWWMainActivity", "Checking splash finished: dataLoaded=$isDataLoaded, elapsed=${elapsed}ms")

        if (isDataLoaded &&
            elapsed >= WWWGlobals.Timing.SPLASH_MIN_DURATION.inWholeMilliseconds
        ) {
            Log.i("WWWMainActivity", "Splash conditions met, dismissing splash screen")
            isSplashFinished.update { true }
        }
    }
}
