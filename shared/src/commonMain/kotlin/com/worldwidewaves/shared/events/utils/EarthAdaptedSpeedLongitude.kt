package com.worldwidewaves.shared.events.utils

import androidx.annotation.VisibleForTesting
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_LINEAR_METERS_REFRESH
import com.worldwidewaves.shared.events.WWWEventWave.Direction
import com.worldwidewaves.shared.events.utils.GeoUtils.EARTH_RADIUS
import com.worldwidewaves.shared.events.utils.GeoUtils.EPSILON
import com.worldwidewaves.shared.events.utils.GeoUtils.MIN_PERCEPTIBLE_SPEED_DIFFERENCE
import com.worldwidewaves.shared.events.utils.GeoUtils.toDegrees
import com.worldwidewaves.shared.events.utils.GeoUtils.toRadians
import io.github.aakira.napier.Napier
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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

class EarthAdaptedSpeedLongitude(
    private val coveredArea : BoundingBox,
    private val speed : Double,
    private val direction: Direction
) : ComposedLongitude(Position(0.0, coveredArea.latitudeOfWidestPart())) {

    /*
     * Latitude-split bands and longitude band for the wave.
     */
    data class LatLonBand(val latitude: Double, val latWidth: Double, val lngWidth: Double)

    // ------------------------

    init {
        require(speed > 0) { "Speed must be greater than 0" }
    }

    // ------------------------

    /*
     * Security checks for the wave.
     */
    private val minBandWidth = 0.001
    private val maxBands = 20000

    /*
     * The duration of a single band refresh window in seconds.
     */
    private val bandStepDuration : Duration = (WAVE_LINEAR_METERS_REFRESH / speed).seconds

    /*
     * The starting longitude of the wave, based on the direction.
     */
    private val startLongitude = when (direction) {
        Direction.EAST -> coveredArea.minLongitude
        Direction.WEST -> coveredArea.maxLongitude
    }

    /*
     * Cached latitude bands for the wave.
     */
    private var cachedBands: Map<Double, LatLonBand>? = null

    // ------------------------

    private fun initialized() : EarthAdaptedSpeedLongitude {
        if (size() == 1) {
            val referenceLng = first().lng
            clear().also {
                bands().forEach { (lat, _) -> add(Position(lat, referenceLng)) }
            }
        }
        return this
    }

    // ------------------------

    /**
     * Calculates the progression of the wave front after a given elapsed time.
     *
     * This function determines the new positions of the wave front across different latitude bands,
     * taking into account the elapsed time, wave speed, and direction of propagation.
     *
     * Algorithm:
     * 1. If the elapsed time is zero or negative, return the initial state.
     * 2. Calculate the number of complete refresh windows that have passed.
     * 3. For each latitude band:
     *    a. Calculate the longitudinal change based on the number of windows and the band's longitude width.
     *    b. Determine the new longitude based on the direction of propagation.
     *    c. Create a new Position at the center of the latitude band with the calculated longitude.
     * 4. Construct and return a new ComposedLongitude from these calculated positions.
     *
     * Note:
     * - The returned ComposedLongitude represents the wave front as a series of points, each at the
     *   center of its respective latitude band.
     *
     */
    fun withProgression(elapsedTime: Duration): ComposedLongitude {
        require(elapsedTime >= Duration.ZERO) { "Elapsed time must be non-negative" }
        if (elapsedTime <= Duration.ZERO) return initialized()

        // Calculate the total distance covered based on speed and elapsed time
        val totalDistanceCovered = speed * elapsedTime.inWholeSeconds

        val bands = bands()
        return fromPositions(bands.map { (bandLatitude, band) ->
            // Convert the total distance to longitude change at the current latitude
            val longitudeChange = (totalDistanceCovered / (EARTH_RADIUS * cos(bandLatitude.toRadians()))).toDegrees()

            val newLatitude = min(90.0, max(-90.0, bandLatitude + band.latWidth / 2))
            Position(newLatitude, when (direction) {
                Direction.EAST -> min(startLongitude + longitudeChange, coveredArea.maxLongitude)
                Direction.WEST -> max(startLongitude - longitudeChange, coveredArea.minLongitude)
            })
        })
    }


    // ------------------------

    @VisibleForTesting
    fun bands(): Map<Double, LatLonBand> {
        if (cachedBands == null) {
            val bands = calculateWaveBands().associateBy { it.latitude }
            require(bands.isNotEmpty()) { "Bands must not be empty" }
            cachedBands = bands
        }
        return cachedBands ?: error("Bands must not be null")
    }

    // ========================================================================

    /**
     * Calculates the latitude and longitude bands for a given bounding box based on the speed.
     *
     * @return A list of LatLonBand objects, each representing a latitude band with its
     *         corresponding latitude and longitude widths.
     *
     * Algorithm:
     * - First, it calculates the middle latitude of the bounding box (average of minLat and maxLat).
     * - Then, it calculates the longitude band width at this middle latitude using the speed.
     * - For each latitude in the bounding box, the algorithm calculates the optimal latitude band
     *   width and adjusts the longitude width.
     * - This ensures that the perceived wave speed is uniform across different latitudes by
     *   adjusting the band widths accordingly.
     */
    @VisibleForTesting
     fun calculateWaveBands(): List<LatLonBand> {
        val (sw, ne) = coveredArea
        val latLonBands = mutableListOf<LatLonBand>()

        // Calculate the latitude within of the bounding box that is closest to the equator
        val longestLat = coveredArea.latitudeOfWidestPart()
        Napier.v { "Latitude of the widest part of the bounding box: $longestLat" }

        // Calculate the longitude band width at the longest latitude
        val lonBandWidthAtLongest = calculateLonBandWidthAtLatitude(longestLat)
        Napier.v { "Longitude band width at the middle latitude: $lonBandWidthAtLongest" }

        latLonBands.add(LatLonBand(-89.9, 0.0, // Lower latitude band
            adjustLongitudeWidthAtLatitude(-89.9, lonBandWidthAtLongest))
        )

        var currentLat = sw.lat
        while (currentLat < ne.lat - EPSILON && latLonBands.size < maxBands) {
            // Calculate the optimal latitude band width at the current latitude
            var optimalLatBandWidth = calculateOptimalLatBandWidth(currentLat, lonBandWidthAtLongest)

            // Ensure the band width is not smaller than the minimum
            optimalLatBandWidth = maxOf(optimalLatBandWidth, minBandWidth)

            // Calculate the adjusted longitude width at the current latitude
            val adjustedLonWidth = adjustLongitudeWidthAtLatitude(currentLat, lonBandWidthAtLongest)

            // Ensure longitude stays within the bbox bounds
            val actualLonWidth = min(adjustedLonWidth, ne.lng - sw.lng)

            // Add the latitude and longitude band for the current latitude
            latLonBands.add(LatLonBand(currentLat, optimalLatBandWidth, actualLonWidth))

            // Move to the next latitude band
            currentLat += optimalLatBandWidth
        }

        latLonBands.add(LatLonBand(89.9, 0.0, // Higher latitude band
            adjustLongitudeWidthAtLatitude(89.9, lonBandWidthAtLongest))
        )

        return latLonBands
    }

    // ---------------------------

    /**
     * Calculates the longitude band width traversed every bandStepDuration at the middle latitude
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
    fun calculateLonBandWidthAtLatitude(latitude: Double): Double {
        require(speed > 0) { "Speed must be greater than 0" }
        val distanceCovered = speed * bandStepDuration.inWholeMilliseconds / 1000
        Napier.v { "Distance covered by the wave in ${bandStepDuration}s at speed speed: $distanceCovered" }
        return (distanceCovered / (EARTH_RADIUS * cos(latitude.toRadians()))).toDegrees()
        // return (speed / (EARTH_RADIUS * cos(latitude.toRadians()))).toDegrees()
    }

    // ---------------------------

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
        val lonDistanceAtThisLat = (EARTH_RADIUS * cos(latitudeInRadians) * lonBandWidthAtEquator).toDegrees()

        // Find how many degrees of latitude are required for a perceptible difference in longitude width
        val perceptibleLatDifference = MIN_PERCEPTIBLE_SPEED_DIFFERENCE / lonDistanceAtThisLat

        // Return the latitude band width in degrees
        return perceptibleLatDifference
    }

    // ---------------------------

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
    fun adjustLongitudeWidthAtLatitude(latitude: Double, lonWidthAtTheLongest: Double) : Double {
        require(abs(latitude) < 90) // Prevent division by zero
        return lonWidthAtTheLongest / cos(latitude.toRadians())
    }

}