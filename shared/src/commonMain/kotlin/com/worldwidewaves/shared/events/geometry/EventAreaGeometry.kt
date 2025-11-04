package com.worldwidewaves.shared.events.geometry

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

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.data.GeoJsonDataProvider
import com.worldwidewaves.shared.events.geometry.PolygonOperations.polygonsBbox
import com.worldwidewaves.shared.events.utils.Area
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.utils.Log
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Event area geometry calculations.
 *
 * Handles computation of:
 * - Bounding boxes from various sources
 * - Center positions
 * - Extent calculations from GeoJSON
 */
object EventAreaGeometry {
    // Track events that have already logged GeoJSON parsing warnings to prevent spam
    private val loggedGeoJsonErrors = mutableSetOf<String>()

    private fun shouldLogGeoJsonError(eventId: String): Boolean =
        synchronized(loggedGeoJsonErrors) {
            if (eventId in loggedGeoJsonErrors) {
                false
            } else {
                loggedGeoJsonErrors.add(eventId)
                true
            }
        }

    /**
     * Computes the bounding box for the event area using multiple strategies.
     *
     * Tries in order:
     * 1. Explicit bbox string override
     * 2. Bbox from GeoJSON root
     * 3. Extent computed from GeoJSON coordinates
     * 4. Bbox calculated from loaded polygons
     */
    @Suppress("ReturnCount") // Early returns for guard clauses improve readability
    suspend fun computeBoundingBox(
        event: IWWWEvent,
        bboxOverride: String?,
        geoJsonDataProvider: GeoJsonDataProvider,
        polygons: Area?,
    ): BoundingBox {
        // Try different strategies to get bounding box
        val bboxFromString = parseBboxString(bboxOverride)
        if (bboxFromString != null) {
            return bboxFromString
        }

        val bboxFromGeoJson = parseGeoJsonBbox(event.id, geoJsonDataProvider)
        if (bboxFromGeoJson != null) {
            return bboxFromGeoJson
        }

        val bboxFromExtent = computeExtentFromGeoJson(event.id, geoJsonDataProvider)
        if (bboxFromExtent != null) {
            return bboxFromExtent
        }

        // Fallback: calculate from polygons
        return calculateBboxFromPolygons(polygons)
    }

    /**
     * Parses a bbox string in the format "minLng, minLat, maxLng, maxLat"
     */
    fun parseBboxString(bbox: String?): BoundingBox? {
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
                Log.e("parseBboxString", "Failed to parse bbox string: ${e.message}")
            }
        }
        return null
    }

    /**
     * Calculates bounding box from loaded polygons
     */
    private fun calculateBboxFromPolygons(polygons: Area?): BoundingBox {
        val hasPolygons = polygons?.isNotEmpty() ?: false

        return if (hasPolygons) {
            polygonsBbox(polygons!!)
        } else {
            BoundingBox.fromCorners(Position(0.0, 0.0), Position(0.0, 0.0))
        }
    }

    /**
     * Calculates the center position of the bounding box.
     */
    fun computeCenter(bbox: BoundingBox): Position =
        Position(
            lat = (bbox.ne.lat + bbox.sw.lat) / 2,
            lng = (bbox.ne.lng + bbox.sw.lng) / 2,
        )

    /**
     * Checks if a position is within a bounding box
     */
    fun checkPositionInBoundingBox(
        position: Position,
        boundingBox: BoundingBox,
    ): Boolean =
        position.lat >= boundingBox.sw.lat &&
            position.lat <= boundingBox.ne.lat &&
            position.lng >= boundingBox.sw.lng &&
            position.lng <= boundingBox.ne.lng

    /**
     * Attempt to read a "bbox" array from the GeoJSON root and convert it to BoundingBox.
     * Format expected: [minLng, minLat, maxLng, maxLat].
     */
    private suspend fun parseGeoJsonBbox(
        eventId: String,
        geoJsonDataProvider: GeoJsonDataProvider,
    ): BoundingBox? =
        try {
            geoJsonDataProvider
                .getGeoJsonData(eventId)
                ?.get("bbox")
                ?.jsonArray
                ?.takeIf { it.size >= 4 }
                ?.let { arr ->
                    val minLng = arr[0].jsonPrimitive.double
                    val minLat = arr[1].jsonPrimitive.double
                    val maxLng = arr[2].jsonPrimitive.double
                    val maxLat = arr[3].jsonPrimitive.double

                    Log.i(
                        "parseGeoJsonBbox",
                        "$eventId: Using bbox from GeoJSON [$minLng,$minLat,$maxLng,$maxLat]",
                    )

                    BoundingBox.fromCorners(
                        sw = Position(minLat, minLng),
                        ne = Position(maxLat, maxLng),
                    )
                }
        } catch (e: Exception) {
            // Only log once per event to prevent spam from multiple threads/retries
            if (shouldLogGeoJsonError(eventId)) {
                Log.w(
                    "parseGeoJsonBbox",
                    "$eventId: Malformed or missing bbox in GeoJSON (${e.message})",
                )
            }
            null
        }

    /**
     * Compute an extent by scanning every coordinate pair in the GeoJSON.
     * Useful when the file has no explicit "bbox" property and polygons
     * parsing has not yet happened.
     */
    private suspend fun computeExtentFromGeoJson(
        eventId: String,
        geoJsonDataProvider: GeoJsonDataProvider,
    ): BoundingBox? =
        try {
            val extentAccumulator = ExtentAccumulator()
            processGeoJsonForExtent(eventId, geoJsonDataProvider, extentAccumulator)
            extentAccumulator.createBoundingBox(eventId)
        } catch (e: Exception) {
            // Only log once per event to prevent spam from multiple threads/retries
            if (shouldLogGeoJsonError(eventId)) {
                Log.w(
                    "computeExtentFromGeoJson",
                    "$eventId: Error scanning GeoJSON for extent (${e.message})",
                )
            }
            null
        }

    /**
     * Processes GeoJSON data to compute extent
     */
    private suspend fun processGeoJsonForExtent(
        eventId: String,
        geoJsonDataProvider: GeoJsonDataProvider,
        accumulator: ExtentAccumulator,
    ) {
        geoJsonDataProvider.getGeoJsonData(eventId)?.let { root ->
            when (root["type"]?.jsonPrimitive?.content) {
                "FeatureCollection" -> processFeatureCollectionForExtent(root, accumulator)
                "Polygon", "MultiPolygon" -> processGeometryForExtent(root, accumulator)
            }
        }
    }

    /**
     * Processes FeatureCollection for extent calculation
     */
    private fun processFeatureCollectionForExtent(
        root: JsonObject,
        accumulator: ExtentAccumulator,
    ) {
        root["features"]?.jsonArray?.forEach { feature ->
            feature.jsonObject["geometry"]
                ?.jsonObject
                ?.get("coordinates")
                ?.jsonArray
                ?.let { accumulator.consumeCoords(it) }
        }
    }

    /**
     * Processes direct geometry for extent calculation
     */
    private fun processGeometryForExtent(
        root: JsonObject,
        accumulator: ExtentAccumulator,
    ) {
        root["coordinates"]?.jsonArray?.let { accumulator.consumeCoords(it) }
    }

    /**
     * Accumulator for tracking extent boundaries
     */
    private class ExtentAccumulator {
        var minLat = Double.POSITIVE_INFINITY
        var minLng = Double.POSITIVE_INFINITY
        var maxLat = Double.NEGATIVE_INFINITY
        var maxLng = Double.NEGATIVE_INFINITY
        var pointsFound = 0

        fun consumeCoords(array: kotlinx.serialization.json.JsonArray) {
            // Deep-walk coordinates arrays of unknown depth
            array.forEach { element ->
                val isCoordinatePair = isValidCoordinatePair(element)

                if (isCoordinatePair) {
                    processCoordinatePair(element as kotlinx.serialization.json.JsonArray)
                } else if (element is kotlinx.serialization.json.JsonArray) {
                    consumeCoords(element)
                }
            }
        }

        @Suppress("ReturnCount") // Early returns for guard clauses improve readability
        private fun isValidCoordinatePair(element: JsonElement): Boolean {
            if (element !is kotlinx.serialization.json.JsonArray) {
                return false
            }

            if (element.size != 2) {
                return false
            }

            val firstElement = element.firstOrNull() ?: return false
            if (firstElement !is kotlinx.serialization.json.JsonPrimitive) {
                return false
            }

            if (firstElement.isString) {
                return false
            }

            return true
        }

        private fun processCoordinatePair(element: kotlinx.serialization.json.JsonArray) {
            val lng = element[0].jsonPrimitive.double
            val lat = element[1].jsonPrimitive.double
            minLat = minOf(minLat, lat)
            maxLat = maxOf(maxLat, lat)
            minLng = minOf(minLng, lng)
            maxLng = maxOf(maxLng, lng)
            pointsFound++
        }

        fun createBoundingBox(eventId: String): BoundingBox? =
            if (pointsFound > 0) {
                Log.i(
                    "computeExtentFromGeoJson",
                    "$eventId: Extent computed from GeoJSON [$minLng,$minLat,$maxLng,$maxLat] (points=$pointsFound)",
                )
                BoundingBox.fromCorners(
                    sw = Position(minLat, minLng),
                    ne = Position(maxLat, maxLng),
                )
            } else {
                null
            }
    }
}
