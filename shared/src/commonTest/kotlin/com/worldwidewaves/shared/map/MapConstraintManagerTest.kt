package com.worldwidewaves.shared.map

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapConstraintManagerTest {

    val mockMapLibreAdapter : MapLibreAdapter<*> = mockk()

    // Test fixtures with realistic map bounds
    private val sanFranciscoBounds = BoundingBox.fromPositions(
        Position(37.7079, -122.5181),  // Southwest corner
        Position(37.8199, -122.3786)   // Northeast corner
    )
    
    private val tokyoBounds = BoundingBox.fromPositions(
        Position(35.5979, 139.6823),  // Southwest corner
        Position(35.7185, 139.8628)   // Northeast corner
    )
    
    private val smallIslandBounds = BoundingBox.fromPositions(
        Position(-0.01, -0.01),  // Very small area
        Position(0.01, 0.01)
    )
    
    private val worldBounds = BoundingBox.fromPositions(
        Position(-90.0, -180.0),  // Full world
        Position(90.0, 180.0)
    )

    @Test
    fun `test calculateConstraintBounds with no padding`() {
        val manager = MapConstraintManager(sanFranciscoBounds, mockMapLibreAdapter)
        
        // Default padding should be zero
        val bounds = manager.calculateConstraintBounds()
        
        // Should match the original bounds
        assertEquals(sanFranciscoBounds.southwest.latitude, bounds.southwest.latitude)
        assertEquals(sanFranciscoBounds.southwest.longitude, bounds.southwest.longitude)
        assertEquals(sanFranciscoBounds.northeast.latitude, bounds.northeast.latitude)
        assertEquals(sanFranciscoBounds.northeast.longitude, bounds.northeast.longitude)
    }
    
    @Test
    fun `test setVisibleRegionPadding updates padding correctly`() {
        val manager = MapConstraintManager(tokyoBounds, mockMapLibreAdapter)
        
        // Initial padding
        val initialPadding = MapConstraintManager.VisibleRegionPadding(0.01, 0.02)
        manager.setVisibleRegionPadding(initialPadding)
        
        var bounds = manager.calculateConstraintBounds()
        assertEquals(tokyoBounds.southwest.latitude + 0.01, bounds.southwest.latitude)
        assertEquals(tokyoBounds.southwest.longitude + 0.02, bounds.southwest.longitude)
        
        // Update padding
        val newPadding = MapConstraintManager.VisibleRegionPadding(0.03, 0.04)
        manager.setVisibleRegionPadding(newPadding)
        
        bounds = manager.calculateConstraintBounds()
        assertEquals(tokyoBounds.southwest.latitude + 0.03, bounds.southwest.latitude)
        assertEquals(tokyoBounds.southwest.longitude + 0.04, bounds.southwest.longitude)
    }
    
    @Test
    fun `test isValidBounds with valid bounds`() {
        val manager = MapConstraintManager(sanFranciscoBounds, mockMapLibreAdapter)
        
        // Center position
        val center = Position(
            (sanFranciscoBounds.southwest.latitude + sanFranciscoBounds.northeast.latitude) / 2,
            (sanFranciscoBounds.southwest.longitude + sanFranciscoBounds.northeast.longitude) / 2
        )
        
        // Valid bounds that contain the center position
        val validBounds = BoundingBox.fromPositions(
            Position(center.latitude - 0.01, center.longitude - 0.01),
            Position(center.latitude + 0.01, center.longitude + 0.01)
        )
        
        assertTrue(manager.isValidBounds(validBounds, center))
    }
    
    @Test
    fun `test isValidBounds with invalid bounds - too small`() {
        val manager = MapConstraintManager(sanFranciscoBounds, mockMapLibreAdapter)
        val padding = MapConstraintManager.VisibleRegionPadding(0.1, 0.1)
        manager.setVisibleRegionPadding(padding)
        
        // Center position
        val center = Position(
            (sanFranciscoBounds.southwest.latitude + sanFranciscoBounds.northeast.latitude) / 2,
            (sanFranciscoBounds.southwest.longitude + sanFranciscoBounds.northeast.longitude) / 2
        )
        
        // Bounds that are too small (less than 10% of padding)
        val tooSmallBounds = BoundingBox.fromPositions(
            Position(center.latitude - 0.001, center.longitude - 0.001),
            Position(center.latitude + 0.001, center.longitude + 0.001)
        )
        
        assertFalse(manager.isValidBounds(tooSmallBounds, center))
    }
    
    @Test
    fun `test isValidBounds with invalid bounds - position outside bounds`() {
        val manager = MapConstraintManager(sanFranciscoBounds, mockMapLibreAdapter)
        
        // Position outside the bounds
        val outsidePosition = Position(
            sanFranciscoBounds.northeast.latitude + 1.0,
            sanFranciscoBounds.northeast.longitude + 1.0
        )
        
        // Bounds that don't contain the position
        val boundsNotContainingPosition = BoundingBox.fromPositions(
            Position(sanFranciscoBounds.southwest.latitude, sanFranciscoBounds.southwest.longitude),
            Position(sanFranciscoBounds.northeast.latitude - 0.1, sanFranciscoBounds.northeast.longitude - 0.1)
        )
        
        assertFalse(manager.isValidBounds(boundsNotContainingPosition, outsidePosition))
    }
    
    @Test
    fun `test isValidBounds with position near bounds`() {
        val manager = MapConstraintManager(sanFranciscoBounds, mockMapLibreAdapter)
        val padding = MapConstraintManager.VisibleRegionPadding(0.1, 0.1)
        manager.setVisibleRegionPadding(padding)
        
        // Position just outside the bounds but within padding/2
        val nearPosition = Position(
            sanFranciscoBounds.northeast.latitude + 0.04, // Just outside but within padding/2 (0.05)
            sanFranciscoBounds.northeast.longitude
        )
        
        // Bounds that don't contain the position but are close
        val boundsNearPosition = BoundingBox.fromPositions(
            Position(sanFranciscoBounds.southwest.latitude, sanFranciscoBounds.southwest.longitude),
            Position(sanFranciscoBounds.northeast.latitude, sanFranciscoBounds.northeast.longitude)
        )
        
        assertTrue(manager.isValidBounds(boundsNearPosition, nearPosition))
    }
    
    @Test
    fun `test isValidBounds with inverted bounds`() {
        val manager = MapConstraintManager(sanFranciscoBounds, mockMapLibreAdapter)
        
        // Center position
        val center = Position(
            (sanFranciscoBounds.southwest.latitude + sanFranciscoBounds.northeast.latitude) / 2,
            (sanFranciscoBounds.southwest.longitude + sanFranciscoBounds.northeast.longitude) / 2
        )
        
        // Inverted bounds (northeast < southwest)
        val invertedBounds = BoundingBox(
            center.latitude + 0.01, center.longitude + 0.01,  // swLat, swLng (inverted)
            center.latitude - 0.01, center.longitude - 0.01   // neLat, neLng (inverted)
        )
        
        // BoundingBox constructor should fix the inversion, so this should be valid
        assertTrue(manager.isValidBounds(invertedBounds, center))
    }
    
    @Test
    fun `test calculateSafeBounds with center position`() {
        val manager = MapConstraintManager(sanFranciscoBounds, mockMapLibreAdapter)
        val padding = MapConstraintManager.VisibleRegionPadding(0.1, 0.1)
        manager.setVisibleRegionPadding(padding)
        
        // Center position
        val center = Position(
            (sanFranciscoBounds.southwest.latitude + sanFranciscoBounds.northeast.latitude) / 2,
            (sanFranciscoBounds.southwest.longitude + sanFranciscoBounds.northeast.longitude) / 2
        )
        
        val safeBounds = manager.calculateSafeBounds(center)
        
        // Check that the center position is within the safe bounds
        assertTrue(safeBounds.contains(center))
        
        // Check that the safe bounds are within the map bounds
        assertTrue(safeBounds.southwest.latitude >= sanFranciscoBounds.southwest.latitude)
        assertTrue(safeBounds.southwest.longitude >= sanFranciscoBounds.southwest.longitude)
        assertTrue(safeBounds.northeast.latitude <= sanFranciscoBounds.northeast.latitude)
        assertTrue(safeBounds.northeast.longitude <= sanFranciscoBounds.northeast.longitude)
    }
    
    @Test
    fun `test calculateSafeBounds with position near edge`() {
        val manager = MapConstraintManager(sanFranciscoBounds, mockMapLibreAdapter)
        val padding = MapConstraintManager.VisibleRegionPadding(0.1, 0.1)
        manager.setVisibleRegionPadding(padding)
        
        // Position near the southwest corner
        val cornerPosition = Position(
            sanFranciscoBounds.southwest.latitude + 0.01,
            sanFranciscoBounds.southwest.longitude + 0.01
        )
        
        val safeBounds = manager.calculateSafeBounds(cornerPosition)
        
        // Check that the position is within the safe bounds
        assertTrue(safeBounds.contains(cornerPosition))
        
        // Check that the safe bounds are within the map bounds
        assertTrue(safeBounds.southwest.latitude >= sanFranciscoBounds.southwest.latitude)
        assertTrue(safeBounds.southwest.longitude >= sanFranciscoBounds.southwest.longitude)
        assertTrue(safeBounds.northeast.latitude <= sanFranciscoBounds.northeast.latitude)
        assertTrue(safeBounds.northeast.longitude <= sanFranciscoBounds.northeast.longitude)
    }

    @Test
    fun `test calculateSafeBounds with position outside map bounds`() {
        val manager = MapConstraintManager(sanFranciscoBounds, mockMapLibreAdapter)
        val padding = MapConstraintManager.VisibleRegionPadding(0.1, 0.1)
        manager.setVisibleRegionPadding(padding)

        // Position outside the map bounds
        val outsidePosition = Position(
            sanFranciscoBounds.northeast.latitude + 1.0,
            sanFranciscoBounds.northeast.longitude + 1.0
        )

        val safeBounds = manager.calculateSafeBounds(outsidePosition)

        // Check that the safe bounds are within the map bounds
        assertTrue(safeBounds.southwest.latitude >= sanFranciscoBounds.southwest.latitude)
        assertTrue(safeBounds.southwest.longitude >= sanFranciscoBounds.southwest.longitude)
        assertTrue(safeBounds.northeast.latitude <= sanFranciscoBounds.northeast.latitude)
        assertTrue(safeBounds.northeast.longitude <= sanFranciscoBounds.northeast.longitude)

        // Calculate the actual usable padding (same logic as the function)
        val neededLatPadding = padding.latPadding * 1.5  // 0.15
        val neededLngPadding = padding.lngPadding * 1.5  // 0.15

        val mapLatSpan = sanFranciscoBounds.northeast.latitude - sanFranciscoBounds.southwest.latitude
        val mapLngSpan = sanFranciscoBounds.northeast.longitude - sanFranciscoBounds.southwest.longitude

        val expectedUsableLatPadding = minOf(neededLatPadding, mapLatSpan * 0.4)
        val expectedUsableLngPadding = minOf(neededLngPadding, mapLngSpan * 0.4)

        // When position is outside northeast, the center gets constrained to northeast - usablePadding
        val expectedCenterLat = sanFranciscoBounds.northeast.latitude - expectedUsableLatPadding
        val expectedCenterLng = sanFranciscoBounds.northeast.longitude - expectedUsableLngPadding

        val expectedSafeSouth = maxOf(sanFranciscoBounds.southwest.latitude, expectedCenterLat - expectedUsableLatPadding)
        val expectedSafeWest = maxOf(sanFranciscoBounds.southwest.longitude, expectedCenterLng - expectedUsableLngPadding)

        assertEquals(expectedSafeSouth, safeBounds.southwest.latitude, 0.0001)
        assertEquals(expectedSafeWest, safeBounds.southwest.longitude, 0.0001)
    }

    @Test
    fun `test calculateSafeBounds with very small map`() {
        val manager = MapConstraintManager(smallIslandBounds, mockMapLibreAdapter)

        // Set padding larger than the map dimensions
        val largePadding = MapConstraintManager.VisibleRegionPadding(0.1, 0.1)
        manager.setVisibleRegionPadding(largePadding)

        // Use a center position that's actually within the small island bounds
        val center = Position(
            (smallIslandBounds.southwest.latitude + smallIslandBounds.northeast.latitude) / 2,
            (smallIslandBounds.southwest.longitude + smallIslandBounds.northeast.longitude) / 2
        )

        val safeBounds = manager.calculateSafeBounds(center)

        // Check that the bounds are always within the map bounds (this is the priority)
        assertTrue(safeBounds.southwest.latitude >= smallIslandBounds.southwest.latitude)
        assertTrue(safeBounds.southwest.longitude >= smallIslandBounds.southwest.longitude)
        assertTrue(safeBounds.northeast.latitude <= smallIslandBounds.northeast.latitude)
        assertTrue(safeBounds.northeast.longitude <= smallIslandBounds.northeast.longitude)

        // Check that the bounds are reasonable (not inverted)
        assertTrue(safeBounds.northeast.latitude > safeBounds.southwest.latitude)
        assertTrue(safeBounds.northeast.longitude > safeBounds.southwest.longitude)

        // For very small maps, the function prioritizes staying within map bounds
        // over achieving minimum size, so we check that it tries to achieve minimum size
        // but only within the map constraints
        val minLatSpan = largePadding.latPadding * 0.2
        val minLngSpan = largePadding.lngPadding * 0.2
        val mapLatSpan = smallIslandBounds.height
        val mapLngSpan = smallIslandBounds.width

        // The bounds should be either the minimum size OR the full map size (whichever is smaller)
        val expectedMaxLatSpan = minOf(minLatSpan, mapLatSpan)
        val expectedMaxLngSpan = minOf(minLngSpan, mapLngSpan)

        // The bounds should be as large as possible within the constraints
        assertTrue(safeBounds.height <= expectedMaxLatSpan + 0.001)
        assertTrue(safeBounds.width <= expectedMaxLngSpan + 0.001)
    }
    
    @Test
    fun `test getNearestValidPoint with point inside bounds`() {
        val manager = MapConstraintManager(sanFranciscoBounds, mockMapLibreAdapter)
        
        // Point inside bounds
        val insidePoint = Position(
            (sanFranciscoBounds.southwest.latitude + sanFranciscoBounds.northeast.latitude) / 2,
            (sanFranciscoBounds.southwest.longitude + sanFranciscoBounds.northeast.longitude) / 2
        )
        
        val bounds = manager.calculateConstraintBounds()
        val nearestPoint = manager.getNearestValidPoint(insidePoint, bounds)
        
        // The nearest point should be the same as the input point
        assertEquals(insidePoint.latitude, nearestPoint.latitude)
        assertEquals(insidePoint.longitude, nearestPoint.longitude)
    }
    
    @Test
    fun `test getNearestValidPoint with point outside bounds`() {
        val manager = MapConstraintManager(sanFranciscoBounds, mockMapLibreAdapter)
        
        // Point outside bounds
        val outsidePoint = Position(
            sanFranciscoBounds.northeast.latitude + 1.0,
            sanFranciscoBounds.northeast.longitude + 1.0
        )
        
        val bounds = manager.calculateConstraintBounds()
        val nearestPoint = manager.getNearestValidPoint(outsidePoint, bounds)
        
        // The nearest point should be at the boundary
        assertEquals(bounds.northeast.latitude, nearestPoint.latitude)
        assertEquals(bounds.northeast.longitude, nearestPoint.longitude)
    }
    
    @Test
    fun `test hasSignificantPaddingChange with small change`() {
        val manager = MapConstraintManager(sanFranciscoBounds, mockMapLibreAdapter)
        
        // Set initial padding
        val initialPadding = MapConstraintManager.VisibleRegionPadding(0.1, 0.1)
        manager.setVisibleRegionPadding(initialPadding)
        
        // Small change (less than 10%)
        val smallChangePadding = MapConstraintManager.VisibleRegionPadding(0.105, 0.105)
        
        assertFalse(manager.hasSignificantPaddingChange(smallChangePadding))
    }
    
    @Test
    fun `test hasSignificantPaddingChange with significant change`() {
        val manager = MapConstraintManager(sanFranciscoBounds, mockMapLibreAdapter)
        
        // Set initial padding
        val initialPadding = MapConstraintManager.VisibleRegionPadding(0.1, 0.1)
        manager.setVisibleRegionPadding(initialPadding)
        
        // Significant change (more than 10%)
        val significantChangePadding = MapConstraintManager.VisibleRegionPadding(0.12, 0.12)
        
        assertTrue(manager.hasSignificantPaddingChange(significantChangePadding))
    }
    
    @Test
    fun `test hasSignificantPaddingChange with significant change in only one dimension`() {
        val manager = MapConstraintManager(sanFranciscoBounds, mockMapLibreAdapter)
        
        // Set initial padding
        val initialPadding = MapConstraintManager.VisibleRegionPadding(0.1, 0.1)
        manager.setVisibleRegionPadding(initialPadding)
        
        // Significant change in latitude only
        val latChangePadding = MapConstraintManager.VisibleRegionPadding(0.12, 0.101)
        assertTrue(manager.hasSignificantPaddingChange(latChangePadding))
        
        // Significant change in longitude only
        val lngChangePadding = MapConstraintManager.VisibleRegionPadding(0.101, 0.12)
        assertTrue(manager.hasSignificantPaddingChange(lngChangePadding))
    }

    @Test
    fun `test with zero padding`() {
        val manager = MapConstraintManager(sanFranciscoBounds, mockMapLibreAdapter)
        
        // Zero padding
        val zeroPadding = MapConstraintManager.VisibleRegionPadding(0.0, 0.0)
        manager.setVisibleRegionPadding(zeroPadding)
        
        val bounds = manager.calculateConstraintBounds()
        
        // Should match the original bounds
        assertEquals(sanFranciscoBounds.southwest.latitude, bounds.southwest.latitude)
        assertEquals(sanFranciscoBounds.southwest.longitude, bounds.southwest.longitude)
        assertEquals(sanFranciscoBounds.northeast.latitude, bounds.northeast.latitude)
        assertEquals(sanFranciscoBounds.northeast.longitude, bounds.northeast.longitude)
    }
    
    @Test
    fun `test with negative padding`() {
        val manager = MapConstraintManager(sanFranciscoBounds, mockMapLibreAdapter)
        
        // Negative padding
        val negativePadding = MapConstraintManager.VisibleRegionPadding(-0.1, -0.1)
        manager.setVisibleRegionPadding(negativePadding)
        
        val bounds = manager.calculateConstraintBounds()
        
        // Bounds should be expanded outward
        assertTrue(bounds.southwest.latitude < sanFranciscoBounds.southwest.latitude)
        assertTrue(bounds.southwest.longitude < sanFranciscoBounds.southwest.longitude)
        assertTrue(bounds.northeast.latitude > sanFranciscoBounds.northeast.latitude)
        assertTrue(bounds.northeast.longitude > sanFranciscoBounds.northeast.longitude)
    }
}
