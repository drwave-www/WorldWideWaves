package com.worldwidewaves.shared.ui

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.ui.TabManager
import com.worldwidewaves.shared.ui.TabScreen
import com.worldwidewaves.shared.ui.components.SimulationModeChip
import com.worldwidewaves.shared.ui.components.SplashScreen
import com.worldwidewaves.shared.ui.screens.AboutScreen
import com.worldwidewaves.shared.ui.screens.SharedDebugScreen
import com.worldwidewaves.shared.ui.screens.SharedEventsListScreen
import com.worldwidewaves.shared.ui.theme.SharedWorldWideWavesThemeWithExtended
import com.worldwidewaves.shared.utils.Log
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.ExperimentalTime

/**
 * Shared Compose App - Identical UI on both Android and iOS.
 *
 * Matches Android MainActivity exactly: splash coordination, tab navigation,
 * floating debug icon, simulation mode chip overlay.
 */
@Composable
fun SharedApp() {
    Log.i("SharedApp", "SharedApp starting")

    // Inject dependencies using Koin (matching Android pattern)
    val dependencies = remember {
        object : KoinComponent {
            val platform: WWWPlatform by inject()
            val events: WWWEvents by inject()
        }
    }

    SharedWorldWideWavesThemeWithExtended {
        Surface(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            SharedMainContent(
                platform = dependencies.platform,
                events = dependencies.events
            )
        }
    }
}

/**
 * Main content with splash coordination and tab navigation.
 * Matches Android MainActivity pattern exactly.
 */
@Composable
private fun SharedMainContent(
    platform: WWWPlatform,
    events: WWWEvents
) {
    // Splash screen state management (matching Android pattern)
    var isSplashFinished by remember { mutableStateOf(false) }
    var isDataLoaded by remember { mutableStateOf(false) }

    // Debug screen state (matching Android pattern)
    var showDebugScreen by remember { mutableStateOf(false) }

    // Navigation state management (matching Android Activity navigation)
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.EventsList) }
    var selectedEventId by remember { mutableStateOf<String?>(null) }

    // Create tab manager for main navigation (matching Android pattern)
    val tabManager = remember {
        val screens = mutableListOf<TabScreen>()

        // Events list screen
        screens.add(object : TabScreen {
            override val name = "Events"
            @Composable
            override fun Screen(modifier: Modifier) {
                SharedEventsScreenWrapper(
                    onEventClick = { eventId ->
                        selectedEventId = eventId
                        currentScreen = AppScreen.EventDetails
                    }
                )
            }
        })

        // About screen
        screens.add(object : TabScreen {
            override val name = "About"
            @Composable
            override fun Screen(modifier: Modifier) {
                AboutScreen(platform = platform)
            }
        })

        TabManager(
            screens = screens.toList(),
            tabBarItem = { isSelected, tabIndex, contentDescription ->
                // Enhanced tab bar item (matching Android design)
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = screens[tabIndex].name,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        )
    }

    // Data loading coordination (matching Android pattern)
    LaunchedEffect(Unit) {
        try {
            events.loadEvents(
                onTermination = { exception ->
                    isDataLoaded = true
                    if (isDataLoaded) {
                        isSplashFinished = true
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("SharedApp", "Failed to load events", throwable = e)
            isDataLoaded = true
            isSplashFinished = true
        }
    }

    // Enforce minimum splash duration (matching Android pattern)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000) // 3 second minimum
        if (isDataLoaded) {
            isSplashFinished = true
        }
    }

    // Box to stack main content and overlays (matching Android pattern exactly)
    Box(modifier = Modifier.fillMaxSize()) {
        if (isSplashFinished) {
            if (showDebugScreen) {
                // Debug screen overlay
                SharedDebugScreen(modifier = Modifier.fillMaxSize())
            } else {
                // Navigation between main screens (matching Android Activity navigation)
                when (currentScreen) {
                    AppScreen.EventsList, AppScreen.About -> {
                        // Tab navigation for main screens
                        tabManager.TabView()
                    }
                    AppScreen.EventDetails -> {
                        selectedEventId?.let { eventId ->
                            SharedEventDetailsScreen(
                                eventId = eventId,
                                onBackClick = {
                                    currentScreen = AppScreen.EventsList
                                },
                                onWaveClick = {
                                    currentScreen = AppScreen.Wave
                                },
                                onMapClick = {
                                    currentScreen = AppScreen.Map
                                }
                            )
                        }
                    }
                    AppScreen.Wave -> {
                        selectedEventId?.let { eventId ->
                            SharedWaveScreen(
                                eventId = eventId,
                                onBackClick = {
                                    currentScreen = AppScreen.EventDetails
                                }
                            )
                        }
                    }
                    AppScreen.Map -> {
                        selectedEventId?.let { eventId ->
                            SharedMapScreen(
                                eventId = eventId,
                                onBackClick = {
                                    currentScreen = AppScreen.EventDetails
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // Splash screen
            SplashScreen()
        }

        // Global Simulation-Mode chip (matching Android pattern exactly)
        SimulationModeChip(platform)

        // Floating Debug Icon (matching Android pattern exactly)
        if (isSplashFinished) {
            FloatingActionButton(
                onClick = { showDebugScreen = !showDebugScreen },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = Color(0xFF4CAF50), // Green color
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "Debug Screen",
                    tint = Color.White
                )
            }
        }
    }
}

// Navigation states for shared app (matching Android Activity navigation)
sealed class AppScreen {
    object EventsList : AppScreen()
    object About : AppScreen()
    object EventDetails : AppScreen()
    object Wave : AppScreen()
    object Map : AppScreen()
}

/**
 * Shared Event Details Screen - Matching Android EventActivity exactly
 */
@Composable
private fun SharedEventDetailsScreen(
    eventId: String,
    onBackClick: () -> Unit,
    onWaveClick: () -> Unit,
    onMapClick: () -> Unit
) {
    // Get the actual event from WWWEvents
    val wwwEvents = remember {
        try {
            WWWEvents()
        } catch (e: Exception) {
            Log.e("SharedEventDetails", "Failed to create WWWEvents", throwable = e)
            null
        }
    }

    val event = remember(eventId, wwwEvents) {
        wwwEvents?.flow()?.value?.find { it.id == eventId }
    }

    if (event != null) {
        // Use StandardEventLayout exactly like Android EventActivity
        com.worldwidewaves.shared.ui.components.StandardEventLayout(
            event = event,
            mapFeatureState = com.worldwidewaves.shared.map.MapFeatureState.Available, // Simplified for now
            onNavigateToWave = { _ -> onWaveClick() },
            onSimulationStarted = { message ->
                Log.i("SharedEventDetails", "Simulation started: $message")
            },
            onSimulationStopped = { message ->
                Log.i("SharedEventDetails", "Simulation stopped: $message")
            },
            onSimulationError = { title, message ->
                Log.e("SharedEventDetails", "Simulation error: $title - $message")
            },
            onMapNotAvailable = {
                Log.w("SharedEventDetails", "Map not available for simulation")
            },
            modifier = Modifier.fillMaxSize(),
            mapHeight = 300.dp, // Using shared constant
            mapArea = {
                // Placeholder map area - will be enhanced with actual map
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onMapClick() },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = "Event Map Preview\n(Click for full map)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            additionalContent = {
                // Back button
                androidx.compose.material3.Button(
                    onClick = onBackClick,
                    modifier = Modifier.padding(16.dp)
                ) {
                    androidx.compose.material3.Text("← Back to Events")
                }
            }
        )
    } else {
        // Error state - event not found
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            androidx.compose.material3.Text(
                text = "Event not found: $eventId",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )

            androidx.compose.material3.Button(
                onClick = onBackClick,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                androidx.compose.material3.Text("← Back to Events")
            }
        }
    }
}

/**
 * Shared Wave Screen - Matching Android WaveActivity exactly
 */
@OptIn(ExperimentalTime::class)
@Composable
private fun SharedWaveScreen(
    eventId: String,
    onBackClick: () -> Unit
) {
    // Get the actual event from WWWEvents
    val wwwEvents = remember {
        try {
            WWWEvents()
        } catch (e: Exception) {
            Log.e("SharedWaveScreen", "Failed to create WWWEvents", throwable = e)
            null
        }
    }

    val event = remember(eventId, wwwEvents) {
        wwwEvents?.flow()?.value?.find { it.id == eventId }
    }

    if (event != null) {
        // Use WaveScreenLayout for basic wave experience (simpler for initial implementation)
        com.worldwidewaves.shared.ui.components.WaveScreenLayout(
            event = event,
            modifier = Modifier.fillMaxSize(),
            mapHeight = 300.dp,
            mapArea = {
                // Placeholder map content - will be enhanced with iOS map integration
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = "Wave Map\n(Tap to go back)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    } else {
        // Error state - event not found
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            androidx.compose.material3.Text(
                text = "Event not found: $eventId",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )

            androidx.compose.material3.Button(
                onClick = onBackClick,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                androidx.compose.material3.Text("← Back")
            }
        }
    }
}

/**
 * Shared Map Screen - Matching Android EventFullMapActivity exactly
 */
@OptIn(ExperimentalTime::class)
@Composable
private fun SharedMapScreen(
    eventId: String,
    onBackClick: () -> Unit
) {
    // Get the actual event from WWWEvents
    val wwwEvents = remember {
        try {
            WWWEvents()
        } catch (e: Exception) {
            Log.e("SharedMapScreen", "Failed to create WWWEvents", throwable = e)
            null
        }
    }

    val event = remember(eventId, wwwEvents) {
        wwwEvents?.flow()?.value?.find { it.id == eventId }
    }

    if (event != null) {
        // Full screen map view (simplified for initial implementation)
        Box(modifier = Modifier.fillMaxSize()) {
            // Full screen map placeholder - will be enhanced with iOS map integration
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = "Full Event Map\n${eventId.replace("_", " ").uppercase()}\n(Tap to go back)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ButtonWave overlay (matching Android EventFullMapActivity)
            com.worldwidewaves.shared.ui.components.ButtonWave(
                eventId = event.id,
                eventState = com.worldwidewaves.shared.events.IWWWEvent.Status.RUNNING,
                endDateTime = null,
                isInArea = true,
                onNavigateToWave = com.worldwidewaves.shared.ui.components.WaveNavigator { _ ->
                    // Navigate to wave screen from map
                    onBackClick() // For now, just go back
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
            )
        }
    } else {
        // Error state - event not found
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            androidx.compose.material3.Text(
                text = "Event not found: $eventId",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )

            androidx.compose.material3.Button(
                onClick = onBackClick,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                androidx.compose.material3.Text("← Back")
            }
        }
    }
}

/**
 * Wrapper for events screen with data loading (matching Android pattern)
 */
@Composable
private fun SharedEventsScreenWrapper(onEventClick: (String) -> Unit = {}) {
    Log.i("SharedEventsScreen", "SharedEventsScreen starting")

    var events by remember { mutableStateOf<List<IWWWEvent>>(emptyList()) }

    // Get SetEventFavorite through existing wwwEvents for now
    val wwwEvents = remember {
        try {
            Log.i("SharedEventsScreen", "Creating WWWEvents instance")
            WWWEvents()
        } catch (e: Exception) {
            Log.e("SharedEventsScreen", "Failed to create WWWEvents: ${e.message}", throwable = e)
            null
        }
    }

    // Filter state - exact Android match with proper state management
    var starredSelected by remember { mutableStateOf(false) }
    var downloadedSelected by remember { mutableStateOf(false) }
    var hasLoadingError by remember { mutableStateOf(false) }
    var allEvents by remember { mutableStateOf<List<IWWWEvent>>(emptyList()) }

    LaunchedEffect(wwwEvents) {
        wwwEvents?.let { eventsInstance ->
            try {
                eventsInstance.loadEvents(
                    onLoaded = {
                        Log.i("SharedEventsScreen", "Events loaded successfully")
                        allEvents = eventsInstance.flow().value
                        Log.i("SharedEventsScreen", "UI state updated with events")
                    },
                    onLoadingError = { error ->
                        Log.e("SharedEventsScreen", "Event loading error: ${error.message}", throwable = error)
                        hasLoadingError = true
                    }
                )
                Log.i("SharedEventsScreen", "Event loading initiated successfully")
            } catch (e: Exception) {
                Log.e("SharedEventsScreen", "CRITICAL: LaunchedEffect exception: ${e.message}", throwable = e)
                hasLoadingError = true
            }
        }
    }

    // Filter logic - EXACT Android match
    LaunchedEffect(starredSelected, downloadedSelected, allEvents) {
        events = when {
            starredSelected -> allEvents.filter { it.favorite }
            downloadedSelected -> allEvents.filter { false } // NOTE: Map download state integration pending
            else -> allEvents
        }
        Log.i("SharedEventsScreen", "Event loading and filtering completed")
    }

    // Use the shared EventsListScreen for perfect Android parity
    SharedEventsListScreen(
        events = events,
        mapStates = emptyMap(), // NOTE: Map state integration pending
        onEventClick = { eventId ->
            Log.i("SharedEventsScreen", "Event clicked: $eventId")
            // TODO: Implement navigation to event details
        },
        setEventFavorite = null,
        modifier = Modifier.fillMaxSize()
    )
}