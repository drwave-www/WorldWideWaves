/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package com.worldwidewaves.compose.tabs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.worldwidewaves.shared.ui.TabScreen
import com.worldwidewaves.shared.ui.screens.SharedDebugScreen
import com.worldwidewaves.shared.monitoring.AndroidPerformanceMonitor

/**
 * Android wrapper for SharedDebugScreen.
 * Delegates to shared implementation for perfect cross-platform parity.
 */
class DebugScreen(
    private val performanceMonitor: AndroidPerformanceMonitor,
) : TabScreen {
    override val name = "Debug"

    @Composable
    override fun Screen(modifier: Modifier) {
        SharedDebugScreen(
            modifier = modifier,
            onPerformanceClick = {
                // Performance monitor integration handled by shared debug screen
            }
        )
    }
}
