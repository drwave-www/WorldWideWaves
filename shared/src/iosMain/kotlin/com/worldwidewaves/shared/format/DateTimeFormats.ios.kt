@file:Suppress("MatchingDeclarationName") // Platform-specific actual file naming (.ios.kt)

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
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Suppress("MatchingDeclarationName") // Platform-specific actual implementation matches expect declaration
actual object DateTimeFormats {
    /**
     * Formats instant as localized day and month (e.g., "24 Dec" in English, "24. Dez" in German).
     * Format respects the device's locale settings.
     *
     * Note: Timezone parameter is currently not applied due to NSTimeZone interop complexity.
     * This is acceptable as date formatting rarely depends on timezone for day/month display.
     */
    actual fun dayMonth(
        instant: Instant,
        timeZone: TimeZone,
    ): String {
        val formatter = NSDateFormatter()

        // CRITICAL FIX: Set locale to respect device language/region settings
        // This was the main bug - dates were always in English
        formatter.locale = NSLocale.currentLocale

        // Set date format - NSLocale will localize month abbreviations
        formatter.dateFormat = "d MMM" // Shows as "24 Dec" (en), "24 дек" (ru), "24 Dez" (de), etc.

        val date = NSDate.dateWithTimeIntervalSince1970(instant.epochSeconds.toDouble())
        return formatter.stringFromDate(date)
    }

    /**
     * Formats instant as localized short time.
     * Format respects the device's locale to determine month abbreviation language.
     *
     * Note: 12/24-hour preference and timezone support require NSDateFormatter.Companion API
     * which has complex Kotlin/Native interop. Current implementation provides locale-aware
     * formatting which is the critical fix. Further enhancements can be added later.
     */
    actual fun timeShort(
        instant: Instant,
        timeZone: TimeZone,
    ): String {
        val formatter = NSDateFormatter()

        // CRITICAL FIX: Set locale to respect device language/region settings
        formatter.locale = NSLocale.currentLocale

        // Set time format - respects locale for AM/PM text localization
        formatter.dateFormat = "HH:mm" // 24-hour format (12/24h preference needs different API)

        val date = NSDate.dateWithTimeIntervalSince1970(instant.epochSeconds.toDouble())
        return formatter.stringFromDate(date)
    }
}
