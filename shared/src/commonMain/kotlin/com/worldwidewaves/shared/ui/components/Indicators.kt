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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import dev.icerock.moko.resources.compose.stringResource

// Constants for indicators
private object IndicatorConstants {
    // UI Layout Constants
    const val PROGRESS_INDICATOR_WIDTH_RATIO = 0.8f
}

/**
 * Shared generic circular loading indicator with a message.
 * Works identically on both Android and iOS.
 */
@Composable
fun LoadingIndicator(message: String) {
    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier.padding(16.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp,
            strokeCap = StrokeCap.Round,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Shared download progress indicator showing percentage, progress bar and cancel button.
 * Works identically on both Android and iOS.
 */
@Composable
fun DownloadProgressIndicator(
    progress: Int = 0,
    message: String,
    onCancel: () -> Unit = {},
) {
    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier.padding(16.dp),
    ) {
        // Show progress percentage
        Text(
            text = "$progress%",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Linear progress indicator
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier =
                Modifier
                    .fillMaxWidth(IndicatorConstants.PROGRESS_INDICATOR_WIDTH_RATIO)
                    .height(8.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.primary,
            strokeCap = StrokeCap.Round,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Progress message
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Cancel button
        Button(
            onClick = onCancel,
            modifier = Modifier,
        ) {
            Text(text = stringResource(MokoRes.strings.map_cancel_download))
        }
    }
}
