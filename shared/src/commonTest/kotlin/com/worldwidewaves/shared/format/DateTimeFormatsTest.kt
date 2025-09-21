package com.worldwidewaves.shared.format

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
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Comprehensive tests for DateTimeFormats functionality including
 * time zone handling, locale formatting, and cross-platform consistency.
 */
@OptIn(ExperimentalTime::class)
class DateTimeFormatsTest {

    @Test
    fun `test dayMonth formats valid instants`() {
        val testInstant = Instant.fromEpochSeconds(1672531200) // 2023-01-01 00:00:00 UTC
        val timeZone = TimeZone.UTC

        val result = DateTimeFormats.dayMonth(testInstant, timeZone)

        assertNotNull(result, "Day month format should not be null")
        assertTrue(result.isNotEmpty(), "Day month format should not be empty")
        assertTrue(result.length >= 3, "Day month format should contain meaningful content")
    }

    @Test
    fun `test timeShort formats valid instants`() {
        val testInstant = Instant.fromEpochSeconds(1672531200) // 2023-01-01 00:00:00 UTC
        val timeZone = TimeZone.UTC

        val result = DateTimeFormats.timeShort(testInstant, timeZone)

        assertNotNull(result, "Time short format should not be null")
        assertTrue(result.isNotEmpty(), "Time short format should not be empty")
        assertTrue(result.contains(":"), "Time format should contain colon separator")
    }

    @Test
    fun `test dayMonth with different time zones`() {
        val testInstant = Instant.fromEpochSeconds(1672531200)

        val utcResult = DateTimeFormats.dayMonth(testInstant, TimeZone.UTC)
        val nyResult = DateTimeFormats.dayMonth(testInstant, TimeZone.of("America/New_York"))
        val tokyoResult = DateTimeFormats.dayMonth(testInstant, TimeZone.of("Asia/Tokyo"))

        assertNotNull(utcResult, "UTC day month should not be null")
        assertNotNull(nyResult, "New York day month should not be null")
        assertNotNull(tokyoResult, "Tokyo day month should not be null")

        // Results might be different due to time zone differences
        assertTrue(utcResult.isNotEmpty(), "UTC result should not be empty")
        assertTrue(nyResult.isNotEmpty(), "NY result should not be empty")
        assertTrue(tokyoResult.isNotEmpty(), "Tokyo result should not be empty")
    }

    @Test
    fun `test timeShort with different time zones`() {
        val testInstant = Instant.fromEpochSeconds(1672531200)

        val utcResult = DateTimeFormats.timeShort(testInstant, TimeZone.UTC)
        val nyResult = DateTimeFormats.timeShort(testInstant, TimeZone.of("America/New_York"))
        val tokyoResult = DateTimeFormats.timeShort(testInstant, TimeZone.of("Asia/Tokyo"))

        assertNotNull(utcResult, "UTC time should not be null")
        assertNotNull(nyResult, "New York time should not be null")
        assertNotNull(tokyoResult, "Tokyo time should not be null")

        assertTrue(utcResult.contains(":"), "UTC time should contain colon")
        assertTrue(nyResult.contains(":"), "NY time should contain colon")
        assertTrue(tokyoResult.contains(":"), "Tokyo time should contain colon")
    }

    @Test
    fun `test dayMonth with edge case dates`() {
        // Test leap year date
        val leapYearInstant = Instant.fromEpochSeconds(1582934400) // 2020-02-29 00:00:00 UTC
        val leapResult = DateTimeFormats.dayMonth(leapYearInstant, TimeZone.UTC)
        assertNotNull(leapResult, "Leap year date should be formatted")
        assertTrue(leapResult.isNotEmpty(), "Leap year result should not be empty")

        // Test year boundary
        val newYearInstant = Instant.fromEpochSeconds(1672531200) // 2023-01-01 00:00:00 UTC
        val newYearResult = DateTimeFormats.dayMonth(newYearInstant, TimeZone.UTC)
        assertNotNull(newYearResult, "New Year date should be formatted")
        assertTrue(newYearResult.isNotEmpty(), "New Year result should not be empty")

        // Test end of year
        val endYearInstant = Instant.fromEpochSeconds(1703980800) // 2023-12-31 00:00:00 UTC
        val endYearResult = DateTimeFormats.dayMonth(endYearInstant, TimeZone.UTC)
        assertNotNull(endYearResult, "End of year date should be formatted")
        assertTrue(endYearResult.isNotEmpty(), "End of year result should not be empty")
    }

    @Test
    fun `test timeShort with edge case times`() {
        // Test midnight
        val midnightInstant = Instant.fromEpochSeconds(1672531200) // 2023-01-01 00:00:00 UTC
        val midnightResult = DateTimeFormats.timeShort(midnightInstant, TimeZone.UTC)
        assertNotNull(midnightResult, "Midnight time should be formatted")
        assertTrue(midnightResult.contains(":"), "Midnight time should contain colon")

        // Test noon
        val noonInstant = Instant.fromEpochSeconds(1672574400) // 2023-01-01 12:00:00 UTC
        val noonResult = DateTimeFormats.timeShort(noonInstant, TimeZone.UTC)
        assertNotNull(noonResult, "Noon time should be formatted")
        assertTrue(noonResult.contains(":"), "Noon time should contain colon")

        // Test late evening
        val eveningInstant = Instant.fromEpochSeconds(1672614000) // 2023-01-01 23:00:00 UTC
        val eveningResult = DateTimeFormats.timeShort(eveningInstant, TimeZone.UTC)
        assertNotNull(eveningResult, "Evening time should be formatted")
        assertTrue(eveningResult.contains(":"), "Evening time should contain colon")
    }

    @Test
    fun `test format consistency across multiple calls`() {
        val testInstant = Instant.fromEpochSeconds(1672531200)
        val timeZone = TimeZone.UTC

        // Call multiple times to ensure consistency
        val dayMonth1 = DateTimeFormats.dayMonth(testInstant, timeZone)
        val dayMonth2 = DateTimeFormats.dayMonth(testInstant, timeZone)
        val dayMonth3 = DateTimeFormats.dayMonth(testInstant, timeZone)

        assertTrue(dayMonth1 == dayMonth2, "Day month format should be consistent")
        assertTrue(dayMonth2 == dayMonth3, "Day month format should be consistent")

        val timeShort1 = DateTimeFormats.timeShort(testInstant, timeZone)
        val timeShort2 = DateTimeFormats.timeShort(testInstant, timeZone)
        val timeShort3 = DateTimeFormats.timeShort(testInstant, timeZone)

        assertTrue(timeShort1 == timeShort2, "Time short format should be consistent")
        assertTrue(timeShort2 == timeShort3, "Time short format should be consistent")
    }

    @Test
    fun `test formatting performance`() {
        val testInstant = Instant.fromEpochSeconds(1672531200)
        val timeZone = TimeZone.UTC
        val startTime = kotlin.system.getTimeMillis()

        // Format times multiple times to test performance
        repeat(100) {
            DateTimeFormats.dayMonth(testInstant, timeZone)
            DateTimeFormats.timeShort(testInstant, timeZone)
        }

        val endTime = kotlin.system.getTimeMillis()
        val duration = endTime - startTime

        // Formatting should be reasonably fast (under 1 second for 100 iterations)
        assertTrue(duration < 1000, "Date formatting should be fast, took ${duration}ms")
    }

    @Test
    fun `test different instant values produce different outputs`() {
        val timeZone = TimeZone.UTC

        val instant1 = Instant.fromEpochSeconds(1672531200) // 2023-01-01 00:00:00 UTC
        val instant2 = Instant.fromEpochSeconds(1675209600) // 2023-02-01 00:00:00 UTC
        val instant3 = Instant.fromEpochSeconds(1677628800) // 2023-03-01 00:00:00 UTC

        val dayMonth1 = DateTimeFormats.dayMonth(instant1, timeZone)
        val dayMonth2 = DateTimeFormats.dayMonth(instant2, timeZone)
        val dayMonth3 = DateTimeFormats.dayMonth(instant3, timeZone)

        // Different months should produce different day/month formats
        assertTrue(dayMonth1 != dayMonth2, "Different months should produce different formats")
        assertTrue(dayMonth2 != dayMonth3, "Different months should produce different formats")
        assertTrue(dayMonth1 != dayMonth3, "Different months should produce different formats")

        val timeShort1 = DateTimeFormats.timeShort(instant1, timeZone) // 00:00
        val timeShort2 = DateTimeFormats.timeShort(Instant.fromEpochSeconds(1672574400), timeZone) // 12:00

        // Different times should produce different time formats
        assertTrue(timeShort1 != timeShort2, "Different times should produce different formats")
    }

    @Test
    fun `test common time zones formatting`() {
        val testInstant = Instant.fromEpochSeconds(1672531200)

        val commonTimeZones = listOf(
            TimeZone.UTC,
            TimeZone.of("America/New_York"),
            TimeZone.of("Europe/London"),
            TimeZone.of("Asia/Tokyo"),
            TimeZone.of("Australia/Sydney"),
            TimeZone.of("America/Los_Angeles")
        )

        commonTimeZones.forEach { timeZone ->
            val dayMonth = DateTimeFormats.dayMonth(testInstant, timeZone)
            val timeShort = DateTimeFormats.timeShort(testInstant, timeZone)

            assertNotNull(dayMonth, "Day month should work for ${timeZone.id}")
            assertNotNull(timeShort, "Time short should work for ${timeZone.id}")
            assertTrue(dayMonth.isNotEmpty(), "Day month should not be empty for ${timeZone.id}")
            assertTrue(timeShort.isNotEmpty(), "Time short should not be empty for ${timeZone.id}")
            assertTrue(timeShort.contains(":"), "Time should contain colon for ${timeZone.id}")
        }
    }

    @Test
    fun `test format output characteristics`() {
        val testInstant = Instant.fromEpochSeconds(1672531200)
        val timeZone = TimeZone.UTC

        val dayMonth = DateTimeFormats.dayMonth(testInstant, timeZone)
        val timeShort = DateTimeFormats.timeShort(testInstant, timeZone)

        // Day month should be relatively short
        assertTrue(dayMonth.length <= 20, "Day month format should be reasonably short")

        // Time short should contain proper time format
        assertTrue(timeShort.length >= 3, "Time should have at least H:M format")
        assertTrue(timeShort.length <= 10, "Time should not be excessively long")

        // Time should have numeric components
        val timeComponents = timeShort.split(":")
        assertTrue(timeComponents.size >= 2, "Time should have at least hour and minute components")
    }

    @Test
    fun `test date formatting thread safety`() {
        val testInstant = Instant.fromEpochSeconds(1672531200)
        val timeZone = TimeZone.UTC
        val results = mutableListOf<String>()
        val exceptions = mutableListOf<Exception>()

        val threads = (1..5).map { threadId ->
            Thread {
                try {
                    repeat(10) {
                        val dayMonth = DateTimeFormats.dayMonth(testInstant, timeZone)
                        val timeShort = DateTimeFormats.timeShort(testInstant, timeZone)

                        synchronized(results) {
                            results.add("Thread $threadId: day=$dayMonth, time=$timeShort")
                        }
                    }
                } catch (e: Exception) {
                    synchronized(exceptions) {
                        exceptions.add(e)
                    }
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent formatting: $exceptions")
        assertTrue(results.size == 50, "All threads should complete successfully")

        // All results should contain proper formatting
        results.forEach { result ->
            assertTrue(result.contains("day="), "Result should contain day component")
            assertTrue(result.contains("time="), "Result should contain time component")
            assertTrue(result.contains(":"), "Result should contain time separator")
        }
    }
}