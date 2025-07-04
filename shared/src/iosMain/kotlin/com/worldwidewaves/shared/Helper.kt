package com.worldwidewaves.shared

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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

import com.worldwidewaves.shared.di.IOSModule
import com.worldwidewaves.shared.di.sharedModule
import org.koin.core.context.startKoin
import org.koin.core.KoinApplication

/**
 * Initialise Koin for iOS.
 *
 * Swift code calls this via `HelperKt.doInitKoin()`.
 * We load every common module *and* the iOS-specific module only once.
 */
fun doInitKoin() {
    // Prevent multiple initialisations when called repeatedly from Swift previews/tests.
    if (koinApp != null) return

    koinApp = startKoin {
        // `sharedModule` is already a List<Module>; add the iOS-specific one.
        modules(sharedModule + IOSModule)
    }
}

/**
 * Deprecated alias kept so existing Kotlin callers (if any) continue to compile.
 */
@Deprecated(
    message = "Renamed to doInitKoin() to match Swift side.",
    replaceWith = ReplaceWith("doInitKoin()")
)
fun initKoin() = doInitKoin()

// ------------------------------------------------------------
// Private holder to remember if Koin has already been started.
// `KoinApplication` is available on every KMP target so we can
// safely keep the reference here.
// ------------------------------------------------------------

private var koinApp: KoinApplication? = null