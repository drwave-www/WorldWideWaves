@file:OptIn(ExperimentalTime::class)

package com.worldwidewaves.compose

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.worldwidewaves.R
import com.worldwidewaves.activities.event.WaveActivity
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_COMMON_DONE_IMAGE_WIDTH
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_COMMON_SOCIALNETWORKS_ACCOUNT_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_COMMON_SOCIALNETWORKS_HASHTAG_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_COMMON_SOCIALNETWORKS_INSTAGRAM_LOGO_WIDTH
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_COMMON_SOONRUNNING_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_COMMON_SOONRUNNING_HEIGHT
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_COMMON_SOONRUNNING_PADDING
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_INT_PADDING
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_SPACER_MEDIUM
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DIVIDER_THICKNESS
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DIVIDER_WIDTH
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_WAVEBUTTON_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_WAVEBUTTON_HEIGHT
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_WAVEBUTTON_WIDTH
import com.worldwidewaves.shared.WWWGlobals.Companion.URL_BASE_INSTAGRAM
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.event_done
import com.worldwidewaves.shared.generated.resources.instagram_icon
import com.worldwidewaves.theme.commonBoldStyle
import com.worldwidewaves.theme.commonTextStyle
import com.worldwidewaves.theme.onQuaternaryLight
import com.worldwidewaves.theme.quinaryColoredBoldTextStyle
import dev.icerock.moko.resources.compose.stringResource
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import androidx.compose.ui.res.painterResource as painterResourceAndroid

// ----------------------------

@Composable
/** Horizontal white divider reused across screens. */
fun DividerLine(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.width(DIM_DIVIDER_WIDTH.dp),
        color = Color.White, thickness = DIM_DIVIDER_THICKNESS.dp
    )
}

// ----------------------------

@Composable
/** Top-right banner indicating SOON / RUNNING event states. */
fun EventOverlaySoonOrRunning(eventStatus: Status?, modifier: Modifier = Modifier) {
    if (eventStatus == Status.SOON || eventStatus == Status.RUNNING) {
        val (backgroundColor, textId) = if (eventStatus == Status.SOON) {
            MaterialTheme.colorScheme.secondary to MokoRes.strings.event_soon
        } else {
            MaterialTheme.colorScheme.tertiary to MokoRes.strings.event_running
        }

        Box(
            modifier = modifier.fillMaxWidth().offset(y = (-5).dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .padding(top = DIM_COMMON_SOONRUNNING_PADDING.dp, end = DIM_COMMON_SOONRUNNING_PADDING.dp)
                    .height(DIM_COMMON_SOONRUNNING_HEIGHT.dp)
                    .background(backgroundColor)
                    .padding(horizontal = DIM_DEFAULT_INT_PADDING.dp), // Changed to horizontal padding
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(textId),
                    style = commonTextStyle(DIM_COMMON_SOONRUNNING_FONTSIZE),
                    textAlign = TextAlign.Center // Added text alignment
                )
            }
        }
    }
}

// ----------------------------


// ---------------------------------------------------------------------------
//  Adaptive single-line text helper
// ---------------------------------------------------------------------------

/**
 * Renders [text] on a SINGLE line, automatically shrinking the font size until
 * it fits the available width (down to [minFontSizeSp]).
 *
 * Useful for variable-length strings that must never wrap – e.g. headings or
 * status banners.
 */
@Composable
fun AutoResizeSingleLineText(
    text: String,
    style: TextStyle,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    minFontSizeSp: Float = 8f,
    stepScale: Float = 0.9f,
    modifier: Modifier = Modifier
) {
    var fontSize by remember { mutableStateOf(style.fontSize) }

    Text(
        text = text,
        style = style.copy(fontSize = fontSize),
        color = color,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize.value > minFontSizeSp) {
                fontSize *= stepScale
            }
        },
        modifier = modifier
    )
}

@Composable
/** Semi-transparent overlay with "done" image when the event is finished. */
fun EventOverlayDone(eventStatus: Status?, modifier: Modifier = Modifier) {
    if (eventStatus == Status.DONE) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Surface(
                color = Color.run { White.copy(alpha = 0.5f) },
                modifier = Modifier.fillMaxSize()
            ) { }
            Image(
                painter = painterResource(Res.drawable.event_done),
                contentDescription = stringResource(MokoRes.strings.event_done),
                modifier = Modifier.width(DIM_COMMON_DONE_IMAGE_WIDTH.dp),
            )
        }
    }
}

// ----------------------------

@Composable
/** Primary button that navigates to [WaveActivity] when the wave is active or imminent. */
fun ButtonWave(eventId: String, eventState: Status, endDateTime: Instant?, clock: IClock, isInArea: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val isRunning = eventState == Status.RUNNING
    val isSoon = eventState == Status.SOON
    val isEndDateTimeRecent = endDateTime?.let {
        val now = clock.now()
        it > (now - 1.hours) && it <= now
    } ?: false
    val isEnabled = isInArea && (isRunning || isSoon || isEndDateTimeRecent)

    Surface(
        color = if (isEnabled) MaterialTheme.colorScheme.primary else onQuaternaryLight,
        modifier = modifier
            .width(DIM_EVENT_WAVEBUTTON_WIDTH.dp)
            .height(DIM_EVENT_WAVEBUTTON_HEIGHT.dp)
            .clickable(enabled = isEnabled, onClick = {
                context.startActivity(Intent(context, WaveActivity::class.java).apply {
                    putExtra("eventId", eventId)
                })
            })
    ) {
        Text(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight(align = Alignment.CenterVertically),
            text = stringResource(MokoRes.strings.wave_now),
            style = quinaryColoredBoldTextStyle(DIM_EVENT_WAVEBUTTON_FONTSIZE).copy(
                textAlign = TextAlign.Center
            )
        )
    }
}

// ----------------------------

@Composable
/** Displays clickable Instagram account & hashtag with logo; opens external URI on tap. */
fun WWWSocialNetworks(
    modifier: Modifier = Modifier,
    instagramAccount: String,
    instagramHashtag: String
) {
    val uriHandler = LocalUriHandler.current

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(Res.drawable.instagram_icon),
            contentDescription = stringResource(MokoRes.strings.instagram_logo_description),
            modifier = Modifier.width(DIM_COMMON_SOCIALNETWORKS_INSTAGRAM_LOGO_WIDTH.dp)
        )
        Column(
            modifier = Modifier.padding(start = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                modifier = Modifier.clickable(onClick = {
                    try {
                        uriHandler.openUri("$URL_BASE_INSTAGRAM/$instagramAccount")
                    } catch (e: Exception) {
                        Log.e("AboutWWWSocialNetworks", "Failed to open URI", e)
                    }
                }),
                text = instagramAccount,
                style = commonBoldStyle(DIM_COMMON_SOCIALNETWORKS_ACCOUNT_FONTSIZE).copy(
                    textDecoration = TextDecoration.Underline
                )
            )
            Text(
                text = instagramHashtag,
                style = commonTextStyle(DIM_COMMON_SOCIALNETWORKS_HASHTAG_FONTSIZE)
            )
        }
    }
    Spacer(modifier = Modifier.size(DIM_DEFAULT_SPACER_MEDIUM.dp))
}

// ----------------------------

// Reusable composable for showing loading state
@Composable
/** Generic circular loading indicator with a message. */
fun LoadingIndicator(message: String) {
    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

// Reusable composable for showing download progress
@Composable
/** Shows percentage, progress bar and cancel button while downloading. */
fun DownloadProgressIndicator(progress: Int = 0, message: String, onCancel: () -> Unit = {}) {
    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        // Show progress percentage
        Text(
            text = "$progress%",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Linear progress indicator
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(8.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.primary,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Progress message
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Cancel button
        Button(
            onClick = onCancel,
            modifier = Modifier
        ) {
            Text(text = stringResource(MokoRes.strings.map_cancel_download))
        }
    }
}

@Composable
/** Error block with retry action – used for map download failures. */
fun ErrorMessage(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = modifier.padding(16.dp)
    ) {
        Icon(
            painter = painterResourceAndroid(R.drawable.ic_info),
            contentDescription = stringResource(MokoRes.strings.error),
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
        ) {
            Icon(
                painter = painterResourceAndroid(R.drawable.ic_refresh),
                contentDescription = stringResource(MokoRes.strings.map_retry_download),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(MokoRes.strings.map_retry_download))
        }
    }
}