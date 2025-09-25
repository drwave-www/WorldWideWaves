package com.worldwidewaves.shared.ui.components

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Common
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.event_done
import dev.icerock.moko.resources.compose.stringResource
import org.jetbrains.compose.resources.painterResource

/**
 * Shared cross-platform event overlay components.
 * These components provide visual indicators for event states.
 */


/**
 * Top-right banner indicating SOON / RUNNING event states.
 */
@Composable
fun EventOverlaySoonOrRunning(
    eventStatus: Status?,
    modifier: Modifier = Modifier,
) {
    if (eventStatus == Status.SOON || eventStatus == Status.RUNNING) {
        val backgroundColor =
            if (eventStatus == Status.SOON) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.tertiary
            }

        val textResource = if (eventStatus == Status.SOON) {
            MokoRes.strings.event_soon
        } else {
            MokoRes.strings.event_running
        }

        Box(
            modifier = modifier.fillMaxWidth().offset(y = (-5).dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            Box(
                modifier =
                    Modifier
                        .padding(top = Common.SOONRUNNING_PADDING.dp, end = Common.SOONRUNNING_PADDING.dp)
                        .height(Common.SOONRUNNING_HEIGHT.dp)
                        .background(backgroundColor)
                        .padding(horizontal = Dimensions.DEFAULT_INT_PADDING.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(textResource),
                    style = TextStyle(
                        fontSize = Common.SOONRUNNING_FONTSIZE.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSecondary
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Semi-transparent overlay with "done" image when the event is finished.
 */
@Composable
fun EventOverlayDone(
    eventStatus: Status?,
    modifier: Modifier = Modifier,
) {
    if (eventStatus == Status.DONE) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Surface(
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxSize(),
            ) { }
            Image(
                painter = painterResource(Res.drawable.event_done),
                contentDescription = stringResource(MokoRes.strings.event_done),
                modifier = Modifier.width(Common.DONE_IMAGE_WIDTH.dp),
            )
        }
    }
}