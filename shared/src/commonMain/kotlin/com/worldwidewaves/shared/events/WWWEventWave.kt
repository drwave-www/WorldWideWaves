package com.worldwidewaves.shared.events

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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
 * limitations under the License.
 */


data class WaveNumbers(
    val waveTimezone: String,
    val waveSpeed: String,
    val waveStartTime: String,
    val waveEndTime: String,
    val waveTotalTime: String,
    val waveProgression: String
)

// ---------------------------

abstract class WWWEventWave {

    abstract val event: WWWEvent
    private var cachedLiteralStartTime: String? = null

    abstract suspend fun getAllNumbers(): WaveNumbers

    abstract suspend fun getLiteralEndTime(): String
    abstract suspend fun getLiteralTotalTime(): String
    abstract suspend fun getLiteralProgression(): String

    // ---------------------------

    /**
     * Retrieves the literal speed of the event in meters per second.
     *
     * This function returns the speed of the event as a string formatted with "m/s".
     *
     * @return A string representing the speed of the event in meters per second.
     */
    fun getLiteralSpeed(): String = "${event.speed} m/s"

    /**
     * Retrieves the literal start time of the event in "HH:mm" format.
     *
     * This function first checks if the start time has been cached. If it has, it returns the cached value.
     * If not, it calculates the start time by converting the event's start date and time to a local `LocalDateTime`,
     * formats the hour and minute to ensure they are two digits each, and then caches and returns the formatted time.
     *
     * @return A string representing the start time of the event in "HH:mm" format.
     */
    fun getLiteralStartTime(): String {
        return cachedLiteralStartTime ?: event.getStartDateTime().let { localDateTime ->
            val hour = localDateTime.hour.toString().padStart(2, '0')
            val minute = localDateTime.minute.toString().padStart(2, '0')
            "$hour:$minute"
        }.also { cachedLiteralStartTime = it }
    }

    /**
     * Retrieves the event's time zone offset in the form "UTC+x".
     *
     * This function calculates the current offset of the event's time zone from UTC
     * and returns it as a string in the format "UTC+x".
     *
     * @return A string representing the event's time zone offset in the form "UTC+x".
     */
    fun getLiteralTimezone(): String {
        val offset = TimeZone.of(event.timeZone).offsetAt(Clock.System.now())
        return "UTC${offset.totalSeconds / 3600}"
    }

}