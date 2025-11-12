@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.worldwidewaves.shared.ui.components

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.map.MapFeatureState
import com.worldwidewaves.shared.ui.components.event.EventNumbers
import com.worldwidewaves.shared.ui.components.event.EventOverlay
import com.worldwidewaves.shared.ui.components.event.NotifyAreaUserPosition
import com.worldwidewaves.shared.ui.components.event.WWWEventSocialNetworks
import com.worldwidewaves.shared.ui.components.shared.ButtonWave
import com.worldwidewaves.shared.ui.components.shared.SimulationButton
import com.worldwidewaves.shared.ui.formatters.rememberEventState
import com.worldwidewaves.shared.ui.theme.sharedCommonBoldStyle
import com.worldwidewaves.shared.ui.utils.focusIndicator
import com.worldwidewaves.shared.ui.utils.getIosSafePlatform
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch

/**
 * Standard event screen layout pattern used across multiple event activities.
 * Provides the common structure: overlay → description → divider → buttons → map → components.
 * Eliminates layout duplication between EventActivity, WaveActivity, and EventFullMapActivity.
 * Integrates button area logic directly with platform callbacks for navigation and feedback.
 */
@Composable
fun EventLayout(
    event: IWWWEvent,
    mapFeatureState: MapFeatureState,
    onNavigateToWave: (String) -> Unit,
    onSimulationStarted: (String) -> Unit = {},
    onSimulationStopped: (String) -> Unit = {},
    onSimulationError: (String, String) -> Unit = { _, _ -> },
    onMapNotAvailable: () -> Unit = {},
    onUrlOpen: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    mapHeight: Dp,
    mapArea: @Composable () -> Unit = {},
    additionalContent: @Composable () -> Unit = {},
    // iOS FIX: Dependencies passed as parameters to prevent deadlock
    platform: WWWPlatform = getIosSafePlatform(),
) {
    // iOS FIX: Removed dangerous object : KoinComponent pattern
    // Dependencies now resolved safely outside composition

    val eventState = rememberEventState(event, platform)

    // Dialog state for join wave requirements
    var showRequirementsDialog by remember { mutableStateOf(false) }

    // Stable coroutine scope for polygon preloading (iOS-safe, survives recomposition)
    val stableScope = rememberCoroutineScope()

    // Preload event area polygons in composition-stable scope
    // Prevents polygon loading from being cancelled during observer lifecycle changes
    DisposableEffect(event.id) {
        val preloadJob =
            stableScope.launch {
                try {
                    // Force polygon loading before observer starts
                    // This ensures polygons are cached and won't be cancelled by observer restarts
                    event.area.getPolygons()
                } catch (e: Exception) {
                    // Log but don't crash - observer will retry if needed
                    com.worldwidewaves.shared.utils.Log.w(
                        "EventLayout",
                        "Polygon preload failed for ${event.id}: ${e.message}",
                    )
                }
            }

        onDispose {
            preloadJob.cancel()
        }
    }

    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            // Standard event header components with simulation button overlay
            Box(modifier = Modifier.fillMaxWidth()) {
                EventOverlay(event)

                // Simulation button when enabled - positioned at top-left
                if (eventState.isSimulationModeEnabled) {
                    SimulationButton(
                        event = event,
                        mapFeatureState = mapFeatureState,
                        onMapNotAvailable = onMapNotAvailable,
                        onSimulationStarted = onSimulationStarted,
                        onSimulationStopped = onSimulationStopped,
                        onError = onSimulationError,
                    )
                }
            }

            EventDescription(event)
            DividerLine()

            // Integrated button area
            Box(modifier = Modifier.fillMaxWidth()) {
                ButtonWave(
                    event.id,
                    eventState.eventStatus,
                    eventState.endDateTime,
                    eventState.isInArea,
                    eventState.isUserWarmingInProgress,
                    eventState.userHasBeenHit,
                    onNavigateToWave =
                        { eventId ->
                            onNavigateToWave(eventId)
                        },
                    onDisabledClick = { showRequirementsDialog = true },
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            // Map area with calculated height
            Box(modifier = Modifier.fillMaxWidth().height(mapHeight)) {
                mapArea()
            }

            // Standard event footer components
            NotifyAreaUserPosition(event)
            EventNumbers(event)

            // Link to search for places in event
            val linkText = stringResource(MokoRes.strings.search_for_places_in_event)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .focusIndicator()
                        .clickable(onClick = {
                            try {
                                val lumaUrlWithTag = "${WWWGlobals.Common.LUMA_URL}?tag=%23${event.instagramHashtag.removePrefix("#")}"
                                onUrlOpen(lumaUrlWithTag)
                            } catch (e: Exception) {
                                Log.e("EventLayout", "Error opening Luma URL", throwable = e)
                            }
                        })
                        .semantics {
                            role = Role.Button
                            contentDescription = linkText
                        },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = linkText,
                    style =
                        sharedCommonBoldStyle(WWWGlobals.Dimensions.FONTSIZE_DEFAULT).copy(
                            color = Color.White,
                            textDecoration = TextDecoration.Underline,
                        ),
                )
            }

            WWWEventSocialNetworks(event, onUrlOpen = onUrlOpen)

            // Optional additional content
            additionalContent()
        }

        // Show requirements dialog when inactive button is clicked
        if (showRequirementsDialog) {
            AlertJoinWaveRequirements { showRequirementsDialog = false }
        }
    }
}
