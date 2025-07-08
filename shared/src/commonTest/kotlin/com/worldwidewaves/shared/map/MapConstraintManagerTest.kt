package com.worldwidewaves.shared.map

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapConstraintManagerTest {

    // Test fixtures with realistic map bounds
    private val sanFranciscoBounds = BoundingBox(
        Position(37.7079, -122.5181),  // Southwest corner
        Position(37.8199, -122.3786)   // Northeast corner
    )
    
    private val tokyoBounds = BoundingBox(
        Position(35.5979, 139.6823),  // Southwest corner
        Position(35.7185, 139.8628)   // Northeast corner
    )
    
    private val smallIslandBounds = BoundingBox(
        Position(-0.01, -0.01),  // Very small area
        Position(0.01, 0.01)
    )
    
    private val worldBounds = BoundingBox(
        Position(-90.0, -180.0),  // Full world
        Position(90.0, 180.0)
    )

    @Test
    fun `test calculateConstraintBounds with no padding`() {
        val manager = MapConstraintManager(sanFranciscoBounds)
        
        // Default padding should be zero
        val bounds = manager.calculateConstraintBounds()
        
        // Should match the original bounds
        assertEquals(sanFranciscoBounds.southwest.latitude, bounds.southwest.latitude)
        assertEquals(sanFranciscoBounds.southwest.longitude, bounds.southwest.longitude)
        assertEquals(sanFranciscoBounds.northeast.latitude, bounds.northeast.latitude)
        assertEquals(sanFranciscoBounds.northeast.longitude, bounds.northeast.longitude)
    }
    
    @Test
    fun `test calculateConstraintBounds with padding`() {
        val manager = MapConstraintManager(sanFranciscoBounds)
        
        // Set padding values
        val padding = MapConstraintManager.VisibleRegionPadding(0.05, 0.1)
        manager.setVisibleRegionPadding(padding)
        
        val bounds = manager.calculateConstraintBounds()
        
        // Check that padding was applied correctly
        assertEquals(sanFranciscoBounds.southwest.latitude + padding.latPadding, bounds.southwest.latitude)
        assertEquals(sanFranciscoBounds.southwest.longitude + padding.lngPadding, bounds.southwest.longitude)
        assertEquals(sanFranciscoBounds.northeast.latitude - padding.latPadding, bounds.northeast.latitude)
        assertEquals(sanFranciscoBounds.northeast.longitude - padding.lngPadding, bounds.northeast.longitude)
    }
    
    @Test
    fun `test setVisibleRegionPadding updates padding correctly`() {
        val manager = MapConstraintManager(tokyoBounds)
        
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
        val manager = MapConstraintManager(sanFranciscoBounds)
        
        // Center position
        val center = Position(
            (sanFranciscoBounds.southwest.latitude + sanFranciscoBounds.northeast.latitude) / 2,
            (sanFranciscoBounds.southwest.longitude + sanFranciscoBounds.northeast.longitude) / 2
        )
        
        // Valid bounds that contain the center position
        val validBounds = BoundingBox(
            Position(center.latitude - 0.01, center.longitude - 0.01),
            Position(center.latitude + 0.01, center.longitude + 0.01)
        )
        
        assertTrue(manager.isValidBounds(validBounds, center))
    }
    
    @Test
    fun `test isValidBounds with invalid bounds - too small`() {
        val manager = MapConstraintManager(sanFranciscoBounds)
        val padding = MapConstraintManager.VisibleRegionPadding(0.1, 0.1)
        manager.setVisibleRegionPadding(padding)
        
        // Center position
        val center = Position(
            (sanFranciscoBounds.southwest.latitude + sanFranciscoBounds.northeast.latitude) / 2,
            (sanFranciscoBounds.southwest.longitude + sanFranciscoBounds.northeast.longitude) / 2
        )
        
        // Bounds that are too small (less than 10% of padding)
        val tooSmallBounds = BoundingBox(
            Position(center.latitude - 0.001, center.longitude - 0.001),
            Position(center.latitude + 0.001, center.longitude + 0.001)
        )
        
        assertFalse(manager.isValidBounds(tooSmallBounds, center))
    }
    
    @Test
    fun `test isValidBounds with invalid bounds - position outside bounds`() {
        val manager = MapConstraintManager(sanFranciscoBounds)
        
        // Position outside the bounds
        val outsidePosition = Position(
            sanFranciscoBounds.northeast.latitude + 1.0,
            sanFranciscoBounds.northeast.longitude + 1.0
        )
        
        // Bounds that don't contain the position
        val boundsNotContainingPosition = BoundingBox(
            Position(sanFranciscoBounds.southwest.latitude, sanFranciscoBounds.southwest.longitude),
            Position(sanFranciscoBounds.northeast.latitude - 0.1, sanFranciscoBounds.northeast.longitude - 0.1)
        )
        
        assertFalse(manager.isValidBounds(boundsNotContainingPosition, outsidePosition))
    }
    
    @Test
    fun `test isValidBounds with position near bounds`() {
        val manager = MapConstraintManager(sanFranciscoBounds)
        val padding = MapConstraintManager.VisibleRegionPadding(0.1, 0.1)
        manager.setVisibleRegionPadding(padding)
        
        // Position just outside the bounds but within padding/2
        val nearPosition = Position(
            sanFranciscoBounds.northeast.latitude + 0.04, // Just outside but within padding/2 (0.05)
            sanFranciscoBounds.northeast.longitude
        )
        
        // Bounds that don't contain the position but are close
        val boundsNearPosition = BoundingBox(
            Position(sanFranciscoBounds.southwest.latitude, sanFranciscoBounds.southwest.longitude),
            Position(sanFranciscoBounds.northeast.latitude, sanFranciscoBounds.northeast.longitude)
        )
        
        assertTrue(manager.isValidBounds(boundsNearPosition, nearPosition))
    }
    
    @Test
    fun `test isValidBounds with inverted bounds`() {
        val manager = MapConstraintManager(sanFranciscoBounds)
        
        // Center position
        val center = Position(
            (sanFranciscoBounds.southwest.latitude + sanFranciscoBounds.northeast.latitude) / 2,
            (sanFranciscoBounds.southwest.longitude + sanFranciscoBounds.northeast.longitude) / 2
        )
        
        // Inverted bounds (northeast < southwest)
        val invertedBounds = BoundingBox(
            Position(center.latitude + 0.01, center.longitude + 0.01),
            Position(center.latitude - 0.01, center.longitude - 0.01)
        )
        
        // BoundingBox constructor should fix the inversion, so this should be valid
        assertTrue(manager.isValidBounds(invertedBounds, center))
    }
    
    @Test
    fun `test calculateSafeBounds with center position`() {
        val manager = MapConstraintManager(sanFranciscoBounds)
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
        val manager = MapConstraintManager(sanFranciscoBounds)
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
        val manager = MapConstraintManager(sanFranciscoBounds)
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
        
        // The bounds should be anchored to the edge of the map
        assertEquals(sanFranciscoBounds.northeast.latitude - padding.latPadding, safeBounds.southwest.latitude)
        assertEquals(sanFranciscoBounds.northeast.longitude - padding.lngPadding, safeBounds.southwest.longitude)
    }
    
    @Test
    fun `test calculateSafeBounds with very small map`() {
        val manager = MapConstraintManager(smallIslandBounds)
        
        // Set padding larger than the map dimensions
        val largePadding = MapConstraintManager.VisibleRegionPadding(0.1, 0.1)
        manager.setVisibleRegionPadding(largePadding)
        
        // Center position
        val center = Position(0.0, 0.0)
        
        val safeBounds = manager.calculateSafeBounds(center)
        
        // Check that the safe bounds are within the map bounds
        assertTrue(safeBounds.southwest.latitude >= smallIslandBounds.southwest.latitude)
        assertTrue(safeBounds.southwest.longitude >= smallIslandBounds.southwest.longitude)
        assertTrue(safeBounds.northeast.latitude <= smallIslandBounds.northeast.latitude)
        assertTrue(safeBounds.northeast.longitude <= smallIslandBounds.northeast.longitude)
        
        // Check that the bounds have at least the minimum size (20% of padding)
        assertTrue(safeBounds.height >= largePadding.latPadding * 0.2)
        assertTrue(safeBounds.width >= largePadding.lngPadding * 0.2)
    }
    
    @Test
    fun `test getNearestValidPoint with point inside bounds`() {
        val manager = MapConstraintManager(sanFranciscoBounds)
        
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
        val manager = MapConstraintManager(sanFranciscoBounds)
        
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
        val manager = MapConstraintManager(sanFranciscoBounds)
        
        // Set initial padding
        val initialPadding = MapConstraintManager.VisibleRegionPadding(0.1, 0.1)
        manager.setVisibleRegionPadding(initialPadding)
        
        // Small change (less than 10%)
        val smallChangePadding = MapConstraintManager.VisibleRegionPadding(0.105, 0.105)
        
        assertFalse(manager.hasSignificantPaddingChange(smallChangePadding))
    }
    
    @Test
    fun `test hasSignificantPaddingChange with significant change`() {
        val manager = MapConstraintManager(sanFranciscoBounds)
        
        // Set initial padding
        val initialPadding = MapConstraintManager.VisibleRegionPadding(0.1, 0.1)
        manager.setVisibleRegionPadding(initialPadding)
        
        // Significant change (more than 10%)
        val significantChangePadding = MapConstraintManager.VisibleRegionPadding(0.12, 0.12)
        
        assertTrue(manager.hasSignificantPaddingChange(significantChangePadding))
    }
    
    @Test
    fun `test hasSignificantPaddingChange with significant change in only one dimension`() {
        val manager = MapConstraintManager(sanFranciscoBounds)
        
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
    fun `test with extreme padding values`() {
        val manager = MapConstraintManager(worldBounds)
        
        // Very large padding (larger than the map)
        val largePadding = MapConstraintManager.VisibleRegionPadding(200.0, 400.0)
        manager.setVisibleRegionPadding(largePadding)
        
        val bounds = manager.calculateConstraintBounds()
        
        // The resulting bounds should be inverted (and thus invalid)
        assertTrue(bounds.southwest.latitude > bounds.northeast.latitude)
        assertTrue(bounds.southwest.longitude > bounds.northeast.longitude)
        
        // Center position
        val center = Position(0.0, 0.0)
        
        // Safe bounds should handle this gracefully
        val safeBounds = manager.calculateSafeBounds(center)
        
        // Safe bounds should have at least minimum size
        assertTrue(safeBounds.height >= largePadding.latPadding * 0.2)
        assertTrue(safeBounds.width >= largePadding.lngPadding * 0.2)
    }
    
    @Test
    fun `test with zero padding`() {
        val manager = MapConstraintManager(sanFranciscoBounds)
        
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
        val manager = MapConstraintManager(sanFranciscoBounds)
        
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
