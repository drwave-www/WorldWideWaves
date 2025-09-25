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
 * iOS implementation of performance dashboard.
 * Uses a simple placeholder until iOS-specific performance monitoring is implemented.
 */
@Composable
actual fun PlatformSpecificPerformanceDashboard(
    modifier: Modifier,
) {
    Text(
        text = "iOS Performance Dashboard\n(iOS-specific monitoring to be implemented)",
        modifier = modifier
    )
}