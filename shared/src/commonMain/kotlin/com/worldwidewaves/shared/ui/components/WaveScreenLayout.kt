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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.ui.components.choreographies.WaveChoreographies
import com.worldwidewaves.shared.ui.screens.UserWaveStatusText
import com.worldwidewaves.shared.ui.screens.WaveHitCounter
import com.worldwidewaves.shared.ui.screens.WaveProgressionBar
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.ExperimentalTime

/**
 * Wave-specific state data class containing all wave-related observables.
 * Used by wave-focused screens like WaveActivity.
 */
@OptIn(ExperimentalTime::class)
data class WaveState(
    val isWarmingInProgress: Boolean,
    val hitDateTime: kotlin.time.Instant,
    val isGoingToBeHit: Boolean,
    val hasBeenHit: Boolean,
)

/**
 * Collects all wave-specific state observables.
 * Eliminates state collection duplication across wave activities.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun rememberWaveState(event: IWWWEvent): WaveState {
    val isWarmingInProgress by event.observer.isUserWarmingInProgress.collectAsState(false)
    val hitDateTime by event.observer.hitDateTime.collectAsState()
    val isGoingToBeHit by event.observer.userIsGoingToBeHit.collectAsState(false)
    val hasBeenHit by event.observer.userHasBeenHit.collectAsState(false)

    return WaveState(
        isWarmingInProgress = isWarmingInProgress,
        hitDateTime = hitDateTime,
        isGoingToBeHit = isGoingToBeHit,
        hasBeenHit = hasBeenHit,
    )
}

/**
 * Helper class for accessing IClock via Koin injection in Composables.
 */
private class WaveScreenLayoutHelper : KoinComponent {
    val clock: IClock by inject()
}

/**
 * Shared wave screen layout pattern used by wave-focused activities.
 * Provides the structure: status → map → progression → spacer → counter + overlaid choreographies.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun WaveScreenLayout(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
    mapHeight: Dp,
    mapArea: @Composable () -> Unit = {},
) {
    // WaveScreenLayoutHelper removed - was unused

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            // Standard wave components
            UserWaveStatusText(event)

            // Map area with calculated height
            Box(modifier = Modifier.fillMaxWidth().height(mapHeight)) {
                mapArea()
            }

            // Wave progression bar
            WaveProgressionBar(event)

            // Spacer to push counter to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Wave hit counter
            WaveHitCounter(event)
            Spacer(modifier = Modifier.height(30.dp))
        }

        // Integrated wave choreographies with proper z-index layering
        WaveChoreographies(event, Modifier.zIndex(10f))
    }
}
