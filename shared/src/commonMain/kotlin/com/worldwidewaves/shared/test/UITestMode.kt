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
import kotlin.concurrent.Volatile

/**
 * UI Test Mode singleton for managing test environment state.
 *
 * ## Purpose
 * Provides a global flag to detect when the app is running in UI test mode,
 * allowing different behavior for automated testing (e.g., test data seeding,
 * faster animations, mock services).
 *
 * ## Usage
 * ```kotlin
 * // From iOS (SceneDelegate)
 * enableUITestMode()
 *
 * // From Android (MainActivity)
 * enableUITestMode()
 *
 * // Check in app code
 * if (UITestMode.isEnabled) {
 *     // Use test data or mock services
 * }
 * ```
 *
 * ## Thread Safety
 * All operations are thread-safe via `@Volatile` annotation.
 *
 * ## Test Data Seeding
 * When enabled, automatically calls `TestDataSeeder.seedTestData()` to populate:
 * - Paris France event in "running" state
 * - paris_france map marked as downloaded
 * - Clear favorites list
 *
 * @see TestDataSeeder
 */
object UITestMode {
    private const val TAG = "UITestMode"

    /**
     * Flag indicating if UI test mode is enabled.
     * Set via `enableUITestMode()` from platform code.
     */
    @Volatile
    var isEnabled: Boolean = false
        private set

    /**
     * Enables UI test mode and seeds test data.
     *
     * Called from platform code when `--uitesting` launch argument is detected:
     * - iOS: SceneDelegate in `scene(_:willConnectTo:options:)`
     * - Android: MainActivity in `onCreate()`
     *
     * ## Side Effects
     * - Sets `isEnabled = true`
     * - Calls `TestDataSeeder.seedTestData()` to populate test fixtures
     * - Logs enablement for debugging
     *
     * ## Thread Safety
     * Thread-safe, can be called from any thread.
     *
     * @throws Exception if test data seeding fails
     */
    @Throws(Exception::class)
    fun enableUITestMode() {
        Log.i(TAG, "Enabling UI test mode...")
        isEnabled = true

        // Seed test data
        try {
            TestDataSeeder.seedTestData()
            Log.i(TAG, "UI test mode enabled - test data seeded")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed test data", throwable = e)
            throw e
        }
    }

    /**
     * Disables UI test mode (for manual testing or cleanup).
     *
     * ## Note
     * Typically not needed as test mode is process-scoped,
     * but useful for manual testing scenarios.
     */
    fun disableUITestMode() {
        Log.i(TAG, "Disabling UI test mode")
        isEnabled = false
    }
}
