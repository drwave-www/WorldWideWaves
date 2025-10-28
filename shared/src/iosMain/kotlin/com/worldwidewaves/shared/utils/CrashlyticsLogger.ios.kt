@file:Suppress("MatchingDeclarationName") // expect/actual pattern requires .ios.kt suffix

package com.worldwidewaves.shared.utils

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

/**
 * iOS implementation of Crashlytics integration.
 *
 * ## Implementation Details
 * - iOS Crashlytics integration is handled natively in Swift (AppDelegate/SceneDelegate)
 * - This implementation is a no-op stub since Firebase is initialized and managed in iOS app layer
 * - Actual crash reporting happens automatically via Firebase iOS SDK
 * - Non-fatal error reporting can be added via Swift bridge pattern if needed in future
 *
 * ## Why No-Op?
 * Firebase Crashlytics on iOS works best when initialized in AppDelegate and used directly from Swift.
 * The Kotlin/Native -> Swift bridging for error reporting introduces complexity without significant benefit
 * since crashes are already captured automatically by Firebase SDK.
 *
 * ## Future Enhancement
 * To enable non-fatal error reporting from Kotlin code:
 * 1. Create Swift wrapper in iosApp/worldwidewaves/Utils/
 * 2. Expose via KMM native interop (not external object)
 * 3. Call from this implementation
 *
 * ## Threading Model
 * No-op implementation is thread-safe (does nothing)
 *
 * ## Production Safety
 * Safe no-op - no crashes, no side effects
 */
actual object CrashlyticsLogger {
    actual fun recordException(
        throwable: Throwable,
        tag: String,
        message: String,
    ) {
        // No-op: iOS Firebase Crashlytics handles crashes automatically
        // Non-fatal errors can be logged via Swift bridge in future if needed
    }

    actual fun log(message: String) {
        // No-op: Breadcrumbs can be added via Swift bridge in future if needed
    }

    actual fun setCustomKey(
        key: String,
        value: String,
    ) {
        // No-op: Custom keys can be set via Swift bridge in future if needed
    }
}
