package com.worldwidewaves.shared.ui.utils

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

import androidx.compose.runtime.Composable
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.DisplayText
import dev.icerock.moko.resources.compose.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Formats a duration in minutes into a human-readable string (e.g., "1 hour 5 minutes").
 * If totalMinutes is null, returns the original value string.
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun formatDurationMinutes(
    totalMinutes: Long?,
    defaultValue: String,
): String = totalMinutes?.let { mins ->
    val hours = (mins / 60).toInt()
    val minutesLeft = (mins % 60).toInt()
    val parts = buildList {
        if (hours > 0) {
            add(
                if (hours == 1) {
                    stringResource(MokoRes.strings.hour_singular, hours)
                } else {
                    stringResource(MokoRes.strings.hour_plural, hours)
                }
            )
        }
        if (minutesLeft > 0) {
            add(
                if (minutesLeft == 1) {
                    stringResource(MokoRes.strings.minute_singular, minutesLeft)
                } else {
                    stringResource(MokoRes.strings.minute_plural, minutesLeft)
                }
            )
        }
    }
    if (parts.isNotEmpty()) parts.joinToString(" ") else defaultValue
} ?: defaultValue

/**
 * Formats a Duration into time display format (MM:SS or HH:MM).
 * EXACT replica of original Android WaveActivity formatDuration function.
 */
fun formatDuration(duration: Duration): String = when {
    duration.isInfinite() || duration < Duration.ZERO -> "--:--" // Protection
    duration < 1.hours -> {
        val minutes = duration.inWholeMinutes.toString().padStart(2, '0')
        val seconds = (duration.inWholeSeconds % 60).toString().padStart(2, '0')
        "$minutes:$seconds"
    }
    duration < 99.hours -> {
        val hours = duration.inWholeHours.toString().padStart(2, '0')
        val minutes = (duration.inWholeMinutes % 60).toString().padStart(2, '0')
        "$hours:$minutes"
    }
    else -> DisplayText.EMPTY_COUNTER
}