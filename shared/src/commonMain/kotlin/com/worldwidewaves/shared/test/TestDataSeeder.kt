/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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

package com.worldwidewaves.shared.test

import com.worldwidewaves.shared.utils.Log

/**
 * Test Data Seeder for UI tests.
 *
 * ## Purpose
 * Populates the app with test data fixtures required for UI tests to run successfully.
 * Called automatically when UI test mode is enabled via `UITestMode.enableUITestMode()`.
 *
 * ## Test Data Fixtures
 * - **Paris France Event**: Running event with paris_france map
 * - **Downloaded Maps**: Marks paris_france map as downloaded
 * - **Empty Favorites**: Clears favorites list for fresh state
 *
 * ## Usage
 * ```kotlin
 * // Automatically called from UITestMode.enableUITestMode()
 * TestDataSeeder.seedTestData()
 * ```
 *
 * ## Implementation Notes
 * This is a minimal implementation that sets up the environment for UI tests.
 * For full test data seeding with actual events, this would need to:
 * - Create mock WWWEvent objects
 * - Populate Firestore with test data
 * - Configure map download state
 *
 * Currently, it serves as a placeholder to ensure:
 * - App knows it's in test mode
 * - Proper initialization sequence
 * - Logging for debugging test failures
 *
 * @see UITestMode
 */
object TestDataSeeder {
    private const val TAG = "TestDataSeeder"

    /**
     * Seeds test data for UI tests.
     *
     * ## Current Implementation
     * Minimal setup that:
     * - Logs test data seeding start
     * - Ensures app is in a known state for testing
     * - Returns successfully to allow test to proceed
     *
     * ## Future Implementation
     * Should populate:
     * 1. Paris France event in Firestore (running state)
     * 2. Mark paris_france map as downloaded
     * 3. Clear favorites list
     * 4. Set user position in Paris bounds for simulation
     *
     * ## Thread Safety
     * Thread-safe, can be called from any thread.
     *
     * @throws Exception if data seeding fails (currently never throws)
     */
    fun seedTestData() {
        Log.i(TAG, "Seeding test data for UI tests...")

        // TODO: Implement actual test data seeding
        // For now, just log that we're in test mode
        // The test will need to rely on:
        // 1. Existing Firebase data (if any)
        // 2. Proper accessibility labels on empty states
        // 3. Longer timeouts for slower operations

        Log.i(TAG, "Test data seeding complete (placeholder)")
        Log.w(
            TAG,
            "NOTE: Actual test data seeding not yet implemented. " +
                "Tests may fail if Firebase data is missing.",
        )
    }

    /**
     * Creates a test Paris France event (placeholder).
     *
     * ## Future Implementation
     * Should create and return a WWWEvent with:
     * - id: "paris_france_test"
     * - country: "France"
     * - community: "Paris"
     * - status: RUNNING
     * - map: paris_france (marked as downloaded)
     * - Area covering Paris coordinates
     *
     * @return Mock event object (not yet implemented)
     */
    private fun createParisTestEvent() {
        // TODO: Create actual WWWEvent
        Log.d(TAG, "createParisTestEvent() - not yet implemented")
    }

    /**
     * Marks the paris_france map as downloaded (placeholder).
     *
     * ## Future Implementation
     * Should update map download state to mark paris_france as:
     * - Downloaded = true
     * - Available for offline use
     * - Visible in "Downloaded" filter
     */
    private fun markParisMapAsDownloaded() {
        // TODO: Update map download state
        Log.d(TAG, "markParisMapAsDownloaded() - not yet implemented")
    }

    /**
     * Clears the favorites list (placeholder).
     *
     * ## Future Implementation
     * Should:
     * - Clear all favorited events
     * - Ensure "Favorites" filter shows empty state
     * - Return favorites to clean slate for testing
     */
    private fun clearFavorites() {
        // TODO: Clear favorites
        Log.d(TAG, "clearFavorites() - not yet implemented")
    }
}
