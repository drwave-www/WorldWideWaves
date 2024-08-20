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

import com.worldwidewaves.shared.WWWGlobals.Companion.FS_MAPS_FOLDERS
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
import org.koin.core.component.KoinComponent

// ---------------------------

data class Position(val latitude: Double, val longitude: Double)
data class BoundingBox(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double
)

// ---------------------------

interface GeoJsonDataProvider {
    suspend fun getGeoJsonData(eventId: String): JsonObject
}

class DefaultGeoJsonDataProvider : GeoJsonDataProvider {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getGeoJsonData(eventId: String): JsonObject {
        val geojsonData = withContext(Dispatchers.IO) {
            Res.readBytes("$FS_MAPS_FOLDERS/$eventId.geojson").decodeToString()
        }
        return Json.parseToJsonElement(geojsonData).jsonObject
    }
}

// ---------------------------

open class WWWEventArea(
    private val event: WWWEvent,
    private val geoJsonDataProvider: GeoJsonDataProvider = DefaultGeoJsonDataProvider()
) : KoinComponent {

    private val areaPolygon: MutableList<Position> = mutableListOf()
    private var cachedBoundingBox: BoundingBox? = null

    // ---------------------------

    internal suspend fun getGeoJsonFilePath(): String? {
        return getMapFileAbsolutePath(event.id, "geojson")
    }

    // ---------------------------

    suspend fun isPositionWithin(position: Position): Boolean {
        return getCachedPolygon().let { it.isNotEmpty() && isPointInPolygon(position, it) }
    }

    open suspend fun getBoundingBox(): BoundingBox {
        cachedBoundingBox?.let { return it }

        val polygon = getCachedPolygon().takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Polygon is empty")

        return polygonBbox(polygon).also { cachedBoundingBox = it }
    }

    // ---------------------------

    suspend fun getCachedPolygon(): List<Position> {
        if (this.areaPolygon.isEmpty()) {
            this.areaPolygon.addAll(
                withContext(Dispatchers.Default) {
                    val geometryCollection = geoJsonDataProvider.getGeoJsonData(event.id)
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


//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.jsonObject
//import kotlinx.serialization.json.jsonPrimitive
//import kotlinx.serialization.json.jsonArray
//import kotlinx.serialization.json.double
//import kotlinx.serialization.json.decodeFromJsonElement
//import kotlinx.serialization.Serializable
//import java.io.File

//@Serializable
//data class GeoJsonPolygon(val type: String, val coordinates: List<List<List<Double>>>)
//
//fun parseGeoJson(filePath: String): GeoJsonPolygon {
//    val jsonString = File(filePath).readText()
//    val jsonElement = Json.parseToJsonElement(jsonString)
//    return Json.decodeFromJsonElement(jsonElement.jsonObject["geometry"]!!)
//}
//
//fun splitPolygonByLongitude(polygon: GeoJsonPolygon, longitude: Double): List<List<List<Double>>> {
//    val coordinates = polygon.coordinates[0]
//    val leftSide = mutableListOf<List<Double>>()
//    val rightSide = mutableListOf<List<Double>>()
//    var isLeft = true
//
//    for (i in coordinates.indices) {
//        val point = coordinates[i]
//        if (point[0] < longitude) {
//            if (!isLeft) {
//                isLeft = true
//                leftSide.add(listOf(longitude, point[1]))
//                rightSide.add(listOf(longitude, point[1]))
//            }
//            leftSide.add(point)
//        } else {
//            if (isLeft) {
//                isLeft = false
//                leftSide.add(listOf(longitude, point[1]))
//                rightSide.add(listOf(longitude, point[1]))
//            }
//            rightSide.add(point)
//        }
//    }
//
//    return listOf(leftSide, rightSide)
//}
//
//fun main() {
//    val geoJsonFilePath = "path/to/your/geojson/file.geojson"
//    val specifiedLongitude = 2.3522 // Example longitude for Paris
//
//    val polygon = parseGeoJson(geoJsonFilePath)
//    val (leftPolygon, rightPolygon) = splitPolygonByLongitude(polygon, specifiedLongitude)
//
//    println("Left Polygon: $leftPolygon")
//    println("Right Polygon: $rightPolygon")
//}