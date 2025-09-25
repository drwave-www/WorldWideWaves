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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Android implementation of performance dashboard.
 * For now, uses a simple placeholder since PerformanceDashboard has complex dependencies.
 * TODO: Integrate proper AndroidPerformanceMonitor when available in shared DI.
 */
@Composable
actual fun PlatformSpecificPerformanceDashboard(
    modifier: Modifier,
) {
    Text(
        text = "Android Performance Dashboard\n(Integration pending - see PerformanceDashboard)",
        modifier = modifier
    )
}