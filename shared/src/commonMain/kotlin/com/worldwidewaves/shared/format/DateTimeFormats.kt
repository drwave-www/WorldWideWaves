package com.worldwidewaves.shared.format

import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
expect object DateTimeFormats {
    // Localized day + month only (no year). May not be zero-padded depending on locale.
    fun dayMonth(instant: Instant, timeZone: TimeZone): String

    // Localized short time (hours:minutes, 12/24h per platform conventions/preferences).
    fun timeShort(instant: Instant, timeZone: TimeZone): String
}
