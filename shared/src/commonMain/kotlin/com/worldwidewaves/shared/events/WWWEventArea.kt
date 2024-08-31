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
    suspend fun getGeoJsonData(eventId: String): JsonObject?
}

class DefaultGeoJsonDataProvider : GeoJsonDataProvider {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getGeoJsonData(eventId: String): JsonObject? {
        return try {
            val geojsonData = withContext(Dispatchers.IO) {
                Napier.i("Loading geojson data for event $eventId")
                Res.readBytes("$FS_MAPS_FOLDER/$eventId.geojson").decodeToString()
            }
            Json.parseToJsonElement(geojsonData).jsonObject
        } catch (e: Exception) {
            Napier.e("Error loading geojson data for event $eventId", e)
            null
        }
    }
}

// ---------------------------

open class WWWEventArea(
    private val event: WWWEvent,
    private val geoJsonDataProvider: GeoJsonDataProvider = DefaultGeoJsonDataProvider()
) : KoinComponent {

    private val areaPolygon: Polygon = mutableListOf()
    private var cachedBoundingBox: BoundingBox? = null
    private var cachedCenter: Position? = null

    // ---------------------------

    /**
     * Retrieves the file path of the GeoJSON file for the event.
     *
     * This function attempts to get the absolute path of the GeoJSON file associated with the event.
     * It uses the event's ID to locate the file within the cache directory.
     *
     * @return The absolute path of the GeoJSON file as a String, or `null` if the file is not found.
     */
    internal suspend fun getGeoJsonFilePath(): String? {
        return getMapFileAbsolutePath(event.id, "geojson")
    }

    // ---------------------------

    /**
     * Checks if a given position is within the event area.
     *
     * This function retrieves the polygon representing the event area and uses the ray-casting algorithm
     * to determine if the specified position lies within the polygon.
     *
     * @param position The position to check.
     * @return `true` if the position is within the polygon, `false` otherwise.
     */
    suspend fun isPositionWithin(position: Position): Boolean {
        return getPolygon().let { it.isNotEmpty() && isPointInPolygon(position, it) }
    }

    // ---------------------------

    /**
     * Retrieves the bounding box of the polygon.
     *
     * This function calculates the bounding box of the polygon associated with the event area.
     * If the bounding box has been previously calculated and cached, it returns the cached value.
     * Otherwise, it calculates the bounding box, caches it, and then returns it.
     *
     * @return A [BoundingBox] object representing the bounding box of the polygon.
     * @throws IllegalStateException if the polygon is empty.
     */
    open suspend fun getBoundingBox(): BoundingBox {
        cachedBoundingBox?.let { return it }

        val polygon = getPolygon().takeIf { it.isNotEmpty() }
            ?: return BoundingBox( // Default bounding box
                ne = Position(0.0, 0.0),
                sw = Position(0.0, 0.0)
            )

        return polygonBbox(polygon).also { cachedBoundingBox = it }
    }

    /**
     * Calculates the center position of the event area.
     *
     * This function computes the center of the event area by averaging the latitudes and longitudes
     * of the northeast and southwest corners of the bounding box. If the center has been previously
     * calculated and cached, it returns the cached value.
     *
     * @return The center position of the event area as a [Position] object.
     */
    suspend fun getCenter(): Position {
        cachedCenter?.let { return it }

        val boundingBox = getBoundingBox()
        val center = Position(
            lat = (boundingBox.ne.lat + boundingBox.sw.lat) / 2,
            lng = (boundingBox.ne.lng + boundingBox.sw.lng) / 2
        )

        return center.also { cachedCenter = it }
    }

    // ---------------------------

    /**
     * Retrieves the polygon representing the event area.
     *
     * This function fetches the polygon data from the `geoJsonDataProvider` if the `areaPolygon` is empty.
     * It supports both "Polygon" and "MultiPolygon" types from the GeoJSON data.
     *
     * @return A `Polygon` object representing the event area.
     */
    suspend fun getPolygon(): Polygon {
        if (this.areaPolygon.isEmpty()) {
            val newPolygon = withContext(Dispatchers.Default) {
                val geometryCollection = geoJsonDataProvider.getGeoJsonData(event.id)

                if (geometryCollection == null) {
                    Napier.e("Error loading geojson data for event ${event.id}")
                    return@withContext emptyList()
                }

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

}
