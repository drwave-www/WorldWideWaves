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

import com.worldwidewaves.shared.events.WWWEventWaveWarming.Type.LONGITUDE_CUT
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.GeoUtils.calculateDistance
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.PolygonUtils.splitPolygonByLongitude
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
    override val direction: Direction,
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
                LONGITUDE_CUT -> event.area.getPolygons().flatMap { polygon ->
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
    override suspend fun getWaveDuration(): Duration {
        return cachedTotalTime ?: run {
            val bbox = event.area.getBoundingBox()
            val latitude = (bbox.maxLatitude + bbox.minLatitude) / 2
            val maxEastWestDistance = calculateDistance(bbox.minLongitude, bbox.maxLongitude, latitude)
            val durationInSeconds = maxEastWestDistance / speed
            durationInSeconds.toDuration(DurationUnit.SECONDS)
                .also { cachedTotalTime = it }
        }
    }

    // ---------------------------

    override suspend fun hasUserBeenHit(): Boolean {
        val userPosition = getUserPosition() ?: return false
        val bbox = event.area.getBoundingBox()
        val waveCurrentLongitude = currentWaveLongitude(bbox)
        return userPosition.lng in bbox.minLongitude..waveCurrentLongitude
    }

    fun currentWaveLongitude(bbox: BoundingBox): Double {
        val latitude = (bbox.maxLatitude + bbox.minLatitude) / 2
        val maxEastWestDistance = calculateDistance(bbox.minLongitude, bbox.maxLongitude, latitude)
        val distanceTraveled = speed * (clock.now() - event.getStartDateTime()).inWholeSeconds

        val longitudeDelta = (distanceTraveled / maxEastWestDistance) * (bbox.maxLongitude - bbox.minLongitude)
        return if (direction == Direction.WEST) {
            bbox.maxLongitude - longitudeDelta
        } else {
            bbox.minLongitude + longitudeDelta
        }
    }

    // ---------------------------

    override fun validationErrors() : List<String>? {
        val superValid = super.validationErrors()
        val errors = superValid?.toMutableList() ?: mutableListOf()

        // TODO

        return errors.takeIf { it.isNotEmpty() }?.map { "${WWWEventWaveLinear::class.simpleName}: $it" }
    }

}