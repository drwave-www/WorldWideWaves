package com.worldwidewaves.shared.events

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
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

import com.worldwidewaves.shared.events.utils.Area
import com.worldwidewaves.shared.events.utils.ComposedLongitude
import com.worldwidewaves.shared.events.utils.EarthAdaptedSpeedLongitude
import com.worldwidewaves.shared.events.utils.GeoUtils.calculateDistance
import com.worldwidewaves.shared.events.utils.MutableArea
import com.worldwidewaves.shared.events.utils.PolygonUtils.SplitResult
import com.worldwidewaves.shared.events.utils.PolygonUtils.splitByLongitude
import com.worldwidewaves.shared.events.utils.Position
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// ---------------------------

@OptIn(ExperimentalTime::class)
@Serializable
data class WWWEventWaveLinear(
    override val speed: Double,
    override val direction: Direction,
    override val approxDuration: Int
) : KoinComponent, WWWEventWave() {

    @Transient private var cachedLongitude: EarthAdaptedSpeedLongitude? = null
    @Transient private var cachedWaveDuration: Duration? = null
    @Transient private var cachedHitDateTime: Instant? = null
    @Transient private var cachedHitPosition: Position? = null
    @Transient private val epsilonLatPosition = 0.000009 // Approximately 1 meter
    @Transient private val epsilonLngPosition = 0.000009 // Approximately 1 meter at the equator

    // ---------------------------

    override suspend fun getWavePolygons(): WavePolygons? {
        if(!event.isRunning()) return null

        val elapsedTime = clock.now() - event.getWaveStartDateTime()
        if (elapsedTime <= 0.seconds) return null

        val composedLongitude = // Compose an earth-aware speed longitude with bands
            (cachedLongitude ?: EarthAdaptedSpeedLongitude(bbox(), speed, direction).also { cachedLongitude = it })
            .withProgression(elapsedTime)

        val traversedPolygons : MutableArea = mutableListOf()
        val remainingPolygons : MutableArea = mutableListOf()

        val areaPolygons = event.area.getPolygons()
        val (traversed, remaining) = splitAreaToWave(areaPolygons, composedLongitude)
        traversedPolygons.addAll(traversed)
        remainingPolygons.addAll(remaining)

        return if (traversedPolygons.isNotEmpty() || remainingPolygons.isNotEmpty()) WavePolygons(
            clock.now(),
            traversedPolygons,
            remainingPolygons
        ) else null
    }

    /**
     * Splits the area polygons along a composed longitude and categorizes them based on wave direction.
     *
     * The function considers the wave direction (EAST or WEST) to determine which side of the split
     * represents the traversed area and which represents the remaining area.
     */
    private fun splitAreaToWave(
        areaPolygons: Area,
        composedLongitude: ComposedLongitude
    ) : Pair<Area, Area> {
        if (areaPolygons.isEmpty()) return Pair(emptyList(), emptyList())
        val splitResults = areaPolygons.map { splitByLongitude(it, composedLongitude) }

        fun flattenNonEmptyPolygons(selector: (SplitResult) -> Area) =
            splitResults.mapNotNull { result -> selector(result).ifEmpty { null } }.flatten()

        val (traversed, remaining) = when (direction) {
            Direction.WEST -> Pair(SplitResult::right, SplitResult::left)
            Direction.EAST -> Pair(SplitResult::left, SplitResult::right)
        }

        return Pair(flattenNonEmptyPolygons(traversed), flattenNonEmptyPolygons(remaining))
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
    override suspend fun getWaveDuration(): Duration = cachedWaveDuration ?: run {
        val bbox = bbox()

        if (bbox.minLongitude == 0.0 && bbox.maxLongitude == 0.0) { // If map has not been loaded yet
            return event.wave.getApproxDuration()
        }

        val longestLat = bbox.latitudeOfWidestPart()
        val maxEastWestDistance = calculateDistance(bbox.minLongitude, bbox.maxLongitude, longestLat)
        val durationInSeconds = maxEastWestDistance / speed
        durationInSeconds.seconds.also { cachedWaveDuration = it }
    }

    // ---------------------------

    override suspend fun hasUserBeenHitInCurrentPosition(): Boolean {
        // Get the time when the user will be/was hit
        val hitTime = userHitDateTime() ?: return false

        // If hit time is in the past or present, the user has been hit
        // Otherwise, the user has not been hit yet
        return hitTime <= clock.now()
    }

    override suspend fun userHitDateTime(): Instant? {
        val userPosition = getUserPosition() ?: return null // FIXME / Check
        if (!event.area.isPositionWithin(userPosition)) return null

        // Check if we have a cached result for a nearby position
        if (cachedHitPosition != null && cachedHitDateTime != null) {
            val isCloseEnough =
                abs(cachedHitPosition!!.lat - userPosition.lat) < epsilonLatPosition &&
                        abs(cachedHitPosition!!.lng - userPosition.lng) < epsilonLngPosition

            // If the user hasn't moved significantly, return the cached result
            if (isCloseEnough) {
                return cachedHitDateTime
            }
        }

        val waveStartTime = event.getWaveStartDateTime()
        val bbox = bbox()

        // Calculate the distance from the wave starting position to the user position
        val distanceToUser = when (direction) {
            Direction.EAST -> calculateDistance(bbox.minLongitude, userPosition.lng, userPosition.lat)
            Direction.WEST -> calculateDistance(bbox.maxLongitude, userPosition.lng, userPosition.lat)
        }

        // Calculate the time it will take for the wave to reach the user from its START position
        val timeToReachUserInSeconds = distanceToUser / speed

        // Calculate the exact hit time by adding the time to reach to the wave START time
        val hitDateTime = waveStartTime + timeToReachUserInSeconds.seconds

        // Cache the result
        cachedHitPosition = userPosition
        cachedHitDateTime = hitDateTime

        return hitDateTime
    }

    override suspend fun closestWaveLongitude(latitude: Double): Double {
        val bbox = bbox()
        val maxEastWestDistance = calculateDistance(bbox.minLongitude, bbox.maxLongitude, latitude)
        val distanceTraveled = speed * (clock.now() - event.getWaveStartDateTime()).inWholeSeconds

        val longitudeDelta = (distanceTraveled / maxEastWestDistance) * (bbox.maxLongitude - bbox.minLongitude)
        return if (direction == Direction.WEST) {
            bbox.maxLongitude - longitudeDelta
        } else {
            bbox.minLongitude + longitudeDelta
        }
    }

    override suspend fun userPositionToWaveRatio(): Double? {
        val userPosition = getUserPosition() ?: return null // FIXME / Check

        if (!event.area.isPositionWithin(userPosition)) {
            return null
        }

        val waveBbox = event.area.bbox()
        val waveWidth = waveBbox.ne.lng - waveBbox.sw.lng
        val userLongitude = userPosition.lng

        val userOffsetFromStart: Double = when (direction) {
            Direction.EAST -> userLongitude - waveBbox.sw.lng
            Direction.WEST -> waveBbox.ne.lng - userLongitude
        }

        return if(userOffsetFromStart < 0) 0.0 else (userOffsetFromStart / waveWidth).coerceIn(0.0, 1.0)
    }

    // ---------------------------

    override fun validationErrors() : List<String>? {
        val superValid = super.validationErrors()
        val errors = superValid?.toMutableList() ?: mutableListOf()

        // No specific validation for linear waves

        return errors.takeIf { it.isNotEmpty() }?.map { "${WWWEventWaveLinear::class.simpleName}: $it" }
    }

}