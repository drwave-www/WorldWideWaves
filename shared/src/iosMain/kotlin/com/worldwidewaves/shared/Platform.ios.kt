package com.worldwidewaves.shared

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.utils.initNapier
import com.worldwidewaves.shared.utils.setupDebugSimulation
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.desc
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.PrintLogger

private const val TAG = "Helper"

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
                modules(com.worldwidewaves.shared.di.sharedModule + com.worldwidewaves.shared.di.IOSModule)
            }
        if (BuildKonfig.DEBUG) {
            try {
                setupDebugSimulation()
            } catch (_: Throwable) {
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "startKoin failed: ${e.message}")
    }
}

private var koinApp: KoinApplication? = null

actual fun localizeString(resource: StringResource): String = resource.desc().localized()
