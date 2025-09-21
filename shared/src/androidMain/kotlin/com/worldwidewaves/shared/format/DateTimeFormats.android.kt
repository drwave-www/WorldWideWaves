package com.worldwidewaves.shared.format

import android.icu.text.SimpleDateFormat
import android.text.format.DateFormat
import kotlinx.datetime.TimeZone
import java.util.Date
import java.util.Locale
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import android.icu.util.TimeZone as IcuTimeZone

@OptIn(ExperimentalTime::class)
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
