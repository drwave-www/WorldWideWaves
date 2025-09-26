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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Wave
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.WWWSimulation
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.generated.resources.Res
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import com.worldwidewaves.shared.map.MapFeatureState
import com.worldwidewaves.shared.ui.theme.onPrimaryLight
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/**
 * Shared simulation button component for testing event waves.
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun BoxScope.SimulationButton(
    event: IWWWEvent,
    platform: WWWPlatform,
    mapFeatureState: MapFeatureState,
    onMapNotAvailable: () -> Unit = {},
    onSimulationStarted: (String) -> Unit = {},
    onSimulationStopped: (String) -> Unit = {},
    onError: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var simulationButtonState by remember { mutableStateOf("idle") }
    val isSimulationEnabled by platform.simulationModeEnabled.collectAsState()

    // Check if map is available for simulation
    val isMapAvailableForSimulation = when (mapFeatureState) {
        is MapFeatureState.Installed -> true
        is MapFeatureState.Available -> true
        else -> false
    }

    Box(
        modifier = modifier
            .align(Alignment.CenterEnd)
            .padding(end = 16.dp)
            .offset(y = (-8).dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(onPrimaryLight)
            .clickable(enabled = simulationButtonState != "loading") {
                handleSimulationClick(
                    simulationButtonState = simulationButtonState,
                    isMapAvailableForSimulation = isMapAvailableForSimulation,
                    onMapNotAvailable = onMapNotAvailable,
                    onStateChange = { simulationButtonState = it },
                    scope = scope,
                    event = event,
                    platform = platform,
                    onSimulationStarted = onSimulationStarted,
                    onSimulationStopped = onSimulationStopped,
                    onError = onError
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        when (simulationButtonState) {
            "idle" -> {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(MokoRes.strings.test_simulation),
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp),
                )
            }
            "loading" -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color.Red,
                )
            }
            "active" -> {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Stop simulation",
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }

    // Reset button state when simulation is disabled externally
    LaunchedEffect(isSimulationEnabled) {
        if (!isSimulationEnabled && simulationButtonState == "active") {
            simulationButtonState = "idle"
        }
    }
}

/**
 * Handles the simulation button click logic with proper error handling.
 */
private fun handleSimulationClick(
    simulationButtonState: String,
    isMapAvailableForSimulation: Boolean,
    onMapNotAvailable: () -> Unit,
    onStateChange: (String) -> Unit,
    scope: CoroutineScope,
    event: IWWWEvent,
    platform: WWWPlatform,
    onSimulationStarted: (String) -> Unit,
    onSimulationStopped: (String) -> Unit,
    onError: (String, String) -> Unit,
) {
    when (simulationButtonState) {
        "idle" -> {
            if (!isMapAvailableForSimulation) {
                onMapNotAvailable()
                return
            }
            onStateChange("loading")
            scope.launch {
                try {
                    startSimulation(event, platform, onSimulationStarted, onStateChange, onError)
                } catch (e: Exception) {
                    onStateChange("idle")
                    onError("Simulation Error", e.message ?: "Unknown error")
                    Log.e("SimulationButton", "Simulation start failed", e)
                }
            }
        }
        "active" -> {
            onStateChange("idle")
            scope.launch {
                try {
                    stopSimulation(event, platform, onSimulationStopped)
                } catch (e: Exception) {
                    onError("Stop Error", e.message ?: "Unknown error")
                    Log.e("SimulationButton", "Simulation stop failed", e)
                }
            }
        }
    }
}

/**
 * Starts simulation with random position and timing.
 */
@OptIn(ExperimentalTime::class)
private suspend fun startSimulation(
    event: IWWWEvent,
    platform: WWWPlatform,
    onSimulationStarted: (String) -> Unit,
    onStateChange: (String) -> Unit,
    onError: (String, String) -> Unit,
) {
    try {
        // Generate random position within event area
        val position = event.area.generateRandomPositionInArea()

        // Calculate time - start now
        val simulationDelay = 0.minutes
        val simulationTime = event.getStartDateTime() + simulationDelay

        // Reset any existing simulation
        platform.disableSimulation()

        // Create new simulation
        val simulation = WWWSimulation(
            startDateTime = simulationTime,
            userPosition = position,
            initialSpeed = Wave.DEFAULT_SPEED_SIMULATION,
        )

        // Set the simulation
        Log.i("SimulationButton", "Setting simulation starting time to $simulationTime from event ${event.id}")
        Log.i("SimulationButton", "Setting simulation user position to $position from event ${event.id}")
        platform.setSimulation(simulation)

        // Restart event observation to apply simulation
        event.observer.stopObservation()
        event.observer.startObservation()

        onSimulationStarted("Simulation started")
        onStateChange("active")
    } catch (ise: IllegalStateException) {
        onStateChange("idle")
        onError("Invalid State", "Invalid state for simulation setup")
        Log.e("SimulationButton", "Invalid state for simulation setup", ise)
    } catch (iae: IllegalArgumentException) {
        onStateChange("idle")
        onError("Invalid Parameters", "Invalid simulation parameters")
        Log.e("SimulationButton", "Invalid simulation parameters", iae)
    } catch (uoe: UnsupportedOperationException) {
        onStateChange("idle")
        onError("Unsupported Operation", "Unsupported simulation operation")
        Log.e("SimulationButton", "Unsupported simulation operation", uoe)
    }
}

/**
 * Stops active simulation.
 */
private suspend fun stopSimulation(
    event: IWWWEvent,
    platform: WWWPlatform,
    onSimulationStopped: (String) -> Unit,
) {
    platform.disableSimulation()
    event.observer.stopObservation()
    event.observer.startObservation()
    onSimulationStopped("Simulation stopped")
}