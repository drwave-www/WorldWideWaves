package com.worldwidewaves.shared.events.utils

import kotlin.math.abs

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

/**
 * Represents a bounding box defined by its southwest and northeast corners.
 *
 * Does not support International Date Line wrapping.
 *
 */
class BoundingBox private constructor(val sw: Position, val ne: Position) {

    operator fun component1(): Position = sw
    operator fun component2(): Position = ne

    constructor(swLat: Double, swLng: Double, neLat: Double, neLng: Double) : this(
        sw = Position(minOf(swLat, neLat), minOf(swLng, neLng)).init(),
        ne = Position(maxOf(swLat, neLat), maxOf(swLng, neLng)).init()
    )

    // --- Companion object

    companion object {
        fun fromCorners(sw: Position, ne: Position): BoundingBox {
            return if (sw.lat <= ne.lat && sw.lng <= ne.lng) {
                // Already in correct order, no need to create new objects
                BoundingBox(sw, ne)
            } else {
                BoundingBox(
                    sw = Position(minOf(sw.lat, ne.lat), minOf(sw.lng, ne.lng)).init(),
                    ne = Position(maxOf(sw.lat, ne.lat), maxOf(sw.lng, ne.lng)).init()
                )
            }
        }

        fun fromCorners(positions: List<Position>): BoundingBox? {
            if (positions.isEmpty()) return null
            val minLat = positions.minOf { it.lat }
            val maxLat = positions.maxOf { it.lat }
            val lngs = positions.map { it.lng }
            val (swLng, neLng) = if (lngs.max() - lngs.min() > 180) {
                lngs.max() to lngs.min()
            } else {
                lngs.min() to lngs.max()
            }
            return BoundingBox(minLat, swLng, maxLat, neLng)
        }
    }

    // --- Properties

    val southwest: Position get() = sw
    val northeast: Position get() = ne

    val minLatitude: Double get() = sw.lat
    val maxLatitude: Double get() = ne.lat
    val minLongitude: Double get() = sw.lng
    val maxLongitude: Double get() = ne.lng

    val southLatitude: Double get() = sw.lat
    val northLatitude: Double get() = ne.lat
    val westLongitude: Double get() = sw.lng
    val eastLongitude: Double get() = ne.lng

    val width: Double get() =
        if (ne.lng >= sw.lng) ne.lng - sw.lng
        else 360 - sw.lng + ne.lng

    val height: Double get() = ne.lat - sw.lat

    // --- Public methods

    fun latitudeOfWidestPart(): Double = when {
        sw.lat <= 0 && ne.lat >= 0 -> 0.0 // Box crosses the equator
        abs(sw.lat) < abs(ne.lat) -> sw.lat // Southern latitude is closer to equator
        else -> ne.lat // Northern latitude is closer to equator
    }

    fun contains(position: Position): Boolean {
        val lat = position.lat
        val lng = position.lng
        return lat in minLatitude..maxLatitude && lng in minLongitude..maxLongitude
    }

    fun intersects(other: BoundingBox): Boolean {
        val latOverlap = !(other.maxLatitude < minLatitude || other.minLatitude > maxLatitude)
        val lngOverlap = !(other.maxLongitude < minLongitude || other.minLongitude > maxLongitude)
        return latOverlap && lngOverlap
    }

    fun expand(factor: Double): BoundingBox {
        val latDelta = height * (factor - 1) / 2
        val lngDelta = width * (factor - 1) / 2
        return BoundingBox(
            sw = Position(minLatitude - latDelta, minLongitude - lngDelta),
            ne = Position(maxLatitude + latDelta, maxLongitude + lngDelta)
        )
    }

    // --- Overrides

    override fun equals(other: Any?): Boolean =
        this === other || (other is BoundingBox && sw == other.sw && ne == other.ne)

    override fun hashCode(): Int = 31 * sw.hashCode() + ne.hashCode()
}