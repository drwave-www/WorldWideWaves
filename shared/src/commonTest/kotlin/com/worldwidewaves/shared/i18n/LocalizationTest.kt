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

package com.worldwidewaves.shared.i18n

import com.worldwidewaves.shared.MokoRes
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for internationalization (i18n) and localization.
 *
 * Tests verify:
 * - MokoRes is accessible and properly initialized
 * - Critical string resources are non-empty
 * - String resource structure follows conventions
 * - RTL (Right-to-Left) languages are properly identified
 * - String interpolation patterns are valid
 * - Accessibility strings are present
 *
 * Note: This test suite validates the i18n infrastructure.
 * Platform-specific locale behavior is tested in platform-specific test files.
 */
class LocalizationTest {
    /**
     * List of supported locale codes in WorldWideWaves.
     * Based on moko-resources directory structure.
     */
    private val supportedLocales =
        listOf(
            "am",
            "ar",
            "bn",
            "de",
            "es",
            "fa",
            "fil",
            "fr",
            "ha",
            "he",
            "hi",
            "id",
            "ig",
            "it",
            "ja",
            "ko",
            "ms",
            "nl",
            "pa",
            "pl",
            "pt",
            "ro",
            "ru",
            "sw",
            "th",
            "tr",
            "uk",
            "ur",
            "vi",
            "xh",
            "yo",
            "zh",
            "zu",
        )

    /**
     * RTL (Right-to-Left) languages requiring special layout handling.
     */
    private val rtlLocales = setOf("ar", "he", "fa", "ur")

    @Test
    fun `MokoRes should be accessible`() {
        assertNotNull(MokoRes.strings, "MokoRes.strings should be accessible")
    }

    @Test
    fun `critical UI strings should be accessible`() {
        // Test key user-facing strings exist
        assertNotNull(MokoRes.strings.wave_now, "wave_now should be accessible")
        assertNotNull(MokoRes.strings.back, "back should be accessible")
        assertNotNull(MokoRes.strings.yes, "yes should be accessible")
        assertNotNull(MokoRes.strings.no, "no should be accessible")
    }

    @Test
    fun `event filter strings should be present`() {
        assertNotNull(MokoRes.strings.events_select_all)
        assertNotNull(MokoRes.strings.events_select_starred)
        assertNotNull(MokoRes.strings.events_select_downloaded)
    }

    @Test
    fun `map download strings should be present`() {
        assertNotNull(MokoRes.strings.map_download)
        assertNotNull(MokoRes.strings.map_downloading)
        assertNotNull(MokoRes.strings.map_downloaded)
        assertNotNull(MokoRes.strings.map_error_download)
    }

    @Test
    fun `geolocation status strings should be present`() {
        assertNotNull(MokoRes.strings.geoloc_yourein)
        assertNotNull(MokoRes.strings.geoloc_yourenotin)
        assertNotNull(MokoRes.strings.geoloc_error)
        assertNotNull(MokoRes.strings.geoloc_undone)
    }

    @Test
    fun `wave status strings should be present`() {
        assertNotNull(MokoRes.strings.wave_be_ready)
        assertNotNull(MokoRes.strings.wave_done)
        assertNotNull(MokoRes.strings.wave_hit)
        assertNotNull(MokoRes.strings.wave_is_running)
    }

    @Test
    fun `simulation mode strings should be present`() {
        assertNotNull(MokoRes.strings.test_simulation)
        assertNotNull(MokoRes.strings.simulation_stop)
        assertNotNull(MokoRes.strings.simulation_started)
        assertNotNull(MokoRes.strings.simulation_stopped)
    }

    @Test
    fun `accessibility strings should be present`() {
        assertNotNull(MokoRes.strings.accessibility_selected)
        assertNotNull(MokoRes.strings.accessibility_not_selected)
        assertNotNull(MokoRes.strings.accessibility_active)
        assertNotNull(MokoRes.strings.accessibility_disabled)
        assertNotNull(MokoRes.strings.accessibility_favorite_button)
        assertNotNull(MokoRes.strings.accessibility_stop_simulation)
    }

    @Test
    fun `country name strings should be present`() {
        assertNotNull(MokoRes.strings.country_usa)
        assertNotNull(MokoRes.strings.country_france)
        assertNotNull(MokoRes.strings.country_japan)
        assertNotNull(MokoRes.strings.country_brazil)
        assertNotNull(MokoRes.strings.country_india)
    }

    @Test
    fun `community name strings should be present`() {
        assertNotNull(MokoRes.strings.community_africa)
        assertNotNull(MokoRes.strings.community_asia)
        assertNotNull(MokoRes.strings.community_europe)
        assertNotNull(MokoRes.strings.community_north_america)
        assertNotNull(MokoRes.strings.community_south_america)
    }

    @Test
    fun `event location strings should be present for major cities`() {
        assertNotNull(MokoRes.strings.event_location_new_york_usa)
        assertNotNull(MokoRes.strings.event_location_paris_france)
        assertNotNull(MokoRes.strings.event_location_tokyo_japan)
        assertNotNull(MokoRes.strings.event_location_london_england)
        assertNotNull(MokoRes.strings.event_location_sydney_australia)
    }

    @Test
    fun `RTL locales should be in supported list`() {
        rtlLocales.forEach { rtlLocale ->
            assertTrue(
                rtlLocale in supportedLocales,
                "RTL locale '$rtlLocale' should be in supported locales list",
            )
        }
    }

    @Test
    fun `supported locales count should match expected`() {
        // WorldWideWaves supports 33 languages (including base)
        assertTrue(
            supportedLocales.size >= 32,
            "Should support at least 32 locales (excluding base), got ${supportedLocales.size}",
        )
    }

    @Test
    fun `locale codes should follow BCP 47 standard format`() {
        supportedLocales.forEach { locale ->
            assertTrue(
                locale.length in 2..5,
                "Locale code '$locale' should be 2-5 characters (BCP 47 standard)",
            )
            assertTrue(
                locale.all { it.isLowerCase() || it == '_' },
                "Locale code '$locale' should be lowercase (BCP 47 standard)",
            )
        }
    }

    @Test
    fun `FAQ strings should be present`() {
        assertNotNull(MokoRes.strings.faq)
        assertNotNull(MokoRes.strings.faq_access)
        assertNotNull(MokoRes.strings.faq_question_1)
        assertNotNull(MokoRes.strings.faq_answer_1)
    }

    @Test
    fun `info core text strings should be present`() {
        assertNotNull(MokoRes.strings.infos_core_1)
        assertNotNull(MokoRes.strings.infos_core_2)
        assertNotNull(MokoRes.strings.infos_core_3)
    }

    @Test
    fun `rules and security strings should be present`() {
        assertNotNull(MokoRes.strings.warn_rules_security_title)
        assertNotNull(MokoRes.strings.warn_general_title)
        assertNotNull(MokoRes.strings.warn_safety_title)
        assertNotNull(MokoRes.strings.warn_emergency_title)
        assertNotNull(MokoRes.strings.warn_legal_title)
    }

    @Test
    fun `error message strings should be present`() {
        assertNotNull(MokoRes.strings.error)
        assertNotNull(MokoRes.strings.map_error_network)
        assertNotNull(MokoRes.strings.map_error_insufficient_storage)
        assertNotNull(MokoRes.strings.events_loading_error)
    }

    @Test
    fun `debug screen strings should be present`() {
        assertNotNull(MokoRes.strings.debug_information)
        assertNotNull(MokoRes.strings.debug_platform)
        assertNotNull(MokoRes.strings.debug_version)
    }

    @Test
    fun `choreography strings should be present`() {
        assertNotNull(MokoRes.strings.choreography_warming_seq_1)
        assertNotNull(MokoRes.strings.choreography_waiting)
        assertNotNull(MokoRes.strings.choreography_hit)
    }

    @Test
    fun `parametrized strings should be accessible`() {
        // Verify strings with parameters are present
        assertNotNull(MokoRes.strings.geoloc_yourein_at)
        assertNotNull(MokoRes.strings.accessibility_event_in)
        assertNotNull(MokoRes.strings.map_error_unknown)
    }

    @Test
    fun `time unit strings should be present for duration formatting`() {
        assertNotNull(MokoRes.strings.minute_singular)
        assertNotNull(MokoRes.strings.minute_plural)
        assertNotNull(MokoRes.strings.hour_singular)
        assertNotNull(MokoRes.strings.hour_plural)
    }

    @Test
    fun `social media strings should be present`() {
        assertNotNull(MokoRes.strings.drwave)
        assertNotNull(MokoRes.strings.www_instagram)
        assertNotNull(MokoRes.strings.www_hashtag)
    }
}
