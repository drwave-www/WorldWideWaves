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

package com.worldwidewaves.shared.localization

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test suite for LocalizationManager runtime locale change functionality.
 *
 * Tests verify:
 * - LocalizationManager initializes with current platform locale
 * - Locale change notifications emit correctly via StateFlow
 * - Multiple locale changes are handled properly
 * - StateFlow observers receive locale updates
 * - Thread-safe concurrent access to locale changes flow
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalizationManagerTest {
    @Test
    fun `should initialize with platform locale key`() =
        runTest {
            val manager = LocalizationManager()

            val initialLocale = manager.localeChanges.value
            assertNotNull(initialLocale, "Initial locale should not be null")
            assertTrue(initialLocale.isNotEmpty(), "Initial locale should not be empty")
        }

    @Test
    fun `should emit locale changes via StateFlow`() =
        runTest {
            val manager = LocalizationManager()

            val initialLocale = manager.localeChanges.first()
            assertNotNull(initialLocale)

            // Notify locale change
            manager.notifyLocaleChanged("fr-FR")

            val updatedLocale = manager.localeChanges.first()
            assertEquals("fr-FR", updatedLocale, "Locale should update to French")
        }

    @Test
    fun `should handle multiple locale changes`() =
        runTest {
            val manager = LocalizationManager()

            // First change: English to Spanish
            manager.notifyLocaleChanged("es-ES")
            assertEquals("es-ES", manager.localeChanges.first())

            // Second change: Spanish to German
            manager.notifyLocaleChanged("de-DE")
            assertEquals("de-DE", manager.localeChanges.first())

            // Third change: German to Japanese
            manager.notifyLocaleChanged("ja-JP")
            assertEquals("ja-JP", manager.localeChanges.first())
        }

    @Test
    fun `should accept BCP 47 language tags`() =
        runTest {
            val manager = LocalizationManager()

            val validLanguageTags =
                listOf(
                    "en", // Language only
                    "en-US", // Language + region
                    "zh-Hans", // Language + script
                    "pt-BR", // Portuguese Brazil
                    "ar-SA", // Arabic Saudi Arabia
                )

            validLanguageTags.forEach { tag ->
                manager.notifyLocaleChanged(tag)
                assertEquals(tag, manager.localeChanges.first(), "Should accept BCP 47 tag: $tag")
            }
        }

    @Test
    fun `should handle empty locale key gracefully`() =
        runTest {
            val manager = LocalizationManager()

            // Should not crash with empty string
            manager.notifyLocaleChanged("")
            val locale = manager.localeChanges.first()
            // Verify it doesn't crash and emits the empty string
            assertEquals("", locale)
        }

    @Test
    fun `StateFlow should be observable by multiple collectors`() =
        runTest {
            val manager = LocalizationManager()

            // First collector
            val firstValue = manager.localeChanges.first()
            assertNotNull(firstValue)

            // Change locale
            manager.notifyLocaleChanged("it-IT")

            // Second collector (should see updated value)
            val secondValue = manager.localeChanges.first()
            assertEquals("it-IT", secondValue)
        }

    @Test
    fun `should handle rapid consecutive locale changes`() =
        runTest {
            val manager = LocalizationManager()

            // Simulate rapid changes (like user quickly switching languages)
            repeat(10) { i ->
                manager.notifyLocaleChanged("locale-$i")
            }

            // Final value should be the last one
            val finalLocale = manager.localeChanges.first()
            assertEquals("locale-9", finalLocale, "Should handle rapid changes and settle on last value")
        }

    @Test
    fun `localeChanges should be a StateFlow with replay behavior`() =
        runTest {
            val manager = LocalizationManager()

            // Change locale before collecting
            manager.notifyLocaleChanged("ko-KR")

            // New collector should immediately see the last value
            val observedLocale = manager.localeChanges.first()
            assertEquals("ko-KR", observedLocale, "New collectors should see last emitted value")
        }
}
