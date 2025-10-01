package com.worldwidewaves.shared.data

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for iOS file system utilities.
 */
class IOSFileSystemUtilsTest {
    @Test
    fun getAppSupportMapsDirectory_returnsValidPath() {
        // When
        val result = getAppSupportMapsDirectory()

        // Then
        assertNotNull(result)
        assertTrue(result.contains("Library/Application Support/Maps"))
    }

    @Test
    fun isMapFileInCache_returnsFalse_forNonExistentFile() {
        // Given
        val nonExistentMap = "definitely_not_existing_map_12345"

        // When
        val result = isMapFileInCache(nonExistentMap, "geojson")

        // Then
        assertFalse(result)
    }

    @Test
    fun isMapFileInCache_handlesMultipleExtensions() {
        // Given
        val mapId = "test_map"

        // When
        val hasGeo = isMapFileInCache(mapId, "geojson")
        val hasMb = isMapFileInCache(mapId, "mbtiles")

        // Then - both should complete without errors (actual availability depends on cache state)
        // Just verify the function doesn't crash
        assertNotNull(hasGeo)
        assertNotNull(hasMb)
    }
}
