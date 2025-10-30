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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages runtime locale changes and notifies observers when the system language changes.
 *
 * ## Purpose
 * Provides a reactive mechanism for detecting and responding to device language changes
 * without requiring app restart. When the user changes their device language in system
 * settings, this manager emits the new locale to observers, allowing the UI to recompose
 * with localized strings.
 *
 * ## Architecture
 * - **Common Interface**: Defines platform-agnostic locale change observable
 * - **Platform Implementations**: Android and iOS detect locale changes via platform APIs
 * - **StateFlow Pattern**: Uses StateFlow for reactive, single-source-of-truth locale tracking
 * - **Koin Integration**: Registered as singleton in CommonModule for DI access
 *
 * ## Usage
 * ```kotlin
 * // In Compose UI
 * val localizationManager = get<LocalizationManager>()
 * val currentLocale by localizationManager.localeChanges.collectAsState()
 *
 * // Trigger recomposition on locale change
 * key(currentLocale) {
 *     MainScreen().Draw()
 * }
 * ```
 *
 * ## Platform-Specific Detection
 * - **Android**: MainActivity.onConfigurationChanged() detects locale changes
 * - **iOS**: SceneDelegate observes NSLocale.currentLocaleDidChangeNotification
 *
 * ## Thread Safety
 * - StateFlow is thread-safe for concurrent read/write
 * - Locale key emission uses tryEmit() for fire-and-forget semantics
 * - No synchronization needed for observers (StateFlow handles concurrency)
 *
 * @see LocalizationManager.android.kt for Android implementation details
 * @see LocalizationBridge.ios.kt for iOS notification bridge
 */
class LocalizationManager {
    /**
     * StateFlow emitting the current locale key (BCP 47 language tag).
     *
     * Format: Language code (e.g., "en", "es", "fr", "zh") or full tag (e.g., "en-US").
     * Emits a new value when the device language changes at runtime.
     *
     * Observers can collect this flow to trigger UI recomposition:
     * ```kotlin
     * val currentLocale by localizationManager.localeChanges.collectAsState()
     * ```
     */
    private val _localeChanges = MutableStateFlow(getCurrentLocaleKey())
    val localeChanges: StateFlow<String> = _localeChanges.asStateFlow()

    /**
     * Notifies the manager that the system locale has changed.
     *
     * Called by platform-specific detection mechanisms:
     * - Android: From MainActivity.onConfigurationChanged()
     * - iOS: From LocalizationBridge.notifyLocaleChanged()
     *
     * Emits the new locale key to all StateFlow observers, triggering UI updates.
     *
     * @param newLocaleKey The new locale key (BCP 47 format, e.g., "fr", "en-US")
     */
    fun notifyLocaleChanged(newLocaleKey: String) {
        _localeChanges.tryEmit(newLocaleKey)
    }

    /**
     * Gets the current device locale key.
     *
     * Platform-specific implementation:
     * - Android: Uses Locale.getDefault().toLanguageTag()
     * - iOS: Uses NSLocale.currentLocale.languageCode
     *
     * @return Current locale key (BCP 47 format, e.g., "en", "es-MX")
     */
    private fun getCurrentLocaleKey(): String = getPlatformLocaleKey()
}

/**
 * Platform-specific function to get the current locale key.
 *
 * Implementations:
 * - Android: Returns Locale.getDefault().toLanguageTag()
 * - iOS: Returns NSLocale.currentLocale.languageCode
 *
 * @return Current locale key in BCP 47 format
 */
expect fun getPlatformLocaleKey(): String
