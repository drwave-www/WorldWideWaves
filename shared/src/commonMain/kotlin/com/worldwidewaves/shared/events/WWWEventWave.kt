package com.worldwidewaves.shared.events

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

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.getLocalDatetime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// ---------------------------

const val METERS_PER_DEGREE_LONGITUDE_AT_EQUATOR = 111320.0

data class WaveNumbers(
    val waveSpeed: String,
    val waveStartTime: String,
    val waveEndTime: String,
    val waveTotalTime: String,
    val waveProgression: String
)

// ---------------------------

class WWWEventWave(val event: WWWEvent) {

    private var cachedLiteralStartTime: String? = null
    private var cachedLiteralEndTime: String? = null
    private var cachedTotalTime: Duration? = null

    // ---------------------------

    suspend fun getAllNumbers(): WaveNumbers {
        return WaveNumbers(
            waveSpeed = event.wave.getLiteralSpeed(),
            waveStartTime = event.wave.getLiteralStartTime(),
            waveEndTime = event.wave.getLiteralEndTime(),
            waveTotalTime = event.wave.getLiteralTotalTime(),
            waveProgression = event.wave.getLiteralProgression()
        )
    }

    // ---------------------------

    fun getLiteralStartTime(): String {
        return cachedLiteralStartTime ?: event.getStartDateTimeAsLocal().let { localDateTime ->
            val hour = localDateTime.hour.toString().padStart(2, '0')
            val minute = localDateTime.minute.toString().padStart(2, '0')
            "$hour:$minute"
        }.also { cachedLiteralStartTime = it }
    }

    // ---------------------------

    fun getLiteralSpeed(): String = "${event.speed} m/s"

    // ---------------------------

    private fun calculateDistance(bbox: BoundingBox, avgLatitude: Double): Double {
        return abs(bbox.maxLongitude - bbox.minLongitude) *
                METERS_PER_DEGREE_LONGITUDE_AT_EQUATOR *
                cos(avgLatitude * PI / 180.0)
    }

    private suspend fun getEndTime(): LocalDateTime {
        val startDateTime = event.getStartDateTimeAsLocal()
        val bbox = event.area.getBoundingBox()
        val avgLatitude = (bbox.minLatitude + bbox.maxLatitude) / 2.0
        val distance = calculateDistance(bbox, avgLatitude)
        val duration = (distance / event.speed).toDuration(DurationUnit.SECONDS)
        return startDateTime.toInstant(event.getTimeZone()).plus(duration).toLocalDateTime(event.getTimeZone())
    }

    suspend fun getLiteralEndTime(): String {
        return cachedLiteralEndTime ?: run {
            val endDateTime = getEndTime()
            val hour = endDateTime.hour.toString().padStart(2, '0')
            val minute = endDateTime.minute.toString().padStart(2, '0')
            "$hour:$minute".also { cachedLiteralEndTime = it }
        }
    }

    // ---------------------------

    private suspend fun getTotalTime(): Duration {
        return cachedTotalTime ?: run {
            val startDateTime = event.getStartDateTimeAsLocal()
            val endDateTime = getEndTime()
            (
                    endDateTime.toInstant(event.getTimeZone()).epochSeconds -
                            startDateTime.toInstant(event.getTimeZone()).epochSeconds
                    ).toDuration(DurationUnit.SECONDS).also { cachedTotalTime = it }
        }
    }

    suspend fun getLiteralTotalTime(): String {
        return "${getTotalTime().inWholeMinutes} min"
    }

    // ---------------------------

    suspend fun getLiteralProgression(): String {
        return when {
            event.isDone() -> "100%"
            !event.isRunning() -> "0%"
            else -> {
                val elapsedTime = getLocalDatetime().toInstant(event.getTimeZone()).epochSeconds -
                        event.getStartDateTimeAsLocal().toInstant(event.getTimeZone()).epochSeconds
                val totalTime = getTotalTime().inWholeSeconds
                (elapsedTime.toDouble() / totalTime * 100).let { "$it%" }
            }
        }
    }

}