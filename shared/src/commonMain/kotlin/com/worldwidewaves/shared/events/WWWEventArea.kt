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
import kotlin.random.Random

// ---------------------------

@Serializable
data class WWWEventArea(
    val osmAdminids: List<Int>,
    val bbox: String? = null
) : KoinComponent, DataValidator {

    private var _event: IWWWEvent? = null
    private var event: IWWWEvent
        get() = _event ?: throw IllegalStateException("Event not set")
        set(value) {
            _event = value
        }

    // ---------------------------

    val bboxIsOverride: Boolean by lazy { parseBboxString() != null }

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
     * This function first checks if the position is within the bounding box.
     * If it's not within the bounding box, returns false immediately.
     * If it is within the bounding box, it then checks if the position is within
     * the polygons using the ray-casting algorithm.
     */
    suspend fun isPositionWithin(position: Position): Boolean {

        // Check if the cached result is within the epsilon
        cachedPositionWithinResult?.let { (cachedPosition, cachedResult) ->
            if (isPositionWithinEpsilon(position, cachedPosition)) {
                return cachedResult
            }
        }

        // First, check if the position is within the bounding box (fast check)
        val boundingBox = bbox()
        val isWithinBbox = position.lat >= boundingBox.sw.lat &&
                position.lat <= boundingBox.ne.lat &&
                position.lng >= boundingBox.sw.lng &&
                position.lng <= boundingBox.ne.lng

        // If not within the bounding box, return false immediately
        if (!isWithinBbox) {
            // Cache the result
            cachedPositionWithinResult = Pair(position, false)
            return false
        }

        // If within bounding box, check if within polygon (more expensive check)
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
     * Generates a random position within the event area.
     * Makes multiple attempts to find a valid position within the area.
     * Falls back to the center of the area if no valid position is found.
     */
    suspend fun generateRandomPositionInArea(): Position {
        val bbox = bbox()
        val maxAttempts = 50
        var attempts = 0
        var shrinkFactor = 1.0

        while (attempts < maxAttempts && shrinkFactor > 0.1) {
            val center = event.area.getCenter()
            val latRange = (bbox.ne.lat - bbox.sw.lat) * shrinkFactor
            val lngRange = (bbox.ne.lng - bbox.sw.lng) * shrinkFactor

            repeat(20) { // Try 20 times with current shrink factor
                val randomLat = center.lat + (Random.nextDouble() - 0.5) * latRange
                val randomLng = center.lng + (Random.nextDouble() - 0.5) * lngRange
                val position = Position(randomLat, randomLng)

                if (event.area.isPositionWithin(position)) {
                    return position
                }
            }

            shrinkFactor *= 0.8 // Shrink the sampling area
            attempts++
        }

        return event.area.getCenter()
    }

    // ---------------------------

    private fun parseBboxString(): BoundingBox? {
        bbox?.let {
            try {
                // Parse the string "minLng, minLat, maxLng, maxLat"
                val coordinates = bbox.split(",").map { it.trim().toDouble() }
                if (coordinates.size >= 4) {
                    return BoundingBox.fromCorners(
                        sw = Position(lat = coordinates[1], lng = coordinates[0]),
                        ne = Position(lat = coordinates[3], lng = coordinates[2])
                    )
                }
            } catch (e: Exception) {
                Log.e(::parseBboxString.name, "Failed to parse bbox string: ${e.message}")
            }
        }
        return null
    }

    suspend fun bbox(): BoundingBox {
        // Return cached bounding box if available
        if (cachedBoundingBox != null) {
            return cachedBoundingBox!!
        }

        // If bbox parameter was provided in constructor, use it
        parseBboxString()?.let { bbox ->
            cachedBoundingBox = bbox
            return bbox
        }

        // Otherwise calculate from polygons
        return getPolygons().takeIf { it.isNotEmpty() }
            ?.let {
                polygonsBbox(it).also { bbox -> cachedBoundingBox = bbox }
            } ?: BoundingBox.fromCorners(Position(0.0, 0.0), Position(0.0, 0.0))
    }

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
     * If any polygon points fall outside the bounding box, they are constrained to the bounding box.
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
                                ).constrainToBoundingBox()
                            }.toPolygon.apply { cachedAreaPolygons.add(this) }
                        }
                        "MultiPolygon" -> coordinates?.flatMap { multiPolygon ->
                            multiPolygon.jsonArray.flatMap { ring ->
                                ring.jsonArray.map { point ->
                                    Position(
                                        point.jsonArray[1].jsonPrimitive.double,
                                        point.jsonArray[0].jsonPrimitive.double
                                    ).constrainToBoundingBox()
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

    private fun Position.constrainToBoundingBox(): Position {
        parseBboxString()?.let { bbox ->
            return Position(
                lat = lat.coerceIn(bbox.sw.lat, bbox.ne.lat),
                lng = lng.coerceIn(bbox.sw.lng, bbox.ne.lng)
            )
        }

        return this
    }

    // ---------------------------

    override fun validationErrors(): List<String>? = mutableListOf<String>().apply {
        when {
            else -> { /* No validation errors */ }
        }
    }.takeIf { it.isNotEmpty() }?.map { "${WWWEventArea::class.simpleName}: $it" }

}
