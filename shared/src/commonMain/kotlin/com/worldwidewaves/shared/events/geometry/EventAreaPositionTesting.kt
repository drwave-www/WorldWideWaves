package com.worldwidewaves.shared.events.geometry

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

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.geometry.EventAreaGeometry.checkPositionInBoundingBox
import com.worldwidewaves.shared.events.geometry.PolygonOperations.isPointInPolygons
import com.worldwidewaves.shared.events.utils.Area
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import kotlin.math.abs
import kotlin.random.Random

/**
 * Event area position testing and random position generation.
 *
 * Handles:
 * - Position containment checking with caching
 * - Random position generation within area
 * - Epsilon-based position comparison
 */
object EventAreaPositionTesting {
    private const val POSITION_EPSILON = 0.0001 // Roughly 10 meters
    private const val MIN_SHRINK_FACTOR = 0.1
    private const val POSITION_ATTEMPTS_PER_SHRINK = 20
    private const val SHRINK_FACTOR_MULTIPLIER = 0.8

    /**
     * Checks if a given position is within the event area.
     *
     * This function first checks if the position is within the bounding box.
     * If it's not within the bounding box, returns false immediately.
     * If it is within the bounding box, it then checks if the position is within
     * the polygons using the ray-casting algorithm.
     */
    @Suppress("ReturnCount") // Early returns for guard clauses improve readability
    suspend fun isPositionWithin(
        position: Position,
        bbox: BoundingBox,
        polygons: Area,
        cachedPositionWithinResult: Pair<Position, Boolean>?,
    ): Pair<Boolean, Pair<Position, Boolean>?> {
        // Check if the cached result is within the epsilon
        val cachedResult = getCachedPositionResultIfValid(position, cachedPositionWithinResult)
        if (cachedResult != null) {
            return Pair(cachedResult, cachedPositionWithinResult)
        }

        // First, check if the position is within the bounding box (fast check)
        val isWithinBbox = checkPositionInBoundingBox(position, bbox)

        // If not within the bounding box, cache and return false immediately
        if (!isWithinBbox) {
            return Pair(false, Pair(position, false))
        }

        // If within bounding box, check if within polygon (more expensive check)
        val hasPolygons = polygons.isNotEmpty()

        if (!hasPolygons) {
            // Don't cache the result when polygons aren't loaded yet - this allows future checks to retry
            return Pair(false, null)
        }

        val result = isPointInPolygons(position, polygons)

        // Return result with new cached value
        return Pair(result, Pair(position, result))
    }

    /**
     * Returns cached position result if the position is within epsilon of cached position
     */
    private fun getCachedPositionResultIfValid(
        position: Position,
        cachedPositionWithinResult: Pair<Position, Boolean>?,
    ): Boolean? {
        val cached = cachedPositionWithinResult ?: return null
        val (cachedPosition, cachedResult) = cached

        return if (isPositionWithinEpsilon(position, cachedPosition)) {
            cachedResult
        } else {
            null
        }
    }

    private fun isPositionWithinEpsilon(
        pos1: Position,
        pos2: Position,
    ): Boolean = abs(pos1.lat - pos2.lat) < POSITION_EPSILON && abs(pos1.lng - pos2.lng) < POSITION_EPSILON

    /**
     * Generates a random position within the event area.
     * Makes multiple attempts to find a valid position within the area.
     * Falls back to the center of the area if no valid position is found.
     */
    suspend fun generateRandomPositionInArea(
        event: IWWWEvent,
        bbox: BoundingBox,
        center: Position,
    ): Position {
        val maxAttempts = 50
        var attempts = 0
        var shrinkFactor = 1.0

        while (attempts < maxAttempts && shrinkFactor > MIN_SHRINK_FACTOR) {
            val latRange = (bbox.ne.lat - bbox.sw.lat) * shrinkFactor
            val lngRange = (bbox.ne.lng - bbox.sw.lng) * shrinkFactor

            repeat(POSITION_ATTEMPTS_PER_SHRINK) {
                // Try multiple times with current shrink factor
                val randomLat = center.lat + (Random.nextDouble() - 0.5) * latRange
                val randomLng = center.lng + (Random.nextDouble() - 0.5) * lngRange
                val position = Position(randomLat, randomLng)

                if (event.area.isPositionWithin(position)) {
                    return position
                }
            }

            shrinkFactor *= SHRINK_FACTOR_MULTIPLIER // Shrink the sampling area
            attempts++
        }

        return center
    }
}
