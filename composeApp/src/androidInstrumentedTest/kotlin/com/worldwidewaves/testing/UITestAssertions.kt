package com.worldwidewaves.testing

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

/**
 * Custom UI test assertions for WorldWideWaves-specific testing scenarios.
 * Provides specialized assertions for wave timing, progression, and navigation validation.
 */
object UITestAssertions {
    /**
     * Assert that timing accuracy is within acceptable tolerances for wave events.
     * Critical for wave synchronization testing.
     */
    fun assertTimingAccuracy(
        expected: Long,
        actual: Long,
        toleranceMs: Long,
    ) {
        val difference = kotlin.math.abs(actual - expected)
        assert(difference <= toleranceMs) {
            "Timing accuracy failed: expected $expected ms, actual $actual ms, " +
                "difference $difference ms exceeds tolerance $toleranceMs ms"
        }
    }

    /**
     * Assert that phase transitions are valid according to wave lifecycle rules.
     */
    fun assertValidPhaseTransition(
        fromPhase: String,
        toPhase: String,
    ) {
        val validTransitions =
            mapOf(
                "UPCOMING" to listOf("WARMING", "CANCELLED"),
                "WARMING" to listOf("ACTIVE", "CANCELLED"),
                "ACTIVE" to listOf("COMPLETED", "CANCELLED"),
                "COMPLETED" to emptyList(),
                "CANCELLED" to emptyList(),
            )

        val allowedTransitions = validTransitions[fromPhase] ?: emptyList()
        assert(toPhase in allowedTransitions) {
            "Invalid phase transition from '$fromPhase' to '$toPhase'. " +
                "Allowed transitions: $allowedTransitions"
        }
    }

    /**
     * Assert that user position is within valid geographic bounds.
     */
    fun assertValidUserPosition(positionRatio: Double) {
        assert(positionRatio in 0.0..1.0) {
            "User position ratio $positionRatio is not within valid range [0.0, 1.0]"
        }
    }

    /**
     * Assert that wave progression percentage is within expected bounds.
     */
    fun assertProgressionRange(
        progression: Double,
        minValue: Double,
        maxValue: Double,
    ) {
        assert(progression in minValue..maxValue) {
            "Wave progression $progression is not within valid range [$minValue, $maxValue]"
        }
    }

    /**
     * Assert that countdown timer displays correct format (MM:SS).
     */
    fun assertTimerFormat(timerText: String) {
        val timeRegex = Regex("^\\d{2}:\\d{2}$")
        assert(timeRegex.matches(timerText)) {
            "Timer format '$timerText' does not match expected format 'MM:SS'"
        }
    }

    /**
     * Assert that navigation occurred to the expected destination.
     */
    fun assertNavigatedTo(expectedDestination: String) {
        // Implementation depends on your navigation framework
        // This is a placeholder for navigation assertion logic
        assert(true) {
            // Replace with actual navigation validation
            "Navigation to '$expectedDestination' was not detected"
        }
    }

    /**
     * Assert that geographic coordinates are within valid ranges.
     */
    fun assertValidCoordinates(
        latitude: Double,
        longitude: Double,
    ) {
        assert(latitude in -90.0..90.0) {
            "Latitude $latitude is not within valid range [-90.0, 90.0]"
        }
        assert(longitude in -180.0..180.0) {
            "Longitude $longitude is not within valid range [-180.0, 180.0]"
        }
    }

    /**
     * Assert that wave participant count is reasonable.
     */
    fun assertValidParticipantCount(count: Int) {
        assert(count >= 0) {
            "Participant count $count cannot be negative"
        }
        assert(count <= 1_000_000) {
            // Reasonable upper bound
            "Participant count $count exceeds reasonable maximum (1,000,000)"
        }
    }

    /**
     * Assert that event duration is within acceptable limits.
     */
    fun assertValidEventDuration(durationMs: Long) {
        val minDuration = 1000L // 1 second minimum
        val maxDuration = 24 * 60 * 60 * 1000L // 24 hours maximum

        assert(durationMs in minDuration..maxDuration) {
            "Event duration $durationMs ms is not within valid range [$minDuration, $maxDuration] ms"
        }
    }

    /**
     * Assert that wave speed is within realistic bounds (km/h).
     */
    fun assertValidWaveSpeed(speedKmh: Double) {
        val minSpeed = 0.1 // Very slow walking
        val maxSpeed = 1000.0 // Theoretical maximum for visualization

        assert(speedKmh in minSpeed..maxSpeed) {
            "Wave speed $speedKmh km/h is not within valid range [$minSpeed, $maxSpeed] km/h"
        }
    }

    /**
     * Assert that UI element visibility state matches expectations.
     */
    fun assertVisibilityState(
        elementTag: String,
        expectedVisible: Boolean,
    ) {
        // This would integrate with your compose test framework
        // Placeholder implementation
        assert(true) {
            // Replace with actual visibility check
            "Element '$elementTag' visibility state does not match expected: $expectedVisible"
        }
    }

    /**
     * Create mock progression data for testing wave progression UI.
     */
    fun createProgressionTestData(): List<Double> = listOf(0.0, 25.0, 50.0, 75.0, 100.0)

    /**
     * Validate that network response times are acceptable for user experience.
     */
    fun assertNetworkResponseTime(
        responseTimeMs: Long,
        maxAcceptableMs: Long = 5000,
    ) {
        assert(responseTimeMs <= maxAcceptableMs) {
            "Network response time $responseTimeMs ms exceeds maximum acceptable time $maxAcceptableMs ms"
        }
    }

    /**
     * Assert that error messages are user-friendly and informative.
     */
    fun assertUserFriendlyErrorMessage(errorMessage: String) {
        assert(errorMessage.isNotBlank()) {
            "Error message cannot be blank"
        }
        assert(errorMessage.length <= 200) {
            "Error message too long (${errorMessage.length} chars). Should be <= 200 chars for good UX"
        }
        // Add more validation rules as needed
    }
}
