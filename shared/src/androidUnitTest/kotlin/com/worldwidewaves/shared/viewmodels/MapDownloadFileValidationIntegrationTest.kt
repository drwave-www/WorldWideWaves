@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.worldwidewaves.shared.viewmodels

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

import com.worldwidewaves.shared.map.MapFeatureState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for map download file validation scenarios.
 * Tests critical production issues: Sydney (session=0) and Cairo (premature INSTALLED).
 *
 * These tests simulate real-world scenarios with file availability timing:
 * 1. PlayCore reports INSTALLED before files extracted (race condition)
 * 2. Module installed but files missing/corrupted
 * 3. Files become available after validation check
 */
class MapDownloadFileValidationIntegrationTest {
    private lateinit var testAdapter: FileValidationTestAdapter
    private lateinit var manager: MapDownloadCoordinator
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeTest
    fun setUp() {
        testAdapter = FileValidationTestAdapter()
        manager = MapDownloadCoordinator(testAdapter)
    }

    /**
     * Sydney Issue: PlayCore reports module as INSTALLED (session=0),
     * but files exist in cache from successful previous download.
     * Should recognize map is already available and skip download.
     */
    @Test
    fun `session zero with valid files should skip download`() =
        runTest {
            // Given: Module installed by PlayCore AND files exist
            testAdapter.setModuleInstalled("sydney_australia", true)
            testAdapter.setFilesAvailable("sydney_australia", true)

            // When: User clicks download
            manager.downloadMap("sydney_australia")

            // Then: Should skip download and immediately mark as installed
            assertEquals(MapFeatureState.Installed, manager.featureState.first())
            assertTrue(testAdapter.isMapInstalledCalled)
            assertFalse(testAdapter.startPlatformDownloadCalled, "Should NOT start download")
        }

    /**
     * Sydney Issue: PlayCore reports module as INSTALLED (session=0),
     * but files are MISSING (previous download failed).
     * Should detect missing files and allow re-download.
     */
    @Test
    fun `session zero with missing files should trigger download`() =
        runTest {
            // Given: Module installed by PlayCore BUT files missing
            testAdapter.setModuleInstalled("sydney_australia", true)
            testAdapter.setFilesAvailable("sydney_australia", false) // Files missing!

            // When: User clicks download
            manager.downloadMap("sydney_australia")

            // Then: Should proceed with download (files missing despite PlayCore installed)
            assertEquals(MapFeatureState.Pending, manager.featureState.first())
            assertTrue(testAdapter.isMapInstalledCalled)
            assertTrue(testAdapter.startPlatformDownloadCalled, "Should start download to get files")
        }

    /**
     * Cairo Issue: PlayCore reports INSTALLED, but files not yet extracted.
     * Simulates race condition where INSTALLED callback fires before file extraction.
     */
    @Test
    fun `PlayCore installed but files not yet extracted should wait for files`() =
        runTest {
            // Given: Module NOT installed yet
            testAdapter.setModuleInstalled("cairo_egypt", false)
            testAdapter.setFilesAvailable("cairo_egypt", false)

            // When: User starts download
            manager.downloadMap("cairo_egypt")
            assertEquals(MapFeatureState.Pending, manager.featureState.first())

            // Simulate download completing in background
            testScope.launch {
                delay(100) // Simulate download time
                testAdapter.setModuleInstalled("cairo_egypt", true)
                testAdapter.simulateInstallComplete("cairo_egypt")
                manager.handleInstallComplete(listOf("cairo_egypt"))
            }

            // Files not extracted yet (race condition)
            testAdapter.setFilesAvailable("cairo_egypt", false)

            testScope.testScheduler.advanceTimeBy(100)
            testScope.testScheduler.runCurrent()

            // Then: State shows Installed from handleInstallComplete
            assertEquals(MapFeatureState.Installed, manager.featureState.first())

            // But: Next availability check should detect missing files
            testAdapter.resetCallFlags()
            val filesReady = testAdapter.isMapInstalled("cairo_egypt")
            assertFalse(filesReady, "Should detect files not yet extracted")

            // When: Files become available (extraction completes)
            testAdapter.setFilesAvailable("cairo_egypt", true)
            val filesReadyAfter = testAdapter.isMapInstalled("cairo_egypt")
            assertTrue(filesReadyAfter, "Should detect files now available")
        }

    /**
     * Tests that corrupted files (empty or zero-byte) are detected.
     */
    @Test
    fun `corrupted empty files should fail validation`() =
        runTest {
            // Given: Module installed
            testAdapter.setModuleInstalled("berlin_germany", true)

            // But files are empty (corrupted)
            testAdapter.setFilesAvailable("berlin_germany", false, fileExists = true, fileEmpty = true)

            // When: Check if map installed
            val isInstalled = testAdapter.isMapInstalled("berlin_germany")

            // Then: Should fail validation
            assertFalse(isInstalled, "Empty files should fail validation")
        }

    /**
     * Tests that partial installations (missing one file) are detected.
     */
    @Test
    fun `missing geojson file should fail validation`() =
        runTest {
            // Given: Module installed with only mbtiles
            testAdapter.setModuleInstalled("tokyo_japan", true)
            testAdapter.setFilesAvailable("tokyo_japan", false, mbtilesOnly = true)

            // When: Check if map installed
            val isInstalled = testAdapter.isMapInstalled("tokyo_japan")

            // Then: Should fail validation (missing geojson)
            assertFalse(isInstalled, "Map with missing geojson should fail validation")
        }

    /**
     * Tests that checking availability multiple times is idempotent.
     */
    @Test
    fun `multiple availability checks should be consistent`() =
        runTest {
            // Given: Map installed with valid files
            testAdapter.setModuleInstalled("paris_france", true)
            testAdapter.setFilesAvailable("paris_france", true)

            // When: Check availability multiple times
            val results =
                (1..5).map {
                    testAdapter.resetCallFlags()
                    testAdapter.isMapInstalled("paris_france")
                }

            // Then: All checks should return same result
            assertTrue(results.all { it }, "All checks should return true consistently")
        }

    /**
     * Beijing Issue: Download already in progress when user enters screen.
     * Should not trigger new download, should show existing progress.
     */
    @Test
    fun `downloadMap with active session should not start duplicate download`() =
        runTest {
            // Given: Download already in progress (69% like Beijing scenario)
            testAdapter.setModuleInstalled("beijing_china", false)
            testAdapter.setFilesAvailable("beijing_china", false)

            // First download starts
            manager.downloadMap("beijing_china")
            assertEquals(MapFeatureState.Pending, manager.featureState.first())

            // Simulate download progressing
            manager.handleDownloadProgress(193849681, 133946804) // 69%
            val progressState = manager.featureState.first() as MapFeatureState.Downloading
            assertEquals(69, progressState.progress)

            // When: User clicks download again (retry button)
            testAdapter.resetCallFlags()
            manager.downloadMap("beijing_china")

            // Then: Should be ignored (not start new download)
            assertFalse(testAdapter.startPlatformDownloadCalled, "Should not start duplicate download")

            // And: State should remain Downloading with same progress
            val stateAfter = manager.featureState.first()
            assertTrue(stateAfter is MapFeatureState.Downloading, "Should still be downloading")
        }

    /**
     * Test helper adapter that simulates file-based validation.
     * Allows controlling module installation state AND file availability separately.
     */
    private class FileValidationTestAdapter : PlatformMapDownloadAdapter {
        var isMapInstalledCalled = false
        var startPlatformDownloadCalled = false
        var errorMessage: String = "Test error"

        private val installedModules = mutableSetOf<String>()
        private val availableFiles = mutableMapOf<String, FileState>()

        data class FileState(
            val filesAvailable: Boolean = false,
            val mbtilesExists: Boolean = false,
            val geojsonExists: Boolean = false,
            val mbtilesEmpty: Boolean = false,
            val geojsonEmpty: Boolean = false,
        )

        fun setModuleInstalled(
            mapId: String,
            installed: Boolean,
        ) {
            if (installed) {
                installedModules.add(mapId)
            } else {
                installedModules.remove(mapId)
            }
        }

        fun setFilesAvailable(
            mapId: String,
            available: Boolean,
            fileExists: Boolean = available,
            fileEmpty: Boolean = false,
            mbtilesOnly: Boolean = false,
        ) {
            availableFiles[mapId] =
                FileState(
                    filesAvailable = available && !fileEmpty && !mbtilesOnly,
                    mbtilesExists = fileExists,
                    geojsonExists = fileExists && !mbtilesOnly,
                    mbtilesEmpty = fileEmpty,
                    geojsonEmpty = fileEmpty,
                )
        }

        fun simulateInstallComplete(mapId: String) {
            // Simulates PlayCore INSTALLED event
            installedModules.add(mapId)
        }

        fun resetCallFlags() {
            isMapInstalledCalled = false
            startPlatformDownloadCalled = false
        }

        override suspend fun isMapInstalled(mapId: String): Boolean {
            isMapInstalledCalled = true

            // Simulate AndroidMapViewModel file validation logic
            val moduleInstalled = installedModules.contains(mapId)
            if (!moduleInstalled) {
                return false
            }

            // Check file availability (simulating actual file system check)
            val fileState = availableFiles[mapId] ?: FileState()

            val mbtilesValid = fileState.mbtilesExists && !fileState.mbtilesEmpty
            val geojsonValid = fileState.geojsonExists && !fileState.geojsonEmpty

            return mbtilesValid && geojsonValid
        }

        override suspend fun startPlatformDownload(
            mapId: String,
            onMapDownloaded: (() -> Unit)?,
        ) {
            startPlatformDownloadCalled = true
            onMapDownloaded?.invoke()
        }

        override suspend fun cancelPlatformDownload() {}

        override fun getLocalizedErrorMessage(errorCode: Int): String = errorMessage

        override fun clearCacheForInstalledMaps(mapIds: List<String>) {}
    }
}
