package com.worldwidewaves.shared.ui.components.shared

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Wave
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.WWWSimulation
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.map.MapFeatureState
import com.worldwidewaves.shared.ui.theme.onPrimaryLight
import com.worldwidewaves.shared.ui.utils.focusIndicator
import com.worldwidewaves.shared.ui.utils.getIosSafePlatform
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/**
 * Shared simulation button component for testing event waves.
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun BoxScope.SimulationButton(
    event: IWWWEvent,
    mapFeatureState: MapFeatureState,
    onMapNotAvailable: () -> Unit = {},
    onSimulationStarted: (String) -> Unit = {},
    onSimulationStopped: (String) -> Unit = {},
    onError: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    // iOS FIX: Platform dependency passed as parameter to prevent deadlock
    platform: WWWPlatform = getIosSafePlatform(),
) {
    // iOS FIX: Removed dangerous object : KoinComponent pattern
    var simulationButtonState by remember { mutableStateOf("idle") }
    var pendingAction by remember { mutableStateOf<(suspend () -> Unit)?>(null) }
    val isSimulationEnabled by platform.simulationModeEnabled.collectAsState()

    // Localized strings for error messages and notifications
    val simulationErrorText = stringResource(MokoRes.strings.simulation_error)
    val stopErrorText = stringResource(MokoRes.strings.simulation_stop_error)
    val simulationStartedText = stringResource(MokoRes.strings.simulation_started)
    val simulationStoppedText = stringResource(MokoRes.strings.simulation_stopped)

    // Check if map is available for simulation
    val isMapAvailableForSimulation =
        when (mapFeatureState) {
            is MapFeatureState.Installed -> true
            is MapFeatureState.Available -> true
            else -> false
        }

    // Determine content description and state based on current state
    val contentDescriptionText =
        when (simulationButtonState) {
            "idle" -> "Start simulation"
            "loading" -> "Simulation loading"
            "active" -> "Stop simulation"
            else -> "Simulation"
        }

    val stateDescriptionText =
        when (simulationButtonState) {
            "idle" -> "Ready to start"
            "loading" -> "Loading"
            "active" -> "Running"
            else -> "Unknown"
        }

    Box(
        modifier =
            modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .offset(y = (-8).dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(onPrimaryLight)
                .focusIndicator()
                .clickable(enabled = simulationButtonState != "loading") {
                    val action =
                        handleSimulationClick(
                            simulationButtonState = simulationButtonState,
                            isMapAvailableForSimulation = isMapAvailableForSimulation,
                            onMapNotAvailable = onMapNotAvailable,
                            onStateChange = { simulationButtonState = it },
                            event = event,
                            platform = platform,
                            onSimulationStarted = onSimulationStarted,
                            onSimulationStopped = onSimulationStopped,
                            onError = onError,
                            simulationErrorText = simulationErrorText,
                            stopErrorText = stopErrorText,
                            simulationStartedText = simulationStartedText,
                            simulationStoppedText = simulationStoppedText,
                        )
                    pendingAction = action
                }.semantics {
                    role = Role.Button
                    contentDescription = contentDescriptionText
                    stateDescription = stateDescriptionText
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
                    contentDescription = stringResource(MokoRes.strings.accessibility_stop_simulation),
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }

    // Execute pending actions - add event.id key to prevent conflicts across recomposition
    LaunchedEffect(event.id, pendingAction) {
        pendingAction?.let { action ->
            try {
                action()
            } catch (e: Exception) {
                Log.e("SimulationButton", "Action failed for event ${event.id}", e)
                simulationButtonState = "idle"
                onError(simulationErrorText, e.message ?: "Unknown error")
            } finally {
                pendingAction = null
            }
        }
    }

    // Reset button state when simulation is disabled externally - add event.id key for safety
    LaunchedEffect(event.id, isSimulationEnabled) {
        if (!isSimulationEnabled && simulationButtonState == "active") {
            simulationButtonState = "idle"
        }
    }
}

/**
 * Handles the simulation button click logic with proper error handling.
 * Returns functions that should be called within LaunchedEffect to manage coroutine lifecycle properly.
 */
@Suppress("LongParameterList") // UI glue function passing through callbacks and resources - refactoring would complicate call site
private fun handleSimulationClick(
    simulationButtonState: String,
    isMapAvailableForSimulation: Boolean,
    onMapNotAvailable: () -> Unit,
    onStateChange: (String) -> Unit,
    event: IWWWEvent,
    platform: WWWPlatform,
    onSimulationStarted: (String) -> Unit,
    onSimulationStopped: (String) -> Unit,
    onError: (String, String) -> Unit,
    simulationErrorText: String,
    stopErrorText: String,
    simulationStartedText: String,
    simulationStoppedText: String,
): (suspend () -> Unit)? =
    when (simulationButtonState) {
        "idle" -> {
            if (!isMapAvailableForSimulation) {
                onMapNotAvailable()
                null
            } else {
                onStateChange("loading")
                suspend {
                    try {
                        startSimulation(
                            event,
                            platform,
                            onSimulationStarted,
                            onStateChange,
                            onError,
                            simulationErrorText,
                            simulationStartedText,
                        )
                    } catch (e: Exception) {
                        Log.e("SimulationButton", "Simulation start failed for event ${event.id}", e)
                        onStateChange("idle")
                        onError(simulationErrorText, e.message ?: "Unknown error")
                    }
                }
            }
        }
        "active" -> {
            onStateChange("idle")
            suspend {
                try {
                    stopSimulation(event, platform, onSimulationStopped, simulationStoppedText)
                } catch (e: Exception) {
                    Log.e("SimulationButton", "Simulation stop failed for event ${event.id}", e)
                    onError(stopErrorText, e.message ?: "Unknown error")
                }
            }
        }
        else -> null
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
    simulationErrorText: String,
    simulationStartedText: String,
) {
    try {
        // If simulation is already running on another event, stop it first
        // This allows switching simulations between events as designed
        if (platform.isOnSimulation()) {
            Log.i("SimulationButton", "Stopping existing simulation to start new one for ${event.id}")
        }

        // Prefer current device position if inside the event area; otherwise fall back to random
        val devicePosition = platform.getCurrentPosition()
        val position =
            if (devicePosition != null && event.area.isPositionWithin(devicePosition)) {
                Log.i("SimulationButton", "Using device position inside area for event ${event.id}: $devicePosition")
                devicePosition
            } else {
                event.area.generateRandomPositionInArea().also {
                    Log.i("SimulationButton", "Using random position inside area for event ${event.id}: $it")
                }
            }

        // Calculate time - start now
        val simulationDelay = 0.minutes
        val simulationTime = event.getStartDateTime() + simulationDelay

        // Create new simulation with unique identifier
        val simulation =
            WWWSimulation(
                startDateTime = simulationTime,
                userPosition = position,
                initialSpeed = Wave.DEFAULT_SPEED_SIMULATION,
            )

        // Atomically reset and set simulation (single notification instead of two)
        Log.i("SimulationButton", "Setting simulation starting time to $simulationTime from event ${event.id}")
        Log.i("SimulationButton", "Setting simulation user position to $position from event ${event.id}")
        platform.resetAndSetSimulation(simulation)

        // Reset event state to avoid validation errors (DONE -> NEXT, userHasBeenHit transitions)
        // Safe to call before restart now that userIsInArea is preserved and
        // updateAreaDetection() is always called on observer start
        event.observer.resetState()

        // Restart event observation to apply simulation
        // Note: stopObservation() is async, so we add a delay to ensure proper cleanup
        event.observer.stopObservation()
        delay(150.milliseconds) // Allow time for async cancellation to complete
        event.observer.startObservation()

        onSimulationStarted(simulationStartedText)
        onStateChange("active")
    } catch (ise: IllegalStateException) {
        onStateChange("idle")
        onError(simulationErrorText, "Invalid state for simulation setup")
        Log.e("SimulationButton", "Invalid state for simulation setup", ise)
    } catch (iae: IllegalArgumentException) {
        onStateChange("idle")
        onError(simulationErrorText, "Invalid simulation parameters")
        Log.e("SimulationButton", "Invalid simulation parameters", iae)
    } catch (uoe: UnsupportedOperationException) {
        onStateChange("idle")
        onError(simulationErrorText, "Unsupported simulation operation")
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
    simulationStoppedText: String,
) {
    platform.disableSimulation()
    // Restart event observation to return to real GPS/time
    // Note: stopObservation() is async, so we add a delay to ensure proper cleanup
    event.observer.stopObservation()
    delay(150.milliseconds) // Allow time for async cancellation to complete
    event.observer.startObservation()
    onSimulationStopped(simulationStoppedText)
}
