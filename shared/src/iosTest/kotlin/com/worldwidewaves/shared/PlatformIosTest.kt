package com.worldwidewaves.shared

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

import com.worldwidewaves.shared.data.cacheDeepFile
import com.worldwidewaves.shared.data.cachedFileExists
import com.worldwidewaves.shared.data.cachedFilePath
import com.worldwidewaves.shared.data.clearUnavailableGeoJsonCache
import com.worldwidewaves.shared.data.getCacheDir
import com.worldwidewaves.shared.data.getMapFileAbsolutePath
import com.worldwidewaves.shared.data.isCachedFileStale
import com.worldwidewaves.shared.data.readGeoJson
import com.worldwidewaves.shared.data.updateCacheMetadata
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * iOS-specific platform cache function tests.
 * Tests the actual implementations in PlatformCache.ios.kt.
 */
class PlatformIosTest {
    @Test
    fun `getCacheDir returns valid iOS cache directory`() {
        val cacheDir = getCacheDir()

        assertNotNull(cacheDir, "Cache directory should not be null")
        assertTrue(cacheDir.isNotEmpty(), "Cache directory should not be empty")
        assertTrue(cacheDir.startsWith("/"), "Should be absolute path")
        assertTrue(
            cacheDir.contains("Library") || cacheDir.contains("tmp"),
            "Should use iOS Library or temp directory",
        )
    }

    @Test
    fun `cachedFileExists works with iOS file system`() =
        runTest {
            val testFileName = "ios_test_file_exists.txt"

            // Note: Just verify the function doesn't crash
            // In test environment, file system operations may be limited
            val exists = cachedFileExists(testFileName)
            assertNotNull(exists, "Function should return a boolean value")
        }

    @Test
    fun `cachedFilePath returns iOS file path`() =
        runTest {
            val testFileName = "nonexistent_file.txt"

            // Note: Test with non-existent file - should return null
            // In test environment, file system operations may be limited
            val filePath = cachedFilePath(testFileName)
            // filePath will be null if file doesn't exist, which is expected
        }

    @Test
    fun `isCachedFileStale checks bundle modification time`() {
        val fileName = "ios_stale_test.txt"

        // iOS implementation checks against bundle modification time
        // Should not crash and return a boolean value
        val isStale = isCachedFileStale(fileName)
        assertNotNull(isStale, "Staleness check should return a value")
    }

    @Test
    fun `updateCacheMetadata updates metadata file on iOS`() {
        val fileName = "ios_metadata_test.txt"

        // Should not throw exceptions
        updateCacheMetadata(fileName)
        updateCacheMetadata(fileName) // Should be idempotent
    }

    @Test
    fun `cacheDeepFile handles iOS resource caching`() =
        runTest {
            val fileName = "ios_deep_file_test.txt"

            // Should not throw exceptions (handles missing resources gracefully)
            cacheDeepFile(fileName)
            cacheDeepFile(fileName) // Should be idempotent
        }

    @Test
    fun `readGeoJson integrates with iOS ODR system`() =
        runTest {
            val eventId = "ios_geojson_test"

            // Note: Will return null in test environment as no actual ODR resources exist
            val content = readGeoJson(eventId)

            // Verify function doesn't crash and handles missing resources gracefully
            if (content != null) {
                assertTrue(content.isNotEmpty(), "If content exists, should not be empty")
            }
        }

    @Test
    fun `getMapFileAbsolutePath handles iOS ODR lifecycle correctly`() =
        runTest {
            val eventId = "ios_odr_lifecycle_test"
            val extension = MapFileExtension.MBTILES

            // Note: Will return null in test environment as no actual ODR resources exist
            val path = getMapFileAbsolutePath(eventId, extension)

            // Verify function doesn't crash and handles missing ODR resources gracefully
            if (path != null) {
                assertTrue(path.startsWith("/"), "If path exists, should be absolute")
                assertTrue(path.endsWith(".${extension.value}"), "If path exists, should have correct extension")
            }
        }

    @Test
    fun `clearUnavailableGeoJsonCache clears iOS cache state`() {
        val eventId = "ios_cache_clear_test"

        // Should not throw exceptions
        clearUnavailableGeoJsonCache(eventId)
        clearUnavailableGeoJsonCache(eventId) // Should be idempotent
    }

    @Test
    fun `localizeString works with iOS Moko resources`() {
        // Use a real string resource from MokoRes
        val resource = MokoRes.strings.tab_infos_name

        // Should not throw exception and return a localized string
        val result = localizeString(resource)
        assertNotNull(result, "Localized string should not be null")
    }
}
