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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.ui.theme.onSurfaceVariantLight
import com.worldwidewaves.shared.ui.theme.scrimLight
import com.worldwidewaves.shared.ui.theme.sharedCommonTextStyle
import dev.icerock.moko.resources.compose.stringResource

/**
 * Confirmation dialog for map uninstall operation.
 * Shows a warning that the map will be removed and requires user confirmation.
 */
@Composable
fun AlertMapUninstall(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(MokoRes.strings.events_uninstall_map_title),
                style =
                    sharedCommonTextStyle().copy(
                        color = scrimLight,
                        fontWeight = FontWeight.Bold,
                    ),
            )
        },
        text = {
            Text(
                text = stringResource(MokoRes.strings.events_uninstall_map_confirmation),
                style = sharedCommonTextStyle().copy(color = onSurfaceVariantLight),
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(MokoRes.strings.events_uninstall))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MokoRes.strings.map_cancel_download))
            }
        },
    )
}
