package com.worldwidewaves.shared

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class WWWGlobals {
    class WWWGlobals {
        companion object {
            fun today(): LocalDate {
                val now = Clock.System.now()
                return now.toLocalDateTime(TimeZone.currentSystemDefault()).date
            }
        }
    }
}