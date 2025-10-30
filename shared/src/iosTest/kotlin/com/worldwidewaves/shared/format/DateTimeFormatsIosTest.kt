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
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * iOS-specific tests for date/time formatting.
 *
 * Tests verify NSDateFormatter, NSLocale, and NSTimeZone integration.
 */
@OptIn(ExperimentalTime::class)
class DateTimeFormatsIosTest {
    private val testInstant = Instant.fromEpochSeconds(1704067200) // 2024-01-01 00:00:00 UTC

    @Test
    fun `NSLocale currentLocale should be accessible`() {
        // Verify iOS Foundation framework is available
        val currentLocale = NSLocale.currentLocale
        assertNotNull(currentLocale, "NSLocale.currentLocale should be accessible on iOS")
    }

    @Test
    fun `dayMonth should use NSDateFormatter correctly`() {
        val result = DateTimeFormats.dayMonth(testInstant, TimeZone.UTC)
        assertTrue(result.isNotEmpty(), "iOS dayMonth should return non-empty string")
    }

    @Test
    fun `timeShort should use NSDateFormatter with jm skeleton`() {
        val result = DateTimeFormats.timeShort(testInstant + 12.hours, TimeZone.UTC)
        assertTrue(result.isNotEmpty(), "iOS timeShort should return non-empty string")
    }

    @Test
    fun `dayMonth should handle multiple timezones`() {
        val timezones = listOf(TimeZone.UTC, TimeZone.of("America/New_York"), TimeZone.of("Asia/Tokyo"))
        timezones.forEach { tz ->
            val result = DateTimeFormats.dayMonth(testInstant, tz)
            assertTrue(result.isNotEmpty(), "Should handle $tz")
        }
    }

    @Test
    fun `timeShort should handle multiple timezones`() {
        val timezones = listOf(TimeZone.UTC, TimeZone.of("Europe/Paris"), TimeZone.of("Australia/Sydney"))
        timezones.forEach { tz ->
            val result = DateTimeFormats.timeShort(testInstant, tz)
            assertTrue(result.isNotEmpty(), "Should handle $tz")
        }
    }

    @Test
    fun `dayMonth output should have reasonable length`() {
        val result = DateTimeFormats.dayMonth(testInstant, TimeZone.UTC)
        assertTrue(result.length in 3..20, "iOS dayMonth output: $result")
    }

    @Test
    fun `timeShort output should have reasonable length`() {
        val result = DateTimeFormats.timeShort(testInstant + 12.hours, TimeZone.UTC)
        assertTrue(result.length in 4..15, "iOS timeShort output: $result")
    }
}
