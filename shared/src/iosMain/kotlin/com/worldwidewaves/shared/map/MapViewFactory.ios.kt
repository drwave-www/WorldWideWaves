package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.utils.Log
import org.koin.mp.KoinPlatform

/**
 * iOS implementation using dependency injection.
 * Gets NativeMapViewProvider from Koin (implementation provided by iOS app).
 */
actual fun createNativeMapViewController(
    event: IWWWEvent,
    styleURL: String,
): Any {
    Log.i("MapViewFactory", "Creating map view for: ${event.id} via Koin provider")

    val provider = KoinPlatform.getKoin().getOrNull<NativeMapViewProvider>()

    return if (provider != null) {
        Log.d("MapViewFactory", "Using NativeMapViewProvider from Koin")
        provider.createMapView(event, styleURL)
    } else {
        Log.w("MapViewFactory", "NativeMapViewProvider not registered in Koin - using default implementation")
        // Fallback: Use default iOS implementation
        IOSNativeMapViewProvider().createMapView(event, styleURL)
    }
}
