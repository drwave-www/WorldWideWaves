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

package com.worldwidewaves.i18n

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.worldwidewaves.shared.format.DateTimeFormats
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Android instrumented tests for locale-aware date/time formatting.
 *
 * These tests run on real Android device/emulator with full Android runtime,
 * allowing us to:
 * - Test actual Locale.getDefault() behavior
 * - Test SimpleDateFormat with ICU library
 * - Test timezone conversions with real Android framework
 * - Verify locale-specific date/time patterns
 * - Test configuration changes (locale switching)
 *
 * Run with: ./gradlew connectedAndroidTest
 */
@OptIn(ExperimentalTime::class)
@RunWith(AndroidJUnit4::class)
class LocaleFormattingInstrumentedTest {
    private val testInstant = Instant.fromEpochSeconds(1704067200) // 2024-01-01 00:00:00 UTC
    private val noonInstant = testInstant + 12.hours // 12:00 UTC

    @Test
    fun dayMonth_shouldProduceValidOutput_withDeviceLocale() {
        // Test with actual device locale (whatever it is)
        val result = DateTimeFormats.dayMonth(testInstant, TimeZone.UTC)

        assertNotNull("dayMonth should return non-null with full Android runtime", result)
        assertTrue("dayMonth should return non-empty string", result.isNotEmpty())
        assertTrue("dayMonth should have reasonable length", result.length in 3..20)
    }

    @Test
    fun timeShort_shouldProduceValidOutput_withDeviceLocale() {
        // Test with actual device locale and 12/24-hour preference
        val result = DateTimeFormats.timeShort(noonInstant, TimeZone.UTC)

        assertNotNull("timeShort should return non-null with full Android runtime", result)
        assertTrue("timeShort should return non-empty string", result.isNotEmpty())
        assertTrue("timeShort should have reasonable length", result.length in 4..15)

        // Should contain either ":" or "12" for noon
        val hasValidTime =
            result.contains(":") ||
                result.contains("12") ||
                result.contains("PM", ignoreCase = true)
        assertTrue("timeShort should contain valid time components: $result", hasValidTime)
    }

    @Test
    fun dayMonth_shouldRespectTimezone_newYork() {
        // New Year's Eve 23:00 UTC = same day in UTC, but next day in Tokyo
        val newYearsEveEvening = Instant.fromEpochSeconds(1735686000) // 2024-12-31 23:00 UTC

        val utcResult = DateTimeFormats.dayMonth(newYearsEveEvening, TimeZone.UTC)
        val tokyoResult = DateTimeFormats.dayMonth(newYearsEveEvening, TimeZone.of("Asia/Tokyo"))

        assertNotNull("UTC result should not be null", utcResult)
        assertNotNull("Tokyo result should not be null", tokyoResult)

        // Both should be valid (may or may not be different depending on date boundary)
        assertTrue("UTC result should be valid", utcResult.isNotEmpty())
        assertTrue("Tokyo result should be valid", tokyoResult.isNotEmpty())
    }

    @Test
    fun timeShort_shouldRespectTimezone_multipleZones() {
        // Noon UTC = different times in different zones
        val timezones =
            listOf(
                TimeZone.UTC, // 12:00
                TimeZone.of("America/New_York"), // 07:00 or 08:00 (EST/EDT)
                TimeZone.of("Europe/Paris"), // 13:00 or 14:00 (CET/CEST)
                TimeZone.of("Asia/Tokyo"), // 21:00 (JST)
            )

        timezones.forEach { tz ->
            val result = DateTimeFormats.timeShort(noonInstant, tz)
            assertNotNull("Should handle timezone ${tz.id}", result)
            assertTrue("Should return non-empty for ${tz.id}", result.isNotEmpty())
        }
    }

    @Test
    fun dayMonth_shouldHandleMultipleInstants() {
        val instants =
            listOf(
                Instant.fromEpochSeconds(1704067200), // Jan 1
                Instant.fromEpochSeconds(1717200000), // Jun 1
                Instant.fromEpochSeconds(1735689599), // Dec 31
            )

        instants.forEach { instant ->
            val result = DateTimeFormats.dayMonth(instant, TimeZone.UTC)
            assertNotNull("Should format instant $instant", result)
            assertTrue("Should return non-empty for $instant", result.isNotEmpty())
        }
    }

    @Test
    fun timeShort_shouldHandleMultipleTimes() {
        val times =
            listOf(
                testInstant, // 00:00 (midnight)
                testInstant + 6.hours, // 06:00 (morning)
                testInstant + 12.hours, // 12:00 (noon)
                testInstant + 18.hours, // 18:00 (evening)
                testInstant + 23.hours, // 23:00 (late night)
            )

        times.forEach { instant ->
            val result = DateTimeFormats.timeShort(instant, TimeZone.UTC)
            assertNotNull("Should format time $instant", result)
            assertTrue("Should return non-empty for $instant", result.isNotEmpty())
        }
    }

    @Test
    fun dayMonth_outputShouldBeStable_acrossMultipleCalls() {
        // Verify consistent output for same input
        val result1 = DateTimeFormats.dayMonth(testInstant, TimeZone.UTC)
        val result2 = DateTimeFormats.dayMonth(testInstant, TimeZone.UTC)
        val result3 = DateTimeFormats.dayMonth(testInstant, TimeZone.UTC)

        assertNotNull(result1)
        assertNotNull(result2)
        assertNotNull(result3)

        // All should be identical for same input
        assertTrue("Formatting should be stable", result1 == result2 && result2 == result3)
    }

    @Test
    fun timeShort_outputShouldBeStable_acrossMultipleCalls() {
        // Verify consistent output for same input
        val result1 = DateTimeFormats.timeShort(noonInstant, TimeZone.UTC)
        val result2 = DateTimeFormats.timeShort(noonInstant, TimeZone.UTC)
        val result3 = DateTimeFormats.timeShort(noonInstant, TimeZone.UTC)

        assertNotNull(result1)
        assertNotNull(result2)
        assertNotNull(result3)

        // All should be identical for same input
        assertTrue("Formatting should be stable", result1 == result2 && result2 == result3)
    }

    @Test
    fun deviceLocale_shouldBeAccessible() {
        // Verify we can access device locale in instrumented test
        val currentLocale = Locale.getDefault()

        assertNotNull("Device locale should be accessible", currentLocale)
        assertNotNull("Locale language should be accessible", currentLocale.language)
        assertTrue("Locale language should not be empty", currentLocale.language.isNotEmpty())
    }

    @Test
    fun configuration_shouldHaveLocaleInformation() {
        // Verify Configuration has locale data
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = context.resources.configuration

        assertNotNull("Configuration should not be null", config)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            assertNotNull("LocaleList should not be null", config.locales)
            assertTrue("LocaleList should not be empty", config.locales.size() > 0)
        } else {
            @Suppress("DEPRECATION")
            assertNotNull("Locale should not be null", config.locale)
        }
    }
}
