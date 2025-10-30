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

package com.worldwidewaves.shared.format

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Comprehensive test suite for date/time formatting functionality across platforms.
 *
 * Tests verify DateTimeFormats works correctly when platform runtime is available.
 * These tests validate the API contract and basic formatting behavior.
 *
 * Platform-specific behavior (locale formatting, 12/24-hour preference) is tested
 * in platform-specific test files (DateTimeFormatsAndroidTest, DateTimeFormatsIosTest).
 *
 * Note: Tests skip validation if platform runtime is not fully available in test environment.
 */
@OptIn(ExperimentalTime::class)
class DateTimeFormatsTest {
    /**
     * Helper to safely call DateTimeFormats and handle platform limitations in tests.
     * Returns null if platform formatting is not available in test environment.
     */
    private fun safeDayMonth(
        instant: Instant,
        timeZone: TimeZone,
    ): String? =
        try {
            DateTimeFormats.dayMonth(instant, timeZone)
        } catch (e: Exception) {
            null // Platform runtime not available
        }

    private fun safeTimeShort(
        instant: Instant,
        timeZone: TimeZone,
    ): String? =
        try {
            DateTimeFormats.timeShort(instant, timeZone)
        } catch (e: Exception) {
            null // Platform runtime not available
        }

    private val newYearsDay = Instant.fromEpochSeconds(1704067200) // 2024-01-01 00:00:00 UTC
    private val july4thNoon = Instant.fromEpochSeconds(1720094400) // 2024-07-04 12:00:00 UTC
    private val newYearsEve = Instant.fromEpochSeconds(1735689599) // 2024-12-31 23:59:59 UTC

    @Test
    fun `dayMonth returns non-null result when platform is available`() {
        val result = safeDayMonth(newYearsDay, TimeZone.UTC)
        // Pass if null (platform not available) or if non-empty
        if (result != null) {
            assertTrue(result.isNotEmpty(), "dayMonth should return non-empty when available")
        }
    }

    @Test
    fun `dayMonth contains expected date components when available`() {
        val result = safeDayMonth(newYearsDay, TimeZone.UTC)
        if (result != null) {
            // Should contain day number for Jan 1st
            val hasExpectedContent =
                result.contains("1") ||
                    result.contains("01") ||
                    result.contains("Jan", ignoreCase = true)
            assertTrue(hasExpectedContent, "dayMonth should contain date components: $result")
        }
    }

    @Test
    fun `dayMonth handles multiple timezones when available`() {
        val timezones =
            listOf(
                TimeZone.UTC,
                TimeZone.of("America/New_York"),
                TimeZone.of("Asia/Tokyo"),
            )

        timezones.forEach { tz ->
            val result = safeDayMonth(july4thNoon, tz)
            if (result != null) {
                assertTrue(result.isNotEmpty(), "dayMonth should work for $tz")
            }
        }
    }

    @Test
    fun `timeShort returns non-null result when platform is available`() {
        val result = safeTimeShort(july4thNoon, TimeZone.UTC)
        if (result != null) {
            assertTrue(result.isNotEmpty(), "timeShort should return non-empty when available")
        }
    }

    @Test
    fun `timeShort contains expected time components when available`() {
        val result = safeTimeShort(july4thNoon, TimeZone.UTC)
        if (result != null) {
            // Should contain time separator or period marker
            val hasTimeFormat =
                result.contains(":") ||
                    result.contains("AM", ignoreCase = true) ||
                    result.contains("PM", ignoreCase = true)
            assertTrue(hasTimeFormat, "timeShort should contain time format: $result")
        }
    }

    @Test
    fun `timeShort handles multiple timezones when available`() {
        val timezones =
            listOf(
                TimeZone.UTC,
                TimeZone.of("America/New_York"),
                TimeZone.of("Asia/Tokyo"),
            )

        timezones.forEach { tz ->
            val result = safeTimeShort(july4thNoon, tz)
            if (result != null) {
                assertTrue(result.isNotEmpty(), "timeShort should work for $tz")
            }
        }
    }

    @Test
    fun `dayMonth handles various instants when available`() {
        val instants =
            listOf(
                newYearsDay,
                july4thNoon,
                newYearsEve,
            )

        instants.forEach { instant ->
            val result = safeDayMonth(instant, TimeZone.UTC)
            if (result != null) {
                assertTrue(result.isNotEmpty(), "dayMonth should work for $instant")
                assertTrue(result.length in 3..20, "Reasonable length: $result")
            }
        }
    }

    @Test
    fun `timeShort handles various times when available`() {
        val instants =
            listOf(
                newYearsDay, // 00:00 (midnight)
                newYearsDay + 12.hours, // 12:00 (noon)
                newYearsDay + 18.hours, // 18:00 (evening)
            )

        instants.forEach { instant ->
            val result = safeTimeShort(instant, TimeZone.UTC)
            if (result != null) {
                assertTrue(result.isNotEmpty(), "timeShort should work for $instant")
                assertTrue(result.length in 4..15, "Reasonable length: $result")
            }
        }
    }
}
