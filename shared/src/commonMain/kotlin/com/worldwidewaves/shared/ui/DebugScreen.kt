/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package com.worldwidewaves.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.worldwidewaves.shared.ui.screens.SharedDebugScreen

/**
 * Android wrapper for SharedDebugScreen.
 * Delegates to shared implementation for perfect cross-platform parity.
 */
class DebugScreen : TabScreen {
    override val name = "Debug"

    @Composable
    override fun Screen(modifier: Modifier) {
        SharedDebugScreen(
            modifier = modifier,
            onPerformanceClick = {
                // Debug functionality handled by shared debug screen
            },
        )
    }
}
