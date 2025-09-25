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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.events.IWWWEvent

/**
 * Android implementation of event map component.
 * Uses AndroidEventMap for MapLibre integration.
 */
@Composable
actual fun PlatformEventMap(
    event: IWWWEvent,
    onMapClick: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current

    val eventMap = remember(event.id) {
        AndroidEventMap(
            event,
            onMapClick = { onMapClick() },
        )
    }

    eventMap.Screen(modifier = modifier)
}