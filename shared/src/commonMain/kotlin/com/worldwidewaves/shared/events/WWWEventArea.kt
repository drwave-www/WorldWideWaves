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

import com.worldwidewaves.shared.WWWGlobals.Companion.FS_MAPS_FOLDER
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.isPointInPolygon
import com.worldwidewaves.shared.events.utils.polygonBbox
import com.worldwidewaves.shared.events.utils.splitPolygonByLongitude
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.getMapFileAbsolutePath
import io.github.aakira.napier.Napier
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

interface GeoJsonDataProvider {
    suspend fun getGeoJsonData(eventId: String): JsonObject
}

class DefaultGeoJsonDataProvider : GeoJsonDataProvider {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getGeoJsonData(eventId: String): JsonObject {
        val geojsonData = withContext(Dispatchers.IO) {
            Napier.i("Loading geojson data for event $eventId")
            Res.readBytes("$FS_MAPS_FOLDER/$eventId.geojson").decodeToString()
        }
        return Json.parseToJsonElement(geojsonData).jsonObject
    }
}

// ---------------------------

open class WWWEventArea(
    private val event: WWWEvent,
    private val geoJsonDataProvider: GeoJsonDataProvider = DefaultGeoJsonDataProvider()
) : KoinComponent {

    private val areaPolygon: Polygon = mutableListOf()
    private var cachedWarmingPolygons: List<Polygon>? = null
    private var cachedBoundingBox: BoundingBox? = null

    // ---------------------------

    internal suspend fun getGeoJsonFilePath(): String? {
        return getMapFileAbsolutePath(event.id, "geojson")
    }

    // ---------------------------

    suspend fun isPositionWithin(position: Position): Boolean {
        return getPolygon().let { it.isNotEmpty() && isPointInPolygon(position, it) }
    }

    open suspend fun getBoundingBox(): BoundingBox {
        cachedBoundingBox?.let { return it }

        val polygon = getPolygon().takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Polygon is empty")

        return polygonBbox(polygon).also { cachedBoundingBox = it }
    }

    // ---------------------------

    suspend fun getPolygon(): Polygon {
        if (this.areaPolygon.isEmpty()) {
            val newPolygon = withContext(Dispatchers.Default) {
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
            return newPolygon
        }

        return this.areaPolygon
    }

    // ---------------------------

    suspend fun getWarmingPolygons(): List<Polygon> {
        return cachedWarmingPolygons ?: splitPolygonByLongitude(getPolygon(), event.mapWarmingZoneLongitude).right.also {
            cachedWarmingPolygons = it
        }
    }

}
