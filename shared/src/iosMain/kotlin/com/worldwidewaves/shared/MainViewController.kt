package com.worldwidewaves.shared

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.worldwidewaves.shared.compose.tabs.AboutScreen
import com.worldwidewaves.shared.compose.tabs.DebugScreen
import com.worldwidewaves.shared.compose.tabs.EventsListScreen
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.ui.TabManager
import com.worldwidewaves.shared.ui.components.SimulationModeChip
import com.worldwidewaves.shared.ui.components.SplashScreen
import com.worldwidewaves.shared.ui.components.navigation.ConfigurableTabBarItem
import com.worldwidewaves.shared.ui.theme.WorldWideWavesTheme
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.UIKit.UIViewController

/**
 * iOS Main View Controller that mimics MainActivity structure.
 * Creates TabManager with iOS TabScreen implementations and handles splash screen logic.
 */
fun MainViewController(): UIViewController =
    ComposeUIViewController {
        IOSMainScreen()
    }

@Composable
private fun IOSMainScreen() {
    val iosMainComponent = remember { IOSMainComponent() }

    // Flag updated when events.loadEvents() finishes
    var isDataLoaded by remember { mutableStateOf(false) }
    var isSplashFinished by remember { mutableStateOf(false) }

    // Load events and handle splash timing
    LaunchedEffect(Unit) {
        // Load events
        iosMainComponent.events.loadEvents(onTermination = {
            isDataLoaded = true
            Log.d("IOSMainScreen", "Events loaded")
        })

        // Minimum splash duration using WWWGlobals constants
        delay(WWWGlobals.Timing.SPLASH_MIN_DURATION.inWholeMilliseconds)

        // Mark splash as finished after minimum duration
        if (isDataLoaded) {
            isSplashFinished = true
        }
    }

    // Monitor data loading completion
    LaunchedEffect(isDataLoaded) {
        if (isDataLoaded) {
            delay(100) // Small delay to ensure smooth transition
            isSplashFinished = true
        }
    }

    WorldWideWavesTheme {
        Surface(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                var showDebugScreen by remember { mutableStateOf(false) }

                if (isSplashFinished) {
                    if (showDebugScreen) {
                        iosMainComponent.debugScreen.Screen(platformEnabler, Modifier.fillMaxSize())
                    } else {
                        iosMainComponent.tabManager.TabView()
                    }
                } else {
                    IOSProgrammaticSplashScreen()
                }

                // Global Simulation-Mode chip
                SimulationModeChip(iosMainComponent.platform)

                // Floating Debug Icon - mimicking MainActivity
                if (isSplashFinished) {
                    FloatingActionButton(
                        onClick = { showDebugScreen = !showDebugScreen },
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 120.dp),
                        // Approximate position
                        containerColor = Color(0xFF4CAF50),
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
private fun IOSProgrammaticSplashScreen() {
    SplashScreen()
}

/**
 * iOS component that manages dependencies and creates TabManager.
 * Mimics the dependency injection pattern from MainActivity.
 */
private class IOSMainComponent : KoinComponent {
    val events: WWWEvents by inject()
    val platform: WWWPlatform by inject()
    private val eventsViewModel: com.worldwidewaves.shared.viewmodels.EventsViewModel by inject()
    private val setEventFavorite: com.worldwidewaves.shared.data.SetEventFavorite by inject()

    val eventsListScreen = EventsListScreen(eventsViewModel, setEventFavorite)
    val aboutScreen = AboutScreen(platform)
    val debugScreen = DebugScreen()

    val tabManager by lazy {
        val screens =
            mutableListOf(
                eventsListScreen,
                aboutScreen,
            )
        // Debug screen removed from tab bar - accessed via floating icon

        TabManager(
            screens.toList(),
        ) { isSelected, tabIndex, contentDescription ->
            ConfigurableTabBarItem(isSelected, tabIndex, contentDescription, screens.size)
        }
    }
}
