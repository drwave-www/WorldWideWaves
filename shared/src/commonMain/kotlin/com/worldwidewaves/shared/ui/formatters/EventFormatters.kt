package com.worldwidewaves.shared.ui.formatters

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import com.worldwidewaves.shared.WWWGlobals.Event
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.IWWWEvent.Status
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Calculate responsive event map height based on screen width and aspect ratio.
 */
@Composable
fun calculateEventMapHeight(): Dp {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    return screenWidthDp / Event.MAP_RATIO
}

/**
 * Common event state data class containing all standard event observables.
 * Eliminates duplication of state collection across event screens.
 */
@OptIn(ExperimentalTime::class)
data class EventState(
    val eventStatus: Status,
    val progression: Double,
    val isInArea: Boolean,
    val endDateTime: Instant?,
    val isSimulationModeEnabled: Boolean,
)

/**
 * Shared event state manager - collects all common event observables.
 * Eliminates state collection duplication across EventActivity, WaveActivity, EventFullMapActivity.
 * Works identically on both Android and iOS platforms.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun rememberEventState(
    event: IWWWEvent,
    platform: WWWPlatform,
): EventState {
    // Stabilize state collection with event.id keys to prevent observer multiplication
    val eventStatus by remember(event.id) {
        event.observer.eventStatus
    }.collectAsState(Status.UNDEFINED)

    val progression by remember(event.id) {
        event.observer.progression
    }.collectAsState()

    val isInArea by remember(event.id) {
        event.observer.userIsInArea
    }.collectAsState()

    val isSimulationModeEnabled by remember(platform) {
        platform.simulationModeEnabled
    }.collectAsState()

    val endDateTime = remember(event.id) { mutableStateOf<Instant?>(null) }

    // Recompute end date-time each time progression changes (after polygons load, duration becomes accurate)
    // Use event.id key to prevent duplicate LaunchedEffect execution
    LaunchedEffect(event.id, progression) {
        endDateTime.value = event.getEndDateTime()
    }

    return EventState(
        eventStatus = eventStatus,
        progression = progression,
        isInArea = isInArea,
        endDateTime = endDateTime.value,
        isSimulationModeEnabled = isSimulationModeEnabled,
    )
}
