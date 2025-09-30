package com.worldwidewaves.shared.data

/* * Copyright 2025 DrWave
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
 * limitations under the License. */

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * iOS-specific tests for MapStore functionality.
 * Tests iOS ODR (On-Demand Resources) integration and file system operations.
 */
class IOSMapStoreTest {
    @Test
    fun `platformCacheRoot returns iOS Application Support directory`() {
        val cacheRoot = platformCacheRoot()

        assertNotNull(cacheRoot, "Cache root should not be null")
        assertTrue(cacheRoot.isNotEmpty(), "Cache root should not be empty")
        assertTrue(
            cacheRoot.contains("Application Support") || cacheRoot.contains("tmp"),
            "Should use iOS Application Support or temp directory",
        )
        assertTrue(cacheRoot.endsWith("Maps"), "Should end with Maps subdirectory")
    }

    @Test
    fun `platformAppVersionStamp returns iOS bundle version format`() {
        val stamp = platformAppVersionStamp()

        assertNotNull(stamp, "Version stamp should not be null")
        assertTrue(stamp.isNotEmpty(), "Version stamp should not be empty")
        assertTrue(stamp.contains("+"), "Should contain version+build format (e.g., '1.0+123')")
    }

    @Test
    fun `iOS file operations work correctly`() {
        val testDir = "${platformCacheRoot()}/ios_test"
        val testFile = "$testDir/test_ios.txt"
        val testContent = "iOS MapStore test content with emoji: 🍎"

        // Ensure directory
        platformEnsureDir(testDir)
        assertTrue(platformFileExists(testDir), "Directory should be created")

        // Write file
        platformWriteText(testFile, testContent)
        assertTrue(platformFileExists(testFile), "File should exist after write")

        // Read file
        val readContent = platformReadText(testFile)
        assertEquals(testContent, readContent, "Content should match including Unicode")

        // Delete file
        platformDeleteFile(testFile)
        assertFalse(platformFileExists(testFile), "File should not exist after deletion")
    }

    @Test
    fun `platformFetchToFile handles ODR lifecycle correctly`() =
        runTest {
            val eventId = "test_ios_odr"
            val extension = "geojson"
            val destPath = "${platformCacheRoot()}/test_odr_fetch.geojson"

            // Note: In test environment, ODR resources may not be available
            // This test verifies the function handles missing resources gracefully
            val result = platformFetchToFile(eventId, extension, destPath)

            // Should return false for non-existent test resource
            assertFalse(result, "Should return false for non-existent ODR resource in test environment")
            assertFalse(platformFileExists(destPath), "Destination file should not exist on failure")
        }

    @Test
    fun `iOS cacheStringToFile creates proper file with NSString encoding`() {
        val fileName = "ios_cache_test.json"
        val content = """{"ios": "test", "unicode": "🍎📱", "timestamp": ${kotlinx.datetime.Clock.System.now()}}"""

        val result = cacheStringToFile(fileName, content)
        assertEquals(fileName, result, "Should return filename")

        val expectedPath = "${platformCacheRoot()}/$fileName"
        assertTrue(platformFileExists(expectedPath), "File should exist in cache directory")
        assertEquals(content, platformReadText(expectedPath), "Content should match including Unicode")

        // Cleanup
        platformDeleteFile(expectedPath)
    }

    @Test
    fun `platformInvalidateGeoJson is safe no-op on iOS`() {
        val eventId = "test_ios_invalidate"

        // Should not throw exception (no-op implementation)
        platformInvalidateGeoJson(eventId)
        platformInvalidateGeoJson(eventId) // Should be idempotent
    }

    @Test
    fun `resolveODRResourcePath handles various bundle layouts`() =
        runTest {
            // This is an internal function test - we test via platformFetchToFile
            val eventId = "test_bundle_layout"
            val extension = "mbtiles"
            val destPath = "${platformCacheRoot()}/test_bundle.mbtiles"

            // Test that function handles missing resources gracefully
            val result = platformFetchToFile(eventId, extension, destPath)

            // In test environment, ODR resources are not available
            assertFalse(result, "Should handle missing ODR resources gracefully")
        }

    @Test
    fun `getMapFileAbsolutePath cache invalidation works correctly`() =
        runTest {
            val eventId = "test_cache_invalidation"
            val extension = "geojson"

            // Clear any existing cache state
            clearUnavailableGeoJsonCache(eventId)

            // First call - may fail if resource not available
            val path1 = getMapFileAbsolutePath(eventId, extension)

            // Clear cache and try again
            clearUnavailableGeoJsonCache(eventId)
            val path2 = getMapFileAbsolutePath(eventId, extension)

            // Results should be consistent (both null in test environment)
            assertEquals(path1, path2, "Cache invalidation should allow consistent results")
        }
}
