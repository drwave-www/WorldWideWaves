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

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.DataValidator
import com.worldwidewaves.shared.events.utils.GeoJsonDataProvider
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.isPointInPolygon
import com.worldwidewaves.shared.events.utils.isPointInPolygons
import com.worldwidewaves.shared.events.utils.polygonsBbox
import com.worldwidewaves.shared.events.utils.splitPolygonByLongitude
import com.worldwidewaves.shared.getMapFileAbsolutePath
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// ---------------------------

@Serializable
data class WWWEventArea(

    val osmAdminid: Int,
    val warming: Warming

) : KoinComponent, DataValidator {

    private var _event: WWWEvent? = null
    private var event: WWWEvent
        get() = _event ?: throw IllegalStateException("Event not set")
        set(value) {
            _event = value
        }

    // ---------------------------

    private val geoJsonDataProvider: GeoJsonDataProvider by inject()
    private val coroutineScopeProvider: CoroutineScopeProvider by inject()

    @Transient private val areaPolygon: MutableList<Polygon> = mutableListOf()
    @Transient private var cachedWarmingPolygons: List<Polygon>? = null
    @Transient private var cachedBoundingBox: BoundingBox? = null
    @Transient private var cachedCenter: Position? = null

    // ---------------------------

    @Serializable
    data class Warming(
        val type: String,
        val longitude: Double? = null,
    ) : DataValidator {
        override fun validationErrors(): List<String>? = mutableListOf<String>().apply {
            when {
                type == "longitude-cut" && longitude == null ->
                    add("Longitude must not be null for type 'longitude-cut'")

                type == "longitude-cut" && (longitude!! < -180 || longitude > 180) ->
                    add("Longitude must be between -180 and 180")

                else -> {}
            }
        }.takeIf { it.isNotEmpty() }?.map { "warming: $it" }
    }

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
    suspend fun isPositionWithin(position: Position): Boolean =
        getPolygons().let { it.isNotEmpty() && isPointInPolygons(position, it) }

    // ---------------------------

    /**
     * Retrieves the bounding box of the polygon.
     *
     * This function calculates the bounding box of the polygon associated with the event area.
     * If the bounding box has been previously calculated and cached, it returns the cached value.
     * Otherwise, it calculates the bounding box, caches it, and then returns it.
     *
     */
    suspend fun getBoundingBox(): BoundingBox =
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
        cachedCenter ?: getBoundingBox().let { bbox ->
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
    private suspend fun getPolygons(): List<Polygon> {
        if (areaPolygon.isEmpty()) {
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
                            }.apply { areaPolygon.add(this) }
                        }
                        "MultiPolygon" -> coordinates?.flatMap { multiPolygon ->
                            multiPolygon.jsonArray.flatMap { ring ->
                                ring.jsonArray.map { point ->
                                    Position(
                                        point.jsonArray[1].jsonPrimitive.double,
                                        point.jsonArray[0].jsonPrimitive.double
                                    )
                                }.apply { areaPolygon.add(this) }
                            }
                        }
                        else -> { Log.e(::getPolygons.name, "${event.id}: Unsupported GeoJSON type: $type") }
                    }
                } ?: run {
                    Log.e(::getPolygons.name,"${event.id}: Error loading geojson data for event")
                }
            }
        }
        return areaPolygon
    }

    // ---------------------------

    /**
     * Checks if a given position is within any of the warming polygons.
     *
     * This function retrieves the warming polygons and checks if the specified position
     * is within any of these polygons using the `isPointInPolygon` function.
     *

     */
    suspend fun isPositionWithinWarming(position: Position): Boolean {
        return getWarmingPolygons().any { isPointInPolygon(position, it) }
    }

    /**
     * Retrieves the warming polygons for the event area.
     *
     * This function returns a list of polygons representing the warming zones for the event area.
     * If the warming polygons are already cached, it returns the cached value. Otherwise, it splits
     * the event area polygon by the warming zone longitude and caches the resulting right-side polygons.
     *
     */
    suspend fun getWarmingPolygons(): List<Polygon> {
        if (cachedWarmingPolygons == null) {
            cachedWarmingPolygons = when (warming.type) {
                "longitude-cut" -> event.area.getPolygons().flatMap { polygon ->
                    splitPolygonByLongitude(polygon, warming.longitude!!).right
                }
                else -> emptyList()
            }
        }
        return cachedWarmingPolygons!!
    }

    // ---------------------------

    override fun validationErrors(): List<String>? = mutableListOf<String>()
        .apply {
            when {
                osmAdminid < 0 || osmAdminid == 0 && event.type != "world" ->
                    add("OSM admin ID must be greater than 0 if it's not the world event")

                else -> warming.validationErrors()?.let { addAll(it) }
            }
        }.takeIf { it.isNotEmpty() }?.map { "area: $it" }

}
