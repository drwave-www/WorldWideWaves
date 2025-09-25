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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.worldwidewaves.shared.events.IWWWEvent

/**
 * Android implementation of map renderer.
 * Integrates with AndroidEventMap for MapLibre rendering.
 */
@Composable
actual fun PlatformMapRenderer(
    event: IWWWEvent,
    onMapLoaded: () -> Unit,
    onMapError: () -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onMapClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Android MapLibre for ${event.id}\n(Click for interactions)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Simulate map loaded
    onMapLoaded()
}