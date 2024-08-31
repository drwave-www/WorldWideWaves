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

import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_DEFAULT_REFRESH_INTERVAL
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.getLocalDatetime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// ---------------------------

const val METERS_PER_DEGREE_LONGITUDE_AT_EQUATOR = 111320.0

// ---------------------------

@Serializable
data class WWWEventWaveLinear(
    override val speed: Double,
    override val direction: String,
    override val warming: Warming
) : WWWEventWave() {

    private var observationInterval: Long = WAVE_DEFAULT_REFRESH_INTERVAL
    private var cachedEndTime: LocalDateTime? = null
    private var cachedTotalTime: Duration? = null

    // ---------------------------

    override suspend fun getObservationInterval(): Long = observationInterval

    // ---------------------------

    /**
     * Calculates the distance between the easternmost and westernmost points of a bounding box,
     * adjusted for the average latitude.
     *
     * This function computes the distance in meters between the northeast and southwest corners
     * of the bounding box along the longitude, taking into account the average latitude to adjust
     * for the Earth's curvature.
     *
     * @param bbox The bounding box containing the northeast and southwest coordinates.
     * @param avgLatitude The average latitude of the bounding box, used to adjust the distance calculation.
     * @return The distance in meters between the easternmost and westernmost points of the bounding box.
     */
    private fun calculateDistance(bbox: BoundingBox, avgLatitude: Double): Double {
        return abs(bbox.ne.lng - bbox.sw.lng) *
                METERS_PER_DEGREE_LONGITUDE_AT_EQUATOR *
                cos(avgLatitude * PI / 180.0)
    }

    /**
     * Calculates the end time of the event based on its start time, bounding box, and speed.
     *
     * This function determines the end time of the event by performing the following steps:
     * 1. Retrieves the start date and time of the event in the local time zone.
     * 2. Obtains the bounding box of the event area.
     * 3. Calculates the average latitude of the bounding box.
     * 4. Computes the distance across the bounding box at the average latitude.
     * 5. Calculates the duration of the event based on the distance and the event's speed.
     * 6. Adds the duration to the start time to get the end time.
     *
     * @return The calculated end time of the event as a `LocalDateTime` object.
     */
    override suspend fun getEndTime(): LocalDateTime {
        return cachedEndTime ?: run {
            val startDateTime = event.getStartDateTime()
            val bbox = event.area.getBoundingBox()
            val avgLatitude = (bbox.sw.lat + bbox.ne.lat) / 2.0
            val distance = calculateDistance(bbox, avgLatitude)
            val duration = (distance / speed).toDuration(DurationUnit.SECONDS)
            startDateTime.toInstant(event.getTimeZone()).plus(duration).toLocalDateTime(event.getTimeZone())
        }.also { cachedEndTime = it }
    }

    // ---------------------------

    /**
     * Calculates the total duration of the event from its start time to its end time.
     *
     * This function first checks if the total duration has been previously calculated and cached.
     * If not, it calculates the duration by finding the difference between the event's end time
     * and start time in seconds, and then converts this difference to a `Duration` object.
     * The calculated duration is then cached for future use.
     *
     * @return The total duration of the event as a `Duration` object.
     */
    override suspend fun getTotalTime(): Duration {
        return cachedTotalTime ?: run {
            val startDateTime = event.getStartDateTime()
            val endDateTime = getEndTime()
            (endDateTime.toInstant(event.getTimeZone()).epochSeconds -
                            startDateTime.toInstant(event.getTimeZone()).epochSeconds
                    ).toDuration(DurationUnit.SECONDS).also { cachedTotalTime = it }
        }
    }

    // ---------------------------

    /**
     * Calculates the literal progression of the event as a percentage.
     *
     * This function determines the progression of the event based on its current state and elapsed time.
     * If the event is done, it returns "100%". If the event is not running, it returns "0%".
     * Otherwise, it calculates the elapsed time since the event started and expresses it as a percentage
     * of the total event duration.
     *
     * @return A string representing the progression of the event as a percentage.
     */
    override suspend fun getProgression(): Double {
        return when {
            event.isDone() -> 100.0
            !event.isRunning() -> 0.0
            else -> {
                val elapsedTime = getLocalDatetime().toInstant(event.getTimeZone()).epochSeconds -
                        event.getStartDateTime().toInstant(event.getTimeZone()).epochSeconds
                val totalTime = getTotalTime().inWholeSeconds
                (elapsedTime.toDouble() / totalTime * 100).coerceAtMost(100.0)
            }
        }
    }

    // ---------------------------

    override suspend fun isWarmingEnded(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun hasUserBeenHit(): Boolean {
        TODO("Not yet implemented")
    }

}