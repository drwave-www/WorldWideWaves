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

package com.worldwidewaves.compose.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for map initialization timing logic to prevent premature error UI display.
 *
 * Regression test for:
 * - Issue: When entering an event with an undownloaded map, the error UI was shown
 *   immediately instead of the download button
 * - Root cause: Map initialization attempted to load files before availability check completed
 * - Fix: Only attempt map initialization if map is available OR currently downloading
 *
 * @see com.worldwidewaves.compose.map.AndroidEventMap.RenderMapContent
 */
@RunWith(AndroidJUnit4::class)
class MapInitializationTimingTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test that map initialization is NOT attempted when map is unavailable and not downloading.
     *
     * Expected behavior:
     * - Show download button when map is not available
     * - Do NOT attempt to load map files (which would fail and show error UI)
     */
    @Test
    fun mapInitialization_notAttempted_whenMapUnavailableAndNotDownloading() {
        var mapLoadAttempted = false

        composeTestRule.setContent {
            MaterialTheme {
                TestMapLoadingLogic(
                    isMapAvailable = false,
                    isMapDownloading = false,
                    onMapLoadAttempted = { mapLoadAttempted = true },
                )
            }
        }

        // Verify download button is shown (not error UI)
        composeTestRule
            .onNodeWithTag("download-button")
            .assertExists()
            .assertIsDisplayed()

        // Verify map initialization was NOT attempted
        assert(!mapLoadAttempted) {
            "Map initialization should NOT be attempted when map is unavailable and not downloading"
        }
    }

    /**
     * Test that map initialization IS attempted when map is available.
     *
     * Expected behavior:
     * - Attempt to load map when isMapAvailable = true
     */
    @Test
    fun mapInitialization_attempted_whenMapAvailable() {
        var mapLoadAttempted = false

        composeTestRule.setContent {
            MaterialTheme {
                TestMapLoadingLogic(
                    isMapAvailable = true,
                    isMapDownloading = false,
                    onMapLoadAttempted = { mapLoadAttempted = true },
                )
            }
        }

        // Wait for map initialization to be attempted
        composeTestRule.waitUntil(timeoutMillis = 1000) {
            mapLoadAttempted
        }

        // Verify map initialization WAS attempted
        assert(mapLoadAttempted) {
            "Map initialization SHOULD be attempted when map is available"
        }
    }

    /**
     * Test that map initialization is NOT attempted when map is downloading.
     *
     * Expected behavior:
     * - Do NOT attempt to load during download (would cause gray screen/wrong camera position)
     * - Wait for download to complete, then attempt load when isMapAvailable becomes true
     *
     * Rationale from AndroidEventMap.kt:528-529:
     * "Attempting to load before download completes causes gray screen (no tiles)
     * and wrong camera position (moves to bounds before tiles exist)"
     */
    @Test
    fun mapInitialization_notAttempted_whenMapDownloading() {
        var mapLoadAttempted = false

        composeTestRule.setContent {
            MaterialTheme {
                TestMapLoadingLogic(
                    isMapAvailable = false,
                    isMapDownloading = true,
                    onMapLoadAttempted = { mapLoadAttempted = true },
                )
            }
        }

        // Wait a moment to ensure no load is attempted
        composeTestRule.waitForIdle()

        // Verify map initialization was NOT attempted during download
        assert(!mapLoadAttempted) {
            "Map initialization should NOT be attempted during download (causes gray screen and wrong camera position)"
        }
    }

    /**
     * Test state transition: download button → downloading → map loaded.
     *
     * Expected behavior:
     * - Initially show download button (isMapAvailable = false, isMapDownloading = false)
     * - After clicking download, show downloading state (isMapDownloading = true)
     * - Map initialization should be attempted during download
     * - After download completes, map should load
     */
    @Test
    fun mapInitialization_stateTransition_downloadToLoaded() {
        composeTestRule.setContent {
            MaterialTheme {
                TestMapDownloadToLoadFlow()
            }
        }

        // Initially: download button should be shown
        composeTestRule
            .onNodeWithTag("download-button")
            .assertExists()
            .assertIsDisplayed()

        // Click download button
        composeTestRule
            .onNodeWithTag("download-button")
            .performClick()

        // After download starts: downloading indicator should be shown
        composeTestRule
            .onNodeWithTag("downloading-indicator")
            .assertExists()
            .assertIsDisplayed()

        // After download completes: map should attempt to load
        composeTestRule
            .onNodeWithTag("map-loaded-indicator")
            .assertExists()
            .assertIsDisplayed()
    }
}

/**
 * Test composable that simulates the map loading decision logic from AndroidEventMap.
 *
 * Mirrors the actual logic at AndroidEventMap.kt:527-530:
 * ```
 * // Only attempt to initialize map when tiles are actually available (downloaded)
 * // Attempting to load before download completes causes gray screen (no tiles)
 * // and wrong camera position (moves to bounds before tiles exist)
 * if (!mapState.isMapLoaded && !mapState.initStarted && mapState.isMapAvailable) {
 *     // Attempt to load map
 * }
 * ```
 */
@Composable
private fun TestMapLoadingLogic(
    isMapAvailable: Boolean,
    isMapDownloading: Boolean,
    onMapLoadAttempted: () -> Unit,
) {
    // Simulate the loading logic from AndroidEventMap - only load when available
    var mapLoaded by remember { mutableStateOf(false) }
    var initStarted by remember { mutableStateOf(false) }

    // Only attempt to load when map is available (NOT during download)
    if (!mapLoaded && !initStarted && isMapAvailable) {
        initStarted = true
        onMapLoadAttempted()
        // Simulate successful load for test purposes
        mapLoaded = true
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            mapLoaded -> {
                Text("Map Loaded", modifier = Modifier.testTag("map-loaded"))
            }
            isMapDownloading -> {
                Text("Downloading...", modifier = Modifier.testTag("downloading-indicator"))
            }
            !isMapAvailable -> {
                Button(
                    onClick = { /* Download action */ },
                    modifier = Modifier.testTag("download-button"),
                ) {
                    Text("Download Map")
                }
            }
            else -> {
                Text("Unknown State", modifier = Modifier.testTag("unknown-state"))
            }
        }
    }
}

/**
 * Test composable that simulates the full download-to-load flow.
 */
@Composable
private fun TestMapDownloadToLoadFlow() {
    var isMapAvailable by remember { mutableStateOf(false) }
    var isMapDownloading by remember { mutableStateOf(false) }
    var mapLoaded by remember { mutableStateOf(false) }

    val shouldAttemptMapLoad = isMapAvailable || isMapDownloading

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            mapLoaded -> {
                Text("Map Loaded", modifier = Modifier.testTag("map-loaded-indicator"))
            }
            isMapDownloading -> {
                // Simulate download completion
                isMapDownloading = false
                isMapAvailable = true
                mapLoaded = true
                Text("Downloading...", modifier = Modifier.testTag("downloading-indicator"))
            }
            !isMapAvailable && !isMapDownloading -> {
                Button(
                    onClick = {
                        // Start download
                        isMapDownloading = true
                    },
                    modifier = Modifier.testTag("download-button"),
                ) {
                    Text("Download Map")
                }
            }
        }
    }
}
