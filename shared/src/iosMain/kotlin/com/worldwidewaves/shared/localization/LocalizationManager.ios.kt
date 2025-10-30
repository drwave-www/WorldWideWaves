@file:Suppress("MatchingDeclarationName") // Platform-specific actual file naming (.ios.kt)

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

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

/**
 * iOS implementation of platform locale detection.
 *
 * Uses NSLocale.currentLocale to retrieve the current iOS system locale.
 * This respects:
 * - System-wide language setting (Settings → General → Language & Region)
 * - Per-app language setting (iOS 13+, Settings → [App] → Language)
 * - NSLocale.current changes when user modifies language settings
 *
 * @return Language code (e.g., "en", "fr", "ja", "es")
 */
actual fun getPlatformLocaleKey(): String {
    val currentLocale = NSLocale.currentLocale
    return currentLocale.languageCode ?: "en" // Fallback to English if unavailable
}
