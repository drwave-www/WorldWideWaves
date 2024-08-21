package com.worldwidewaves.shared.events.utils

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

import com.worldwidewaves.shared.events.utils.SplitPolygonResult.ResultPosition.LEFT
import com.worldwidewaves.shared.events.utils.SplitPolygonResult.ResultPosition.RIGHT
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// ----------------------------------------------------------------------------

data class Position(val lat: Double, val lng: Double)

typealias Polygon = List<Position>

data class Segment(val start: Position, val end: Position)

data class BoundingBox(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double
)

// ----------------------------------------------------------------------------

/**
 * Determines if a point is inside a polygon.
 *
 * This function implements a ray casting algorithm to determine if a given point(`tap`) lies
 * inside a polygon.
 *
 * It's based on the algorithm described in
 * [https://github.com/KohlsAdrian/google_maps_utils/blob/master/lib/poly_utils.dart](https://github.com/KohlsAdrian/google_maps_utils/blob/master/lib/poly_utils.dart).
 *
 * @param tap The point to check.
 * @param polygon The polygon to test against.
 * @return `true` if the point is inside the polygon, `false` otherwise.
 *
 */
fun isPointInPolygon(tap: Position, polygon: Polygon): Boolean {
    var (bx, by) = polygon.last().let { it.lat - tap.lat to it.lng - tap.lng }
    var depth = 0

    for (i in polygon.indices) {
        val (ax, ay) = bx to by
        bx = polygon[i].lat - tap.lat
        by = polygon[i].lng - tap.lng

        if ((ay < 0 && by < 0) || (ay > 0 && by > 0) || (ax < 0 && bx < 0)) continue

        val lx = ax - ay * (bx - ax) / (by - ay)
        if (lx == 0.0) return true
        if (lx > 0) depth++
    }

    return (depth and 1) == 1
}

// ----------------------------------------------------------------------------

/**
 * Calculates the bounding box of a polygon.
 *
 * This function takes a polygon represented as a list of [Position]objects and returns a
 * [BoundingBox] object that encompasses the entire polygon.
 *
 * It throws an [IllegalArgumentException] if the input polygon is empty.
 *
 * @param polygon The polygon for which to calculate the bounding box.
 * @return A [BoundingBox] object representing the bounding box of the polygon.
 *
 */

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

fun polygonBbox(polygon: Polygon): BoundingBox {
    if (polygon.isEmpty())
        throw IllegalArgumentException("Event area cannot be empty, cannot determine bounding box")

    val (minLatitude, minLongitude, maxLatitude, maxLongitude) = polygon.fold(
        Quadruple(
            Double.MAX_VALUE, Double.MAX_VALUE,
            Double.MIN_VALUE, Double.MIN_VALUE
        )
    ) { (minLat, minLon, maxLat, maxLon), pos ->
        Quadruple(
            minOf(minLat, pos.lat), minOf(minLon, pos.lng),
            maxOf(maxLat, pos.lat), maxOf(maxLon, pos.lng)
        )
    }
    return BoundingBox(minLatitude, minLongitude, maxLatitude, maxLongitude)
}

// ----------------------------------------------------------------------------

/**
 * Splits a polygon by a given longitude.
 *
 * This function takes a polygon represented as a list of [Position] objects and a longitude value
 * (`longitudeToCut`) as input. It splits the polygon into two parts: the left part containing
 * points with longitudes less than or equal to `longitudeToCut`, and the right part containing
 * points with longitudes greater than or equal to `longitudeToCut`.*
 *
 * If the `longitudeToCut` is completely outside the bounds of the polygon, the entire polygon is
 * returned either on the left or right side, depending on whether the cut is to the east or west
 * of the polygon.
 *
 * The function handles cases where the polygon intersects the cut line by calculating intersection
 * points and adding them to both the left and right sides.
 *
 * @param polygon The polygon to be split, represented as a list of [Position] objects.
 * @param longitudeToCut The longitude value to split the polygon by.
 * @return A[SplitPolygonResult] object containing the left and right polygons.
 *
 */

data class SplitPolygonResult(val left: List<Polygon>, val right: List<Polygon>) {
    enum class ResultPosition { LEFT, RIGHT }
    companion object {
        fun fromPolygon(polygon: Polygon, resultPosition: ResultPosition = RIGHT): SplitPolygonResult {
            return if (polygon.size > 1) {
                when (resultPosition) {
                    LEFT -> SplitPolygonResult(listOf(polygon), emptyList())
                    RIGHT -> SplitPolygonResult(emptyList(), listOf(polygon))
                }
            } else empty()
        }
        fun empty() = SplitPolygonResult(emptyList(), emptyList())
    }
}

fun splitPolygonByLongitude(polygon: List<Position>, longitudeToCut: Double): SplitPolygonResult {
    val leftSide = mutableListOf<Position>()
    val rightSide = mutableListOf<Position>()

    val minLongitude = polygon.minOfOrNull { it.lng } ?: return SplitPolygonResult.empty()
    val maxLongitude = polygon.maxOfOrNull { it.lng } ?: return SplitPolygonResult.empty()

    return when {
        longitudeToCut > maxLongitude -> SplitPolygonResult.fromPolygon(polygon, LEFT)
        longitudeToCut < minLongitude -> SplitPolygonResult.fromPolygon(polygon, RIGHT)
        else -> {
            for (i in polygon.indices) {
                val point = polygon[i]
                val nextPoint = polygon[(i + 1) % polygon.size]

                val intersectionLatitude = point.lat + (nextPoint.lat - point.lat) *
                        (longitudeToCut - point.lng) / (nextPoint.lng - point.lng)
                val intersection = Position(intersectionLatitude, longitudeToCut)

                if (point.lng <= longitudeToCut) leftSide.add(point)
                if (point.lng >= longitudeToCut) rightSide.add(point)

                if ((point.lng < longitudeToCut && nextPoint.lng > longitudeToCut) ||
                    (point.lng > longitudeToCut && nextPoint.lng < longitudeToCut)) {
                    leftSide.add(intersection)
                    rightSide.add(intersection)
                }
            }

            if (leftSide.isNotEmpty() && leftSide.first() != leftSide.last()) leftSide.add(leftSide.first())
            if (rightSide.isNotEmpty() && rightSide.first() != rightSide.last()) rightSide.add(rightSide.first())

            val leftPolygons = groupIntoRingPolygons(leftSide).filter { it.size > 1 }
            val rightPolygons = groupIntoRingPolygons(rightSide).filter { it.size > 1 }

            SplitPolygonResult(leftPolygons, rightPolygons)
        }
    }
}

// Helper function to group polygon points into ring polygons
private fun groupIntoRingPolygons(polygon: List<Position>): List<Polygon> {
    val polygons = mutableListOf<Polygon>()
    val currentPolygon = mutableListOf<Position>()

    for (i in polygon.indices) {
        val point = polygon[i]
        if (i > 0 && point == polygon.last()) break
        currentPolygon.add(point)

        if (currentPolygon.size > 1) {
            val shouldSplit = polygon.indices.any { j ->
                val segment = Segment(polygon[j], polygon[(j + 1) % polygon.size])
                i != j && i != (j + 1) % polygon.size &&
                        isPointOnLineSegment(point, segment) &&
                        (i != j + 1 && point != polygon[j])
            }
            if (shouldSplit) {
                if (point != currentPolygon.first()) currentPolygon.add(currentPolygon.first())
                polygons.add(currentPolygon.toList())
                currentPolygon.clear()
                currentPolygon.add(point)
                continue
            }
        }

        if (currentPolygon.size > 1 && point == currentPolygon.first()) {
            polygons.add(currentPolygon.toList())
            currentPolygon.clear()
        }
    }

    if (currentPolygon.isNotEmpty() && currentPolygon.first() != currentPolygon.last()) {
        currentPolygon.add(currentPolygon.first())
        polygons.add(currentPolygon)
    }

    return polygons.filter {
        it.size >= 3 && !(it.all { p -> p.lat == it[0].lat } || it.all { p -> p.lng == it[0].lng })
    }
}

// Helper function to check if a point lies on a line segment
private const val EPSILON = 1e-10 // Adjust the tolerance as needed

fun isPointOnLineSegment(point: Position, segment: Segment): Boolean {
    val crossProduct = (segment.end.lat - segment.start.lat) * (point.lng - segment.start.lng) -
            (segment.end.lng - segment.start.lng) * (point.lat - segment.start.lat)
    return abs(crossProduct) < EPSILON &&
            point.lat in min(segment.start.lat, segment.end.lat)..max(segment.start.lat, segment.end.lat) &&
            point.lng in min(segment.start.lng, segment.end.lng)..max(segment.start.lng, segment.end.lng)
}