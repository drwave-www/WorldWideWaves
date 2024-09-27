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

import com.worldwidewaves.shared.events.utils.Area
import com.worldwidewaves.shared.events.utils.ComposedLongitude
import com.worldwidewaves.shared.events.utils.EarthAdaptedSpeedLongitude
import com.worldwidewaves.shared.events.utils.GeoUtils.calculateDistance
import com.worldwidewaves.shared.events.utils.MutableArea
import com.worldwidewaves.shared.events.utils.PolygonUtils.PolygonSplitResult
import com.worldwidewaves.shared.events.utils.PolygonUtils.recomposeCutPolygons
import com.worldwidewaves.shared.events.utils.PolygonUtils.splitByLongitude
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
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

    @Transient private var cachedLongitude: EarthAdaptedSpeedLongitude? = null
    @Transient private var cachedTotalTime: Duration? = null

    // ---------------------------

    override suspend fun getWavePolygons(lastWaveState: WavePolygons?, mode: WaveMode): WavePolygons? {
        require(event.isRunning()) { "Event must be running to request teh wave polygons" }
        require(lastWaveState == null || lastWaveState.timestamp <= clock.now()) { "Last wave state must be in the past" }

        val composedLongitude = // Compose an earth-aware speed longitude with bands for the wave
            (cachedLongitude ?: EarthAdaptedSpeedLongitude(bbox(), speed, direction).also { cachedLongitude = it })
            .withProgression(clock.now() - event.getStartDateTime())

        val areaPolygons = event.area.getPolygons()
        val traversedPolygons : MutableArea = mutableListOf()
        val remainingPolygons : MutableArea = mutableListOf()
        val addedTraversedPolygons : MutableArea = mutableListOf()

        if (lastWaveState == null) {
            val (traversed, remaining) = splitAreaToWave(areaPolygons, composedLongitude)
            traversedPolygons.addAll(traversed)
            remainingPolygons.addAll(remaining)
        } else {
            val (newTraversed, remaining) = splitAreaToWave(lastWaveState.remainingPolygons, composedLongitude)
            when(mode) {
                WaveMode.ADD -> { // Add new traversed polygons without reconstruction
                    remainingPolygons.addAll(remaining)
                    traversedPolygons.addAll(lastWaveState.traversedPolygons)
                    traversedPolygons.addAll(newTraversed)
                    addedTraversedPolygons.addAll(newTraversed)
                }
                WaveMode.RECOMPOSE -> { // Recompose the remaining polygons on CutPositions
                    remainingPolygons.addAll(remaining)
                    traversedPolygons.addAll(
                        recomposeCutPolygons(
                            lastWaveState.traversedPolygons +  newTraversed
                        )
                    )
                }
            }
        }

        return if (traversedPolygons.isNotEmpty() || remainingPolygons.isNotEmpty()) WavePolygons(
            clock.now(),
            traversedPolygons,
            remainingPolygons,
            addedTraversedPolygons.ifEmpty { null }
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
        val splitResults = areaPolygons.map { it.splitByLongitude(composedLongitude) }

        fun flattenNonEmptyPolygons(selector: (PolygonSplitResult) -> Area) =
            splitResults.mapNotNull { result -> selector(result).ifEmpty { null } }.flatten()

        val (traversed, remaining) = when (direction) {
            Direction.WEST -> Pair(PolygonSplitResult::right, PolygonSplitResult::left)
            Direction.EAST -> Pair(PolygonSplitResult::left, PolygonSplitResult::right)
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

    override fun validationErrors() : List<String>? {
        val superValid = super.validationErrors()
        val errors = superValid?.toMutableList() ?: mutableListOf()

        // No specific validation for linear waves

        return errors.takeIf { it.isNotEmpty() }?.map { "${WWWEventWaveLinear::class.simpleName}: $it" }
    }

}