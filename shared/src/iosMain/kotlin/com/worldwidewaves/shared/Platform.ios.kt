package com.worldwidewaves.shared

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.di.initializeSimulationMode
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.utils.initNapier
import com.worldwidewaves.shared.utils.setupDebugSimulation
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.desc
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.PrintLogger
import org.koin.mp.KoinPlatform

private const val TAG = "WWW.Platform.iOS"

/**
 * Initialise Koin (unchanged pattern; keep as used in your app).
 */
@Throws(Throwable::class)
fun doInitPlatform() {
    if (koinApp != null) return
    Log.v(TAG, "HELPER: doInitKoin()")

    try {
        initNapier()
    } catch (_: Throwable) {
    }

    try {
        koinApp =
            startKoin {
                logger(PrintLogger(if (BuildKonfig.DEBUG) Level.INFO else Level.ERROR))
                modules(com.worldwidewaves.shared.di.sharedModule + com.worldwidewaves.shared.di.IosModule)
            }

        // Use PlatformEnabler.isDebugBuild to determine if simulation should be enabled
        // This ensures simulation is only active in debug builds (set by Swift's #if DEBUG)
        val platformEnabler = KoinPlatform.getKoin().get<PlatformEnabler>()
        if (platformEnabler.isDebugBuild) {
            try {
                // Enable simulation mode (makes the red indicator visible)
                val platform = KoinPlatform.getKoin().get<WWWPlatform>()
                initializeSimulationMode(platform, true)

                // Set up simulation data (time, position, etc.)
                setupDebugSimulation()
            } catch (_: Throwable) {
            }
        }

        // NOTE: RuntimeLogConfig initialization deferred - using build-time configuration fallback.
        // Firebase Remote Config integration requires FirebaseRemoteConfig CocoaPod in iosApp project.
    } catch (e: Exception) {
        Log.e(TAG, "startKoin failed: ${e.message}")
    }
}

private var koinApp: KoinApplication? = null

actual fun localizeString(resource: StringResource): String = resource.desc().localized()
