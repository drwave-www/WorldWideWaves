package com.worldwidewaves.shared.events

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
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

import com.worldwidewaves.shared.events.geometry.PolygonTransformations.SplitResult
import com.worldwidewaves.shared.events.geometry.PolygonTransformations.splitByLongitude
import com.worldwidewaves.shared.events.utils.Area
import com.worldwidewaves.shared.events.utils.ComposedLongitude
import com.worldwidewaves.shared.events.utils.EarthAdaptedSpeedLongitude
import com.worldwidewaves.shared.events.utils.GeoUtils.calculateDistance
import com.worldwidewaves.shared.events.utils.MutableArea
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
    override val approxDuration: Int,
) : WWWEventWave(),
    KoinComponent {
    @Transient private var cachedLongitude: EarthAdaptedSpeedLongitude? = null

    @Transient private var cachedWaveDuration: Duration? = null

    @Transient private var cachedHitDateTime: Instant? = null

    @Transient private var cachedHitPosition: Position? = null

    @Transient private val epsilonLatPosition = 0.000009

    // Approximately 1 meter
    @Transient private val epsilonLngPosition = 0.000009 // Approximately 1 meter at the equator

    // ---------------------------

    @Suppress("ReturnCount") // Early returns for guard clauses improve readability
    override suspend fun getWavePolygons(): WavePolygons? {
        val isRunning = event.isRunning()
        if (!isRunning) {
            return null
        }

        val elapsedTime = clock.now() - event.getWaveStartDateTime()
        val hasElapsedTime = elapsedTime > 0.seconds
        if (!hasElapsedTime) {
            return null
        }

        val composedLongitude = // Compose an earth-aware speed longitude with bands
            (cachedLongitude ?: EarthAdaptedSpeedLongitude(bbox(), speed, direction).also { cachedLongitude = it })
                .withProgression(elapsedTime)

        val traversedPolygons: MutableArea = mutableListOf()
        val remainingPolygons: MutableArea = mutableListOf()

        val areaPolygons = event.area.getPolygons()
        val (traversed, remaining) = splitAreaToWave(areaPolygons, composedLongitude)
        traversedPolygons.addAll(traversed)
        remainingPolygons.addAll(remaining)

        val hasPolygons = traversedPolygons.isNotEmpty() || remainingPolygons.isNotEmpty()
        return if (hasPolygons) {
            WavePolygons(
                clock.now(),
                traversedPolygons,
                remainingPolygons,
            )
        } else {
            null
        }
    }

    /**
     * Splits the area polygons along a composed longitude and categorizes them based on wave direction.
     *
     * The function considers the wave direction (EAST or WEST) to determine which side of the split
     * represents the traversed area and which represents the remaining area.
     */
    private fun splitAreaToWave(
        areaPolygons: Area,
        composedLongitude: ComposedLongitude,
    ): Pair<Area, Area> {
        if (areaPolygons.isEmpty()) return Pair(emptyList(), emptyList())
        val splitResults = areaPolygons.map { splitByLongitude(it, composedLongitude) }

        fun flattenNonEmptyPolygons(selector: (SplitResult) -> Area) =
            splitResults.mapNotNull { result -> selector(result).ifEmpty { null } }.flatten()

        val (traversed, remaining) =
            when (direction) {
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
    override suspend fun getWaveDuration(): Duration =
        cachedWaveDuration ?: run {
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

    @Suppress("ReturnCount") // Early returns for guard clauses improve readability
    override suspend fun userHitDateTime(): Instant? {
        val userPosition = getUserPosition()
        if (userPosition == null) {
            return null
        }

        val isPositionWithinArea = event.area.isPositionWithin(userPosition)
        if (!isPositionWithinArea) {
            return null
        }

        // Check if we have a cached result for a nearby position
        val cachedResult = getCachedHitDateTimeIfValid(userPosition)
        if (cachedResult != null) {
            return cachedResult
        }

        val waveStartTime = event.getWaveStartDateTime()
        val bbox = bbox()

        // Calculate the distance from the wave starting position to the user position
        val distanceToUser =
            when (direction) {
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

    /**
     * Returns cached hit datetime if the user hasn't moved significantly
     */
    private fun getCachedHitDateTimeIfValid(userPosition: Position): Instant? {
        val cached = cachedHitPosition
        val hitTime = cachedHitDateTime
        if (cached == null || hitTime == null) {
            return null
        }

        val isCloseEnough =
            abs(cached.lat - userPosition.lat) < epsilonLatPosition &&
                abs(cached.lng - userPosition.lng) < epsilonLngPosition

        return if (isCloseEnough) hitTime else null
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

    /**
     * Information about the wave front edge positions.
     *
     * @property averageLatitude The average latitude of all positions on the wave front edge
     * @property minLatitude The minimum (southernmost) latitude of the wave front edge
     * @property maxLatitude The maximum (northernmost) latitude of the wave front edge
     */
    private data class WaveFrontEdgeInfo(
        val averageLatitude: Double,
        val minLatitude: Double,
        val maxLatitude: Double,
    )

    /**
     * Calculates information about the wave front edge positions.
     *
     * The wave front edge is defined as the leading edge of the traversed polygons,
     * which is the easternmost edge for waves moving EAST or the westernmost edge
     * for waves moving WEST.
     *
     * @return Edge information including average, min, and max latitude, or null if no wave polygons exist
     */
    @Suppress("TooGenericExceptionCaught") // Defensive: catch any polygon calculation errors
    private suspend fun getWaveFrontEdgeInfo(): WaveFrontEdgeInfo? {
        return try {
            val wavePolygons = getWavePolygons() ?: return null
            val traversedPolygons = wavePolygons.traversedPolygons

            if (traversedPolygons.isEmpty()) {
                return null
            }

            // Collect all positions from all polygons
            val allPositions = traversedPolygons.flatMap { polygon -> polygon.toList() }

            if (allPositions.isEmpty()) {
                return null
            }

            // Find the leading edge longitude based on direction
            val leadingEdgeLongitude =
                when (direction) {
                    Direction.EAST -> allPositions.maxOf { it.lng } // Easternmost
                    Direction.WEST -> allPositions.minOf { it.lng } // Westernmost
                }

            // Define epsilon for "close enough" to the edge (approximately 11 meters at equator)
            val epsilon = 0.0001

            // Filter positions on or near the leading edge
            val edgePositions =
                allPositions.filter { position ->
                    abs(position.lng - leadingEdgeLongitude) < epsilon
                }

            if (edgePositions.isEmpty()) {
                return null
            }

            // Calculate statistics of edge positions
            val latitudes = edgePositions.map { it.lat }
            WaveFrontEdgeInfo(
                averageLatitude = latitudes.average(),
                minLatitude = latitudes.minOrNull() ?: return null,
                maxLatitude = latitudes.maxOrNull() ?: return null,
            )
        } catch (e: Exception) {
            // If any error occurs (e.g., mock not set up in tests), fall back to bbox center
            null
        }
    }

    override suspend fun getWaveFrontCenterPosition(): Position? {
        // Try to get the actual wave front edge information
        val waveFrontInfo = getWaveFrontEdgeInfo()
        val userPosition = getUserPosition()

        // Determine which latitude to use based on user position and wave edge bounds
        val centerLatitude =
            if (userPosition != null && waveFrontInfo != null) {
                // If user is vertically within the wave edge bounds, use user's latitude
                if (userPosition.lat >= waveFrontInfo.minLatitude && userPosition.lat <= waveFrontInfo.maxLatitude) {
                    userPosition.lat
                } else {
                    // User is outside wave edge bounds, use wave front average
                    waveFrontInfo.averageLatitude
                }
            } else {
                // No user position or no wave front info, use wave front average or bbox center
                waveFrontInfo?.averageLatitude ?: run {
                    val bbox = bbox()
                    (bbox.ne.lat + bbox.sw.lat) / 2
                }
            }

        val waveFrontLongitude = closestWaveLongitude(centerLatitude)
        return Position(centerLatitude, waveFrontLongitude)
    }

    @Suppress("ReturnCount") // Early returns for guard clauses improve readability
    override suspend fun userPositionToWaveRatio(): Double? {
        val userPosition = getUserPosition()
        if (userPosition == null) {
            return null
        }

        val isPositionWithinArea = event.area.isPositionWithin(userPosition)
        if (!isPositionWithinArea) {
            return null
        }

        val waveBbox = event.area.bbox()
        val waveWidth = waveBbox.ne.lng - waveBbox.sw.lng
        val userLongitude = userPosition.lng

        val userOffsetFromStart: Double =
            when (direction) {
                Direction.EAST -> userLongitude - waveBbox.sw.lng
                Direction.WEST -> waveBbox.ne.lng - userLongitude
            }

        return if (userOffsetFromStart < 0) 0.0 else (userOffsetFromStart / waveWidth).coerceIn(0.0, 1.0)
    }

    // ---------------------------

    override fun validationErrors(): List<String>? {
        val superValid = super.validationErrors()
        val errors = superValid?.toMutableList() ?: mutableListOf()

        // No specific validation for linear waves

        return errors.takeIf { it.isNotEmpty() }?.map { "${WWWEventWaveLinear::class.simpleName}: $it" }
    }

    // ---------------------------

    /**
     * Clears polygon-dependent cached values.
     *
     * Called when polygons are reloaded (e.g., after map download) to ensure
     * wave calculations use fresh bounding box data. Position-dependent cache
     * (cachedHitPosition) is NOT cleared as it's invalidated separately when
     * user position changes significantly.
     */
    override fun clearDurationCache() {
        cachedWaveDuration = null
        cachedHitDateTime = null
        cachedLongitude = null
        // DON'T clear cachedHitPosition - it's position-dependent, not polygon-dependent
    }
}
