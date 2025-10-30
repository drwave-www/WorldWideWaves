package com.worldwidewaves.shared

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.di.initializeSimulationMode
import com.worldwidewaves.shared.localization.getPlatformLocaleKey
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.utils.initNapier
import com.worldwidewaves.shared.utils.setupDebugSimulation
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.StringDesc
import dev.icerock.moko.resources.desc.desc
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.PrintLogger
import org.koin.mp.KoinPlatform

private const val TAG = "WWW.Platform.iOS"

/**
 * Initialise Koin and configure MokoResources locale for iOS.
 *
 * ## Initialization Order
 * 1. Initialize Napier logging
 * 2. Configure MokoResources locale (CRITICAL for iOS localization)
 * 3. Start Koin dependency injection
 *
 * ## MokoResources Locale Configuration
 * On iOS, MokoResources requires explicit locale configuration via StringDesc.localeType.
 * Unlike Android which uses Context for automatic locale handling, iOS needs manual setup.
 * This ensures the app displays text in the device's system language from launch.
 *
 * @throws Throwable if platform initialization fails
 */
@Throws(Throwable::class)
fun doInitPlatform() {
    if (koinApp != null) return
    Log.v(TAG, "HELPER: doInitKoin()")

    try {
        initNapier()
    } catch (_: Throwable) {
    }

    // Initialize MokoResources with device locale BEFORE Koin
    // This ensures localized strings work correctly from app launch
    try {
        val deviceLocale = getPlatformLocaleKey()
        StringDesc.localeType = StringDesc.LocaleType.Custom(deviceLocale)
        Log.i(TAG, "MokoResources initialized with locale: $deviceLocale")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize locale, falling back to system default: ${e.message}")
        // Don't throw - allow app to continue with default locale
    }

    try {
        koinApp =
            startKoin {
                logger(PrintLogger(if (BuildKonfig.DEBUG) Level.INFO else Level.ERROR))
                modules(com.worldwidewaves.shared.di.sharedModule + com.worldwidewaves.shared.di.IosModule)
            }

        // NOTE: Simulation initialization moved to initializeDebugSimulation()
        // Must be called AFTER PlatformEnabler registration (see SceneDelegate.swift)

        // NOTE: RuntimeLogConfig initialization deferred - using build-time configuration fallback.
        // Firebase Remote Config integration requires FirebaseRemoteConfig CocoaPod in iosApp project.
    } catch (e: Exception) {
        Log.e(TAG, "startKoin failed: ${e.message}")
    }
}

private var koinApp: KoinApplication? = null

/**
 * Initialize debug simulation for iOS.
 * Must be called AFTER PlatformEnabler is registered into Koin.
 * This is separate from doInitPlatform() because PlatformEnabler registration
 * happens after Koin initialization in SceneDelegate.
 */
@Throws(Throwable::class)
fun initializeDebugSimulation() {
    try {
        // Use PlatformEnabler.isDebugBuild to determine if simulation should be enabled
        // This ensures simulation is only active in debug builds (set by Swift's #if DEBUG)
        val platformEnabler = KoinPlatform.getKoin().get<PlatformEnabler>()
        if (platformEnabler.isDebugBuild) {
            Log.d(TAG, "Debug build detected, enabling simulation mode")

            // Enable simulation mode (makes the red indicator visible)
            val platform = KoinPlatform.getKoin().get<WWWPlatform>()
            initializeSimulationMode(platform, true)

            // Set up simulation data (time, position, etc.)
            setupDebugSimulation()

            Log.i(TAG, "Debug simulation initialized successfully")
        } else {
            Log.d(TAG, "Release build, simulation mode not enabled")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize debug simulation: ${e.message}", e)
        throw e
    }
}

actual fun localizeString(resource: StringResource): String = resource.desc().localized()
