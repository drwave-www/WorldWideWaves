/*
 * Copyright (c) 2025 WorldWideWaves.
 * All rights reserved. This file is part of an open-source project.
 * Unauthorized use, reproduction, or distribution is prohibited.
 */

package com.worldwidewaves.shared.events

import com.worldwidewaves.shared.events.geometry.EventAreaGeometry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for bbox override detection and parsing in WWWEventArea.
 *
 * These tests verify that:
 * - bboxIsOverride correctly identifies when a bbox string is provided
 * - parseBboxString correctly parses valid bbox strings
 * - Invalid bbox strings are handled gracefully
 */
class WWWEventAreaBboxOverrideTest {
    @Test
    fun bboxIsOverride_returnsTrueWhenBboxStringProvided() {
        // Given: A valid bbox string (Lagos, Nigeria)
        val area =
            WWWEventArea(
                osmAdminids = listOf(1234),
                bbox = "3.196678,6.371119,3.598022,6.642783",
            )

        // When: Check if bbox is override
        val result = area.bboxIsOverride

        // Then: Should return true
        assertTrue(result, "bboxIsOverride should return true when bbox string is provided")
    }

    @Test
    fun bboxIsOverride_returnsFalseWhenBboxIsNull() {
        // Given: An area without bbox
        val area =
            WWWEventArea(
                osmAdminids = listOf(1234),
                bbox = null,
            )

        // When: Check if bbox is override
        val result = area.bboxIsOverride

        // Then: Should return false
        assertFalse(result, "bboxIsOverride should return false when bbox is null")
    }

    @Test
    fun bboxIsOverride_returnsFalseWhenBboxIsEmpty() {
        // Given: An area with empty bbox string
        val area =
            WWWEventArea(
                osmAdminids = listOf(1234),
                bbox = "",
            )

        // When: Check if bbox is override
        val result = area.bboxIsOverride

        // Then: Should return false
        assertFalse(result, "bboxIsOverride should return false when bbox is empty")
    }

    @Test
    fun bbox_parsesBboxStringCorrectly() {
        // Given: A valid bbox string (San Francisco: minLng, minLat, maxLng, maxLat)
        val bboxString = "-122.539501,37.70559,-122.343807,37.833685"

        // When: Parse the bbox string
        val result = EventAreaGeometry.parseBboxString(bboxString)

        // Then: Should correctly parse coordinates
        assertNotNull(result, "parseBboxString should return non-null for valid bbox")
        assertEquals(-122.539501, result.sw.lng, 0.000001, "SW longitude should match")
        assertEquals(37.70559, result.sw.lat, 0.000001, "SW latitude should match")
        assertEquals(-122.343807, result.ne.lng, 0.000001, "NE longitude should match")
        assertEquals(37.833685, result.ne.lat, 0.000001, "NE latitude should match")
    }

    @Test
    fun bbox_handlesMalformedBboxString() {
        // Given: Various malformed bbox strings
        val malformedBboxes =
            listOf(
                "invalid",
                "1,2,3", // Too few coordinates
                "a,b,c,d", // Non-numeric
                "1.0", // Single coordinate
                "", // Empty
            )

        // When/Then: Each should return null without crashing
        malformedBboxes.forEach { bbox ->
            val result = EventAreaGeometry.parseBboxString(bbox)
            assertNull(result, "parseBboxString should return null for malformed bbox: '$bbox'")
        }
    }

    @Test
    fun bbox_handlesInvalidCoordinates() {
        // Given: Bbox strings with extreme or invalid coordinates
        val extremeBboxes =
            listOf(
                // Out of range coordinates (lat must be -90 to 90, lng must be -180 to 180)
                "200.0,100.0,250.0,150.0", // All out of range
                "-200.0,-100.0,200.0,100.0", // All out of range
                // Edge cases that might cause issues
                "0.0,0.0,0.0,0.0", // All zeros
                "-180.0,-90.0,180.0,90.0", // Extreme valid bounds (entire world)
            )

        // When/Then: Should either parse or return null, but never crash
        extremeBboxes.forEach { bbox ->
            try {
                val result = EventAreaGeometry.parseBboxString(bbox)
                // If it parses, verify the coordinates are stored
                if (result != null) {
                    assertNotNull(result.sw, "SW position should exist")
                    assertNotNull(result.ne, "NE position should exist")
                }
            } catch (e: Exception) {
                throw AssertionError("parseBboxString should not throw for bbox: '$bbox'", e)
            }
        }
    }

    @Test
    fun bbox_parsesRealEventBboxes() {
        // Given: Real bbox strings from events.json
        val realBboxes =
            mapOf(
                "Lagos" to "3.196678,6.371119,3.598022,6.642783",
                "Kinshasa" to "15.222416,-4.377576,15.368929,-4.294815",
                "Bangkok" to "100.327877,13.476602,100.938604,13.942225",
                "Tokyo" to "138.822556,35.450628,139.994659,35.989700",
                "Beijing" to "115.3742,39.4102,117.544,41.0596",
                "Shanghai" to "120.8079,30.6693,122.129,31.9083",
                "San Francisco" to "-122.539501,37.70559,-122.343807,37.833685",
                "Santiago" to "-70.821661,-33.62747,-70.476965,-33.347121",
            )

        // When/Then: All should parse successfully
        realBboxes.forEach { (city, bbox) ->
            val result = EventAreaGeometry.parseBboxString(bbox)
            assertNotNull(result, "Should parse bbox for $city")

            // Verify bbox is valid (NE should be northeast of SW)
            assertTrue(
                result.ne.lat >= result.sw.lat,
                "$city: NE latitude should be >= SW latitude",
            )
            // Note: longitude can wrap around the antimeridian, so we don't check NE >= SW
        }
    }
}
