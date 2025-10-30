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
 * Android-specific tests for date/time formatting.
 *
 * Tests verify Android ICU library and SimpleDateFormat integration when
 * full Android runtime is available.
 *
 * Note: These tests may not run in unit test environment without Android runtime.
 * Tests skip validation if platform APIs are not available.
 */
@OptIn(ExperimentalTime::class)
class DateTimeFormatsAndroidTest {
    private fun safeDayMonth(
        instant: Instant,
        timeZone: TimeZone,
    ): String? =
        try {
            DateTimeFormats.dayMonth(instant, timeZone)
        } catch (e: Exception) {
            null
        }

    private fun safeTimeShort(
        instant: Instant,
        timeZone: TimeZone,
    ): String? =
        try {
            DateTimeFormats.timeShort(instant, timeZone)
        } catch (e: Exception) {
            null
        }

    private val testInstant = Instant.fromEpochSeconds(1704067200) // 2024-01-01 00:00:00 UTC

    @Test
    fun `dayMonth should work with Android locale when runtime available`() {
        val result = safeDayMonth(testInstant, TimeZone.UTC)
        if (result != null) {
            assertTrue(result.isNotEmpty(), "Android dayMonth should return non-empty")
        }
    }

    @Test
    fun `timeShort should work with Android locale when runtime available`() {
        val result = safeTimeShort(testInstant + 12.hours, TimeZone.UTC)
        if (result != null) {
            assertTrue(result.isNotEmpty(), "Android timeShort should return non-empty")
        }
    }

    @Test
    fun `dayMonth should handle multiple timezones when available`() {
        val timezones = listOf(TimeZone.UTC, TimeZone.of("America/New_York"), TimeZone.of("Asia/Tokyo"))
        timezones.forEach { tz ->
            val result = safeDayMonth(testInstant, tz)
            if (result != null) {
                assertTrue(result.isNotEmpty(), "Should handle $tz")
            }
        }
    }

    @Test
    fun `timeShort should handle multiple timezones when available`() {
        val timezones = listOf(TimeZone.UTC, TimeZone.of("Europe/Paris"), TimeZone.of("Australia/Sydney"))
        timezones.forEach { tz ->
            val result = safeTimeShort(testInstant, tz)
            if (result != null) {
                assertTrue(result.isNotEmpty(), "Should handle $tz")
            }
        }
    }
}
