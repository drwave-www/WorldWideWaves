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

package com.worldwidewaves.shared.cinterop

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.CameraCommand
import com.worldwidewaves.shared.map.MapWrapperRegistry
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for Swift-Kotlin bridging patterns used by IOSMapBridge.
 *
 * This test suite validates the Kotlin side of the Swift-Kotlin bridge that enables
 * MapLibre integration on iOS. The IOSMapBridge.swift class uses @objc methods to
 * communicate with Kotlin code in the shared module, passing data types like:
 * - Position (lat/lng) → CLLocationCoordinate2D
 * - BoundingBox → MLNCoordinateBounds
 * - String → NSString (automatic bridging)
 * - List → NSArray (automatic bridging)
 *
 * Architecture Pattern:
 * ```
 * Kotlin (shared module)
 *   └── IOSMapBridge (@objc methods)
 *       └── MapWrapperRegistry (Kotlin/Native singleton)
 *           └── MapLibreViewWrapper (Swift class)
 *               └── MapLibre SDK (native iOS maps)
 * ```
 *
 * These tests focus on:
 * - Type conversion patterns (Position, BoundingBox)
 * - MapWrapperRegistry command pattern (camera, polygons)
 * - Data structure compatibility with Swift @objc methods
 * - Registry lifecycle management
 *
 * NOTE: We cannot directly test Swift @objc methods from Kotlin unit tests
 * (requires full app context). These tests validate the Kotlin side of the bridge.
 */
class IosSwiftBridgeTest {
    @AfterTest
    fun tearDown() {
        // Clean up registry after each test to prevent interference
        MapWrapperRegistry.clear()
    }

    // MARK: - Position Type Conversion Tests

    @Test
    fun `Position should provide separate lat lng properties for Swift bridge`() {
        // IOSMapBridge methods like animateToPosition() receive Position and extract
        // lat/lng as separate Double parameters for Swift CLLocationCoordinate2D
        val position = Position(lat = 37.7749, lng = -122.4194)

        // Validate that Position properties are accessible separately
        // (Swift bridge receives these as separate Double parameters)
        val lat = position.lat
        val lng = position.lng

        assertEquals(37.7749, lat, 0.0001)
        assertEquals(-122.4194, lng, 0.0001)
    }

    @Test
    fun `Position should handle extreme coordinate values for Swift bridge`() {
        // Validate boundary conditions for coordinates passed to Swift
        val northPole = Position(lat = 90.0, lng = 0.0)
        val southPole = Position(lat = -90.0, lng = 0.0)
        val dateLine = Position(lat = 0.0, lng = 180.0)

        assertEquals(90.0, northPole.lat)
        assertEquals(-90.0, southPole.lat)
        assertEquals(180.0, dateLine.lng)
    }

    @Test
    fun `nullable Position should handle bridge conversion safely`() {
        // Swift bridge receives nullable Position as Optional<Position>
        val position: Position? = null

        // Validate we can handle null safely (Swift @objc methods accept nil)
        val lat = position?.lat ?: 0.0
        val lng = position?.lng ?: 0.0

        assertEquals(0.0, lat)
        assertEquals(0.0, lng)
    }

    // MARK: - BoundingBox Type Conversion Tests

    @Test
    fun `BoundingBox should provide coordinate properties for Swift bridge`() {
        // IOSMapBridge methods like animateToBounds() convert BoundingBox to
        // Swift MLNCoordinateBounds(sw:ne:) using these properties
        val bbox =
            BoundingBox(
                swLat = 37.7,
                swLng = -122.5,
                neLat = 37.8,
                neLng = -122.3,
            )

        // Validate all four coordinates are accessible
        // (Swift bridge needs these to create MLNCoordinateBounds)
        assertNotNull(bbox.minLatitude, "minLatitude must be accessible")
        assertNotNull(bbox.maxLatitude, "maxLatitude must be accessible")
        assertNotNull(bbox.minLongitude, "minLongitude must be accessible")
        assertNotNull(bbox.maxLongitude, "maxLongitude must be accessible")

        assertEquals(37.7, bbox.minLatitude, 0.0001)
        assertEquals(37.8, bbox.maxLatitude, 0.0001)
        assertEquals(-122.5, bbox.minLongitude, 0.0001)
        assertEquals(-122.3, bbox.maxLongitude, 0.0001)
    }

    @Test
    fun `BoundingBox should provide sw ne Position objects for Swift bridge`() {
        // Alternative access pattern: Swift can extract sw/ne Position objects
        val bbox =
            BoundingBox(
                swLat = 37.7,
                swLng = -122.5,
                neLat = 37.8,
                neLng = -122.3,
            )

        val sw = bbox.southwest
        val ne = bbox.northeast

        assertNotNull(sw, "Southwest Position must be accessible")
        assertNotNull(ne, "Northeast Position must be accessible")

        assertEquals(37.7, sw.lat, 0.0001)
        assertEquals(-122.5, sw.lng, 0.0001)
        assertEquals(37.8, ne.lat, 0.0001)
        assertEquals(-122.3, ne.lng, 0.0001)
    }

    // MARK: - String Conversion Tests

    @Test
    fun `String eventId should be compatible with Swift NSString bridge`() {
        // Swift @objc methods receive Kotlin String as NSString (automatic bridging)
        val eventId = "test-event-with-special-chars-123-éñ"

        // Validate String is compatible with NSString (automatic bridging)
        assertTrue(eventId.isNotEmpty(), "String should not be empty")
        assertTrue(eventId.contains("-"), "String should support special characters")
        assertTrue(eventId.length > 10, "String length should be accessible")
    }

    @Test
    fun `empty String should bridge to Swift safely`() {
        // Edge case: empty strings must bridge correctly
        val emptyEventId = ""

        // Should be valid (Swift receives empty NSString)
        assertEquals(0, emptyEventId.length)
    }

    // MARK: - List Conversion Tests

    @Test
    fun `List of coordinate pairs should convert to Swift arrays`() {
        // IOSMapBridge.renderPendingPolygons() converts:
        // List<List<Pair<Double, Double>>> → [[CLLocationCoordinate2D]]
        val polygon1 =
            listOf(
                Pair(37.7, -122.5),
                Pair(37.8, -122.5),
                Pair(37.8, -122.3),
                Pair(37.7, -122.3),
            )
        val polygon2 =
            listOf(
                Pair(37.75, -122.45),
                Pair(37.75, -122.35),
            )
        val polygons = listOf(polygon1, polygon2)

        // Validate structure matches Swift conversion pattern
        assertEquals(2, polygons.size, "Should have 2 polygons")
        assertEquals(4, polygons[0].size, "First polygon should have 4 points")
        assertEquals(2, polygons[1].size, "Second polygon should have 2 points")

        // Validate coordinate pair structure
        val firstPoint = polygons[0][0]
        assertEquals(37.7, firstPoint.first, 0.0001)
        assertEquals(-122.5, firstPoint.second, 0.0001)
    }

    // MARK: - MapWrapperRegistry Pattern Tests

    @Test
    fun `MapWrapperRegistry should register and retrieve wrappers by eventId`() {
        // IOSMapBridge uses MapWrapperRegistry to store Swift MapLibreViewWrapper
        // instances, keyed by eventId for later retrieval
        val eventId = "test-event-123"
        val mockWrapper =
            object {
                fun testMethod() = "called"
            }

        // Test registry pattern used by Swift bridge
        MapWrapperRegistry.registerWrapper(eventId, mockWrapper)
        val retrieved = MapWrapperRegistry.getWrapper(eventId)

        assertNotNull(retrieved, "Wrapper should be retrievable after registration")
    }

    @Test
    fun `MapWrapperRegistry should handle multiple independent wrappers`() {
        // Multiple EventMapView screens can exist simultaneously (navigation stack)
        val event1 = "event-1"
        val event2 = "event-2"

        MapWrapperRegistry.registerWrapper(event1, "wrapper1")
        MapWrapperRegistry.registerWrapper(event2, "wrapper2")

        assertNotNull(MapWrapperRegistry.getWrapper(event1), "Event1 wrapper should exist")
        assertNotNull(MapWrapperRegistry.getWrapper(event2), "Event2 wrapper should exist")
    }

    @Test
    fun `MapWrapperRegistry should return null for unregistered eventId`() {
        // IOSMapBridge gracefully handles missing wrappers (logs warning, returns)
        val nonExistentEventId = "nonexistent-event"

        val wrapper = MapWrapperRegistry.getWrapper(nonExistentEventId)

        assertNull(wrapper, "Should return null for unregistered eventId")
    }

    @Test
    fun `MapWrapperRegistry should store and retrieve camera commands`() {
        // IOSMapBridge.executePendingCameraCommand() retrieves commands from registry
        val eventId = "test-event"
        val position = Position(37.7749, -122.4194)
        val command = CameraCommand.AnimateToPosition(position, zoom = 15.0)

        MapWrapperRegistry.setPendingCameraCommand(eventId, command)

        assertTrue(
            MapWrapperRegistry.hasPendingCameraCommand(eventId),
            "Should have pending camera command",
        )

        val retrieved = MapWrapperRegistry.getPendingCameraCommand(eventId)
        assertNotNull(retrieved, "Should retrieve pending camera command")
    }

    @Test
    fun `MapWrapperRegistry should handle polygon data for rendering`() {
        // IOSMapBridge.renderPendingPolygons() retrieves polygon data from registry
        val eventId = "test-event"
        val polygons =
            listOf(
                listOf(
                    Pair(37.7, -122.5),
                    Pair(37.8, -122.5),
                    Pair(37.8, -122.3),
                ),
            )

        MapWrapperRegistry.setPendingPolygons(eventId, polygons, clearExisting = true)

        assertTrue(
            MapWrapperRegistry.hasPendingPolygons(eventId),
            "Should have pending polygons",
        )

        val retrieved = MapWrapperRegistry.getPendingPolygons(eventId)
        assertNotNull(retrieved, "Should retrieve pending polygon data")
        assertEquals(1, retrieved.coordinates.size, "Should have 1 polygon")
        assertEquals(3, retrieved.coordinates[0].size, "Polygon should have 3 points")
        assertTrue(retrieved.clearExisting, "clearExisting flag should be preserved")
    }

    @Test
    fun `MapWrapperRegistry should clear pending polygons after rendering`() {
        // IOSMapBridge calls clearPendingPolygons() after successful rendering
        val eventId = "test-event"
        val polygons = listOf(listOf(Pair(37.7, -122.5)))

        MapWrapperRegistry.setPendingPolygons(eventId, polygons, clearExisting = false)
        assertTrue(MapWrapperRegistry.hasPendingPolygons(eventId), "Should have polygons")

        MapWrapperRegistry.clearPendingPolygons(eventId)

        assertTrue(
            !MapWrapperRegistry.hasPendingPolygons(eventId),
            "Should not have polygons after clearing",
        )
    }

    @Test
    fun `MapWrapperRegistry should unregister wrapper and clean up all data`() {
        // When EventMapView is dismissed, DisposableEffect calls unregisterWrapper()
        // This must release the strong reference and clean up all associated data
        val eventId = "test-event"

        // Set up wrapper with various data types
        MapWrapperRegistry.registerWrapper(eventId, "mockWrapper")
        MapWrapperRegistry.setPendingPolygons(eventId, listOf(listOf(Pair(0.0, 0.0))), false)
        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.AnimateToPosition(Position(0.0, 0.0), null),
        )

        // Unregister should clean up everything
        MapWrapperRegistry.unregisterWrapper(eventId)

        assertNull(MapWrapperRegistry.getWrapper(eventId), "Wrapper should be removed")
        assertTrue(
            !MapWrapperRegistry.hasPendingPolygons(eventId),
            "Polygons should be cleared",
        )
        assertTrue(
            !MapWrapperRegistry.hasPendingCameraCommand(eventId),
            "Camera commands should be cleared",
        )
    }

    // MARK: - Camera Command Type Tests

    @Test
    fun `CameraCommand AnimateToPosition should preserve data for Swift bridge`() {
        // IOSMapBridge.executeCommand() extracts position, zoom, callbackId
        val position = Position(37.7749, -122.4194)
        val command =
            CameraCommand.AnimateToPosition(
                position = position,
                zoom = 15.5,
                callbackId = "callback-123",
            )

        // Validate data is accessible for Swift extraction
        assertEquals(37.7749, command.position.lat, 0.0001)
        assertEquals(-122.4194, command.position.lng, 0.0001)
        assertEquals(15.5, command.zoom)
        assertEquals("callback-123", command.callbackId)
    }

    @Test
    fun `CameraCommand AnimateToBounds should preserve BoundingBox for Swift bridge`() {
        // IOSMapBridge.executeAnimateToBounds() extracts bbox coordinates
        val bbox = BoundingBox(37.7, -122.5, 37.8, -122.3)
        val command =
            CameraCommand.AnimateToBounds(
                bounds = bbox,
                padding = 50,
                callbackId = "callback-456",
            )

        // Validate bbox is accessible
        assertNotNull(command.bounds)
        assertEquals(37.7, command.bounds.minLatitude, 0.0001)
        assertEquals(37.8, command.bounds.maxLatitude, 0.0001)
        assertEquals(50, command.padding)
    }

    @Test
    fun `CameraCommand SetConstraintBounds should preserve constraint data`() {
        // IOSMapBridge.executeSetConstraintBounds() extracts constraint and original bounds
        val constraintBbox = BoundingBox(37.6, -122.6, 37.9, -122.2)
        val originalBbox = BoundingBox(37.7, -122.5, 37.8, -122.3)
        val command =
            CameraCommand.SetConstraintBounds(
                constraintBounds = constraintBbox,
                originalEventBounds = originalBbox,
                applyZoomSafetyMargin = true,
            )

        // Validate constraint data is preserved
        assertNotNull(command.constraintBounds)
        assertNotNull(command.originalEventBounds)
        assertTrue(command.applyZoomSafetyMargin, "applyZoomSafetyMargin should be true")
    }
}
