package com.worldwidewaves.shared.ui.screens

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.map.AbstractEventMap
import com.worldwidewaves.shared.ui.components.shared.ButtonWave
import com.worldwidewaves.shared.ui.components.shared.MapActions
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Shared full map screen component.
 * Works with any AbstractEventMap implementation (Android, iOS).
 * Provides full-screen map with wave button and map actions.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun FullMapScreen(
    event: IWWWEvent,
    eventMap: AbstractEventMap<*>?,
    modifier: Modifier = Modifier,
    onNavigateToWave: (String) -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    val eventStatus by event.observer.eventStatus.collectAsState()
    val progression by event.observer.progression.collectAsState()
    val endDateTime by produceState<Instant?>(initialValue = null, key1 = event, key2 = progression) {
        value = event.getEndDateTime()
    }
    val isInArea by event.observer.userIsInArea.collectAsState()

    // Screen composition
    Box(modifier = modifier.fillMaxSize()) {
        eventMap?.Draw(
            autoMapDownload = true,
            modifier = Modifier.fillMaxSize(),
        )

        ButtonWave(
            event.id,
            eventStatus,
            endDateTime,
            isInArea,
            onNavigateToWave = { eventId -> onNavigateToWave(eventId) },
            Modifier.align(Alignment.TopCenter).padding(top = 40.dp),
        )

        MapActions(
            event = event,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            onTargetWave = {
                scope.launch {
                    eventMap?.markUserInteracted()
                    eventMap?.targetWave()
                }
            },
            onTargetUser = {
                scope.launch {
                    eventMap?.markUserInteracted()
                    // Only target user if within event area to prevent camera moving to positions without tiles
                    val position = eventMap?.getCurrentPosition()
                    if (position != null && event.area.isPositionWithin(position)) {
                        eventMap.targetUser()
                    }
                }
            },
        )
    }
}
