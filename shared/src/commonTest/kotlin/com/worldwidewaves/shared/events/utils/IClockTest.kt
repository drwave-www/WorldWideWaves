package com.worldwidewaves.shared.events.utils

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

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Tests for IClock interface and DateTimeFormats functionality.
 * Tests DateTimeFormats indirectly through the IClock.instantToLiteral() consumer API.
 * Uses pattern-based assertions rather than exact string matching to handle locale variations.
 */
@OptIn(ExperimentalTime::class)
class IClockTest {

    @Test
    fun `should format time with standard pattern structure`() {
        // GIVEN: A known instant (Unix epoch)
        val instant = Instant.fromEpochSeconds(0)
        val timeZone = TimeZone.UTC

        // WHEN: Formatting the instant to literal time
        val result = IClock.instantToLiteral(instant, timeZone)

        // THEN: Should return a non-empty string with time-like pattern
        assertNotNull(result, "Result should not be null")
        assertTrue(result.isNotEmpty(), "Result should not be empty")
        assertTrue(result.length >= 3, "Time string should be at least 3 characters (e.g., '0:0')")
        assertTrue(result.length <= 8, "Time string should not exceed 8 characters (e.g., '12:34 AM')")
    }

    @Test
    fun `should contain time separator characters`() {
        // GIVEN: Various test instants
        val testCases = listOf(
            Instant.fromEpochSeconds(0), // 00:00 UTC
            Instant.fromEpochSeconds(3661), // 01:01 UTC
            Instant.fromEpochSeconds(43200), // 12:00 UTC
            Instant.fromEpochSeconds(86399) // 23:59 UTC
        )
        val timeZone = TimeZone.UTC

        testCases.forEach { instant ->
            // WHEN: Formatting the instant
            val result = IClock.instantToLiteral(instant, timeZone)

            // THEN: Should contain common time separators
            val hasTimePattern = result.contains(":") || result.contains(".") || result.contains("-")
            assertTrue(hasTimePattern, "Time string '$result' should contain time separator (:, ., or -)")
        }
    }

    @Test
    fun `should produce consistent results for same instant`() {
        // GIVEN: The same instant and timezone
        val instant = Instant.fromEpochSeconds(12345)
        val timeZone = TimeZone.UTC

        // WHEN: Formatting multiple times
        val result1 = IClock.instantToLiteral(instant, timeZone)
        val result2 = IClock.instantToLiteral(instant, timeZone)
        val result3 = IClock.instantToLiteral(instant, timeZone)

        // THEN: All results should be identical
        assertTrue(result1 == result2, "Multiple calls should produce identical results")
        assertTrue(result2 == result3, "Multiple calls should produce identical results")
    }

    @Test
    fun `should handle different timezones appropriately`() {
        // GIVEN: Same instant with different timezones
        val instant = Instant.fromEpochSeconds(43200) // 12:00 UTC
        val utc = TimeZone.UTC
        val zones = listOf(
            utc,
            TimeZone.of("America/New_York"),
            TimeZone.of("Europe/London"),
            TimeZone.of("Asia/Tokyo")
        )

        // WHEN: Formatting with different timezones
        val results = zones.map { timeZone ->
            IClock.instantToLiteral(instant, timeZone)
        }

        // THEN: All results should be valid time strings
        results.forEach { result ->
            assertNotNull(result, "Result should not be null for any timezone")
            assertTrue(result.isNotEmpty(), "Result should not be empty for any timezone")
            assertTrue(result.length >= 3, "Time string should be properly formatted")
        }

        // Note: We don't assert different values since some might be the same due to DST/locale formatting
    }

    @Test
    fun `should handle edge case times correctly`() {
        // GIVEN: Edge case instants
        val edgeCases = listOf(
            Instant.fromEpochSeconds(0), // Unix epoch start
            Instant.fromEpochSeconds(86400 - 1), // Last second of first day
            Instant.fromEpochSeconds(86400), // Start of second day
            Instant.fromEpochSeconds(1234567890), // Random future timestamp
            Instant.fromEpochSeconds(-86400) // Before epoch (if supported)
        )
        val timeZone = TimeZone.UTC

        edgeCases.forEach { instant ->
            // WHEN: Formatting edge case instant
            val result = IClock.instantToLiteral(instant, timeZone)

            // THEN: Should produce valid time string
            assertNotNull(result, "Edge case instant should format successfully")
            assertTrue(result.isNotEmpty(), "Edge case result should not be empty")
            assertTrue(result.length <= 10, "Edge case result should be reasonable length")
        }
    }

    @Test
    fun `should handle sub-second precision instants`() {
        // GIVEN: Instants with nanosecond precision
        val baseSeconds = 1609459200L // 2021-01-01 00:00:00 UTC
        val instantsWithNanos = listOf(
            Instant.fromEpochSeconds(baseSeconds, 0),
            Instant.fromEpochSeconds(baseSeconds, 500_000_000), // 0.5 seconds
            Instant.fromEpochSeconds(baseSeconds, 999_999_999), // 0.999... seconds
        )
        val timeZone = TimeZone.UTC

        instantsWithNanos.forEach { instant ->
            // WHEN: Formatting instant with sub-second precision
            val result = IClock.instantToLiteral(instant, timeZone)

            // THEN: Should handle gracefully (time formatters typically ignore sub-second)
            assertNotNull(result, "Sub-second precision should be handled")
            assertTrue(result.isNotEmpty(), "Sub-second precision result should not be empty")
        }
    }

    @Test
    fun `should produce valid time format patterns`() {
        // GIVEN: Various test times throughout the day
        val testTimes = listOf(
            0, // Midnight
            3600, // 1 AM
            21600, // 6 AM
            43200, // Noon
            54000, // 3 PM
            72000, // 8 PM
            82800 // 11 PM
        )
        val timeZone = TimeZone.UTC

        testTimes.forEach { epochSeconds ->
            // GIVEN: An instant for this time
            val instant = Instant.fromEpochSeconds(epochSeconds.toLong())

            // WHEN: Formatting the time
            val result = IClock.instantToLiteral(instant, timeZone)

            // THEN: Should match common time patterns
            val validPatterns = listOf(
                Regex("""\d{1,2}:\d{2}"""), // HH:mm or H:mm
                Regex("""\d{1,2}:\d{2}\s*(AM|PM|am|pm)"""), // 12-hour with AM/PM
                Regex("""\d{1,2}\.\d{2}"""), // H.mm (some locales)
                Regex("""\d{1,2}-\d{2}"""), // H-mm (some locales)
                Regex("""\d{1,2}h\d{2}"""), // Hhmm (some locales)
                Regex("""\d{1,2}:\d{2}:\d{2}""") // HH:mm:ss (if seconds included)
            )

            val matchesPattern = validPatterns.any { pattern -> pattern.containsMatchIn(result) }
            assertTrue(
                matchesPattern,
                "Time string '$result' should match a valid time pattern for epoch $epochSeconds"
            )
        }
    }

    @Test
    fun `should handle hour boundary transitions correctly`() {
        // GIVEN: Instants around hour boundaries
        val hourBoundaryTests = listOf(
            Instant.fromEpochSeconds(3599), // 23:59:59 of first hour
            Instant.fromEpochSeconds(3600), // 01:00:00 exactly
            Instant.fromEpochSeconds(3601), // 01:00:01
            Instant.fromEpochSeconds(43199), // 11:59:59
            Instant.fromEpochSeconds(43200), // 12:00:00 exactly
            Instant.fromEpochSeconds(43201) // 12:00:01
        )
        val timeZone = TimeZone.UTC

        hourBoundaryTests.forEach { instant ->
            // WHEN: Formatting boundary time
            val result = IClock.instantToLiteral(instant, timeZone)

            // THEN: Should produce valid result without errors
            assertNotNull(result, "Hour boundary should be handled correctly")
            assertTrue(result.isNotEmpty(), "Hour boundary result should not be empty")
            assertTrue(result.length >= 3, "Hour boundary result should be properly formatted")
        }
    }

    @Test
    fun `should handle midnight and noon correctly`() {
        // GIVEN: Special times - midnight and noon in different days
        val specialTimes = listOf(
            Instant.fromEpochSeconds(0), // 1970-01-01 00:00:00 UTC (midnight)
            Instant.fromEpochSeconds(43200), // 1970-01-01 12:00:00 UTC (noon)
            Instant.fromEpochSeconds(86400), // 1970-01-02 00:00:00 UTC (next midnight)
            Instant.fromEpochSeconds(129600), // 1970-01-02 12:00:00 UTC (next noon)
        )
        val timeZone = TimeZone.UTC

        specialTimes.forEach { instant ->
            // WHEN: Formatting special time
            val result = IClock.instantToLiteral(instant, timeZone)

            // THEN: Should handle special times correctly
            assertNotNull(result, "Special time should be formatted")
            assertTrue(result.isNotEmpty(), "Special time result should not be empty")

            // Verify it contains typical midnight/noon indicators or neutral time format
            val epochSeconds = instant.epochSeconds
            val timeOfDay = (epochSeconds % 86400).toInt()
            when (timeOfDay) {
                0 -> { // Midnight
                    // Could be "00:00", "12:00 AM", "0:00", etc.
                    val hasMidnightPattern = result.contains("00") || result.contains("12") || result.contains("0")
                    assertTrue(hasMidnightPattern, "Midnight should contain appropriate time indicators: '$result'")
                }
                43200 -> { // Noon
                    // Could be "12:00", "12:00 PM", etc.
                    val hasNoonPattern = result.contains("12")
                    assertTrue(hasNoonPattern, "Noon should contain '12': '$result'")
                }
            }
        }
    }

    @Test
    fun `should handle rapid successive calls efficiently`() {
        // GIVEN: Multiple rapid calls
        val instant = Instant.fromEpochSeconds(12345)
        val timeZone = TimeZone.UTC
        val iterations = 100

        // WHEN: Making many rapid calls
        val results = (1..iterations).map {
            IClock.instantToLiteral(instant, timeZone)
        }

        // THEN: All results should be consistent and valid
        val firstResult = results.first()
        results.forEach { result ->
            assertTrue(result == firstResult, "All rapid calls should produce identical results")
            assertNotNull(result, "Each result should be valid")
            assertTrue(result.isNotEmpty(), "Each result should not be empty")
        }
    }
}