package com.worldwidewaves.shared.ui.components.shared

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Event
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.ui.theme.sharedExtraBoldTextStyle
import dev.icerock.moko.resources.compose.stringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Interface for platform-specific navigation to wave screen.
 */
fun interface WaveNavigator {
    fun navigateToWave(eventId: String)
}

/**
 * Shared cross-platform wave button component.
 * Primary button that navigates to wave screen when the wave is active or imminent.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun ButtonWave(
    eventId: String,
    eventState: Status,
    endDateTime: Instant?,
    isInArea: Boolean,
    onNavigateToWave: WaveNavigator,
    modifier: Modifier = Modifier,
) {
    val clockComponent =
        object : KoinComponent {
            val clock: IClock by inject()
        }
    val clock = clockComponent.clock

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
        color = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
        modifier =
            modifier
                .width(Event.WAVEBUTTON_WIDTH.dp)
                .height(Event.WAVEBUTTON_HEIGHT.dp)
                .alpha(if (isEnabled) alpha else 1f) // Apply blinking only when enabled
                .clickable(enabled = isEnabled, onClick = {
                    onNavigateToWave.navigateToWave(eventId)
                }),
    ) {
        Text(
            modifier =
                Modifier
                    .fillMaxSize()
                    .wrapContentHeight(align = Alignment.CenterVertically),
            text = stringResource(MokoRes.strings.wave_now),
            style =
                sharedExtraBoldTextStyle(Event.WAVEBUTTON_FONTSIZE).copy(
                    color = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                ),
        )
    }
}
