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
import org.koin.mp.KoinPlatform

/**
 * Bridge between Swift and Kotlin for locale change notifications on iOS.
 *
 * ## Purpose
 * Provides a Swift-callable function that notifies the Kotlin layer when the iOS
 * system locale changes. This enables runtime language switching without app restart.
 *
 * ## Architecture
 * ```
 * iOS System Settings (Language Change)
 *       ↓
 * NSLocale.currentLocaleDidChangeNotification
 *       ↓
 * SceneDelegate.swift (NotificationCenter observer)
 *       ↓
 * LocalizationBridge.notifyLocaleChanged() (this file)
 *       ↓
 * LocalizationManager.notifyLocaleChanged()
 *       ↓
 * StateFlow emission
 *       ↓
 * Compose UI recomposition
 * ```
 *
 * ## Threading Model
 * - Called from Swift on main thread (NotificationCenter default)
 * - StateFlow emission is thread-safe
 * - No special threading handling required
 *
 * ## Error Handling
 * - Catches and logs Koin access failures
 * - Throws exception to Swift caller for visibility
 * - Swift must use try-catch when calling:
 *   ```swift
 *   do {
 *       try LocalizationBridgeKt.notifyLocaleChanged()
 *   } catch {
 *       NSLog("Locale change notification failed: \(error)")
 *   }
 *   ```
 *
 * ## Koin Dependency
 * Requires LocalizationManager to be registered in CommonModule before calling.
 * This is guaranteed if called after platform initialization in SceneDelegate.
 *
 * @throws Exception if LocalizationManager is not available in Koin (initialization failure)
 * @see LocalizationManager for the locale change observable
 * @see SceneDelegate.swift for Swift caller implementation
 */
private const val TAG = "LocalizationBridge"

/**
 * Notifies the Kotlin layer that the iOS system locale has changed.
 *
 * ## When to Call
 * Call this function when receiving NSLocale.currentLocaleDidChangeNotification
 * in Swift code (typically in SceneDelegate or AppDelegate).
 *
 * ## What It Does
 * 1. Retrieves current locale key via getPlatformLocaleKey()
 * 2. Gets LocalizationManager singleton from Koin
 * 3. Calls notifyLocaleChanged() to emit new locale to StateFlow
 * 4. Logs the locale change for debugging
 *
 * ## Thread Safety
 * Safe to call from main thread (standard NotificationCenter behavior).
 *
 * @throws Throwable if Koin is not initialized or LocalizationManager is unavailable
 */
@Throws(Throwable::class)
fun notifyLocaleChanged() {
    try {
        val newLocaleKey = getPlatformLocaleKey()
        val localizationManager = KoinPlatform.getKoin().get<LocalizationManager>()

        localizationManager.notifyLocaleChanged(newLocaleKey)

        Log.i(TAG, "Locale change notified: $newLocaleKey")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to notify locale change: ${e.message}", e)
        throw e
    }
}
