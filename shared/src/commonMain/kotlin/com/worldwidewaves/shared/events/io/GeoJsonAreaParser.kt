package com.worldwidewaves.shared.events.io

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
import com.worldwidewaves.shared.events.geometry.toPolygon
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.MutableArea
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.utils.Log
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * GeoJSON area parser for event areas.
 *
 * Handles parsing of GeoJSON data for event areas including:
 * - Feature collections
 * - Polygons and MultiPolygons
 * - Ring extraction and validation
 * - Position extraction and constraint
 */
object GeoJsonAreaParser {
    /**
     * Loads polygons from GeoJSON data for the given event.
     */
    suspend fun loadPolygonsFromGeoJson(
        event: IWWWEvent,
        geoJsonDataProvider: GeoJsonDataProvider,
        bboxOverride: String?,
        tempPolygons: MutableArea,
    ) {
        try {
            val geoJsonData = geoJsonDataProvider.getGeoJsonData(event.id)

            if (geoJsonData != null) {
                processGeoJsonData(event.id, geoJsonData, bboxOverride, tempPolygons)
            }
        } catch (e: kotlinx.serialization.SerializationException) {
            Log.w("GeoJsonAreaParser", "GeoJSON parsing error for event ${event.id}: ${e.message}")
            // GeoJSON data loading errors are handled gracefully
        } catch (e: Exception) {
            Log.w("GeoJsonAreaParser", "Error loading GeoJSON for event ${event.id}: ${e.message}")
            // GeoJSON data loading errors are handled gracefully
        }
    }

    private fun processGeoJsonData(
        eventId: String,
        geoJsonData: JsonObject,
        bboxOverride: String?,
        tempPolygons: MutableArea,
    ) {
        val rootType = geoJsonData["type"]?.jsonPrimitive?.content

        when (rootType) {
            "FeatureCollection" -> {
                processFeatureCollection(eventId, geoJsonData, bboxOverride, tempPolygons)
            }
            "Polygon", "MultiPolygon" -> {
                processDirectGeometry(eventId, geoJsonData, bboxOverride, tempPolygons)
            }
        }
    }

    private fun processFeatureCollection(
        eventId: String,
        geoJsonData: JsonObject,
        bboxOverride: String?,
        tempPolygons: MutableArea,
    ) {
        val features = geoJsonData["features"]?.jsonArray
        features?.forEach { feature ->
            try {
                val geometry = feature.jsonObject["geometry"]?.jsonObject
                if (geometry != null) {
                    processGeometry(eventId, geometry, bboxOverride, tempPolygons)
                }
            } catch (e: kotlinx.serialization.SerializationException) {
                Log.v("GeoJsonAreaParser", "GeoJSON feature parsing error: ${e.message}")
                // Feature geometry processing errors are handled gracefully
            } catch (e: Exception) {
                Log.v("GeoJsonAreaParser", "Unexpected feature geometry processing error: ${e.message}")
                // Feature geometry processing errors are handled gracefully
            }
        }
    }

    private fun processDirectGeometry(
        eventId: String,
        geoJsonData: JsonObject,
        bboxOverride: String?,
        tempPolygons: MutableArea,
    ) {
        try {
            processGeometry(eventId, geoJsonData, bboxOverride, tempPolygons)
        } catch (e: kotlinx.serialization.SerializationException) {
            Log.v("GeoJsonAreaParser", "GeoJSON direct geometry parsing error: ${e.message}")
            // Direct geometry processing errors are handled gracefully
        } catch (e: Exception) {
            Log.v("GeoJsonAreaParser", "Unexpected direct geometry processing error: ${e.message}")
            // Direct geometry processing errors are handled gracefully
        }
    }

    private fun processGeometry(
        eventId: String,
        geometry: JsonObject,
        bboxOverride: String?,
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
                            processRing(eventId, ring, bboxOverride, tempPolygons)
                        } catch (e: NumberFormatException) {
                            Log.v("GeoJsonAreaParser", "Invalid numeric data in ring geometry: ${e.message}")
                            // Ignore invalid ring geometry and continue processing
                        } catch (e: kotlinx.serialization.SerializationException) {
                            Log.v("GeoJsonAreaParser", "Ring geometry parsing error: ${e.message}")
                            // Ignore invalid ring geometry and continue processing
                        } catch (e: Exception) {
                            Log.v("GeoJsonAreaParser", "Unexpected ring geometry error: ${e.message}")
                            // Ignore invalid ring geometry and continue processing
                        }
                    }
                }
                // For a MultiPolygon, keep only the first ring (exterior) of each polygon element
                "MultiPolygon" -> {
                    processMultiPolygon(eventId, coordinates, bboxOverride, tempPolygons)
                }
                else -> {
                }
            }
        } catch (e: kotlinx.serialization.SerializationException) {
            Log.v("GeoJsonAreaParser", "Geometry processing parsing error: ${e.message}")
            // Geometry processing errors are handled gracefully
        } catch (e: Exception) {
            Log.v("GeoJsonAreaParser", "Unexpected geometry processing error: ${e.message}")
            // Geometry processing errors are handled gracefully
        }
    }

    private fun processMultiPolygon(
        eventId: String,
        coordinates: kotlinx.serialization.json.JsonArray?,
        bboxOverride: String?,
        tempPolygons: MutableList<Polygon>,
    ) {
        if (coordinates == null) {
            return
        }

        if (coordinates.isEmpty()) {
            return
        }

        coordinates.forEachIndexed { polygonIndex, polygon ->
            try {
                // Verify polygon is a valid JsonArray
                val polygonArray =
                    try {
                        polygon.jsonArray
                    } catch (e: Exception) {
                        Log.w("GeoJsonAreaParser", "Invalid polygon JSON array at index $polygonIndex in MultiPolygon", e)
                        return@forEachIndexed
                    }

                if (polygonArray.isEmpty()) {
                    return@forEachIndexed
                }

                // Each polygon in MultiPolygon has rings (exterior + holes)
                polygonArray.forEachIndexed { ringIndex, ring ->
                    try {
                        // Verify ring is a valid JsonArray
                        val ringArray =
                            try {
                                ring.jsonArray
                            } catch (e: Exception) {
                                Log.w("GeoJsonAreaParser", "Invalid ring JSON array at index $ringIndex in polygon", e)
                                return@forEachIndexed
                            }

                        if (ringArray.isEmpty()) {
                            return@forEachIndexed
                        }

                        processRing(eventId, ring, bboxOverride, tempPolygons)
                    } catch (e: NumberFormatException) {
                        Log.v("GeoJsonAreaParser", "Invalid numeric data in MultiPolygon ring: ${e.message}")
                        // Ring processing errors are handled gracefully
                    } catch (e: kotlinx.serialization.SerializationException) {
                        Log.v("GeoJsonAreaParser", "MultiPolygon ring parsing error: ${e.message}")
                        // Ring processing errors are handled gracefully
                    } catch (e: Exception) {
                        Log.v("GeoJsonAreaParser", "Unexpected MultiPolygon ring error: ${e.message}")
                        // Ring processing errors are handled gracefully
                    }
                }
            } catch (e: NumberFormatException) {
                Log.v("GeoJsonAreaParser", "Invalid numeric data in MultiPolygon: ${e.message}")
                // MultiPolygon processing errors are handled gracefully
            } catch (e: kotlinx.serialization.SerializationException) {
                Log.v("GeoJsonAreaParser", "MultiPolygon parsing error: ${e.message}")
                // MultiPolygon processing errors are handled gracefully
            } catch (e: Exception) {
                Log.v("GeoJsonAreaParser", "Unexpected MultiPolygon processing error: ${e.message}")
                // MultiPolygon processing errors are handled gracefully
            }
        }
    }

    private fun processRing(
        eventId: String,
        ring: JsonElement,
        bboxOverride: String?,
        polygons: MutableArea,
    ) {
        try {
            val ringArray = validateRingArray(ring) ?: return
            val positions = extractPositionsFromRing(ringArray, bboxOverride)

            val hasPositions = positions.isNotEmpty()
            if (!hasPositions) {
                return
            }

            val polygon = createPolygonFromPositions(positions) ?: return

            val isValidPolygon = polygon.size > 1
            if (isValidPolygon) {
                polygons.add(polygon)
            }
        } catch (e: NumberFormatException) {
            Log.v("GeoJsonAreaParser", "Invalid numeric data in ring processing: ${e.message}")
            // Ring processing errors are handled gracefully
        } catch (e: kotlinx.serialization.SerializationException) {
            Log.v("GeoJsonAreaParser", "Ring processing parsing error: ${e.message}")
            // Ring processing errors are handled gracefully
        } catch (e: Exception) {
            Log.v("GeoJsonAreaParser", "Unexpected ring processing error: ${e.message}")
            // Ring processing errors are handled gracefully
        }
    }

    /**
     * Validates that the ring is a non-empty JsonArray
     */
    private fun validateRingArray(ring: JsonElement): kotlinx.serialization.json.JsonArray? {
        val ringArray =
            try {
                ring.jsonArray
            } catch (e: Exception) {
                Log.w("GeoJsonAreaParser", "Invalid ring JSON array in processRing", e)
                return null
            }

        return if (ringArray.isEmpty()) null else ringArray
    }

    /**
     * Extracts positions from ring array
     */
    private fun extractPositionsFromRing(
        ringArray: kotlinx.serialization.json.JsonArray,
        bboxOverride: String?,
    ): List<Position> =
        ringArray
            .mapIndexed { pointIndex, point ->
                extractPositionFromPoint(point, pointIndex, bboxOverride)
            }.filterNotNull()

    /**
     * Extracts a position from a point element
     */
    private fun extractPositionFromPoint(
        point: JsonElement,
        pointIndex: Int,
        bboxOverride: String?,
    ): Position? {
        val pointArray = validatePointArray(point, pointIndex) ?: return null

        val hasValidCoordinates = pointArray.size >= 2
        if (!hasValidCoordinates) {
            return null
        }

        val lng = extractCoordinate(pointArray[0], "longitude", pointIndex) ?: return null
        val lat = extractCoordinate(pointArray[1], "latitude", pointIndex) ?: return null

        return Position(lat, lng).constrainToBoundingBox(bboxOverride)
    }

    /**
     * Validates that the point is a JsonArray
     */
    private fun validatePointArray(
        point: JsonElement,
        pointIndex: Int,
    ): kotlinx.serialization.json.JsonArray? =
        try {
            point.jsonArray
        } catch (e: Exception) {
            Log.w("GeoJsonAreaParser", "Invalid point JSON array at index $pointIndex", e)
            null
        }

    /**
     * Extracts a coordinate value from a JsonPrimitive
     */
    private fun extractCoordinate(
        element: JsonElement,
        coordinateName: String,
        pointIndex: Int,
    ): Double? =
        try {
            element.jsonPrimitive.double
        } catch (e: Exception) {
            Log.w("GeoJsonAreaParser", "Invalid $coordinateName value at point index $pointIndex", e)
            null
        }

    /**
     * Creates a polygon from positions
     */
    private fun createPolygonFromPositions(positions: List<Position>): Polygon? =
        try {
            positions.toPolygon
        } catch (e: Exception) {
            Log.w("GeoJsonAreaParser", "Failed to convert positions to polygon", e)
            null
        }

    /**
     * Constrains a position to the bounding box if bbox override is provided
     */
    private fun Position.constrainToBoundingBox(bboxOverride: String?): Position {
        val bbox = parseBboxString(bboxOverride)
        bbox?.let {
            return Position(
                lat = lat.coerceIn(it.sw.lat, it.ne.lat),
                lng = lng.coerceIn(it.sw.lng, it.ne.lng),
            )
        }

        return this
    }

    /**
     * Parses a bbox string in the format "minLng, minLat, maxLng, maxLat"
     */
    private fun parseBboxString(bbox: String?): BoundingBox? {
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
}
