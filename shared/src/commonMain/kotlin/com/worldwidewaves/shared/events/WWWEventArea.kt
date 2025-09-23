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
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.events.utils.MutableArea
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.PolygonUtils.isPointInPolygons
import com.worldwidewaves.shared.events.utils.PolygonUtils.polygonsBbox
import com.worldwidewaves.shared.events.utils.PolygonUtils.toPolygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.getMapFileAbsolutePath
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

    // ---------------------------

    /**
     * Clears all cached data to force fresh calculations.
     * Useful when debugging or when polygon data has been updated.
     */
    fun clearCache() {
        Log.i("WWWEventArea", "[AREA_DEBUG] clearCache: clearing all cached data for eventId=${event.id}")
        cachedAreaPolygons = null
        cachedBoundingBox = null
        cachedCenter = null
        cachedPositionWithinResult = null
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
        Log.v("WWWEventArea", "[AREA_DEBUG] isPositionWithin: checking position=$position, eventId=${event.id}")

        // Check if the cached result is within the epsilon
        cachedPositionWithinResult?.let { (cachedPosition, cachedResult) ->
            if (isPositionWithinEpsilon(position, cachedPosition)) {
                Log.v("WWWEventArea", "[AREA_DEBUG] Using cached result: $cachedResult for position=$position (cached: $cachedPosition), eventId=${event.id}")
                return cachedResult
            }
        }

        // First, check if the position is within the bounding box (fast check)
        val boundingBox = bbox()
        Log.v("WWWEventArea", "[AREA_DEBUG] Bounding box check: bbox=$boundingBox, position=$position, eventId=${event.id}")

        val isWithinBbox =
            position.lat >= boundingBox.sw.lat &&
                position.lat <= boundingBox.ne.lat &&
                position.lng >= boundingBox.sw.lng &&
                position.lng <= boundingBox.ne.lng

        Log.v("WWWEventArea", "[AREA_DEBUG] Position within bbox: $isWithinBbox, eventId=${event.id}")
        Log.v("WWWEventArea", "[AREA_DEBUG] Bbox details: lat ${position.lat} in [${boundingBox.sw.lat}, ${boundingBox.ne.lat}] = ${position.lat >= boundingBox.sw.lat && position.lat <= boundingBox.ne.lat}, eventId=${event.id}")
        Log.v("WWWEventArea", "[AREA_DEBUG] Bbox details: lng ${position.lng} in [${boundingBox.sw.lng}, ${boundingBox.ne.lng}] = ${position.lng >= boundingBox.sw.lng && position.lng <= boundingBox.ne.lng}, eventId=${event.id}")

        // If not within the bounding box, return false immediately
        if (!isWithinBbox) {
            Log.v("WWWEventArea", "[AREA_DEBUG] Position outside bbox, returning false, eventId=${event.id}")
            // Cache the result
            cachedPositionWithinResult = Pair(position, false)
            return false
        }

        // If within bounding box, check if within polygon (more expensive check)
        val polygons = getPolygons()
        Log.v("WWWEventArea", "[AREA_DEBUG] Position within bbox, checking polygons: count=${polygons.size}, eventId=${event.id}")

        if (polygons.isEmpty()) {
            Log.w("WWWEventArea", "[AREA_DEBUG] No polygons available, returning false, eventId=${event.id}")
            // Cache the result
            cachedPositionWithinResult = Pair(position, false)
            return false
        }

        val result = isPointInPolygons(position, polygons)
        Log.v("WWWEventArea", "[AREA_DEBUG] Polygon containment result: $result for position=$position, eventId=${event.id}")

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
        Log.v("WWWEventArea", "[AREA_DEBUG] bbox: called for eventId=${event.id}")

        // Return cached bounding box if available
        if (cachedBoundingBox != null) {
            Log.v("WWWEventArea", "[AREA_DEBUG] bbox: returning cached bbox=${cachedBoundingBox}, eventId=${event.id}")
            return cachedBoundingBox!!
        }

        Log.v("WWWEventArea", "[AREA_DEBUG] bbox: cache empty, calculating bbox for eventId=${event.id}")

        // If bbox parameter was provided in constructor, use it
        parseBboxString()?.let { bbox ->
            Log.v("WWWEventArea", "[AREA_DEBUG] bbox: using parsed bbox string=$bbox, eventId=${event.id}")
            cachedBoundingBox = bbox
            return bbox
        }

        // Try to extract bbox directly from GeoJSON if available
        parseGeoJsonBbox()?.let { geoBbox ->
            Log.v("WWWEventArea", "[AREA_DEBUG] bbox: using GeoJSON bbox=$geoBbox, eventId=${event.id}")
            cachedBoundingBox = geoBbox
            return geoBbox
        }

        // Fallback: compute extent by scanning every coordinate in the GeoJSON
        computeExtentFromGeoJson()?.let { extentBbox ->
            Log.v("WWWEventArea", "[AREA_DEBUG] bbox: using computed extent bbox=$extentBbox, eventId=${event.id}")
            cachedBoundingBox = extentBbox
            return extentBbox
        }

        // Otherwise calculate from polygons
        Log.v("WWWEventArea", "[AREA_DEBUG] bbox: calculating from polygons for eventId=${event.id}")
        val polygons = getPolygons()
        return polygons
            .takeIf { it.isNotEmpty() }
            ?.let {
                val bbox = polygonsBbox(it)
                Log.v("WWWEventArea", "[AREA_DEBUG] bbox: calculated from ${it.size} polygons: $bbox, eventId=${event.id}")
                cachedBoundingBox = bbox
                bbox
            } ?: run {
            val defaultBbox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(0.0, 0.0))
            Log.w("WWWEventArea", "[AREA_DEBUG] bbox: no polygons available, using default bbox=$defaultBbox, eventId=${event.id}")
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
        Log.v("WWWEventArea", "[AREA_DEBUG] getPolygons: called for eventId=${event.id}")

        // Fast path: if cache is already populated, return immediately
        cachedAreaPolygons?.let {
            Log.v("WWWEventArea", "[AREA_DEBUG] getPolygons: returning cached polygons (count=${it.size}), eventId=${event.id}")
            return it
        }

        Log.v("WWWEventArea", "[AREA_DEBUG] getPolygons: cache empty, loading polygons for eventId=${event.id}")

        // Slow path: populate cache with mutex protection
        polygonsCacheMutex.withLock {
            // Double-check pattern: another coroutine might have populated the cache
            cachedAreaPolygons?.let {
                Log.v("WWWEventArea", "[AREA_DEBUG] getPolygons: cache populated by another coroutine (count=${it.size}), eventId=${event.id}")
                return it
            }

            Log.v("WWWEventArea", "[AREA_DEBUG] getPolygons: acquiring mutex, building polygons for eventId=${event.id}")

            // Build polygons in a temporary mutable list
            val tempPolygons: MutableArea = mutableListOf()

            try {
                coroutineScopeProvider.withDefaultContext {
                    Log.v("WWWEventArea", "[AREA_DEBUG] getPolygons: entering coroutine context for eventId=${event.id}")
                    loadPolygonsFromGeoJson(tempPolygons)
                    Log.v("WWWEventArea", "[AREA_DEBUG] getPolygons: exiting coroutine context for eventId=${event.id}")
                }
            } catch (e: Exception) {
                Log.e("WWWEventArea", "[AREA_DEBUG] getPolygons: ${event.id}: Exception in withDefaultContext: ${e.message}", e)
            }

            // Atomically assign the complete immutable list
            cachedAreaPolygons =
                tempPolygons.toList().also {
                    Log.i("WWWEventArea", "[AREA_DEBUG] getPolygons: ${event.id}: Built ${it.size} polygons")
                    it.forEachIndexed { index, polygon ->
                        Log.v("WWWEventArea", "[AREA_DEBUG] getPolygons: polygon $index has ${polygon.size} vertices, bbox=${polygon.bbox()}, eventId=${event.id}")
                        // Log first few vertices for debugging
                        if (polygon.isNotEmpty()) {
                            val vertices = polygon.toList()
                            Log.v("WWWEventArea", "[AREA_DEBUG] getPolygons: polygon $index first vertices: ${vertices.take(3)}, eventId=${event.id}")
                        }
                    }
                }
        }

        val result = cachedAreaPolygons ?: emptyList()
        Log.v("WWWEventArea", "[AREA_DEBUG] getPolygons: returning ${result.size} polygons for eventId=${event.id}")
        return result
    }

    private suspend fun loadPolygonsFromGeoJson(tempPolygons: MutableArea) {
        try {
            Log.v("WWWEventArea", "[AREA_DEBUG] loadPolygonsFromGeoJson: entry - getting GeoJSON data for eventId=${event.id}")
            val geoJsonData = geoJsonDataProvider.getGeoJsonData(event.id)

            if (geoJsonData != null) {
                Log.v("WWWEventArea", "[AREA_DEBUG] loadPolygonsFromGeoJson: GeoJSON data loaded successfully for eventId=${event.id}")
                val startPolygonCount = tempPolygons.size
                processGeoJsonData(geoJsonData, tempPolygons)
                val endPolygonCount = tempPolygons.size
                Log.v("WWWEventArea", "[AREA_DEBUG] loadPolygonsFromGeoJson: processGeoJsonData completed, polygons added: ${endPolygonCount - startPolygonCount}, total: $endPolygonCount, eventId=${event.id}")
            } else {
                Log.e("WWWEventArea", "[AREA_DEBUG] loadPolygonsFromGeoJson: ${event.id}: GeoJSON data is null - geojson file might not exist or failed to load")
            }

            Log.v("WWWEventArea", "[AREA_DEBUG] loadPolygonsFromGeoJson: completed - total polygons: ${tempPolygons.size}, eventId=${event.id}")
        } catch (e: Exception) {
            Log.e("WWWEventArea", "[AREA_DEBUG] loadPolygonsFromGeoJson: ${event.id}: Exception in coroutine context: ${e.message}", e)
        }
    }

    private fun processGeoJsonData(geoJsonData: JsonObject, tempPolygons: MutableArea) {
        Log.v("WWWEventArea", "[AREA_DEBUG] processGeoJsonData: entry - processing GeoJSON data for eventId=${event.id}")

        val rootType = geoJsonData["type"]?.jsonPrimitive?.content
        // Now that we know the type, log it for diagnostics
        Log.i("WWWEventArea", "[AREA_DEBUG] processGeoJsonData: ${event.id}: GeoJSON rootType=$rootType")
        Log.v("WWWEventArea", "[AREA_DEBUG] processGeoJsonData: GeoJSON keys: ${geoJsonData.keys}, eventId=${event.id}")

        val startPolygonCount = tempPolygons.size

        when (rootType) {
            "FeatureCollection" -> {
                Log.v("WWWEventArea", "[AREA_DEBUG] processGeoJsonData: processing as FeatureCollection, eventId=${event.id}")
                processFeatureCollection(geoJsonData, tempPolygons)
            }
            "Polygon", "MultiPolygon" -> {
                Log.v("WWWEventArea", "[AREA_DEBUG] processGeoJsonData: processing as direct geometry ($rootType), eventId=${event.id}")
                processDirectGeometry(geoJsonData, tempPolygons)
            }
            else -> {
                Log.e("WWWEventArea", "[AREA_DEBUG] processGeoJsonData: ${event.id}: Unsupported GeoJSON type: $rootType")
            }
        }

        val endPolygonCount = tempPolygons.size
        Log.v("WWWEventArea", "[AREA_DEBUG] processGeoJsonData: completed - polygons added: ${endPolygonCount - startPolygonCount}, total: $endPolygonCount, eventId=${event.id}")
    }

    private fun processFeatureCollection(geoJsonData: JsonObject, tempPolygons: MutableArea) {
        val features = geoJsonData["features"]?.jsonArray
        Log.v("WWWEventArea", "[AREA_DEBUG] processFeatureCollection: processing FeatureCollection with ${features?.size} features, eventId=${event.id}")
        features?.forEach { feature ->
            try {
                val geometry = feature.jsonObject["geometry"]?.jsonObject
                if (geometry != null) {
                    Log.v("WWWEventArea", "[AREA_DEBUG] processFeatureCollection: processing feature geometry for eventId=${event.id}")
                    processGeometry(geometry, tempPolygons)
                } else {
                    Log.w("WWWEventArea", "[AREA_DEBUG] processFeatureCollection: feature has null geometry for eventId=${event.id}")
                }
            } catch (e: Exception) {
                Log.e("WWWEventArea", "[AREA_DEBUG] processFeatureCollection: error processing feature for eventId=${event.id}: ${e.message}", e)
            }
        }
    }

    private fun processDirectGeometry(geoJsonData: JsonObject, tempPolygons: MutableArea) {
        val rootType = geoJsonData["type"]?.jsonPrimitive?.content
        Log.v("WWWEventArea", "[AREA_DEBUG] processDirectGeometry: entry - processing direct geometry type=$rootType, eventId=${event.id}")

        val startPolygonCount = tempPolygons.size

        try {
            processGeometry(geoJsonData, tempPolygons)
            val endPolygonCount = tempPolygons.size
            Log.v("WWWEventArea", "[AREA_DEBUG] processDirectGeometry: processGeometry completed, polygons added: ${endPolygonCount - startPolygonCount}, total: $endPolygonCount, eventId=${event.id}")
        } catch (e: Exception) {
            Log.e("WWWEventArea", "[AREA_DEBUG] processDirectGeometry: error processing direct geometry for eventId=${event.id}: ${e.message}", e)
        }

        Log.v("WWWEventArea", "[AREA_DEBUG] processDirectGeometry: completed successfully for eventId=${event.id}")
    }

    private fun processGeometry(
        geometry: JsonObject,
        tempPolygons: MutableList<Polygon>,
    ) {
        try {
            val type = geometry["type"]?.jsonPrimitive?.content
            val coordinates = geometry["coordinates"]?.jsonArray

            Log.v("WWWEventArea", "[AREA_DEBUG] processGeometry: type=$type, coordinates size=${coordinates?.size}, eventId=${event.id}")

            when (type) {
                // For a Polygon we add every ring (first is exterior, others holes are ignored downstream)
                "Polygon" -> {
                    Log.v("WWWEventArea", "[AREA_DEBUG] processGeometry: processing Polygon with ${coordinates?.size} rings, eventId=${event.id}")
                    coordinates?.forEachIndexed { ringIndex, ring ->
                        try {
                            Log.v("WWWEventArea", "[AREA_DEBUG] processGeometry: processing Polygon ring $ringIndex, eventId=${event.id}")
                            processRing(ring, tempPolygons)
                        } catch (e: Exception) {
                            Log.e("WWWEventArea", "[AREA_DEBUG] processGeometry: error processing Polygon ring $ringIndex for eventId=${event.id}: ${e.message}", e)
                        }
                    }
                }
                // For a MultiPolygon, keep only the first ring (exterior) of each polygon element
                "MultiPolygon" -> {
                    Log.v("WWWEventArea", "[AREA_DEBUG] processGeometry: processing MultiPolygon with ${coordinates?.size} polygons, eventId=${event.id}")
                    processMultiPolygon(coordinates, tempPolygons)
                }
                else -> {
                    Log.e("WWWEventArea", "[AREA_DEBUG] processGeometry: unsupported geometry type: $type for eventId=${event.id}")
                }
            }

            // Lightweight diagnostics
            val ringsCount =
                when (type) {
                    "Polygon" -> coordinates?.size ?: 0
                    "MultiPolygon" -> coordinates?.sumOf { it.jsonArray.size } ?: 0
                    else -> 0
                }
            Log.d("WWWEventArea", "[AREA_DEBUG] processGeometry: ${event.id}: processed geometry type=$type rings=$ringsCount")
        } catch (e: Exception) {
            Log.e("WWWEventArea", "[AREA_DEBUG] processGeometry: error processing geometry for eventId=${event.id}: ${e.message}", e)
        }
    }

    private fun processMultiPolygon(
        coordinates: kotlinx.serialization.json.JsonArray?,
        tempPolygons: MutableList<Polygon>,
    ) {
        if (coordinates == null) {
            Log.e("WWWEventArea", "[AREA_DEBUG] processMultiPolygon: MultiPolygon coordinates is null, eventId=${event.id}")
            return
        }

        if (coordinates.size == 0) {
            Log.w("WWWEventArea", "[AREA_DEBUG] processMultiPolygon: MultiPolygon has zero polygons, eventId=${event.id}")
            return
        }

        coordinates.forEachIndexed { polygonIndex, polygon ->
            try {
                Log.v("WWWEventArea", "[AREA_DEBUG] processMultiPolygon: starting MultiPolygon polygon $polygonIndex processing, eventId=${event.id}")

                // Verify polygon is a valid JsonArray
                val polygonArray = try {
                    polygon.jsonArray
                } catch (e: Exception) {
                    Log.e("WWWEventArea", "[AREA_DEBUG] processMultiPolygon: polygon $polygonIndex is not a valid JsonArray for eventId=${event.id}: ${e.message}", e)
                    return@forEachIndexed
                }

                Log.v("WWWEventArea", "[AREA_DEBUG] processMultiPolygon: polygon $polygonIndex has ${polygonArray.size} rings, eventId=${event.id}")

                if (polygonArray.isEmpty()) {
                    Log.w("WWWEventArea", "[AREA_DEBUG] processMultiPolygon: polygon $polygonIndex has zero rings, skipping, eventId=${event.id}")
                    return@forEachIndexed
                }

                // Each polygon in MultiPolygon has rings (exterior + holes)
                polygonArray.forEachIndexed { ringIndex, ring ->
                    try {
                        Log.v("WWWEventArea", "[AREA_DEBUG] processMultiPolygon: starting polygon $polygonIndex ring $ringIndex processing, eventId=${event.id}")

                        // Verify ring is a valid JsonArray
                        val ringArray = try {
                            ring.jsonArray
                        } catch (e: Exception) {
                            Log.e("WWWEventArea", "[AREA_DEBUG] processMultiPolygon: polygon $polygonIndex ring $ringIndex is not a valid JsonArray for eventId=${event.id}: ${e.message}", e)
                            return@forEachIndexed
                        }

                        Log.v("WWWEventArea", "[AREA_DEBUG] processMultiPolygon: polygon $polygonIndex ring $ringIndex has ${ringArray.size} vertices, eventId=${event.id}")

                        if (ringArray.isEmpty()) {
                            Log.w("WWWEventArea", "[AREA_DEBUG] processMultiPolygon: polygon $polygonIndex ring $ringIndex has zero vertices, skipping, eventId=${event.id}")
                            return@forEachIndexed
                        }

                        Log.v("WWWEventArea", "[AREA_DEBUG] processMultiPolygon: calling processRing for polygon $polygonIndex ring $ringIndex, eventId=${event.id}")
                        val ringStartPolygonCount = tempPolygons.size
                        processRing(ring, tempPolygons)
                        val ringEndPolygonCount = tempPolygons.size
                        Log.v("WWWEventArea", "[AREA_DEBUG] processMultiPolygon: processRing completed for polygon $polygonIndex ring $ringIndex, polygons added: ${ringEndPolygonCount - ringStartPolygonCount}, eventId=${event.id}")
                    } catch (e: Exception) {
                        Log.e("WWWEventArea", "[AREA_DEBUG] processMultiPolygon: error processing MultiPolygon polygon $polygonIndex ring $ringIndex for eventId=${event.id}: ${e.message}", e)
                    }
                }

                Log.v("WWWEventArea", "[AREA_DEBUG] processMultiPolygon: completed MultiPolygon polygon $polygonIndex processing, eventId=${event.id}")
            } catch (e: Exception) {
                Log.e("WWWEventArea", "[AREA_DEBUG] processMultiPolygon: error processing MultiPolygon polygon $polygonIndex for eventId=${event.id}: ${e.message}", e)
            }
        }

        Log.v("WWWEventArea", "[AREA_DEBUG] processMultiPolygon: completed MultiPolygon processing with ${tempPolygons.size} total polygons, eventId=${event.id}")
    }

    private fun processRing(
        ring: JsonElement,
        polygons: MutableArea,
    ) {
        try {
            Log.v("WWWEventArea", "[AREA_DEBUG] processRing: entry - processing ring, eventId=${event.id}")

            // Verify ring is a JsonArray
            val ringArray = try {
                ring.jsonArray
            } catch (e: Exception) {
                Log.e("WWWEventArea", "[AREA_DEBUG] processRing: ring is not a valid JsonArray for eventId=${event.id}: ${e.message}", e)
                return
            }

            Log.v("WWWEventArea", "[AREA_DEBUG] processRing: ring has ${ringArray.size} points, eventId=${event.id}")

            if (ringArray.isEmpty()) {
                Log.w("WWWEventArea", "[AREA_DEBUG] processRing: ring has zero points, returning, eventId=${event.id}")
                return
            }

            Log.v("WWWEventArea", "[AREA_DEBUG] processRing: starting coordinate conversion, eventId=${event.id}")

            val positions =
                ringArray.mapIndexed { pointIndex, point ->
                    try {
                        Log.v("WWWEventArea", "[AREA_DEBUG] processRing: processing point $pointIndex, eventId=${event.id}")

                        val pointArray = try {
                            point.jsonArray
                        } catch (e: Exception) {
                            Log.e("WWWEventArea", "[AREA_DEBUG] processRing: point $pointIndex is not a valid JsonArray for eventId=${event.id}: ${e.message}", e)
                            return@mapIndexed null
                        }

                        if (pointArray.size >= 2) {
                            val lng = try {
                                pointArray[0].jsonPrimitive.double
                            } catch (e: Exception) {
                                Log.e("WWWEventArea", "[AREA_DEBUG] processRing: point $pointIndex longitude parsing failed for eventId=${event.id}: ${e.message}", e)
                                return@mapIndexed null
                            }

                            val lat = try {
                                pointArray[1].jsonPrimitive.double
                            } catch (e: Exception) {
                                Log.e("WWWEventArea", "[AREA_DEBUG] processRing: point $pointIndex latitude parsing failed for eventId=${event.id}: ${e.message}", e)
                                return@mapIndexed null
                            }

                            Log.v("WWWEventArea", "[AREA_DEBUG] processRing: point $pointIndex: [$lng, $lat], eventId=${event.id}")
                            Position(lat, lng).constrainToBoundingBox()
                        } else {
                            Log.e("WWWEventArea", "[AREA_DEBUG] processRing: point $pointIndex has insufficient coordinates (${pointArray.size}), eventId=${event.id}")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("WWWEventArea", "[AREA_DEBUG] processRing: error processing point $pointIndex for eventId=${event.id}: ${e.message}", e)
                        null
                    }
                }.filterNotNull()

            Log.v("WWWEventArea", "[AREA_DEBUG] processRing: coordinate conversion completed - converted ${positions.size} valid positions from ${ringArray.size} points, eventId=${event.id}")

            if (positions.isEmpty()) {
                Log.w("WWWEventArea", "[AREA_DEBUG] processRing: no valid positions found in ring, returning, eventId=${event.id}")
                return
            }

            Log.v("WWWEventArea", "[AREA_DEBUG] processRing: creating polygon from ${positions.size} positions, eventId=${event.id}")

            val polygon = try {
                positions.toPolygon
            } catch (e: Exception) {
                Log.e("WWWEventArea", "[AREA_DEBUG] processRing: polygon creation failed for eventId=${event.id}: ${e.message}", e)
                return
            }

            Log.v("WWWEventArea", "[AREA_DEBUG] processRing: polygon created with ${polygon.size} vertices, eventId=${event.id}")

            if (polygon.size > 1) {
                Log.v("WWWEventArea", "[AREA_DEBUG] processRing: adding polygon with ${polygon.size} vertices to collection, eventId=${event.id}")
                polygons.add(polygon)
                Log.v("WWWEventArea", "[AREA_DEBUG] processRing: polygon added successfully, total polygons now: ${polygons.size}, eventId=${event.id}")
            } else {
                Log.w("WWWEventArea", "[AREA_DEBUG] processRing: polygon has too few vertices (${polygon.size}), skipping, eventId=${event.id}")
            }

            Log.v("WWWEventArea", "[AREA_DEBUG] processRing: completed successfully, eventId=${event.id}")
        } catch (e: Exception) {
            Log.e("WWWEventArea", "[AREA_DEBUG] processRing: error processing ring for eventId=${event.id}: ${e.message}", e)
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
