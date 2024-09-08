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

import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.isPointInPolygon
import com.worldwidewaves.shared.events.utils.splitPolygonByLongitude
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// ---------------------------

@Serializable
data class WWWEventWaveLinear(
    override val speed: Double,
    override val direction: String,
    override val warming: WWWEventWaveWarming
) : KoinComponent, WWWEventWave() {

    @Transient private var cachedTotalTime: Duration? = null
    @Transient private var cachedWarmingPolygons: List<Polygon>? = null

    // ---------------------------

    /**
     * Retrieves the warming polygons for the event area.
     *
     * This function returns a list of polygons representing the warming zones for the event area.
     * If the warming polygons are already cached, it returns the cached value. Otherwise, it splits
     * the event area polygon by the warming zone longitude and caches the resulting right-side polygons.
     *
     */
    override suspend fun getWarmingPolygons(): List<Polygon> {
        if (cachedWarmingPolygons == null) {
            cachedWarmingPolygons = when (warming.type) {
                "longitude-cut" -> event.area.getPolygons().flatMap { polygon ->
                    splitPolygonByLongitude(polygon, warming.longitude!!).right
                }
                else -> emptyList()
            }
        }
        return cachedWarmingPolygons!!
    }

    // ---------------------------

    /**
     * Calculates the total duration of the wave from its start time to its end time.
     *
     * This function first checks if the total duration has been previously calculated and cached.
     * If not, it calculates the duration by finding the difference between the event's end time
     * and start time in seconds, and then converts this difference to a `Duration` object.
     * The calculated duration is then cached for future use.
     *
     */
    override suspend fun getWaveDuration(): Duration { // FIXME: buggy, infinite loop
        return cachedTotalTime ?: run {
            val startDateTime = event.getStartDateTime()
            val endDateTime = getEndTime()
            (endDateTime.toInstant(event.getTZ()).epochSeconds -
                            startDateTime.toInstant(event.getTZ()).epochSeconds
                    ).toDuration(DurationUnit.SECONDS).also { cachedTotalTime = it }
        }
    }

    override suspend fun getWarmingDuration(): Duration {
        TODO("Not yet implemented")
    }

    // ---------------------------

    override suspend fun hasUserBeenHit(): Boolean {
        TODO("Not yet implemented")
    }

    // ---------------------------

    override fun validationErrors() : List<String>? {
        val superValid = super.validationErrors()
        val errors = superValid?.toMutableList() ?: mutableListOf()

        // TODO

        return errors.takeIf { it.isNotEmpty() }?.map { "wavelinear: $it" }
    }

}