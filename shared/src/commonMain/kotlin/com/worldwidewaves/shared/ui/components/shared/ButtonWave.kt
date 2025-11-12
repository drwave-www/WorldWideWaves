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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Event
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.ui.theme.sharedExtraBoldTextStyle
import com.worldwidewaves.shared.ui.utils.focusIndicator
import com.worldwidewaves.shared.ui.utils.getIosSafeClock
import dev.icerock.moko.resources.compose.stringResource
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
    isUserWarmingInProgress: Boolean,
    userHasBeenHit: Boolean,
    onNavigateToWave: WaveNavigator,
    modifier: Modifier = Modifier,
    onDisabledClick: (() -> Unit)? = null,
    // iOS FIX: Clock dependency passed as parameter to prevent deadlock
    clock: IClock = getIosSafeClock(),
) {
    // iOS FIX: Removed dangerous object : KoinComponent pattern

    val isRunning = eventState == Status.RUNNING
    val isSoon = eventState == Status.SOON
    val isEndDateTimeRecent =
        endDateTime?.let {
            val now = clock.now()
            it > (now - 1.hours) && it <= now
        } ?: false
    val isEnabled = isInArea && (isRunning || isSoon || isUserWarmingInProgress || isEndDateTimeRecent)
    val shouldBlink = isEnabled && !userHasBeenHit

    // Blinking animation - faster during user warming (300ms), normal during SOON (800ms)
    val blinkDuration = if (isUserWarmingInProgress) 300 else 800
    val infiniteTransition = rememberInfiniteTransition(label = "blinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (shouldBlink) 0.3f else 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = blinkDuration, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "alpha",
    )

    val buttonText = stringResource(MokoRes.strings.wave_now)
    val activeStateDesc = stringResource(MokoRes.strings.accessibility_active)
    val disabledStateDesc = stringResource(MokoRes.strings.accessibility_disabled)

    // Color logic: RED during user warming, primary color otherwise
    val buttonColor =
        when {
            !isEnabled -> Color.Gray
            isUserWarmingInProgress -> Color.Red
            else -> MaterialTheme.colorScheme.primary
        }

    Surface(
        color = buttonColor,
        modifier =
            modifier
                .testTag("JoinWaveButton")
                .width(Event.WAVEBUTTON_WIDTH.dp)
                .height(Event.WAVEBUTTON_HEIGHT.dp)
                .alpha(if (shouldBlink) alpha else 1f) // Apply blinking only when shouldBlink
                .focusIndicator()
                .clickable(onClick = {
                    if (isEnabled) {
                        onNavigateToWave.navigateToWave(eventId)
                    } else {
                        onDisabledClick?.invoke()
                    }
                })
                .semantics {
                    role = Role.Button
                    contentDescription = buttonText
                    stateDescription = if (isEnabled) activeStateDesc else disabledStateDesc
                },
    ) {
        Text(
            modifier =
                Modifier
                    .fillMaxSize()
                    .wrapContentHeight(align = Alignment.CenterVertically),
            text = buttonText,
            style =
                sharedExtraBoldTextStyle(Event.WAVEBUTTON_FONTSIZE).copy(
                    color = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                ),
        )
    }
}

/**
 * Interface for platform-specific navigation to wave screen.
 */
fun interface WaveNavigator {
    fun navigateToWave(eventId: String)
}
