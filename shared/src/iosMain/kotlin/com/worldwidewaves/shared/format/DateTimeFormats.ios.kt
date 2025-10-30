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
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.dateFormatFromTemplate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeZoneWithName
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Suppress("MatchingDeclarationName") // Platform-specific actual implementation matches expect declaration
actual object DateTimeFormats {
    /**
     * Formats instant as localized day and month (e.g., "24 Dec" in English, "24. Dez" in German).
     * Format respects the device's locale settings and applies the specified timezone.
     */
    actual fun dayMonth(
        instant: Instant,
        timeZone: TimeZone,
    ): String {
        val formatter = NSDateFormatter()

        // Set locale to respect device language/region settings
        formatter.locale = NSLocale.currentLocale

        // Use locale-aware date format template (equivalent to Android's getBestDateTimePattern)
        val template = "dMMM" // day + abbreviated month
        val localeFormat =
            NSDateFormatter.dateFormatFromTemplate(
                template,
                options = 0uL,
                locale = NSLocale.currentLocale,
            )
        formatter.dateFormat = localeFormat ?: "d MMM" // Fallback to English if template fails

        // Apply timezone (convert Kotlin TimeZone to NSTimeZone)
        formatter.timeZone = NSTimeZone.timeZoneWithName(timeZone.id)
            ?: NSTimeZone.timeZoneWithName("UTC") // Fallback to UTC if invalid timezone

        val date = NSDate.dateWithTimeIntervalSince1970(instant.epochSeconds.toDouble())
        return formatter.stringFromDate(date)
    }

    /**
     * Formats instant as localized short time (e.g., "2:30 PM" in 12-hour, "14:30" in 24-hour).
     * Format respects the device's locale and 12/24-hour preference, and applies the specified timezone.
     */
    actual fun timeShort(
        instant: Instant,
        timeZone: TimeZone,
    ): String {
        val formatter = NSDateFormatter()

        // Set locale to respect device language/region settings
        formatter.locale = NSLocale.currentLocale

        // Use "jm" skeleton (hour + minute) which adapts to 12/24-hour preference per locale
        // This matches Android's behavior with "jm" skeleton
        val template = "jm" // hour + minute in locale order with device 12/24h preference
        val localeFormat =
            NSDateFormatter.dateFormatFromTemplate(
                template,
                options = 0uL,
                locale = NSLocale.currentLocale,
            )
        formatter.dateFormat = localeFormat ?: "HH:mm" // Fallback to 24-hour if template fails

        // Apply timezone (convert Kotlin TimeZone to NSTimeZone)
        formatter.timeZone = NSTimeZone.timeZoneWithName(timeZone.id)
            ?: NSTimeZone.timeZoneWithName("UTC") // Fallback to UTC if invalid timezone

        val date = NSDate.dateWithTimeIntervalSince1970(instant.epochSeconds.toDouble())
        return formatter.stringFromDate(date)
    }
}
