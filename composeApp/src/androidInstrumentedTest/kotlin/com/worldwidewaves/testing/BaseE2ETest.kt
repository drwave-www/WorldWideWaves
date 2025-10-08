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

package com.worldwidewaves.testing

import android.os.Build
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.worldwidewaves.activities.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule

/**
 * Base class for Firebase Test Lab End-to-End UI tests.
 *
 * Provides:
 * - MainActivity launch with Compose test rule
 * - Screenshot capture utilities with device info
 * - Test setup/teardown with proper cleanup
 *
 * Usage:
 * ```
 * class MyE2ETest : BaseE2ETest() {
 *     @Test
 *     fun testUserJourney() {
 *         captureScreenshot("01_step_one")
 *         // ... test logic
 *     }
 * }
 * ```
 */
abstract class BaseE2ETest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    protected var screenshotCounter = 0

    /**
     * Setup performed before each E2E test.
     * - Resets screenshot counter
     * - Waits for app to be ready
     */
    @Before
    open fun setUp() {
        screenshotCounter = 0

        // Wait for app to idle before starting test
        composeTestRule.waitForIdle()
    }

    /**
     * Teardown performed after each E2E test.
     * - Captures final screenshot if needed
     * - Performs cleanup
     */
    @After
    open fun tearDown() {
        // Optional: Capture final state screenshot
        // captureScreenshot("final_state")
    }

    /**
     * Captures a screenshot with device information in filename.
     * Automatically includes device model and API level for Firebase Test Lab.
     *
     * @param name Base name for the screenshot (e.g., "01_app_launch")
     * @param folder Optional folder to organize screenshots (default: "e2e_screenshots")
     */
    protected fun captureScreenshot(
        name: String,
        folder: String = "e2e_screenshots",
    ) {
        val deviceModel = Build.MODEL.replace(" ", "_")
        val deviceVersion = "API${Build.VERSION.SDK_INT}"
        val filename = "${name}_${deviceModel}_$deviceVersion"

        ScreenshotTestUtils.captureScreenshot(composeTestRule, filename, folder)
        screenshotCounter++
    }

    /**
     * Captures a numbered screenshot for sequential test steps.
     * Automatically increments counter.
     *
     * @param stepDescription Description of the step (e.g., "app_launch")
     */
    protected fun captureStepScreenshot(stepDescription: String) {
        val stepNumber = String.format("%02d", screenshotCounter + 1)
        captureScreenshot("${stepNumber}_$stepDescription")
    }

    /**
     * Waits for a specified condition with timeout.
     *
     * @param timeoutMs Timeout in milliseconds (default: 5000)
     * @param condition The condition to wait for
     * @return true if condition met within timeout, false otherwise
     */
    protected fun waitForCondition(
        timeoutMs: Long = 5000,
        condition: () -> Boolean,
    ): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition()) {
                return true
            }
            Thread.sleep(100)
        }
        return false
    }

    /**
     * Waits for the Compose UI to be idle.
     * Useful between steps that trigger async operations.
     */
    protected fun waitForIdle() {
        composeTestRule.waitForIdle()
    }
}
