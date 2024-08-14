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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.ExperimentalResourceApi

// ---------------------------

data class Position(val latitude: Double, val longitude: Double)

private class WWWEventAreaCompanion {
    companion object {
        private val areaPolygonMap = mutableMapOf<String, List<Position>>()

        fun getAreaPolygon(id: String): List<Position> {
            return areaPolygonMap[id] ?: mutableListOf()
        }

        fun setAreaPolygon(id: String, polygon: List<Position>) {
            areaPolygonMap[id] = polygon
        }
    }
}

@Transient
internal var WWWEvent.areaPolygon: List<Position>
    get() = WWWEventAreaCompanion.getAreaPolygon(id)
    set(value) {
        WWWEventAreaCompanion.setAreaPolygon(id, value)
    }

// ---------------------------

suspend fun WWWEvent.isPositionWithinArea(position: Position): Boolean {
    val polygon = getCachedPolygon()
    return polygon.isNotEmpty() && isPointInPolygon(position, polygon)
}

// ---------------------------

@OptIn(ExperimentalResourceApi::class)
private suspend fun WWWEvent.getGeoJsonData(): JsonObject {
    val geojsonData = withContext(Dispatchers.IO) {
        Res.readBytes("files/maps/$id.geojson").decodeToString() // TODO static folder name
    }
    return Json.parseToJsonElement(geojsonData).jsonObject
}

suspend fun WWWEvent.getCachedPolygon(): List<Position> {
    if (this.areaPolygon.isEmpty()) {
        this.areaPolygon = withContext(Dispatchers.Default) {
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
    }
    return this.areaPolygon
}

// ---------------------------

// from https://github.com/KohlsAdrian/google_maps_utils/blob/master/lib/poly_utils.dart
fun isPointInPolygon(tap: Position, polygon: List<Position>): Boolean {
    var ax: Double
    var ay: Double
    var bx = polygon[polygon.size - 1].latitude - tap.latitude
    var by = polygon[polygon.size - 1].longitude - tap.longitude
    var depth =0

    for (i in polygon.indices) {
        ax = bx
        ay = by
        bx = polygon[i].latitude - tap.latitude
        by = polygon[i].longitude - tap.longitude

        if ((ay < 0 && by < 0) || (ay > 0 && by > 0) || (ax < 0 && bx < 0)) {
            continue // both "up" or both "down", or both points on left
        }

        val lx = ax - ay * (bx - ax) / (by - ay)

        if (lx == 0.0) return true // point on edge
        if (lx > 0) depth++
    }

    return (depth and 1) == 1
}