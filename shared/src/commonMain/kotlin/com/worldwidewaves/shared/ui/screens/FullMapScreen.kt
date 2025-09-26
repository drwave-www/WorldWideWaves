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
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.map.AbstractEventMap
import com.worldwidewaves.shared.ui.components.ButtonWave
import com.worldwidewaves.shared.ui.components.SharedMapActions
import com.worldwidewaves.shared.ui.components.WaveNavigator
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Shared full map screen component.
 * Works with any AbstractEventMap implementation (Android, iOS).
 * Provides full-screen map with wave button and map actions.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun <T> FullMapScreen(
    event: IWWWEvent,
    eventMap: AbstractEventMap<T>,
    modifier: Modifier = Modifier,
    onNavigateToWave: (String) -> Unit = {},
    mapContent: @Composable (Modifier) -> Unit,
) {
    val clockComponent =
        object : KoinComponent {
            val clock: IClock by inject()
        }
    val clock = clockComponent.clock
    val scope = rememberCoroutineScope()

    val eventStatus by event.observer.eventStatus.collectAsState()
    val progression by event.observer.progression.collectAsState()
    val endDateTime by produceState<Instant?>(initialValue = null, key1 = event, key2 = progression) {
        value = event.getEndDateTime()
    }
    val isInArea by event.observer.userIsInArea.collectAsState()

    // Screen composition
    Box(modifier = modifier.fillMaxSize()) {
        mapContent(Modifier.fillMaxSize())

        ButtonWave(
            event.id,
            eventStatus,
            endDateTime,
            isInArea,
            onNavigateToWave =
                WaveNavigator { eventId ->
                    onNavigateToWave(eventId)
                },
            Modifier.align(Alignment.TopCenter).padding(top = 40.dp),
        )

        SharedMapActions(
            event = event,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            onTargetWave = {
                scope.launch {
                    eventMap.targetUserAndWave()
                }
            },
            onCenterWave = {
                scope.launch {
                    eventMap.targetWave()
                }
            },
        )
    }
}
