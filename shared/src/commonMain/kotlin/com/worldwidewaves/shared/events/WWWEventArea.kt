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

import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.getMapFileAbsolutePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.ExperimentalResourceApi

// ---------------------------

data class Position(val latitude: Double, val longitude: Double)
data class BoundingBox(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double
)

// ---------------------------

open class WWWEventArea(private val event: WWWEvent) {

    private val areaPolygon: MutableList<Position> = mutableListOf()
    private var cachedBoundingBox: BoundingBox? = null

    // ---------------------------

    suspend fun isPositionWithin(position: Position): Boolean {
        return getCachedPolygon().let { it.isNotEmpty() && isPointInPolygon(position, it) }
    }

    suspend fun getBoundingBox(): BoundingBox {
        cachedBoundingBox?.let { return it }

        val polygon = getCachedPolygon().takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Polygon is empty")

        return polygonBbox(polygon).also { cachedBoundingBox = it }
    }

    // ---------------------------

    suspend fun getGeoJsonFilePath(): String? {
        return getMapFileAbsolutePath(this.event.id, "geojson")
    }

    @OptIn(ExperimentalResourceApi::class)
    open suspend fun getGeoJsonData(): JsonObject {
        val geojsonData = withContext(Dispatchers.IO) {
            Res.readBytes("files/maps/${event.id}.geojson").decodeToString() // TODO static folder name
        }
        return Json.parseToJsonElement(geojsonData).jsonObject
    }

    suspend fun getCachedPolygon(): List<Position> {
        if (this.areaPolygon.isEmpty()) {
            this.areaPolygon.addAll(
                withContext(Dispatchers.Default) {
                    val geometryCollection = getGeoJsonData()
                    val type = geometryCollection["type"]?.jsonPrimitive?.content
                    val coordinates = geometryCollection["coordinates"]?.jsonArray

                    when (type) {
                        "Polygon" -> {
                            coordinates?.flatMap { ring ->
                                ring.jsonArray.map { point ->
                                    Position(
                                        point.jsonArray[1].jsonPrimitive.double,
                                        point.jsonArray[0].jsonPrimitive.double
                                    )
                                }
                            } ?: emptyList()
                        }

                        "MultiPolygon" -> {
                            coordinates?.flatMap { multiPolygon ->
                                multiPolygon.jsonArray.flatMap { ring ->
                                    ring.jsonArray.map { point ->
                                        Position(
                                            point.jsonArray[1].jsonPrimitive.double,
                                            point.jsonArray[0].jsonPrimitive.double
                                        )
                                    }
                                }
                            } ?: emptyList()
                        }

                        else -> emptyList()
                    }
                }
            )
        }
        return this.areaPolygon
    }
}

// ---------------------------

// from https://github.com/KohlsAdrian/google_maps_utils/blob/master/lib/poly_utils.dart
fun isPointInPolygon(tap: Position, polygon: List<Position>): Boolean {
    var (bx, by) = polygon.last().let { it.latitude - tap.latitude to it.longitude - tap.longitude }
    var depth = 0

    for (i in polygon.indices) {
        val (ax, ay) = bx to by
        bx = polygon[i].latitude - tap.latitude
        by = polygon[i].longitude - tap.longitude

        if ((ay < 0 && by < 0) || (ay > 0 && by > 0) || (ax < 0 && bx < 0)) continue

        val lx = ax - ay * (bx - ax) / (by - ay)
        if (lx == 0.0) return true
        if (lx > 0) depth++
    }

    return (depth and 1) == 1
}

// ---------------------------

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

fun polygonBbox(polygon: List<Position>): BoundingBox {
    if (polygon.isEmpty())
        throw IllegalArgumentException("Event area cannot be empty, cannot determine bounding box")
    val (minLatitude, minLongitude, maxLatitude, maxLongitude) = polygon.fold(
        Quadruple(
            Double.MAX_VALUE, Double.MAX_VALUE,
            Double.MIN_VALUE, Double.MIN_VALUE
        )
    )
    { (minLat, minLon, maxLat, maxLon), pos ->
        Quadruple(
            minOf(minLat, pos.latitude),
            minOf(minLon, pos.longitude),
            maxOf(maxLat, pos.latitude),
            maxOf(maxLon, pos.longitude)
        )
    }
    return BoundingBox(minLatitude, minLongitude, maxLatitude, maxLongitude)
}