package com.worldwidewaves.shared

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

import com.worldwidewaves.shared.di.IOSModule
import com.worldwidewaves.shared.di.sharedModule
import com.worldwidewaves.shared.utils.initNapier
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.PrintLogger

/**
 * Initialise Koin for iOS.
 *
 * Swift code calls this via `HelperKt.doInitKoin()`.
 * We load every common module *and* the iOS-specific module only once.
 */
@Throws(Throwable::class)
fun doInitKoin() {
    // Prevent multiple initialisations when called repeatedly from Swift previews/tests.
    if (koinApp != null) return

    // Initialize Napier logging for iOS
    platform.Foundation.NSLog("HELPER: doInitKoin() starting with enhanced coroutine exception handling")

    // Initialize MokoRes bundle BEFORE anything else
    platform.Foundation.NSLog("HELPER: About to initialize MokoRes bundle")
    try {
        val bundleInitialized = BundleInitializer.initializeBundle()
        platform.Foundation.NSLog("HELPER: MokoRes bundle initialization result: $bundleInitialized")
    } catch (e: Exception) {
        platform.Foundation.NSLog("ERROR: MokoRes bundle initialization failed: ${e.message}")
    }

    // Re-enable initNapier with bulletproof NSLogAntilog
    platform.Foundation.NSLog("HELPER: About to call initNapier()")
    try {
        initNapier()
        platform.Foundation.NSLog("HELPER: initNapier() completed successfully")
    } catch (e: Exception) {
        platform.Foundation.NSLog("ERROR: initNapier() failed: ${e.message}")
    }

    platform.Foundation.NSLog("HELPER: About to call startKoin")
    try {
        platform.Foundation.NSLog("HELPER: Testing sharedModule access...")
        val sharedModulesCount = sharedModule.size
        platform.Foundation.NSLog("HELPER: sharedModule has $sharedModulesCount modules")

        platform.Foundation.NSLog("HELPER: Testing IOSModule access...")
        val iosModuleName = IOSModule.toString()
        platform.Foundation.NSLog("HELPER: IOSModule: $iosModuleName")

        platform.Foundation.NSLog("HELPER: About to create startKoin block...")
        koinApp =
            startKoin {
                platform.Foundation.NSLog("HELPER: Inside startKoin block")
                // Add iOS logging equivalent to Android's androidLogger()
                logger(PrintLogger(Level.DEBUG))
                platform.Foundation.NSLog("HELPER: Logger added")
                // `sharedModule` is already a List<Module>; add the iOS-specific one.
                modules(sharedModule + IOSModule)
                platform.Foundation.NSLog("HELPER: Modules added")
            }
        platform.Foundation.NSLog("HELPER: startKoin completed successfully")
    } catch (e: Exception) {
        platform.Foundation.NSLog("ERROR: startKoin failed: ${e.message}")
        platform.Foundation.NSLog("ERROR: Exception type: ${e::class.simpleName}")
    }
}

/**
 * Deprecated alias kept so existing Kotlin callers (if any) continue to compile.
 */
@Deprecated(
    message = "Renamed to doInitKoin() to match Swift side.",
    replaceWith = ReplaceWith("doInitKoin()"),
)
fun initKoin() = doInitKoin()

// ------------------------------------------------------------
// Private holder to remember if Koin has already been started.
// `KoinApplication` is available on every KMP target so we can
// safely keep the reference here.
// ------------------------------------------------------------

private var koinApp: KoinApplication? = null
