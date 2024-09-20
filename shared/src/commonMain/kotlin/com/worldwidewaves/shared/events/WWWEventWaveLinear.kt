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
import com.worldwidewaves.shared.events.WWWEventWaveWarming.Type.LONGITUDE_CUT
import com.worldwidewaves.shared.events.utils.GeoUtils.EARTH_RADIUS
import com.worldwidewaves.shared.events.utils.GeoUtils.MIN_PERCEPTIBLE_DIFFERENCE
import com.worldwidewaves.shared.events.utils.GeoUtils.calculateDistance
import com.worldwidewaves.shared.events.utils.GeoUtils.normalizeLongitude
import com.worldwidewaves.shared.events.utils.GeoUtils.toDegrees
import com.worldwidewaves.shared.events.utils.GeoUtils.toRadians
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.PolygonUtils.splitByLongitude
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

    // Data class to hold latitude and longitude band information
    data class LatLonBand(val latitude: Double, val latWidth: Double, val lngWidth: Double)

    @Transient private var cachedBands: Map<Double, LatLonBand>? = null
    @Transient private var cachedTotalTime: Duration? = null
    @Transient private var cachedWarmingPolygons: List<Polygon>? = null
    @Transient private var cachedWavePolygons: List<Polygon>? = null

    // ---------------------------

    suspend fun bands(): Map<Double, LatLonBand> {
        if (cachedBands == null) {
            val refreshDuration = WAVE_LINEAR_METERS_REFRESH / speed
            cachedBands = calculateWaveBands(refreshDuration.seconds).associateBy { it.latitude }
        }
        return cachedBands!!
    }

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
                    polygon.splitByLongitude(warming.longitude!!).right
                }
                else -> emptyList()
            }
        }
        return cachedWarmingPolygons!!
    }

    // ---------------------------

    override suspend fun getWavePolygons(): List<Polygon> {
        if (cachedWavePolygons == null) {
            cachedWavePolygons = null
        }
        return cachedWavePolygons!!
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
            val bbox = getBbox()
            val longestLat = longestLatitudeToTraverse()
            val maxEastWestDistance = calculateDistance(bbox.minLongitude, bbox.maxLongitude, longestLat)
            val durationInSeconds = maxEastWestDistance / speed
            durationInSeconds.toDuration(DurationUnit.SECONDS)
                .also { cachedTotalTime = it }
        }
    }

    // ---------------------------

    override suspend fun hasUserBeenHitInCurrentPosition(): Boolean {
        val userPosition = getUserPosition() ?: return false
        val bbox = getBbox()
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
        val bbox = getBbox()
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
        val (sw, ne) = getBbox()
        val latLonBands = mutableListOf<LatLonBand>()

        // Calculate the latitude within of the bounding box that is closest to the equator
        val longestLat = longestLatitudeToTraverse()

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
     * - First, it converts the speed into the angular distance covered at the middle latitude.
     * - The formula is: lonBandWidth = (S / (R * cos(latitude))) * (180 / π)
     * - Where R is the Earth's radius (approximately 6,371,000 meters), and cos(latitude) adjusts
     *   for the shrinking distance between meridians at higher latitudes.
     * - The result is the longitude band width at that specific latitude.
     */
    @VisibleForTesting
    fun calculateLonBandWidthAtMiddleLatitude(middleLatitude: Double, refreshDuration: Duration = 1.seconds): Double {
        require(speed > 0) { "Speed must be greater than 0" }
        val distanceCovered = speed * refreshDuration.inWholeSeconds
        return (distanceCovered / (EARTH_RADIUS * cos(middleLatitude.toRadians()))) * (180 / PI) // Convert radians to degrees
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
     * Calculates the optimal latitude band width based on the longitude band width at the equator
     * or middle latitude.
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

    /*
     * Calculate the latitude within of the bounding box that is closest to the equator
     */
    suspend fun longestLatitudeToTraverse() : Double {
        val (sw, ne) = getBbox()
        return 0.0.coerceIn(sw.lat.toRadians(), ne.lat.toRadians()).toDegrees()
    }

    // ---------------------------

    override fun validationErrors() : List<String>? {
        val superValid = super.validationErrors()
        val errors = superValid?.toMutableList() ?: mutableListOf()

        // TODO

        return errors.takeIf { it.isNotEmpty() }?.map { "${WWWEventWaveLinear::class.simpleName}: $it" }
    }

}