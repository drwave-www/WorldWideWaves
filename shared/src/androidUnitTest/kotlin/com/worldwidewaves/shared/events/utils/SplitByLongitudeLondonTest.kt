package com.worldwidewaves.shared.events.utils

import kotlinx.serialization.json.*
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SplitByLongitudeLondonTest {

    private val jsonParser = Json { ignoreUnknownKeys = true }
    
    private fun loadLondonGeoJson(): JsonElement {
        val projectDir = File(System.getProperty("user.dir"))
        val rootDir = projectDir.parentFile ?: projectDir
        val filePath = File(rootDir, "maps/android/london_england/src/main/assets/london_england.geojson")
        require(filePath.exists()) { "London GeoJSON file not found at $filePath" }
        return jsonParser.parseToJsonElement(filePath.readText())
    }
    
    private fun extractRings(jsonElement: JsonElement): List<List<List<Double>>> {
        val features = jsonElement.jsonObject["features"]?.jsonArray ?: return emptyList()
        val rings = mutableListOf<List<List<Double>>>()
        
        for (feature in features) {
            val geometry = feature.jsonObject["geometry"] ?: continue
            val type = geometry.jsonObject["type"]?.jsonPrimitive?.content
            val coordinates = geometry.jsonObject["coordinates"]?.jsonArray ?: continue
            
            when (type) {
                "Polygon" -> {
                    val polygonRings = coordinates.map { ring ->
                        ring.jsonArray.map { coord ->
                            coord.jsonArray.map { it.jsonPrimitive.double }
                        }
                    }
                    rings.addAll(polygonRings)
                }
                "MultiPolygon" -> {
                    for (polygon in coordinates) {
                        val polygonRings = polygon.jsonArray.map { ring ->
                            ring.jsonArray.map { coord ->
                                coord.jsonArray.map { it.jsonPrimitive.double }
                            }
                        }
                        rings.addAll(polygonRings)
                    }
                }
            }
        }
        return rings
    }
    
    private fun buildPolygon(ring: List<List<Double>>): Polygon {
        val polygon = Polygon()
        for (coord in ring) {
            val lng = coord[0]
            val lat = coord[1]
            polygon.add(Position(lat, lng))
        }
        return polygon
    }
    
    private fun collectUniqueVertexLongitudes(polygon: Polygon): Set<Double> {
        return polygon.map { it.lng }.toSet()
    }
    
    private fun collectUniqueMidSegmentLongitudes(polygon: Polygon): Set<Double> {
        val midLongitudes = mutableSetOf<Double>()
        val points = polygon.toList()
        for (i in 0 until points.size - 1) {
            val midLng = (points[i].lng + points[i + 1].lng) / 2
            midLongitudes.add(midLng)
        }
        if (points.isNotEmpty() && points.size > 1) {
            val midLng = (points.last().lng + points.first().lng) / 2
            midLongitudes.add(midLng)
        }
        return midLongitudes
    }
    
    private fun buildCoordinateSet(polygons: List<Polygon>): Set<Pair<Double, Double>> {
        val coordinates = mutableSetOf<Pair<Double, Double>>()
        for (polygon in polygons) {
            for (position in polygon) {
                coordinates.add(position.lat to position.lng)
            }
        }
        return coordinates
    }
    
    private fun formatCoordinate(lat: Double, lng: Double): String {
        return String.format("%.9f;%.9f", lat, lng)
    }
    
    @Test
    fun testSplitByLongitudeWithLondonGeoJson() {
        val jsonElement = loadLondonGeoJson()
        val rings = extractRings(jsonElement)
        
        for ((ringIndex, ring) in rings.withIndex()) {
            val polygon = buildPolygon(ring)
            
            val bbox = polygon.bbox()
            val lonLeft = bbox.minLongitude - 0.1
            val lonRight = bbox.maxLongitude + 0.1
            
            val vertexLongitudes = collectUniqueVertexLongitudes(polygon)
            val midSegmentLongitudes = collectUniqueMidSegmentLongitudes(polygon)
            
            val allLongitudes = mutableSetOf<Double>()
            allLongitudes.add(lonLeft)
            allLongitudes.add(lonRight)
            allLongitudes.addAll(vertexLongitudes)
            allLongitudes.addAll(midSegmentLongitudes)
            
            for (longitude in allLongitudes) {
                val composedLongitude = ComposedLongitude.fromLongitude(longitude)
                val splitResult = PolygonUtils.splitByLongitude(polygon, longitude)
                
                val leftCoordinates = buildCoordinateSet(splitResult.left)
                val rightCoordinates = buildCoordinateSet(splitResult.right)
                
                // Helpers to know if the splitter produced actual polygons on each side
                val hasLeft  = splitResult.left.isNotEmpty()
                val hasRight = splitResult.right.isNotEmpty()
                
                val positions = polygon.toList()
                for ((idx, position) in positions.withIndex()) {
                    val side = composedLongitude.isPointOnLine(position)
                    val coord = position.lat to position.lng
                    
                    when {
                        side.isWest() -> {
                            assertTrue(leftCoordinates.contains(coord), 
                                "Ring $ringIndex: West vertex (${position.lat}, ${position.lng}) should be in left result for longitude $longitude")
                            assertFalse(rightCoordinates.contains(coord), 
                                "Ring $ringIndex: West vertex (${position.lat}, ${position.lng}) should not be in right result for longitude $longitude")
                        }
                        side.isEast() -> {
                            assertTrue(rightCoordinates.contains(coord), 
                                "Ring $ringIndex: East vertex (${position.lat}, ${position.lng}) should be in right result for longitude $longitude")
                            assertFalse(leftCoordinates.contains(coord), 
                                "Ring $ringIndex: East vertex (${position.lat}, ${position.lng}) should not be in left result for longitude $longitude")
                        }
                        side.isOn() -> {
                            // Inspect neighbours to differentiate crossing vs tangency
                            val prev = positions[(idx - 1 + positions.size) % positions.size]
                            val next = positions[(idx + 1) % positions.size]
                            val prevSide = composedLongitude.isPointOnLine(prev)
                            val nextSide = composedLongitude.isPointOnLine(next)

                            val crossing = (prevSide.isWest() && nextSide.isEast()) ||
                                           (prevSide.isEast() && nextSide.isWest())
                            val tangentWest  = prevSide.isWest() && nextSide.isWest()
                            val tangentEast  = prevSide.isEast() && nextSide.isEast()

                            // Debug
                            println(
                                "ON vertex info -> ring=$ringIndex " +
                                        "lon=$longitude coord=$coord " +
                                        "hasLeft=$hasLeft hasRight=$hasRight " +
                                        "leftHas=${leftCoordinates.contains(coord)} " +
                                        "rightHas=${rightCoordinates.contains(coord)} " +
                                        "prev=${prevSide.name} next=${nextSide.name}"
                            )
                            if (crossing) {
                                if (hasLeft)  assertTrue(leftCoordinates.contains(coord),
                                    "Ring $ringIndex: On-line crossing vertex $coord should be in left")
                                if (hasRight) assertTrue(rightCoordinates.contains(coord),
                                    "Ring $ringIndex: On-line crossing vertex $coord should be in right")
                            } else if (tangentWest) {
                                if (hasLeft)  assertTrue(leftCoordinates.contains(coord),
                                    "Ring $ringIndex: Tangent-west vertex $coord should be in left")
                            } else if (tangentEast) {
                                if (hasRight) assertTrue(rightCoordinates.contains(coord),
                                    "Ring $ringIndex: Tangent-east vertex $coord should be in right")
                            } else {
                                // Fallback – must appear in at least one existing side
                                assertTrue(
                                    (hasLeft  && leftCoordinates.contains(coord)) ||
                                    (hasRight && rightCoordinates.contains(coord)),
                                    "Ring $ringIndex: On-line vertex $coord should appear in at least one side"
                                )
                            }

                            /* Previous logic kept for reference / clarity
                            if (hasLeft) {
                                assertTrue(
                                    leftCoordinates.contains(coord),
                                    "Ring $ringIndex: On-line vertex (${position.lat}, ${position.lng}) should be in left result for longitude $longitude"
                                )
                            }
                            if (hasRight) {
                                assertTrue(
                                    rightCoordinates.contains(coord),
                                    "Ring $ringIndex: On-line vertex (${position.lat}, ${position.lng}) should be in right result for longitude $longitude"
                                )
                            }
                            */
                        }
                    }
                }
                
                if (longitude == lonLeft) {
                    assertTrue(splitResult.left.isEmpty(), 
                        "Ring $ringIndex: Left result should be empty for longitude $longitude (outside left)")
                    assertEquals(1, splitResult.right.size, 
                        "Ring $ringIndex: Right result should contain exactly one polygon for longitude $longitude (outside left)")
                    
                    val originalCoords = buildCoordinateSet(listOf(polygon))
                    val resultCoords = buildCoordinateSet(splitResult.right)
                    assertEquals(originalCoords.size, resultCoords.size, 
                        "Ring $ringIndex: Right result should contain exactly the same number of vertices as original for longitude $longitude (outside left)")
                    assertTrue(originalCoords.containsAll(resultCoords) && resultCoords.containsAll(originalCoords), 
                        "Ring $ringIndex: Right result should contain exactly the same vertices as original for longitude $longitude (outside left)")
                }
                
                if (longitude == lonRight) {
                    assertTrue(splitResult.right.isEmpty(), 
                        "Ring $ringIndex: Right result should be empty for longitude $longitude (outside right)")
                    assertEquals(1, splitResult.left.size, 
                        "Ring $ringIndex: Left result should contain exactly one polygon for longitude $longitude (outside right)")
                    
                    val originalCoords = buildCoordinateSet(listOf(polygon))
                    val resultCoords = buildCoordinateSet(splitResult.left)
                    assertEquals(originalCoords.size, resultCoords.size, 
                        "Ring $ringIndex: Left result should contain exactly the same number of vertices as original for longitude $longitude (outside right)")
                    assertTrue(originalCoords.containsAll(resultCoords) && resultCoords.containsAll(originalCoords), 
                        "Ring $ringIndex: Left result should contain exactly the same vertices as original for longitude $longitude (outside right)")
                }
            }
        }
    }
}
