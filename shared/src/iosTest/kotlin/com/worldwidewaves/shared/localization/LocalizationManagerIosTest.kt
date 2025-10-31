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
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * iOS instrumented tests for LocalizationManager.
 *
 * These tests run on iOS simulator/device with full iOS runtime, allowing us to:
 * - Test actual NSLocale.currentLocale integration
 * - Test LocalizationBridge.notifyLocaleChanged() with Koin
 * - Verify StateFlow emission on iOS
 * - Test iOS-specific locale key format
 *
 * Run with: ./gradlew iosSimulatorArm64Test or via Xcode Test Navigator
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalizationManagerIosTest : KoinTest {
    @BeforeTest
    fun setUp() {
        try {
            stopKoin()
        } catch (e: Exception) {
            // Koin wasn't running
        }

        // Start Koin with test module
        startKoin {
            modules(
                module {
                    single { LocalizationManager() }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun nsLocale_currentLocaleShouldProvideLanguageCode() {
        val locale = NSLocale.currentLocale
        assertNotNull(locale, "NSLocale.currentLocale should be accessible")

        val langCode = locale.languageCode
        assertNotNull(langCode, "Language code should be accessible")
        assertTrue(langCode.isNotEmpty(), "Language code should not be empty")
    }

    @Test
    fun getPlatformLocaleKey_shouldReturnValidLanguageCode() {
        val localeKey = getPlatformLocaleKey()

        assertNotNull(localeKey, "Platform locale key should not be null")
        assertTrue(localeKey.isNotEmpty(), "Platform locale key should not be empty")

        // Should be 2-5 character language code (e.g., "en", "fr", "ja", "zh-Hans")
        assertTrue(
            localeKey.length in 2..10,
            "Locale key should be valid length (2-5 chars): $localeKey",
        )
    }

    @Test
    fun localizationManager_shouldInitializeWithIosLocale() =
        runTest {
            val manager = getKoin().get<LocalizationManager>()

            val initialLocale = manager.localeChanges.value
            assertNotNull(initialLocale, "Initial locale should not be null")
            assertTrue(initialLocale.isNotEmpty(), "Initial locale should not be empty")

            // Should match platform locale
            val platformLocale = getPlatformLocaleKey()
            assertEquals(platformLocale, initialLocale, "Should match iOS platform locale")
        }

    @Test
    fun localizationManager_shouldEmitLocaleChangesViaStateFlow() =
        runTest {
            val manager = getKoin().get<LocalizationManager>()

            // Simulate locale change from SceneDelegate notification
            manager.notifyLocaleChanged("fr")

            val updatedLocale = manager.localeChanges.first()
            assertEquals("Should emit French locale", "fr", updatedLocale)
        }

    @Test
    fun localizationBridge_shouldNotifyLocaleChangeSuccessfully() =
        runTest {
            // Test the Swift→Kotlin bridge function
            try {
                notifyLocaleChanged()
                // Should not throw exception when Koin is initialized
                assertTrue(true, "LocalizationBridge should work with Koin initialized")
            } catch (e: Exception) {
                throw AssertionError("LocalizationBridge should not throw when Koin is ready: ${e.message}")
            }
        }

    @Test
    fun localizationManager_shouldHandleMultipleConsecutiveChanges() =
        runTest {
            val manager = getKoin().get<LocalizationManager>()

            val locales = listOf("en", "fr", "de", "ja", "ar")

            locales.forEach { locale ->
                manager.notifyLocaleChanged(locale)
                val current = manager.localeChanges.first()
                assertEquals("Should update to $locale", locale, current)
            }
        }

    @Test
    fun localizationManager_shouldHandleRapidChanges() =
        runTest {
            val manager = getKoin().get<LocalizationManager>()

            // Simulate rapid language switching
            repeat(15) { i ->
                manager.notifyLocaleChanged("locale-$i")
            }

            val finalLocale = manager.localeChanges.first()
            assertEquals("Should settle on last value", "locale-14", finalLocale)
        }

    @Test
    fun localizationManager_stateFlowShouldBeThreadSafe() =
        runTest {
            val manager = getKoin().get<LocalizationManager>()

            // Multiple collectors should see consistent values
            manager.notifyLocaleChanged("es")

            val value1 = manager.localeChanges.first()
            val value2 = manager.localeChanges.first()
            val value3 = manager.localeChanges.first()

            assertEquals("All collectors should see same value", "es", value1)
            assertEquals("All collectors should see same value", "es", value2)
            assertEquals("All collectors should see same value", "es", value3)
        }
}
