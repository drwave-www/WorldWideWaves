package com.worldwidewaves.shared.format

import kotlinx.datetime.TimeZone
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
actual object DateTimeFormats {
    actual fun dayMonth(
        instant: Instant,
        timeZone: TimeZone,
    ): String {
        val formatter = NSDateFormatter()
        formatter.locale = NSLocale.currentLocale
        formatter.dateFormat = "d MMM"
        val date = NSDate(timeIntervalSince1970 = instant.epochSeconds.toDouble())
        return formatter.stringFromDate(date)
    }

    actual fun timeShort(
        instant: Instant,
        timeZone: TimeZone,
    ): String {
        val formatter = NSDateFormatter()
        formatter.locale = NSLocale.currentLocale
        formatter.dateFormat = "HH:mm"
        val date = NSDate(timeIntervalSince1970 = instant.epochSeconds.toDouble())
        return formatter.stringFromDate(date)
    }
}
