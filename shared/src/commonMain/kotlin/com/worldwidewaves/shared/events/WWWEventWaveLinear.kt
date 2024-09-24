package com.worldwidewaves.shared.events

/*
 * Copyright 2024 DrWave
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

import androidx.annotation.VisibleForTesting
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_LINEAR_METERS_REFRESH
import com.worldwidewaves.shared.events.utils.ComposedLongitude
import com.worldwidewaves.shared.events.utils.GeoUtils.EARTH_RADIUS
import com.worldwidewaves.shared.events.utils.GeoUtils.MIN_PERCEPTIBLE_DIFFERENCE
import com.worldwidewaves.shared.events.utils.GeoUtils.calculateDistance
import com.worldwidewaves.shared.events.utils.GeoUtils.normalizeLongitude
import com.worldwidewaves.shared.events.utils.GeoUtils.toRadians
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.PolygonUtils.PolygonSplitResult
import com.worldwidewaves.shared.events.utils.PolygonUtils.recomposeCutPolygons
import com.worldwidewaves.shared.events.utils.PolygonUtils.splitByLongitude
import com.worldwidewaves.shared.events.utils.Position
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// ---------------------------

@Serializable
data class WWWEventWaveLinear(
    override val speed: Double,
    override val direction: Direction,
    override val warming: WWWEventWaveWarming
) : KoinComponent, WWWEventWave() {

    data class LatLonBand(val latitude: Double, val latWidth: Double, val lngWidth: Double)

    @Transient private var cachedBands: Map<Double, LatLonBand>? = null
    @Transient private var cachedTotalTime: Duration? = null

    // ---------------------------

    private suspend fun bands(): Map<Double, LatLonBand> {
        if (cachedBands == null) {
            val refreshDuration = WAVE_LINEAR_METERS_REFRESH / speed
            val bands = calculateWaveBands(refreshDuration.seconds).associateBy { it.latitude }
            require(bands.isNotEmpty()) { "Bands must not be empty" }
            cachedBands = bands
        }
        return cachedBands!!
    }

    // ---------------------------

    override suspend fun getWavePolygons(lastWaveState: WavePolygons?, mode: WaveMode): WavePolygons {
        require(event.isRunning()) { "Event must be running to request teh wave polygons" }
        // TODO: - get the longitude diff since last one
        //       - decide of the polygon needs to be completed or not
        //       - create the polygon from scratch or from the previous one
        //           --> first split then replace the longitude line by the one coming from bands if more than one
        //           --> do it for each polygon, deciding if the bands stuff must be done for each one or not
        val bbox = bbox()
        val areaPolygons = event.area.getPolygons()
        val referenceLongitude = currentWaveLongitude(bbox.latitudeOfWidestPart())
        val composedLongitude = currentComposedLongitude(referenceLongitude, lastWaveState?.referenceLongitude)

        // FIXME: Decide if we change something depending on elapsed time or not (store in WavePolygons)

        val traversedPolygons : MutableList<Polygon> = mutableListOf()
        val remainingPolygons : MutableList<Polygon> = mutableListOf()
        val addedTraversedPolygons : MutableList<Polygon> = mutableListOf()

        if (lastWaveState == null) {
            val (traversed, remaining) = splitAreaToWave(areaPolygons, composedLongitude)
            traversedPolygons.addAll(traversed)
            remainingPolygons.addAll(remaining)
        } else {
            val (newTraversed, remaining) = splitAreaToWave(lastWaveState.remainingPolygons, composedLongitude)
            when(mode) {
                WaveMode.ADD -> { // Add
                    remainingPolygons.addAll(remaining)
                    traversedPolygons.addAll(lastWaveState.traversedPolygons)
                    traversedPolygons.addAll(newTraversed)
                    addedTraversedPolygons.addAll(newTraversed)
                }
                WaveMode.RECOMPOSE -> {
                    remainingPolygons.addAll(remaining)
                    traversedPolygons.addAll(
                        recomposeCutPolygons(
                            lastWaveState.traversedPolygons +  newTraversed
                        )
                    )
                }
            }
        }

        return WavePolygons(
            clock.now(),
            referenceLongitude,
            traversedPolygons,
            remainingPolygons,
            addedTraversedPolygons.ifEmpty { null }
        )
    }

    /**
     * Splits the area polygons along a composed longitude and categorizes them based on wave direction.
     *
     * The function considers the wave direction (EAST or WEST) to determine which side of the split
     * represents the traversed area and which represents the remaining area.
     */
    private fun splitAreaToWave(
        areaPolygons: List<Polygon>,
        composedLongitude: ComposedLongitude
    ) : Pair<List<Polygon>, List<Polygon>> {
        val splitResults = areaPolygons.map { it.splitByLongitude(composedLongitude) }

        fun flattenNonEmptyPolygons(selector: (PolygonSplitResult) -> List<Polygon>) =
            splitResults.mapNotNull { result ->
                val polygons = selector(result)
                polygons.ifEmpty { null }
            }.flatten()

        val (traversed, remaining) = when (direction) {
            Direction.WEST -> Pair(PolygonSplitResult::right, PolygonSplitResult::left)
            Direction.EAST -> Pair(PolygonSplitResult::left, PolygonSplitResult::right)
        }

        return Pair(flattenNonEmptyPolygons(traversed), flattenNonEmptyPolygons(remaining))
    }

    /**
     * Calculates the current composed longitude positions for the wave.
     *
     * This function computes a list of positions representing the current wave's longitude
     * at various latitude bands within the bounding box. It divides the bounding box into
     * latitude bands and calculates the longitude for each band based on the current wave
     * position.
     *
     */
    private suspend fun currentComposedLongitude(
        currentReferenceLongitude: Double,
        previousReferenceLongitude: Double?
    ): ComposedLongitude {
        val bbox = bbox()
        val bands = bands()
        val latitudeOfReference = bbox.latitudeOfWidestPart()

        // FIXME: use bands delta instead of currentWaveLongitude in the loops below

        val bandHeight = bbox.height / bands.size
        val startLat = bbox.sw.lat

        return ComposedLongitude.fromPositions(List(bands.size) { index ->
            val bandLat = startLat + (index + 0.5) * bandHeight
            val bandLng = currentWaveLongitude(bandLat)
            Position(bandLat, bandLng)
        })
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
    override suspend fun getWaveDuration(): Duration = cachedTotalTime ?: run {
        val bbox = bbox()
        val longestLat = bbox.latitudeOfWidestPart()
        val maxEastWestDistance = calculateDistance(bbox.minLongitude, bbox.maxLongitude, longestLat)
        val durationInSeconds = maxEastWestDistance / speed
        durationInSeconds.toDuration(DurationUnit.SECONDS)
            .also { cachedTotalTime = it }
    }

    // ---------------------------

    override suspend fun hasUserBeenHitInCurrentPosition(): Boolean {
        val userPosition = getUserPosition() ?: return false
        val bbox = bbox()
        val waveCurrentLongitude = currentWaveLongitude(userPosition.lat)
        return if (userPosition.lng in bbox.minLongitude..waveCurrentLongitude)
            event.area.isPositionWithin(userPosition) // Take now into consideration the terrain
        else false
    }

    override suspend fun timeBeforeHit(): Duration? {
        val userPosition = getUserPosition() ?: return null
        if (!event.area.isPositionWithin(userPosition)) return null

        val waveCurrentLongitude = currentWaveLongitude(userPosition.lat)
        val distanceToUser = calculateDistance(waveCurrentLongitude, userPosition.lng, userPosition.lat)

        val timeInSeconds = distanceToUser / speed
        return timeInSeconds.seconds
    }

    suspend fun currentWaveLongitude(latitude: Double): Double {
        val bbox = bbox()
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

    /**
     * Calculates the latitude and longitude bands for a given bounding box based on the speed S.
     *
     * @return A list of LatLonBand objects, each representing a latitude band with its
     *         corresponding latitude and longitude widths.
     *
     * Algorithm:
     * - First, it calculates the middle latitude of the bounding box (average of minLat and maxLat).
     * - Then, it calculates the longitude band width at this middle latitude using the speed S.
     * - For each latitude in the bounding box, the algorithm calculates the optimal latitude band
     *   width and adjusts the longitude width.
     * - This ensures that the perceived wave speed is uniform across different latitudes by
     *   adjusting the band widths accordingly.
     */
    suspend fun calculateWaveBands(refreshDuration : Duration = 1.seconds): List<LatLonBand> {
        val bbox = bbox()
        val (sw, ne) = bbox
        val latLonBands = mutableListOf<LatLonBand>()

        // Calculate the latitude within of the bounding box that is closest to the equator
        val longestLat = bbox.latitudeOfWidestPart()

        // Calculate the longitude band width at the middle latitude
        val lonBandWidthAtMiddle = calculateLonBandWidthAtMiddleLatitude(longestLat, refreshDuration)

        // Security: Set a minimum and maximum band width to prevent excessive band creation
        val minBandWidth = 0.001
        val maxBands = 20000

        var currentLat = sw.lat
        while (currentLat < ne.lat && latLonBands.size < maxBands) {
            // Calculate the optimal latitude band width at the current latitude
            var optimalLatBandWidth = calculateOptimalLatBandWidth(currentLat, lonBandWidthAtMiddle)

            // Ensure the band width is not smaller than the minimum
            optimalLatBandWidth = maxOf(optimalLatBandWidth, minBandWidth)

            // Calculate the adjusted longitude width at the current latitude
            val adjustedLonWidth = adjustLongitudeWidthAtLatitude(currentLat, lonBandWidthAtMiddle)

            // Ensure longitude stays within the bbox bounds, handling the -180/180 wrap-around
            val actualLonWidth = min(adjustedLonWidth, normalizeLongitude(ne.lng) - normalizeLongitude(sw.lng))

            // Add the latitude and longitude band for the current latitude
            latLonBands.add(LatLonBand(currentLat, optimalLatBandWidth, actualLonWidth))

            // Move to the next latitude band
            currentLat += optimalLatBandWidth
        }

        return latLonBands
    }

    /**
     * Calculates the longitude band width traversed every second at the middle latitude
     * based on a given speed.
     *
     * Algorithm:
     * - First, it converts the speed into the angular distance covered at a given latitude.
     * - The formula is: lonBandWidth = (S / (R * cos(latitude))) * (180 / π)
     * - Where R is the Earth's radius (approximately 6,371,000 meters), and cos(latitude) adjusts
     *   for the shrinking distance between meridians at higher latitudes.
     * - The result is the longitude band width at that specific latitude.
     */
    @VisibleForTesting
    fun calculateLonBandWidthAtMiddleLatitude(latitude: Double, refreshDuration: Duration = 1.seconds): Double {
        require(speed > 0) { "Speed must be greater than 0" }
        val distanceCovered = speed * refreshDuration.inWholeSeconds
        return (distanceCovered / (EARTH_RADIUS * cos(latitude.toRadians()))) * (180 / PI) // Convert radians to degrees
    }

    /**
     * Adjusts the longitude width at a given latitude to account for shrinking longitudinal distances.
     *
     * Algorithm:
     * - Adjusts the longitude width based on the cosine of the latitude. As you move away from the
     *   equator, the longitudinal distance decreases, and this adjustment ensures the wave speed
     *   is perceived the same at different latitudes.
     * - The formula is: adjustedLonWidth = lonWidth / cos(latitude)
     */
    @VisibleForTesting
    fun adjustLongitudeWidthAtLatitude(latitude: Double, lonWidth: Double) =
        lonWidth / cos(latitude.toRadians())

    /**
     * Calculates the optimal latitude band width based on the longitude band width
     *
     * Algorithm:
     * - The latitude band width is inversely proportional to the cosine of the latitude.
     * - This ensures that the wave speed is perceived uniformly across different latitudes.
     * - The formula is: latBandWidth = lonBandWidthAtEquator / cos(latitude)
     */
    @VisibleForTesting
    fun calculateOptimalLatBandWidth(latitude: Double, lonBandWidthAtEquator: Double): Double {
        // Convert latitude to radians
        val latitudeInRadians = latitude.toRadians()

        // Calculate the longitudinal distance at this latitude
        val lonDistanceAtThisLat = EARTH_RADIUS * cos(latitudeInRadians) * lonBandWidthAtEquator * (PI / 180)

        // Find how many degrees of latitude are required for a perceptible difference in longitude width
        val perceptibleLatDifference = MIN_PERCEPTIBLE_DIFFERENCE / lonDistanceAtThisLat

        // Return the latitude band width in degrees
        return perceptibleLatDifference
    }

    // ---------------------------

    override fun validationErrors() : List<String>? {
        val superValid = super.validationErrors()
        val errors = superValid?.toMutableList() ?: mutableListOf()

        // No specific validation for linear waves

        return errors.takeIf { it.isNotEmpty() }?.map { "${WWWEventWaveLinear::class.simpleName}: $it" }
    }

}