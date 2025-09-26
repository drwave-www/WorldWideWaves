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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.WWWGlobals.Dimensions

/**
 * Shared debug screen that contains development tools and system information.
 * Provides useful debugging information for development and testing.
 * Only visible in debug builds.
 */
@Composable
fun SharedDebugScreen(
    modifier: Modifier = Modifier,
    onPerformanceClick: () -> Unit = {},
) {
    Surface(modifier = modifier.padding(Dimensions.DEFAULT_EXT_PADDING.dp)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Debug Information",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Platform Information
            item {
                DebugInfoCard(
                    title = "Platform Information",
                    items =
                        listOf(
                            "Platform" to "Multiplatform",
                            "Version" to "1.0.0", // Could be dynamic from BuildConfig
                            "Build Type" to "Debug",
                        ),
                )
            }

            // Application State
            item {
                DebugInfoCard(
                    title = "Application State",
                    items =
                        listOf(
                            "Simulation Mode" to "Disabled", // Could be dynamic
                            "Location Provider" to "GPS", // Could be dynamic
                            "Network Status" to "Connected", // Could be dynamic
                        ),
                )
            }

            // System Information
            item {
                DebugInfoCard(
                    title = "System Resources",
                    items =
                        listOf(
                            "Memory Usage" to "Calculating...", // Could show actual memory
                            "Storage Available" to "Available", // Could show actual storage
                            "Device Orientation" to "Portrait", // Could be dynamic
                        ),
                )
            }

            // Platform-specific information
            item {
                PlatformSpecificPerformanceDashboard(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DebugInfoCard(
    title: String,
    items: List<Pair<String, String>>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            items.forEach { pair ->
                val (label, value) = pair
                DebugInfoRow(label = label, value = value)
            }
        }
    }
}

@Composable
private fun DebugInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Platform-specific performance dashboard component.
 * Android: Simple debug screen placeholder
 * iOS: Simple debug screen placeholder
 */
@Composable
expect fun PlatformSpecificPerformanceDashboard(modifier: Modifier = Modifier)
