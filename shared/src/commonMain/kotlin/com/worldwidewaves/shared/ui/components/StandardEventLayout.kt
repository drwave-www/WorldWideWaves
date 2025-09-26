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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.map.MapFeatureState
import com.worldwidewaves.shared.ui.utils.rememberEventState

/**
 * Standard event screen layout pattern used across multiple event activities.
 * Provides the common structure: overlay → description → divider → buttons → map → components.
 * Eliminates layout duplication between EventActivity, WaveActivity, and EventFullMapActivity.
 * Integrates button area logic directly with platform callbacks for navigation and feedback.
 */
@Composable
fun StandardEventLayout(
    event: IWWWEvent,
    mapFeatureState: MapFeatureState,
    onNavigateToWave: (String) -> Unit,
    onSimulationStarted: (String) -> Unit = {},
    onSimulationStopped: (String) -> Unit = {},
    onSimulationError: (String, String) -> Unit = { _, _ -> },
    onMapNotAvailable: () -> Unit = {},
    modifier: Modifier = Modifier,
    mapHeight: Dp,
    mapArea: @Composable () -> Unit = {},
    additionalContent: @Composable () -> Unit = {},
) {
    val platformComponent = object : org.koin.core.component.KoinComponent {
        val platform: WWWPlatform by org.koin.core.component.inject()
        val clock: IClock by org.koin.core.component.inject()
    }
    val platform = platformComponent.platform
    val clock = platformComponent.clock

    val eventState = rememberEventState(event, platform)

    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            // Standard event header components
            EventOverlay(event)
            EventDescription(event)
            DividerLine()

            // Integrated button area with simulation support
            Box(modifier = Modifier.fillMaxWidth()) {
                ButtonWave(
                    event.id,
                    eventState.eventStatus,
                    eventState.endDateTime,
                    eventState.isInArea,
                    onNavigateToWave = WaveNavigator { eventId ->
                        onNavigateToWave(eventId)
                    },
                    modifier = Modifier.align(Alignment.Center),
                )

                // Simulation button when enabled
                if (eventState.isSimulationModeEnabled) {
                    SimulationButton(
                        event = event,
                        mapFeatureState = mapFeatureState,
                        onMapNotAvailable = onMapNotAvailable,
                        onSimulationStarted = onSimulationStarted,
                        onSimulationStopped = onSimulationStopped,
                        onError = onSimulationError
                    )
                }
            }

            // Map area with calculated height
            Box(modifier = Modifier.fillMaxWidth().height(mapHeight)) {
                mapArea()
            }

            // Standard event footer components
            NotifyAreaUserPosition(event)
            EventNumbers(event)
            WWWEventSocialNetworks(event)

            // Optional additional content
            additionalContent()
        }
    }
}