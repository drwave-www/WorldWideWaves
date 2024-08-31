package com.worldwidewaves.shared.events

import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.isPointInPolygon
import com.worldwidewaves.shared.events.utils.splitPolygonByLongitude
import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.serialization.Serializable

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

@Serializable
data class Warming(
    val type: String,
    val longitude: Double? = null,
)

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

    private var _event: WWWEvent? = null

    abstract val speed: Double
    abstract val direction: String
    abstract val warming: Warming

    // ---------------------------

    private var cachedLiteralStartTime: String? = null

    // ---------------------------

    abstract suspend fun getLiteralEndTime(): String
    abstract suspend fun getLiteralTotalTime(): String
    abstract suspend fun getLiteralProgression(): String

    // ---------------------------

    protected val event: WWWEvent
        get() = this._event ?: run {
            Napier.e(tag = "WWWEventWave", message = "Event not set")
            throw IllegalStateException("Event not set")
        }

    fun setEvent(event: WWWEvent) = apply { this._event = event }

    // ---------------------------

    /**
     * Retrieves all wave-related numbers for the event.
     *
     * This function gathers various wave-related metrics such as speed, start time, end time,
     * total time, and progression. It constructs a `WaveNumbers` object containing these metrics.
     *
     * @return A `WaveNumbers` object containing the wave speed, start time, end time, total time, and progression.
     */
    suspend fun getAllNumbers(): WaveNumbers {
        return WaveNumbers(
            waveTimezone = getLiteralTimezone(),
            waveSpeed = getLiteralSpeed(),
            waveStartTime = getLiteralStartTime(),
            waveEndTime = getLiteralEndTime(),
            waveTotalTime = getLiteralTotalTime(),
            waveProgression = getLiteralProgression()
        )
    }

    // ---------------------------

    /**
     * Retrieves the literal speed of the event in meters per second.
     *
     * This function returns the speed of the event as a string formatted with "m/s".
     *
     * @return A string representing the speed of the event in meters per second.
     */
    fun getLiteralSpeed(): String = "$speed m/s"

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
        val hoursOffset = offset.totalSeconds / 3600
        return when {
            hoursOffset == 0 -> "UTC"
            hoursOffset > 0 -> "UTC+$hoursOffset"
            else -> "UTC$hoursOffset"
        }
    }

    // ---------------------------

    private var cachedWarmingPolygons: List<Polygon>? = null

    /**
     * Checks if a given position is within any of the warming polygons.
     *
     * This function retrieves the warming polygons and checks if the specified position
     * is within any of these polygons using the `isPointInPolygon` function.
     *
     * @param position The position to check.
     * @return `true` if the position is within any warming polygon, `false` otherwise.
     */
    suspend fun isPositionWithinWarming(position: Position): Boolean {
        return getWarmingPolygons().any { isPointInPolygon(position, it) }
    }

    /**
     * Retrieves the warming polygons for the event area.
     *
     * This function returns a list of polygons representing the warming zones for the event area.
     * If the warming polygons are already cached, it returns the cached value. Otherwise, it splits
     * the event area polygon by the warming zone longitude and caches the resulting right-side polygons.
     *
     * @return A list of polygons representing the warming zones.
     */
    suspend fun getWarmingPolygons(): List<Polygon> {
        if (cachedWarmingPolygons == null) {
            cachedWarmingPolygons = when (warming.type) {
                "longitude-cut" -> splitPolygonByLongitude(
                    event.area.getPolygon(),
                    warming.longitude ?: run {
                        Napier.e(tag = "WWWEventWave", message = "Longitude not set in configuration")
                        return emptyList()
                    }
                ).right
                else -> emptyList()
            }
        }
        return cachedWarmingPolygons!!
    }

}