package com.worldwidewaves.compose.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_COMMON_DONE_IMAGE_WIDTH
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_COMMON_SOONRUNNING_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_COMMON_SOONRUNNING_HEIGHT
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_COMMON_SOONRUNNING_PADDING
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_INT_PADDING
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.event_done
import com.worldwidewaves.theme.commonTextStyle
import dev.icerock.moko.resources.compose.stringResource
import org.jetbrains.compose.resources.painterResource

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
