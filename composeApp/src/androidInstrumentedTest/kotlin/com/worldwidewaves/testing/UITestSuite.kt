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

import com.worldwidewaves.compose.common.CommonComponentsTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive UI Test Suite for WorldWideWaves
 *
 * This suite covers all critical user workflows identified in the project evaluation:
 *
 * 1. **Core Navigation Flow** (MainActivityTest)
 *    - App startup and splash screen
 *    - Tab navigation between Events/About
 *    - Location permission handling
 *
 * 2. **Events Discovery** (EventsListScreenTest)
 *    - Events list display and filtering
 *    - Favorite and download management
 *    - Event selection workflow
 *
 * 3. **Wave Participation** (WaveActivityTest) - CRITICAL
 *    - Real-time wave coordination
 *    - Choreography and timing display
 *    - User participation tracking
 *
 * 4. **Common UI Components** (CommonComponentsTest)
 *    - Reusable component functionality
 *    - Accessibility compliance
 *    - Theming and responsiveness
 *
 * Run this suite to validate the complete user experience end-to-end.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    CommonComponentsTest::class
)
class UITestSuite

/**
 * Priority-based test categories for different testing scenarios
 */
object TestCategories {
    /**
     * Critical path tests - must pass for core functionality
     * Focus on wave participation and core user flows
     */
    const val CRITICAL = "critical"

    /**
     * Feature tests - important functionality but not blocking
     * Focus on secondary features and edge cases
     */
    const val FEATURE = "feature"

    /**
     * Accessibility tests - ensure inclusive design
     * Focus on screen reader support and accessibility compliance
     */
    const val ACCESSIBILITY = "accessibility"

    /**
     * Performance tests - validate UI responsiveness
     * Focus on smooth animations and responsive interactions
     */
    const val PERFORMANCE = "performance"
}

/**
 * Test execution configuration and utilities
 */
object UITestConfig {
    /**
     * Whether to run tests in isolation (slower but more reliable)
     */
    const val ISOLATED_EXECUTION = true

    /**
     * Maximum timeout for UI interactions (milliseconds)
     */
    const val UI_TIMEOUT_MS = 5000L

    /**
     * Whether to capture screenshots on test failures
     */
    const val CAPTURE_SCREENSHOTS = true

    /**
     * Test device configuration
     */
    const val TEST_DEVICE_DPI = 420
    const val TEST_DEVICE_WIDTH = 1080
    const val TEST_DEVICE_HEIGHT = 2340
}