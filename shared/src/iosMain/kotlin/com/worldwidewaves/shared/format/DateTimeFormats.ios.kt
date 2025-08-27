package com.worldwidewaves.shared.format

import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.setLocalizedDateFormatFromTemplate

@OptIn(ExperimentalTime::class)
actual object DateTimeFormats {
    actual fun dayMonth(instant: Instant, timeZone: TimeZone): String {
        val formatter = NSDateFormatter()
        formatter.setLocalizedDateFormatFromTemplate("dMMM")
        formatter.timeZone = NSTimeZone.timeZoneWithName(timeZone.id)
        val date = NSDate.dateWithTimeIntervalSince1970(instant.epochSeconds.toDouble())
        return formatter.stringFromDate(date)
    }

    actual fun timeShort(instant: Instant, timeZone: TimeZone): String {
        val formatter = NSDateFormatter()
        formatter.setLocalizedDateFormatFromTemplate("jm")
        formatter.timeZone = NSTimeZone.timeZoneWithName(timeZone.id)
        val date = NSDate.dateWithTimeIntervalSince1970(instant.epochSeconds.toDouble())
        return formatter.stringFromDate(date)
    }
}
