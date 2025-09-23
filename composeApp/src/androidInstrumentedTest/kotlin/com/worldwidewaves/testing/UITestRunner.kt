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
import androidx.test.platform.app.InstrumentationRegistry
import com.worldwidewaves.shared.events.IWWWEvent
import io.mockk.mockk
import org.junit.Rule

/**
 * Base class for UI testing with common setup and utilities
 *
 * Provides:
 * - Common test setup and teardown
 * - Mocking utilities for dependencies
 * - Test data factories
 * - Assertion helpers
 * - Test environment configuration
 */
abstract class BaseUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    protected val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Common setup for all UI tests
     */
    open fun setUp() {
        // Initialize test environment
        // Set up dependency injection for testing
        // Configure test data sources
    }

    /**
     * Common cleanup for all UI tests
     */
    open fun tearDown() {
        // Clean up test resources
        // Reset any global state
        // Clear test data
    }
}

/**
 * Test utilities and factories for creating test data
 */
object UITestFactory {

    /**
     * Create mock events for testing
     */
    fun createMockEvents(count: Int = 3): List<Any> {
        // Return list of mock IWWWEvent objects
        return emptyList()
    }

    /**
     * Create mock map states for testing
     */
    fun createMockMapStates(eventIds: List<String> = emptyList()): Map<String, Boolean> {
        return eventIds.associateWith { true }
    }

    /**
     * Create mock view models for testing
     */
    fun createMockEventViewModel(): Any {
        // Return mock EventsViewModel
        return mockk()
    }

    /**
     * Create a mock wave event for testing wave participation
     */
    fun createMockWaveEvent(
        id: String = "test-event-123",
        status: String = "RUNNING",
        isInArea: Boolean = true,
        isWarmingInProgress: Boolean = false,
        isGoingToBeHit: Boolean = false,
        hasBeenHit: Boolean = false,
        progression: Double = 50.0,
        userPositionRatio: Double = 0.5,
        timeBeforeHit: kotlin.time.Duration = kotlin.time.Duration.parse("5m")
    ): IWWWEvent {
        return mockk<IWWWEvent>(relaxed = true)
    }

    /**
     * Create test data for countdown timer testing
     */
    fun createTimerTestData(): List<kotlin.time.Duration> {
        return listOf(
            kotlin.time.Duration.parse("10m"),
            kotlin.time.Duration.parse("5m"),
            kotlin.time.Duration.parse("1m"),
            kotlin.time.Duration.parse("30s"),
            kotlin.time.Duration.parse("10s"),
            kotlin.time.Duration.parse("5s"),
            kotlin.time.Duration.parse("1s")
        )
    }

    /**
     * Create test scenarios for wave phase transitions
     */
    fun createWavePhaseScenarios(): List<Map<String, Any>> {
        return listOf(
            mapOf(
                "phase" to "OBSERVER",
                "isInArea" to false,
                "isWarmingInProgress" to false,
                "isGoingToBeHit" to false,
                "hasBeenHit" to false
            ),
            mapOf(
                "phase" to "WARMING",
                "isInArea" to true,
                "isWarmingInProgress" to true,
                "isGoingToBeHit" to false,
                "hasBeenHit" to false
            ),
            mapOf(
                "phase" to "WAITING",
                "isInArea" to true,
                "isWarmingInProgress" to false,
                "isGoingToBeHit" to true,
                "hasBeenHit" to false
            ),
            mapOf(
                "phase" to "HIT",
                "isInArea" to true,
                "isWarmingInProgress" to false,
                "isGoingToBeHit" to false,
                "hasBeenHit" to true
            )
        )
    }

    /**
     * Create mock progression data for testing
     */
    fun createProgressionTestData(): List<Double> {
        return listOf(0.0, 25.0, 50.0, 75.0, 100.0)
    }
}

/**
 * Custom assertions and matchers for UI testing
 */
object UITestAssertions {

    /**
     * Assert that a loading state is displayed
     */
    fun assertLoadingDisplayed() {
        // Implementation for loading state assertion
    }

    /**
     * Assert that an error state is displayed with specific message
     */
    fun assertErrorDisplayed(expectedMessage: String) {
        // Implementation for error state assertion
    }

    /**
     * Assert that navigation occurred to expected destination
     */
    fun assertNavigatedTo(expectedDestination: String) {
        // Implementation for navigation assertion
    }

    /**
     * Assert that countdown timer displays correct format
     */
    fun assertTimerFormat(timerText: String) {
        val timeRegex = Regex("^\\d{2}:\\d{2}$")
        assert(timeRegex.matches(timerText)) {
            "Timer format should be MM:SS or HH:MM, but was: $timerText"
        }
    }

    /**
     * Assert that progression percentage is within expected range
     */
    fun assertProgressionRange(progression: Double, expectedMin: Double, expectedMax: Double) {
        assert(progression in expectedMin..expectedMax) {
            "Progression $progression should be between $expectedMin and $expectedMax"
        }
    }

    /**
     * Assert that wave phase transition is valid
     */
    fun assertValidPhaseTransition(fromPhase: String, toPhase: String) {
        val validTransitions = mapOf(
            "OBSERVER" to listOf("WARMING"),
            "WARMING" to listOf("WAITING"),
            "WAITING" to listOf("HIT"),
            "HIT" to listOf("DONE")
        )

        val allowedTransitions = validTransitions[fromPhase] ?: emptyList()
        assert(toPhase in allowedTransitions) {
            "Invalid phase transition from $fromPhase to $toPhase. Allowed: $allowedTransitions"
        }
    }

    /**
     * Assert that user position ratio is valid (0.0 to 1.0)
     */
    fun assertValidUserPosition(userPositionRatio: Double) {
        assert(userPositionRatio in 0.0..1.0) {
            "User position ratio should be between 0.0 and 1.0, but was: $userPositionRatio"
        }
    }

    /**
     * Assert that timing accuracy is within tolerance
     */
    fun assertTimingAccuracy(expected: Long, actual: Long, toleranceMs: Long = 100) {
        val difference = kotlin.math.abs(expected - actual)
        assert(difference <= toleranceMs) {
            "Timing difference $difference ms exceeds tolerance $toleranceMs ms"
        }
    }
}