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

import com.worldwidewaves.shared.utils.Log
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

private const val TAG = "LocalizationManager.ios"

/**
 * iOS implementation of platform locale detection.
 *
 * Uses NSLocale.preferredLanguages to retrieve the user's actual language preference.
 * This correctly respects:
 * - System-wide language setting (Settings → General → Language & Region)
 * - Per-app language setting (iOS 13+, Settings → [App] → Language)
 * - Changes when user modifies language settings
 *
 * ## Why preferredLanguages instead of currentLocale.languageCode?
 * - currentLocale.languageCode returns the REGION FORMAT locale (e.g., "en" for US region)
 * - preferredLanguages returns the user's actual LANGUAGE PREFERENCE (e.g., "fr" for French)
 * - Example: User in US region with French language → currentLocale="en", preferredLanguages=["fr-US"]
 *
 * @return Language code (e.g., "en", "fr", "ja", "es")
 * @see <a href="https://stackoverflow.com/q/1522210">NSLocale currentLocale always returns en_US</a>
 */
actual fun getPlatformLocaleKey(): String {
    Log.i(TAG, "=== getPlatformLocaleKey() called ===")

    // Get the user's preferred languages (ordered by preference)
    val preferredLanguages = NSLocale.preferredLanguages
    Log.i(TAG, "Got preferredLanguages, size = ${preferredLanguages.size}")

    if (preferredLanguages.isEmpty()) {
        Log.w(TAG, "EMPTY preferredLanguages array, returning 'en'")
        return "en"
    }

    val firstLang = preferredLanguages.first()
    Log.i(TAG, "First element type: ${firstLang?.let { it::class.simpleName } ?: "null"}")
    Log.i(TAG, "First element toString: $firstLang")

    val primaryLanguage =
        firstLang as? String ?: run {
            Log.w(TAG, "Could not cast first language to String, returning 'en'")
            return "en"
        }
    Log.i(TAG, "primaryLanguage cast to String: '$primaryLanguage'")

    val languageCode = primaryLanguage.split("-", "_").firstOrNull() ?: "en"
    Log.i(TAG, "extracted languageCode: '$languageCode'")

    val result = languageCode.lowercase()
    Log.i(TAG, "=== RETURNING: '$result' ===")
    return result
}
