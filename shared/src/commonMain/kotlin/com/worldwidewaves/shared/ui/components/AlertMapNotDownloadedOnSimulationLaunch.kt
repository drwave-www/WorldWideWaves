package com.worldwidewaves.shared.ui.components

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

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.worldwidewaves.shared.MokoRes
import dev.icerock.moko.resources.compose.stringResource

/**
 * Shared AlertMapNotDownloadedOnSimulationLaunch component - EXACT replica of original.
 * Uses platform abstractions for styling while maintaining identical dialog behavior.
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun SharedAlertMapNotDownloadedOnSimulationLaunch(hideDialog: () -> Unit) {
    AlertDialog(
        onDismissRequest = hideDialog,
        title = {
            Text(
                text = stringResource(MokoRes.strings.simulation_map_required_title),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF424242) // scrimLight equivalent
                ),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = stringResource(MokoRes.strings.simulation_map_required_message),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(onClick = hideDialog) {
                Text(stringResource(MokoRes.strings.ok))
            }
        },
    )
}