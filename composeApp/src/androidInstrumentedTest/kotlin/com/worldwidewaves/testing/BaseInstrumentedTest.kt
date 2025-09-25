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

import androidx.compose.ui.test.junit4.createComposeRule
import com.worldwidewaves.shared.testing.PerformanceMonitor
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule

/**
 * Base class for all Android instrumented tests in WorldWideWaves.
 *
 * Provides common setup, utilities, and patterns to reduce boilerplate
 * and ensure consistency across test suites.
 */
abstract class BaseInstrumentedTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    protected lateinit var mockPerformanceMonitor: PerformanceMonitor

    /**
     * Common setup performed before each test.
     * Subclasses can override and call super.setUp() for additional setup.
     */
    @Before
    open fun setUp() {
        // Initialize common mocks
        mockPerformanceMonitor = mockk(relaxed = true)

        // Setup any common test environment
        setupTestEnvironment()
    }

    /**
     * Sets up the common test environment.
     * Override this method to add test-specific environment setup.
     */
    protected open fun setupTestEnvironment() {
        // Default implementation - can be overridden by subclasses
    }

    /**
     * Helper function to create performance traces for tests.
     * Standardizes performance monitoring across all test suites.
     */
    protected fun createPerformanceTrace(traceName: String): com.worldwidewaves.shared.monitoring.PerformanceTrace {
        val trace = mockk<com.worldwidewaves.shared.monitoring.PerformanceTrace>(relaxed = true)
        io.mockk.every { mockPerformanceMonitor.startTrace(traceName) } returns trace
        return trace
    }

    /**
     * Standard timeout values for different types of operations.
     * Using consistent timeouts helps reduce flaky tests.
     */
    companion object {
        const val STANDARD_TIMEOUT = 3000L
        const val LONG_TIMEOUT = 5000L
        const val SHORT_TIMEOUT = 1000L
        const val ANIMATION_TIMEOUT = 2000L
    }
}
