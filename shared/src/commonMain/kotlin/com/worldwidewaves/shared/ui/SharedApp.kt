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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.ui.screens.SharedEventsListScreen
import com.worldwidewaves.shared.ui.theme.SharedWorldWideWavesThemeWithExtended
import com.worldwidewaves.shared.ui.theme.sharedCommonTextStyle
import com.worldwidewaves.shared.utils.Log

/**
 * Shared Compose App - Identical UI on both Android and iOS.
 *
 * This composable provides the exact same UI experience on both platforms,
 * using shared business logic and identical styling.
 */
@Composable
fun SharedApp() {
    Log.i("SharedApp", "SharedApp starting with working logging")

    SharedWorldWideWavesThemeWithExtended {
        Log.i("SharedApp", "Theme applied successfully")

        // Navigation state for shared screens
        var currentScreen by remember {
            Log.i("SharedApp", "Setting up navigation state")
            mutableStateOf<AppScreen>(AppScreen.EventsList)
        }
        var selectedEventId by remember { mutableStateOf<String?>(null) }

        // Simple Compose UI - identical on both Android and iOS
        Scaffold(
            bottomBar = {
                if (currentScreen == AppScreen.EventsList) {
                    SharedBottomTabBar()
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    AppScreen.EventsList -> {
                        SharedEventsScreenWrapper(
                            onEventClick = { eventId ->
                                selectedEventId = eventId
                                currentScreen = AppScreen.EventDetails
                            }
                        )
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
        }
    }
}

// Navigation states for shared app
sealed class AppScreen {
    object EventsList : AppScreen()
    object EventDetails : AppScreen()
    object Wave : AppScreen()
    object Map : AppScreen()
}

/**
 * Shared bottom tab bar - identical on both platforms
 */
@Composable
private fun SharedBottomTabBar() {
    // Simple shared tab bar implementation
    Box(
        modifier = Modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Shared Tab Bar - Events | About | Debug")
    }
}

/**
 * Placeholder for shared About screen
 */
@Composable
private fun SharedAboutScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("About WorldWideWaves", style = sharedCommonTextStyle().copy(
            fontSize = MaterialTheme.typography.headlineMedium.fontSize,
            fontWeight = MaterialTheme.typography.headlineMedium.fontWeight
        ))
        Text("Shared about screen - identical on both platforms", style = sharedCommonTextStyle())
    }
}

/**
 * Wrapper that loads events and passes them to the shared EventsListScreen
 */
@Composable
private fun SharedEventsScreenWrapper(onEventClick: (String) -> Unit = {}) {
    Log.i("SharedEventsScreen", "SharedEventsScreen starting")

    var events by remember { mutableStateOf<List<IWWWEvent>>(emptyList()) }

    // Get SetEventFavorite through existing wwwEvents for now

    // Safely create WWWEvents with proper error handling
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
        try {
            if (wwwEvents == null) {
                Log.e("SharedEventsScreen", "CRITICAL: WWWEvents instance is null")
                hasLoadingError = true
                return@LaunchedEffect
            }

            Log.i("SharedEventsScreen", "Starting event loading via WWWEvents")

            // Load real events from shared business logic with detailed logging
            wwwEvents.loadEvents(
                onLoaded = {
                    try {
                        Log.i("SharedEventsScreen", "onLoaded callback triggered")
                        val loadedEvents = wwwEvents.list()
                        Log.i("SharedEventsScreen", "Retrieved ${loadedEvents.size} events")
                        allEvents = loadedEvents
                        events = loadedEvents // Initial state shows all events
                        Log.i("SharedEventsScreen", "UI state updated with events")
                    } catch (e: Exception) {
                        Log.e("SharedEventsScreen", "Exception in onLoaded: ${e.message}", throwable = e)
                    }
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

    // Filter logic - EXACT Android match
    LaunchedEffect(starredSelected, downloadedSelected, allEvents) {
        events = when {
            starredSelected -> allEvents.filter { it.favorite }
            downloadedSelected -> allEvents.filter { false } // TODO: Add map download state
            else -> allEvents
        }
        Log.i("SharedEventsScreen", "Event loading and filtering completed")
    }

    // Use the shared EventsListScreen for perfect Android parity
    SharedEventsListScreen(
        events = events,
        mapStates = emptyMap(), // TODO: Add map state integration
        onEventClick = onEventClick,
        setEventFavorite = null,
        modifier = Modifier.fillMaxSize()
    )
}

private fun getEventBackgroundColor(eventId: String): androidx.compose.ui.graphics.Color {
    return when {
        eventId.contains("new_york") -> androidx.compose.ui.graphics.Color(0xFF2196F3).copy(alpha = 0.8f)
        eventId.contains("los_angeles") -> androidx.compose.ui.graphics.Color(0xFFFF5722).copy(alpha = 0.8f)
        eventId.contains("mexico") -> androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.8f)
        eventId.contains("sao_paulo") -> androidx.compose.ui.graphics.Color(0xFFFFEB3B).copy(alpha = 0.8f)
        eventId.contains("buenos_aires") -> androidx.compose.ui.graphics.Color(0xFF00BCD4).copy(alpha = 0.8f)
        else -> androidx.compose.ui.graphics.Color(0xFF3F51B5).copy(alpha = 0.7f)
    }
}

private fun getCommunityName(eventId: String): String {
    val components = eventId.split("_")
    return if (components.size >= 2) {
        components.dropLast(1).joinToString(" ") { it.uppercase() }
    } else {
        "COMMUNITY"
    }
}

/**
 * Shared Event Details Screen - Exact Android EventActivity match
 */
@Composable
private fun SharedEventDetailsScreen(
    eventId: String,
    onBackClick: () -> Unit,
    onWaveClick: () -> Unit,
    onMapClick: () -> Unit
) {
    // Exact Android EventActivity structure: Column > EventOverlay + EventDescription + DividerLine + ButtonWave
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(30.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Event overlay section - matching Android
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp) // Same overlay height as list
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(getEventBackgroundColor(eventId))
            )

            // Event title overlay
            Box(
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = eventId.replace("_", " ").uppercase(),
                    style = sharedCommonTextStyle().copy(
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                        fontWeight = MaterialTheme.typography.headlineLarge.fontWeight,
                        color = Color.White
                    )
                )
            }
        }

        // Event description - matching Android
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Event Description",
                style = sharedCommonTextStyle().copy(
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    fontWeight = MaterialTheme.typography.headlineMedium.fontWeight
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Experience the wave in ${getCommunityName(eventId)}. Join thousands of participants in this synchronized human wave event.",
                style = sharedCommonTextStyle()
            )
        }

        // Divider line - matching Android DividerLine
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )

        // Action buttons - matching Android ButtonWave
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // Wave Now button
            Button(
                onClick = onWaveClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Wave Now")
            }

            // View Map button
            Button(
                onClick = onMapClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("View Map")
            }
        }

        // Back button
        Button(
            onClick = onBackClick,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("← Back to Events")
        }
    }
}

/**
 * Shared Wave Screen - Matching Android WaveActivity
 */
@Composable
private fun SharedWaveScreen(
    eventId: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🌊 Wave: ${eventId.replace("_", " ").uppercase()}",
            style = sharedCommonTextStyle().copy(
                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                fontWeight = MaterialTheme.typography.headlineLarge.fontWeight
            ),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = "Wave participation screen - identical on both platforms",
            style = sharedCommonTextStyle(),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(onClick = onBackClick) {
            Text("← Back")
        }
    }
}

/**
 * Shared Map Screen - Matching Android EventFullMapActivity
 */
@Composable
private fun SharedMapScreen(
    eventId: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🗺️ Map: ${eventId.replace("_", " ").uppercase()}",
            style = sharedCommonTextStyle().copy(
                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                fontWeight = MaterialTheme.typography.headlineLarge.fontWeight
            ),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = "Event map screen - identical on both platforms",
            style = sharedCommonTextStyle(),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(onClick = onBackClick) {
            Text("← Back")
        }
    }
}


/**
 * Placeholder for shared Debug screen
 */
@Composable
private fun SharedDebugScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Debug Settings", style = sharedCommonTextStyle().copy(
            fontSize = MaterialTheme.typography.headlineMedium.fontSize,
            fontWeight = MaterialTheme.typography.headlineMedium.fontWeight
        ))
        Text("Shared debug screen - identical on both platforms", style = sharedCommonTextStyle())
    }
}