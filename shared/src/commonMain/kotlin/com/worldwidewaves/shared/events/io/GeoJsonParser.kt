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

import com.worldwidewaves.shared.events.utils.Area

/**
 * GeoJSON Parser Module
 *
 * This file provides GeoJSON serialization and parsing capabilities for
 * polygon data used in WorldWideWaves events.
 *
 * **Supported Formats**:
 * - GeoJSON FeatureCollection with Polygon geometries
 * - RFC 7946 compliant coordinate ordering (longitude, latitude)
 * - Multi-polygon representations for complex event areas
 *
 * **Current Limitations**:
 * - Export only (serialization to GeoJSON)
 * - No parsing of GeoJSON strings (future enhancement)
 * - No support for MultiPolygon, LineString, or Point geometries
 * - No property/metadata support in features
 *
 * **Use Cases**:
 * - Debugging polygon split operations
 * - Exporting event areas for visualization tools
 * - Integration with external GIS systems
 * - Testing and validation
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7946">RFC 7946: The GeoJSON Format</a>
 */

object GeoJsonParser {
    /**
     * Converts a collection of polygons to GeoJSON FeatureCollection format.
     *
     * **Output Format**:
     * ```json
     * {
     *   "type": "FeatureCollection",
     *   "features": [
     *     {
     *       "type": "Feature",
     *       "geometry": {
     *         "type": "Polygon",
     *         "coordinates": [[[lng1, lat1], [lng2, lat2], ...]]
     *       }
     *     },
     *     ...
     *   ]
     * }
     * ```
     *
     * **Coordinate Ordering**:
     * GeoJSON uses [longitude, latitude] ordering (x, y), which is the
     * OPPOSITE of common (latitude, longitude) geographic notation.
     * This function correctly swaps the coordinates.
     *
     * **Algorithm**: Simple transformation with string building
     * - Iterate through each polygon
     * - Map each Position to [lng, lat] array
     * - Wrap in GeoJSON Feature structure
     * - Combine into FeatureCollection
     *
     * **Time Complexity**: O(n × m) where n = polygons, m = avg vertices
     * **Space Complexity**: O(n × m) for JSON string construction
     *
     * **GeoJSON Compliance**:
     * - Uses correct coordinate ordering ([lng, lat])
     * - Valid FeatureCollection structure
     * - Polygon type for each geometry
     * - Implicit CRS: WGS84 (EPSG:4326) per RFC 7946 default
     *
     * **Example Output**:
     * ```json
     * {
     *   "type": "FeatureCollection",
     *   "features": [
     *     {
     *       "type": "Feature",
     *       "geometry": {
     *         "type": "Polygon",
     *         "coordinates": [
     *           [[-0.1, 51.5], [-0.2, 51.5], [-0.2, 51.6], [-0.1, 51.6], [-0.1, 51.5]]
     *         ]
     *       }
     *     }
     *   ]
     * }
     * ```
     *
     * **Known Limitations**:
     * - No polygon ring validation (exterior vs holes)
     * - No winding order enforcement (should be counter-clockwise for exterior)
     * - No coordinate precision control
     * - No pretty-printing or indentation options
     * - All polygons treated as simple (no holes)
     *
     * **Future Enhancements**:
     * - Parse GeoJSON strings back to polygons
     * - Support for polygon holes (multiple rings)
     * - MultiPolygon geometry type
     * - Feature properties (event metadata)
     * - Coordinate precision control
     * - Schema validation
     *
     * @param polygons The area (collection of polygons) to convert
     * @return GeoJSON string representation as a FeatureCollection
     *
     * **Use Cases**:
     * - Debugging polygon splits: `GeoJsonParser.convertPolygonsToGeoJson(splitResult.left)`
     * - Export for QGIS/ArcGIS: Save output to .geojson file
     * - Mapbox/Leaflet visualization: Use as map layer source
     * - Unit testing: Compare expected vs actual polygon geometry
     */
    fun convertPolygonsToGeoJson(polygons: Area): String {
        val features =
            polygons.map { polygon ->
                val coordinates = polygon.map { listOf(it.lng, it.lat) }
                """
                {
                    "type": "Feature",
                    "geometry": {
                        "type": "Polygon",
                        "coordinates": [$coordinates]
                    }
                }
                """.trimIndent()
            }
        return """
            {
                "type": "FeatureCollection",
                "features": [${features.joinToString(",")}]
            }
            """.trimIndent()
    }
}
