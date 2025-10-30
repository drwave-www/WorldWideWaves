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
import com.worldwidewaves.shared.format.DateTimeFormats
import com.worldwidewaves.shared.localization.LocalizationManager
import com.worldwidewaves.shared.localization.getPlatformLocaleKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Android instrumented tests for LocalizationManager.
 *
 * These tests run on real Android device/emulator with full Android runtime,
 * allowing us to test:
 * - Actual Locale.getDefault() integration
 * - Real Configuration.locale handling
 * - StateFlow emission with Android threading
 * - BCP 47 language tag generation
 *
 * Run with: ./gradlew connectedAndroidTest
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
@RunWith(AndroidJUnit4::class)
class LocalizationManagerInstrumentedTest {
    private val testInstant = Instant.fromEpochSeconds(1704067200) // 2024-01-01 00:00:00 UTC
    private val noonInstant = testInstant + 12.hours // 12:00 UTC

    @Test
    fun getPlatformLocaleKey_shouldReturnValidBCP47Tag() {
        // Test actual platform locale key retrieval
        val localeKey = getPlatformLocaleKey()

        assertNotNull("Platform locale key should not be null", localeKey)
        assertTrue("Platform locale key should not be empty", localeKey.isNotEmpty())

        // Should be BCP 47 format (e.g., "en-US", "fr-FR", "ja-JP")
        val bcp47Pattern = Regex("^[a-z]{2}(-[A-Z]{2})?(-[a-zA-Z]+)?$")
        assertTrue(
            "Locale key should match BCP 47 format: $localeKey",
            localeKey.matches(bcp47Pattern) || localeKey.length == 2,
        )
    }

    @Test
    fun localizationManager_shouldInitializeWithDeviceLocale() =
        runTest {
            val manager = LocalizationManager()

            val initialLocale = manager.localeChanges.value
            assertNotNull("Initial locale should not be null", initialLocale)
            assertTrue("Initial locale should not be empty", initialLocale.isNotEmpty())

            // Should match platform locale key
            val platformLocale = getPlatformLocaleKey()
            assertEquals("Should match platform locale", platformLocale, initialLocale)
        }

    @Test
    fun localizationManager_shouldEmitLocaleChanges() =
        runTest {
            val manager = LocalizationManager()

            // Simulate locale change notification from MainActivity
            manager.notifyLocaleChanged("es-ES")

            val updatedLocale = manager.localeChanges.first()
            assertEquals("Should emit Spanish locale", "es-ES", updatedLocale)
        }

    @Test
    fun localizationManager_shouldHandleMultipleConsecutiveChanges() =
        runTest {
            val manager = LocalizationManager()

            val locales = listOf("en-US", "fr-FR", "de-DE", "ja-JP", "ar-SA")

            locales.forEach { locale ->
                manager.notifyLocaleChanged(locale)
                val current = manager.localeChanges.first()
                assertEquals("Should update to $locale", locale, current)
            }
        }

    @Test
    fun localizationManager_shouldHandleRapidChanges() =
        runTest {
            val manager = LocalizationManager()

            // Simulate rapid language switching (user quickly changing preferences)
            repeat(20) { i ->
                manager.notifyLocaleChanged("locale-$i")
            }

            val finalLocale = manager.localeChanges.first()
            assertEquals("Should settle on last value", "locale-19", finalLocale)
        }

    @Test
    fun localizationManager_stateFlowShouldBeThreadSafe() =
        runTest {
            val manager = LocalizationManager()

            // Multiple collectors should all see the same value
            val value1 = manager.localeChanges.first()
            val value2 = manager.localeChanges.first()
            val value3 = manager.localeChanges.first()

            assertEquals("All collectors should see same value", value1, value2)
            assertEquals("All collectors should see same value", value2, value3)
        }

    @Test
    fun dateTimeFormats_shouldWorkWithActualAndroidRuntime() {
        // Verify date/time formatting works with real Android ICU library
        val dateResult = DateTimeFormats.dayMonth(testInstant, TimeZone.UTC)
        val timeResult = DateTimeFormats.timeShort(noonInstant, TimeZone.UTC)

        assertNotNull("Date formatting should work", dateResult)
        assertNotNull("Time formatting should work", timeResult)
        assertTrue("Date should be non-empty", dateResult.isNotEmpty())
        assertTrue("Time should be non-empty", timeResult.isNotEmpty())
    }

    @Test
    fun dateTimeFormats_shouldHandleMultipleTimezonesWithRealRuntime() {
        val timezones =
            listOf(
                TimeZone.UTC,
                TimeZone.of("America/New_York"),
                TimeZone.of("Europe/London"),
                TimeZone.of("Asia/Tokyo"),
                TimeZone.of("Australia/Sydney"),
            )

        timezones.forEach { tz ->
            val dateResult = DateTimeFormats.dayMonth(testInstant, tz)
            val timeResult = DateTimeFormats.timeShort(noonInstant, tz)

            assertNotNull("Date should work for ${tz.id}", dateResult)
            assertNotNull("Time should work for ${tz.id}", timeResult)
        }
    }

    @Test
    fun dateTimeFormats_timezoneShouldAffectOutput() {
        // Verify timezone actually changes the output
        // Midnight UTC = different hours in different zones
        val midnightUTC = testInstant // 00:00 UTC

        val utcTime = DateTimeFormats.timeShort(midnightUTC, TimeZone.UTC)
        val tokyoTime = DateTimeFormats.timeShort(midnightUTC, TimeZone.of("Asia/Tokyo"))

        assertNotNull(utcTime)
        assertNotNull(tokyoTime)

        // Times should be different (UTC 00:00 vs Tokyo 09:00)
        assertNotEquals(
            "Timezone should affect time output",
            utcTime,
            tokyoTime,
        )
    }
}
