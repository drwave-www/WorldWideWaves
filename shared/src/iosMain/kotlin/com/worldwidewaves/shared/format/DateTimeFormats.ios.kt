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


import kotlinx.datetime.TimeZone
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Suppress("MatchingDeclarationName") // Platform-specific actual implementation matches expect declaration
actual object DateTimeFormats {
    actual fun dayMonth(
        instant: Instant,
        timeZone: TimeZone,
    ): String {
        val formatter = NSDateFormatter()
        formatter.dateFormat = "d MMM"
        val date = NSDate.dateWithTimeIntervalSince1970(instant.epochSeconds.toDouble())
        return formatter.stringFromDate(date)
    }

    actual fun timeShort(
        instant: Instant,
        timeZone: TimeZone,
    ): String {
        val formatter = NSDateFormatter()
        formatter.dateFormat = "HH:mm"
        val date = NSDate.dateWithTimeIntervalSince1970(instant.epochSeconds.toDouble())
        return formatter.stringFromDate(date)
    }
}
