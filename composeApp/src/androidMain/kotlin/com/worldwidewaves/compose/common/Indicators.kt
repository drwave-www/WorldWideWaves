package com.worldwidewaves.compose.common

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

// Reusable composable for showing loading state

/** Generic circular loading indicator with a message. */
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

// Reusable composable for showing download progress

/** Shows percentage, progress bar and cancel button while downloading. */
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
