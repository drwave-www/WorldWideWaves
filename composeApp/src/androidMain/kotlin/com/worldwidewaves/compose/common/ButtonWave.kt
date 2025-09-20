package com.worldwidewaves.compose.common

import android.content.Intent
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldwidewaves.activities.event.WaveActivity
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_WAVEBUTTON_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_WAVEBUTTON_HEIGHT
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_WAVEBUTTON_WIDTH
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.theme.onQuaternaryLight
import com.worldwidewaves.theme.quinaryColoredBoldTextStyle
import dev.icerock.moko.resources.compose.stringResource
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Primary button that navigates to [WaveActivity] when the wave is active or imminent. */
@OptIn(ExperimentalTime::class)
@Composable
fun ButtonWave(
    eventId: String,
    eventState: Status,
    endDateTime: Instant?,
    clock: IClock,
    isInArea: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val isRunning = eventState == Status.RUNNING
    val isSoon = eventState == Status.SOON
    val isEndDateTimeRecent =
        endDateTime?.let {
            val now = clock.now()
            it > (now - 1.hours) && it <= now
        } ?: false
    val isEnabled = isInArea && (isRunning || isSoon || isEndDateTimeRecent)

    // Blinking animation
    val infiniteTransition = rememberInfiniteTransition(label = "blinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isEnabled) 0.3f else 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "alpha",
    )

    Surface(
        color = if (isEnabled) MaterialTheme.colorScheme.primary else onQuaternaryLight,
        modifier =
            modifier
                .width(DIM_EVENT_WAVEBUTTON_WIDTH.dp)
                .height(DIM_EVENT_WAVEBUTTON_HEIGHT.dp)
                .alpha(if (isEnabled) alpha else 1f) // Apply blinking only when enabled
                .clickable(enabled = isEnabled, onClick = {
                    context.startActivity(
                        Intent(context, WaveActivity::class.java).apply {
                            putExtra("eventId", eventId)
                        },
                    )
                }),
    ) {
        Text(
            modifier =
                Modifier
                    .fillMaxSize()
                    .wrapContentHeight(align = Alignment.CenterVertically),
            text = stringResource(MokoRes.strings.wave_now),
            style =
                quinaryColoredBoldTextStyle(DIM_EVENT_WAVEBUTTON_FONTSIZE).copy(
                    textAlign = TextAlign.Center,
                ),
        )
    }
}
