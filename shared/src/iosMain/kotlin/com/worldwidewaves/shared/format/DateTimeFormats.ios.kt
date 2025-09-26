package com.worldwidewaves.shared.format

import kotlinx.datetime.TimeZone
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
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
