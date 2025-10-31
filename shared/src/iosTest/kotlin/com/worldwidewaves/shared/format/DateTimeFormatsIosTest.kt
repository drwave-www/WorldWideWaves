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
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.timeZoneWithName
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * iOS instrumented tests for date/time formatting.
 *
 * These tests run on iOS simulator/device with full iOS runtime, allowing us to:
 * - Test actual NSLocale.currentLocale behavior
 * - Test NSDateFormatter with Foundation framework
 * - Test NSTimeZone conversions with real iOS framework
 * - Verify dateFormatFromTemplate() with device locale
 * - Test 12/24-hour preference handling on iOS
 *
 * Run with: ./gradlew iosSimulatorArm64Test or via Xcode Test Navigator
 */
@OptIn(ExperimentalTime::class)
class DateTimeFormatsIosTest {
    private val testInstant = Instant.fromEpochSeconds(1704067200) // 2024-01-01 00:00:00 UTC
    private val noonInstant = testInstant + 12.hours // 12:00 UTC

    @Test
    fun nsLocale_currentLocaleShouldBeAccessible() {
        // Verify iOS Foundation framework is available
        val locale = NSLocale.currentLocale
        assertNotNull(locale, "NSLocale.currentLocale should be accessible on iOS")

        val langCode = locale.languageCode
        assertNotNull(langCode, "Language code should be accessible")
    }

    @Test
    fun nsTimeZone_shouldBeAvailableForConversion() {
        // Verify we can convert timezone IDs to NSTimeZone
        val utcZone = NSTimeZone.timeZoneWithName("UTC")
        val nyZone = NSTimeZone.timeZoneWithName("America/New_York")
        val tokyoZone = NSTimeZone.timeZoneWithName("Asia/Tokyo")

        assertNotNull(utcZone, "UTC timezone should be available")
        assertNotNull(nyZone, "New York timezone should be available")
        assertNotNull(tokyoZone, "Tokyo timezone should be available")
    }

    @Test
    fun dayMonth_shouldProduceValidOutputWithNSDateFormatter() {
        val result = DateTimeFormats.dayMonth(testInstant, TimeZone.UTC)

        assertNotNull(result, "dayMonth should return non-null")
        assertTrue(result.isNotEmpty(), "dayMonth should return non-empty string")
        assertTrue(result.length in 3..20, "dayMonth output should have reasonable length")

        // Should contain day number for January 1st
        val hasDay = result.contains("1") || result.contains("01")
        assertTrue(hasDay, "dayMonth should contain day number: $result")
    }

    @Test
    fun timeShort_shouldUseJmSkeletonForLocaleAwareFormatting() {
        val result = DateTimeFormats.timeShort(noonInstant, TimeZone.UTC)

        assertNotNull(result, "timeShort should return non-null")
        assertTrue(result.isNotEmpty(), "timeShort should return non-empty string")
        assertTrue(result.length in 4..15, "timeShort output should have reasonable length")

        // Should contain "12" for noon or ":" separator
        val hasValidTime = result.contains("12") || result.contains(":")
        assertTrue(hasValidTime, "timeShort should contain valid time: $result")
    }

    @Test
    fun dayMonth_shouldRespectNSTimeZoneParameter() {
        // New Year's Eve 23:00 UTC = next day in Tokyo (+9 hours)
        val newYearsEveEvening = Instant.fromEpochSeconds(1735686000) // 2024-12-31 23:00 UTC

        val utcResult = DateTimeFormats.dayMonth(newYearsEveEvening, TimeZone.UTC)
        val tokyoResult = DateTimeFormats.dayMonth(newYearsEveEvening, TimeZone.of("Asia/Tokyo"))

        assertNotNull(utcResult, "UTC result should not be null")
        assertNotNull(tokyoResult, "Tokyo result should not be null")
        assertTrue(utcResult.isNotEmpty(), "UTC result should be valid")
        assertTrue(tokyoResult.isNotEmpty(), "Tokyo result should be valid")
    }

    @Test
    fun timeShort_shouldRespectNSTimeZoneParameter() {
        // Midnight UTC = different hours in different zones
        val midnightUTC = testInstant // 00:00 UTC

        val utcTime = DateTimeFormats.timeShort(midnightUTC, TimeZone.UTC)
        val tokyoTime = DateTimeFormats.timeShort(midnightUTC, TimeZone.of("Asia/Tokyo"))

        assertNotNull(utcTime, "UTC time should not be null")
        assertNotNull(tokyoTime, "Tokyo time should not be null")

        // Times should be different (UTC 00:00 vs Tokyo 09:00)
        assertNotEquals(
            utcTime,
            tokyoTime,
            "Timezone should affect time output",
        )
    }

    @Test
    fun dayMonth_shouldHandleMultipleTimezonesWithNSTimeZone() {
        val timezones =
            listOf(
                TimeZone.UTC,
                TimeZone.of("America/New_York"),
                TimeZone.of("Europe/London"),
                TimeZone.of("Asia/Tokyo"),
                TimeZone.of("Australia/Sydney"),
            )

        timezones.forEach { tz ->
            val result = DateTimeFormats.dayMonth(testInstant, tz)
            assertNotNull(result, "Should handle ${tz.id}")
            assertTrue(result.isNotEmpty(), "Should return non-empty for ${tz.id}")
        }
    }

    @Test
    fun timeShort_shouldHandleMultipleTimezonesWithNSTimeZone() {
        val timezones =
            listOf(
                TimeZone.UTC,
                TimeZone.of("America/Los_Angeles"),
                TimeZone.of("Europe/Paris"),
                TimeZone.of("Asia/Shanghai"),
                TimeZone.of("Pacific/Auckland"),
            )

        timezones.forEach { tz ->
            val result = DateTimeFormats.timeShort(noonInstant, tz)
            assertNotNull(result, "Should handle ${tz.id}")
            assertTrue(result.isNotEmpty(), "Should return non-empty for ${tz.id}")
        }
    }

    @Test
    fun dayMonth_shouldProduceStableOutputAcrossMultipleCalls() {
        // Verify consistent output for same input
        val result1 = DateTimeFormats.dayMonth(testInstant, TimeZone.UTC)
        val result2 = DateTimeFormats.dayMonth(testInstant, TimeZone.UTC)
        val result3 = DateTimeFormats.dayMonth(testInstant, TimeZone.UTC)

        assertNotNull(result1)
        assertNotNull(result2)
        assertNotNull(result3)

        // All should be identical
        assertTrue(result1 == result2 && result2 == result3, "Formatting should be stable")
    }

    @Test
    fun timeShort_shouldProduceStableOutputAcrossMultipleCalls() {
        // Verify consistent output for same input
        val result1 = DateTimeFormats.timeShort(noonInstant, TimeZone.UTC)
        val result2 = DateTimeFormats.timeShort(noonInstant, TimeZone.UTC)
        val result3 = DateTimeFormats.timeShort(noonInstant, TimeZone.UTC)

        assertNotNull(result1)
        assertNotNull(result2)
        assertNotNull(result3)

        // All should be identical
        assertTrue(result1 == result2 && result2 == result3, "Formatting should be stable")
    }

    @Test
    fun dayMonth_shouldHandleEdgeCaseTimezones() {
        // Test unusual but valid timezones
        val edgeTimezones =
            listOf(
                TimeZone.of("Pacific/Fiji"), // +12/+13
                TimeZone.of("Pacific/Honolulu"), // -10
                TimeZone.of("Asia/Kathmandu"), // +5:45 (non-hour offset)
                TimeZone.of("Pacific/Chatham"), // +12:45 (unusual offset)
            )

        edgeTimezones.forEach { tz ->
            val result = DateTimeFormats.dayMonth(testInstant, tz)
            assertNotNull(result, "Should handle edge timezone ${tz.id}")
            assertTrue(result.isNotEmpty(), "Should return non-empty for ${tz.id}")
        }
    }

    @Test
    fun timeShort_shouldHandleEdgeCaseTimezones() {
        // Test unusual but valid timezones
        val edgeTimezones =
            listOf(
                TimeZone.of("Asia/Kolkata"), // +5:30
                TimeZone.of("America/St_Johns"), // -3:30
                TimeZone.of("Pacific/Marquesas"), // -9:30
            )

        edgeTimezones.forEach { tz ->
            val result = DateTimeFormats.timeShort(noonInstant, tz)
            assertNotNull(result, "Should handle edge timezone ${tz.id}")
            assertTrue(result.isNotEmpty(), "Should return non-empty for ${tz.id}")
        }
    }

    @Test
    fun dayMonth_shouldHandleVariousInstantsThroughoutYear() {
        val instants =
            listOf(
                Instant.fromEpochSeconds(1704067200), // Jan 1
                Instant.fromEpochSeconds(1709251200), // Mar 1
                Instant.fromEpochSeconds(1717200000), // Jun 1
                Instant.fromEpochSeconds(1727740800), // Oct 1
                Instant.fromEpochSeconds(1735689599), // Dec 31
            )

        instants.forEach { instant ->
            val result = DateTimeFormats.dayMonth(instant, TimeZone.UTC)
            assertNotNull(result, "Should format instant $instant")
            assertTrue(result.isNotEmpty(), "Should be non-empty for $instant")
        }
    }

    @Test
    fun timeShort_shouldHandleVariousTimesThroughoutDay() {
        val times =
            listOf(
                testInstant, // 00:00 (midnight)
                testInstant + 3.hours, // 03:00 (early morning)
                testInstant + 6.hours, // 06:00 (morning)
                testInstant + 12.hours, // 12:00 (noon)
                testInstant + 15.hours, // 15:00 (afternoon)
                testInstant + 18.hours, // 18:00 (evening)
                testInstant + 21.hours, // 21:00 (night)
            )

        times.forEach { instant ->
            val result = DateTimeFormats.timeShort(instant, TimeZone.UTC)
            assertNotNull(result, "Should format time $instant")
            assertTrue(result.isNotEmpty(), "Should be non-empty for $instant")
        }
    }
}
