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
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DataValidator
import com.worldwidewaves.shared.events.utils.GeoJsonDataProvider
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.events.utils.MutableArea
import com.worldwidewaves.shared.events.utils.PolygonUtils.isPointInPolygons
import com.worldwidewaves.shared.events.utils.PolygonUtils.polygonsBbox
import com.worldwidewaves.shared.events.utils.PolygonUtils.toPolygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.getMapFileAbsolutePath
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.abs

// ---------------------------

@Serializable
data class WWWEventArea(
    val osmAdminids: List<Int>
) : KoinComponent, DataValidator {

    private var _event: IWWWEvent? = null
    private var event: IWWWEvent
        get() = _event ?: throw IllegalStateException("Event not set")
        set(value) {
            _event = value
        }

    // ---------------------------

    private val geoJsonDataProvider: GeoJsonDataProvider by inject()
    private val coroutineScopeProvider: CoroutineScopeProvider by inject()

    @Transient private val cachedAreaPolygons: MutableArea = mutableListOf()
    @Transient private var cachedBoundingBox: BoundingBox? = null
    @Transient private var cachedCenter: Position? = null
    @Transient private var cachedPositionWithinResult: Pair<Position, Boolean>? = null

    @Transient private val positionEpsilon = 0.0001 // Roughly 10 meters

    // ---------------------------

    fun setRelatedEvent(event: WWWEvent) {
        this.event = event
    }

    // ---------------------------

    /**
     * Retrieves the file path of the GeoJSON file for the event.
     *
     * This function attempts to get the absolute path of the GeoJSON file associated with the event.
     * It uses the event's ID to locate the file within the cache directory.
     *
     */
    internal suspend fun getGeoJsonFilePath(): String? =
        getMapFileAbsolutePath(event.id, "geojson")

    // ---------------------------

    /**
     * Checks if a given position is within the event area.
     *
     * This function retrieves the polygon representing the event area and uses the ray-casting algorithm
     * to determine if the specified position lies within the polygon.
     *
     */
    suspend fun isPositionWithin(position: Position): Boolean {
        // Check if the cached result is within the epsilon
        cachedPositionWithinResult?.let { (cachedPosition, cachedResult) ->
            if (isPositionWithinEpsilon(position, cachedPosition)) {
                return cachedResult
            }
        }

        // Calculate the result if not cached or outside the epsilon
        val result = getPolygons().let { it.isNotEmpty() && isPointInPolygons(position, it) }

        // Cache the result
        cachedPositionWithinResult = Pair(position, result)
        return result
    }

    private fun isPositionWithinEpsilon(pos1: Position, pos2: Position): Boolean {
        return abs(pos1.lat - pos2.lat) < positionEpsilon && abs(pos1.lng - pos2.lng) < positionEpsilon
    }

    // ---------------------------

    /**
     * Retrieves the bounding box of the polygon.
     *
     * This function calculates the bounding box of the polygon associated with the event area.
     * If the bounding box has been previously calculated and cached, it returns the cached value.
     * Otherwise, it calculates the bounding box, caches it, and then returns it.
     *
     */
    suspend fun bbox(): BoundingBox =
        cachedBoundingBox ?: getPolygons().takeIf { it.isNotEmpty() }
            ?.let {
                polygonsBbox(it).also { bbox -> cachedBoundingBox = bbox }
            } ?: BoundingBox(Position(0.0, 0.0), Position(0.0, 0.0))

    /**
     * Calculates the center position of the event area.
     *
     * This function computes the center of the event area by averaging the latitudes and longitudes
     * of the northeast and southwest corners of the bounding box. If the center has been previously
     * calculated and cached, it returns the cached value.
     *
     */
    suspend fun getCenter(): Position =
        cachedCenter ?: bbox().let { bbox ->
            Position(
                lat = (bbox.ne.lat + bbox.sw.lat) / 2,
                lng = (bbox.ne.lng + bbox.sw.lng) / 2
            ).also { cachedCenter = it }
        }

    // ---------------------------

    /**
     * Retrieves the polygon representing the event area.
     *
     * This function fetches the polygon data from the `geoJsonDataProvider` if the `areaPolygon` is empty.
     * It supports both "Polygon" and "MultiPolygon" types from the GeoJSON data.
     *
     */
     suspend fun getPolygons(): Area {
        if (cachedAreaPolygons.isEmpty()) {
            coroutineScopeProvider.withDefaultContext {
                geoJsonDataProvider.getGeoJsonData(event.id)?.let { geometryCollection ->
                    val type = geometryCollection["type"]?.jsonPrimitive?.content
                    val coordinates = geometryCollection["coordinates"]?.jsonArray

                    when (type) {
                        "Polygon" -> coordinates?.flatMap { ring ->
                            ring.jsonArray.map { point ->
                                Position(
                                    point.jsonArray[1].jsonPrimitive.double,
                                    point.jsonArray[0].jsonPrimitive.double
                                )
                            }.toPolygon.apply { cachedAreaPolygons.add(this) }
                        }
                        "MultiPolygon" -> coordinates?.flatMap { multiPolygon ->
                            multiPolygon.jsonArray.flatMap { ring ->
                                ring.jsonArray.map { point ->
                                    Position(
                                        point.jsonArray[1].jsonPrimitive.double,
                                        point.jsonArray[0].jsonPrimitive.double
                                    )
                                }.toPolygon.apply { cachedAreaPolygons.add(this) }
                            }
                        }
                        else -> { Log.e(::getPolygons.name, "${event.id}: Unsupported GeoJSON type: $type") }
                    }
                } ?: run {
                    Log.e(::getPolygons.name,"${event.id}: Error loading geojson data for event")
                }
            }
        }
        return cachedAreaPolygons
    }

    // ---------------------------

    override fun validationErrors(): List<String>? = mutableListOf<String>().apply {
        when {
            else -> { /* No validation errors */ }
        }
    }.takeIf { it.isNotEmpty() }?.map { "${WWWEventArea::class.simpleName}: $it" }

}
