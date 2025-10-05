package com.worldwidewaves.shared.format

/* * Copyright 2025 DrWave
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
 * limitations under the License. */


import android.icu.text.SimpleDateFormat
import android.text.format.DateFormat
import kotlinx.datetime.TimeZone
import java.util.Date
import java.util.Locale
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import android.icu.util.TimeZone as IcuTimeZone

@OptIn(ExperimentalTime::class)
@Suppress("MatchingDeclarationName") // Platform-specific actual implementation matches expect declaration
actual object DateTimeFormats {
    actual fun dayMonth(
        instant: Instant,
        timeZone: TimeZone,
    ): String {
        val locale = Locale.getDefault()
        val pattern = DateFormat.getBestDateTimePattern(locale, "dMMM")
        val sdf =
            SimpleDateFormat(pattern, locale).apply {
                this.timeZone = IcuTimeZone.getTimeZone(timeZone.id)
            }
        val date = Date(instant.epochSeconds * 1000 + instant.nanosecondsOfSecond / 1000000)
        return sdf.format(date)
    }

    actual fun timeShort(
        instant: Instant,
        timeZone: TimeZone,
    ): String {
        val locale = Locale.getDefault()
        // Use skeleton that adapts 12/24h per locale; note: may not reflect user device pref without context
        val pattern = DateFormat.getBestDateTimePattern(locale, "jm") // hour + minute, locale order
        val sdf =
            SimpleDateFormat(pattern, locale).apply {
                this.timeZone = IcuTimeZone.getTimeZone(timeZone.id)
            }
        val date = Date(instant.epochSeconds * 1000 + instant.nanosecondsOfSecond / 1000000)
        return sdf.format(date)
    }
}
