package com.worldwidewaves.shared.ui.screens

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.WWWGlobals.Dimensions

/**
 * Shared debug screen that contains development tools and performance monitoring.
 * Uses platform-specific performance monitoring through expect/actual pattern.
 * Only visible in debug builds.
 */
@Composable
fun SharedDebugScreen(
    modifier: Modifier = Modifier,
    onPerformanceClick: () -> Unit = {},
) {
    Surface(modifier = modifier.padding(Dimensions.DEFAULT_EXT_PADDING.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Debug Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Shared debug screen - identical on both platforms",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Platform-specific performance dashboard
            PlatformSpecificPerformanceDashboard(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Platform-specific performance dashboard component.
 * Android: Simple debug screen placeholder
 * iOS: Simple debug screen placeholder
 */
@Composable
expect fun PlatformSpecificPerformanceDashboard(
    modifier: Modifier = Modifier,
)