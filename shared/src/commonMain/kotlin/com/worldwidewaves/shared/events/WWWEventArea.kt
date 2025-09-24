package com.worldwidewaves.shared.events

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
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
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.events.utils.MutableArea
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.PolygonUtils.isPointInPolygons
import com.worldwidewaves.shared.events.utils.PolygonUtils.polygonsBbox
import com.worldwidewaves.shared.events.utils.PolygonUtils.toPolygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.getMapFileAbsolutePath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.abs
import kotlin.random.Random

// ---------------------------

/**
 * Geospatial definition of an event: polygons and bounding-box.
 *
 * Loads polygons from the event-specific cached GeoJSON (or an optional `bbox`
 * string override), then:
 *  • Caches the parsed polygons, computed bounding-box and center position.
 *  • Provides fast `isPositionWithin()` tests (bbox pre-check + polygon test).
 *  • Offers helpers such as `generateRandomPositionInArea()` for simulation.
 *  • Exposes lazy `bbox()` / `getCenter()` accessors used by map & wave logic.
 *
 * All heavy I/O / parsing work is executed inside the provided
 * [CoroutineScopeProvider] so callers remain on the main thread.
 */
@Serializable
data class WWWEventArea(
    val osmAdminids: List<Int>,
    val bbox: String? = null,
) : KoinComponent,
    DataValidator {
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

    @Transient private var cachedAreaPolygons: Area? = null

    @Transient private val polygonsCacheMutex = Mutex()

        // Add mutex for cache protection
    @Transient private var cachedBoundingBox: BoundingBox? = null

    @Transient private var cachedCenter: Position? = null

    @Transient private var cachedPositionWithinResult: Pair<Position, Boolean>? = null

    @Transient private val positionEpsilon = 0.0001 // Roughly 10 meters

    // Polygon loading state notification
    @Transient private val _polygonsLoaded = MutableStateFlow(false)
    val polygonsLoaded: StateFlow<Boolean> = _polygonsLoaded.asStateFlow()

    // ---------------------------

    /**
     * Clears all cached data to force fresh calculations.
     * Useful when debugging or when polygon data has been updated.
     */
    fun clearCache() {
        cachedAreaPolygons = null
        cachedBoundingBox = null
        cachedCenter = null
        cachedPositionWithinResult = null
        _polygonsLoaded.value = false
    }

    // ---------------------------

    fun setRelatedEvent(event: WWWEvent) {
        this.event = event
        // Clear cache to ensure fresh calculations for this event
        clearCache()
    }

    // ---------------------------

    /**
     * Retrieves the file path of the GeoJSON file for the event.
     *
     * This function attempts to get the absolute path of the GeoJSON file associated with the event.
     * It uses the event's ID to locate the file within the cache directory.
     *
     */
    internal suspend fun getGeoJsonFilePath(): String? = getMapFileAbsolutePath(event.id, "geojson")

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

        val isWithinBbox =
            position.lat >= boundingBox.sw.lat &&
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
        val polygons = getPolygons()

        if (polygons.isEmpty()) {
            // Don't cache the result when polygons aren't loaded yet - this allows future checks to retry
            return false
        }

        val result = isPointInPolygons(position, polygons)

        // Cache the result
        cachedPositionWithinResult = Pair(position, result)
        return result
    }

    private fun isPositionWithinEpsilon(
        pos1: Position,
        pos2: Position,
    ): Boolean = abs(pos1.lat - pos2.lat) < positionEpsilon && abs(pos1.lng - pos2.lng) < positionEpsilon

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

            repeat(20) {
                // Try 20 times with current shrink factor
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
                        ne = Position(lat = coordinates[3], lng = coordinates[2]),
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

        // Try to extract bbox directly from GeoJSON if available
        parseGeoJsonBbox()?.let { geoBbox ->
            cachedBoundingBox = geoBbox
            return geoBbox
        }

        // Fallback: compute extent by scanning every coordinate in the GeoJSON
        computeExtentFromGeoJson()?.let { extentBbox ->
            cachedBoundingBox = extentBbox
            return extentBbox
        }

        // Otherwise calculate from polygons
        val polygons = getPolygons()
        return polygons
            .takeIf { it.isNotEmpty() }
            ?.let {
                val bbox = polygonsBbox(it)
                cachedBoundingBox = bbox
                bbox
            } ?: run {
            val defaultBbox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(0.0, 0.0))
            defaultBbox
        }
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
                lng = (bbox.ne.lng + bbox.sw.lng) / 2,
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

        // Fast path: if cache is already populated, return immediately
        cachedAreaPolygons?.let {
            return it
        }


        // Slow path: populate cache with mutex protection
        polygonsCacheMutex.withLock {
            // Double-check pattern: another coroutine might have populated the cache
            cachedAreaPolygons?.let {
                return it
            }


            // Build polygons in a temporary mutable list
            val tempPolygons: MutableArea = mutableListOf()

            try {
                coroutineScopeProvider.withDefaultContext {
                    loadPolygonsFromGeoJson(tempPolygons)
                }
            } catch (ignored: Exception) {
                // Polygon loading errors are handled gracefully - empty polygon list is acceptable
            }

            // Atomically assign the complete immutable list
            cachedAreaPolygons = tempPolygons.toList()

            // Notify that polygon data is now available
            _polygonsLoaded.value = true
        }

        val result = cachedAreaPolygons ?: emptyList()
        return result
    }

    private suspend fun loadPolygonsFromGeoJson(tempPolygons: MutableArea) {
        try {
            val geoJsonData = geoJsonDataProvider.getGeoJsonData(event.id)

            if (geoJsonData != null) {
                processGeoJsonData(geoJsonData, tempPolygons)
            }

        } catch (ignored: Exception) {
            // GeoJSON data loading errors are handled gracefully
        }
    }

    private fun processGeoJsonData(geoJsonData: JsonObject, tempPolygons: MutableArea) {
        val rootType = geoJsonData["type"]?.jsonPrimitive?.content

        when (rootType) {
            "FeatureCollection" -> {
                processFeatureCollection(geoJsonData, tempPolygons)
            }
            "Polygon", "MultiPolygon" -> {
                processDirectGeometry(geoJsonData, tempPolygons)
            }
        }
    }

    private fun processFeatureCollection(geoJsonData: JsonObject, tempPolygons: MutableArea) {
        val features = geoJsonData["features"]?.jsonArray
        features?.forEach { feature ->
            try {
                val geometry = feature.jsonObject["geometry"]?.jsonObject
                if (geometry != null) {
                    processGeometry(geometry, tempPolygons)
                }
            } catch (ignored: Exception) {
                // Feature geometry processing errors are handled gracefully
            }
        }
    }

    private fun processDirectGeometry(geoJsonData: JsonObject, tempPolygons: MutableArea) {
        try {
            processGeometry(geoJsonData, tempPolygons)
        } catch (ignored: Exception) {
            // Direct geometry processing errors are handled gracefully
        }
    }

    private fun processGeometry(
        geometry: JsonObject,
        tempPolygons: MutableList<Polygon>,
    ) {
        try {
            val type = geometry["type"]?.jsonPrimitive?.content
            val coordinates = geometry["coordinates"]?.jsonArray


            when (type) {
                // For a Polygon we add every ring (first is exterior, others holes are ignored downstream)
                "Polygon" -> {
                    coordinates?.forEachIndexed { ringIndex, ring ->
                        try {
                            processRing(ring, tempPolygons)
                        } catch (ignored: Exception) {
                            // Ignore invalid ring geometry and continue processing
                        }
                    }
                }
                // For a MultiPolygon, keep only the first ring (exterior) of each polygon element
                "MultiPolygon" -> {
                    processMultiPolygon(coordinates, tempPolygons)
                }
                else -> {
                }
            }

        } catch (ignored: Exception) {
            // Geometry processing errors are handled gracefully
        }
    }

    private fun processMultiPolygon(
        coordinates: kotlinx.serialization.json.JsonArray?,
        tempPolygons: MutableList<Polygon>,
    ) {
        if (coordinates == null) {
            return
        }

        if (coordinates.size == 0) {
            return
        }

        coordinates.forEachIndexed { polygonIndex, polygon ->
            try {

                // Verify polygon is a valid JsonArray
                val polygonArray = try {
                    polygon.jsonArray
                } catch (e: Exception) {
                    return@forEachIndexed
                }


                if (polygonArray.isEmpty()) {
                    return@forEachIndexed
                }

                // Each polygon in MultiPolygon has rings (exterior + holes)
                polygonArray.forEachIndexed { ringIndex, ring ->
                    try {

                        // Verify ring is a valid JsonArray
                        val ringArray = try {
                            ring.jsonArray
                        } catch (e: Exception) {
                            return@forEachIndexed
                        }


                        if (ringArray.isEmpty()) {
                            return@forEachIndexed
                        }

                        processRing(ring, tempPolygons)
                    } catch (ignored: Exception) {
                        // Ring processing errors are handled gracefully
                    }
                }

            } catch (ignored: Exception) {
                // MultiPolygon processing errors are handled gracefully
            }
        }

    }

    private fun processRing(
        ring: JsonElement,
        polygons: MutableArea,
    ) {
        try {

            // Verify ring is a JsonArray
            val ringArray = try {
                ring.jsonArray
            } catch (e: Exception) {
                return
            }


            if (ringArray.isEmpty()) {
                return
            }


            val positions =
                ringArray.mapIndexed { pointIndex, point ->
                    try {

                        val pointArray = try {
                            point.jsonArray
                        } catch (e: Exception) {
                            return@mapIndexed null
                        }

                        if (pointArray.size >= 2) {
                            val lng = try {
                                pointArray[0].jsonPrimitive.double
                            } catch (e: Exception) {
                                return@mapIndexed null
                            }

                            val lat = try {
                                pointArray[1].jsonPrimitive.double
                            } catch (e: Exception) {
                                return@mapIndexed null
                            }

                            Position(lat, lng).constrainToBoundingBox()
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }.filterNotNull()


            if (positions.isEmpty()) {
                return
            }


            val polygon = try {
                positions.toPolygon
            } catch (e: Exception) {
                return
            }


            if (polygon.size > 1) {
                polygons.add(polygon)
            } else {
                // Polygon with only one point is ignored
            }

        } catch (ignored: Exception) {
            // Ring processing errors are handled gracefully
        }
    }

    private fun Position.constrainToBoundingBox(): Position {
        parseBboxString()?.let { bbox ->
            return Position(
                lat = lat.coerceIn(bbox.sw.lat, bbox.ne.lat),
                lng = lng.coerceIn(bbox.sw.lng, bbox.ne.lng),
            )
        }

        return this
    }

    // ---------------------------

    /**
     * Attempt to read a \"bbox\" array from the GeoJSON root and convert it to BoundingBox.
     * Format expected: [minLng, minLat, maxLng, maxLat].
     */
    private suspend fun parseGeoJsonBbox(): BoundingBox? =
        try {
            geoJsonDataProvider
                .getGeoJsonData(event.id)
                ?.get("bbox")
                ?.jsonArray
                ?.takeIf { it.size >= 4 }
                ?.let { arr ->
                    val minLng = arr[0].jsonPrimitive.double
                    val minLat = arr[1].jsonPrimitive.double
                    val maxLng = arr[2].jsonPrimitive.double
                    val maxLat = arr[3].jsonPrimitive.double

                    Log.i(
                        ::parseGeoJsonBbox.name,
                        "${event.id}: Using bbox from GeoJSON [$minLng,$minLat,$maxLng,$maxLat]",
                    )

                    BoundingBox.fromCorners(
                        sw = Position(minLat, minLng),
                        ne = Position(maxLat, maxLng),
                    )
                }
        } catch (e: Exception) {
            Log.w(
                ::parseGeoJsonBbox.name,
                "${event.id}: Malformed or missing bbox in GeoJSON (${e.message})",
            )
            null
        }
    // ---------------------------

    /**
     * Compute an extent by scanning every coordinate pair in the GeoJSON.
     * Useful when the file has no explicit \"bbox\" property and polygons
     * parsing has not yet happened.
     */
    private suspend fun computeExtentFromGeoJson(): BoundingBox? =
        try {
            var minLat = Double.POSITIVE_INFINITY
            var minLng = Double.POSITIVE_INFINITY
            var maxLat = Double.NEGATIVE_INFINITY
            var maxLng = Double.NEGATIVE_INFINITY
            var pointsFound = 0

            fun consumeCoords(array: kotlinx.serialization.json.JsonArray) {
                // Deep-walk coordinates arrays of unknown depth
                array.forEach { element ->
                    if (element is kotlinx.serialization.json.JsonArray &&
                        element.firstOrNull() is JsonElement &&
                        element.first() is kotlinx.serialization.json.JsonPrimitive &&
                        element.size == 2 &&
                        element[0].jsonPrimitive.isString.not()
                    ) {
                        // Element looks like [lng,lat]
                        val lng = element[0].jsonPrimitive.double
                        val lat = element[1].jsonPrimitive.double
                        minLat = minOf(minLat, lat)
                        maxLat = maxOf(maxLat, lat)
                        minLng = minOf(minLng, lng)
                        maxLng = maxOf(maxLng, lng)
                        pointsFound++
                    } else if (element is kotlinx.serialization.json.JsonArray) {
                        consumeCoords(element)
                    }
                }
            }

            geoJsonDataProvider.getGeoJsonData(event.id)?.let { root ->
                when (root["type"]?.jsonPrimitive?.content) {
                    "FeatureCollection" -> {
                        root["features"]?.jsonArray?.forEach { feature ->
                            feature.jsonObject["geometry"]
                                ?.jsonObject
                                ?.get("coordinates")
                                ?.jsonArray
                                ?.let { consumeCoords(it) }
                        }
                    }
                    "Polygon", "MultiPolygon" -> {
                        root["coordinates"]?.jsonArray?.let { consumeCoords(it) }
                    }
                }
            }

            if (pointsFound > 0) {
                Log.i(
                    ::computeExtentFromGeoJson.name,
                    "${event.id}: Extent computed from GeoJSON [$minLng,$minLat,$maxLng,$maxLat] (points=$pointsFound)",
                )
                BoundingBox.fromCorners(
                    sw = Position(minLat, minLng),
                    ne = Position(maxLat, maxLng),
                )
            } else {
                Log.d(
                    ::computeExtentFromGeoJson.name,
                    "${event.id}: No coordinates found while scanning GeoJSON for extent",
                )
                null
            }
        } catch (e: Exception) {
            Log.w(
                ::computeExtentFromGeoJson.name,
                "${event.id}: Error scanning GeoJSON for extent (${e.message})",
            )
            null
        }

    // ---------------------------

    override fun validationErrors(): List<String>? =
        mutableListOf<String>()
            .apply {
                when {
                    else -> { /* No validation errors */ }
                }
            }.takeIf { it.isNotEmpty() }
            ?.map { "${WWWEventArea::class.simpleName}: $it" }
}
