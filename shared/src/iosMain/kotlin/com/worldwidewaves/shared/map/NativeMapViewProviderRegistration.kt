package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.utils.Log
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

/**
 * Register NativeMapViewProvider from Swift/iOS app.
 * This allows the iOS app to provide its own map view implementation.
 *
 * Call this from Swift after doInitPlatform():
 * ```swift
 * registerNativeMapViewProvider(provider: SwiftNativeMapViewProvider())
 * ```
 */
fun registerNativeMapViewProvider(provider: NativeMapViewProvider) {
    Log.i("NativeMapViewProviderRegistration", "Registering NativeMapViewProvider: ${provider::class.simpleName}")

    loadKoinModules(
        module {
            single<NativeMapViewProvider> { provider }
        },
    )

    Log.i("NativeMapViewProviderRegistration", "NativeMapViewProvider registered successfully in Koin")
}
