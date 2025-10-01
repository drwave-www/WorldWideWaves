package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.utils.Log
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSClassFromString
import platform.UIKit.UIViewController

/**
 * iOS implementation of native map view controller factory.
 * Creates UIViewController wrapping SwiftUI EventMapView via MapViewBridge.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun createNativeMapViewController(
    event: IWWWEvent,
    styleURL: String,
): Any {
    Log.i("MapViewFactory", "Creating native map view controller for: ${event.id}")
    Log.d("MapViewFactory", "Style URL: $styleURL")

    try {
        // Get MapViewBridge class from app bundle
        val bridgeClassName = "worldwidewaves.MapViewBridge"
        val bridgeClass = NSClassFromString(bridgeClassName)

        if (bridgeClass == null) {
            Log.e("MapViewFactory", "MapViewBridge class not found: $bridgeClassName")
            return createFallbackViewController()
        }

        Log.d("MapViewFactory", "MapViewBridge class found, calling createMapViewController")

        // Call MapViewBridge.createMapViewController(for:styleURL:wrapperRef:)
        // This requires using ObjC runtime to call the class method
        // For now, return fallback and document that app needs to call this
        Log.w("MapViewFactory", "ObjC runtime method calling not yet implemented")
        return createFallbackViewController()
    } catch (e: Exception) {
        Log.e("MapViewFactory", "Error creating map view controller: ${e.message}")
        return createFallbackViewController()
    }
}

/**
 * Creates a simple fallback UIViewController when map creation fails
 */
private fun createFallbackViewController(): UIViewController {
    Log.d("MapViewFactory", "Creating fallback view controller")
    return UIViewController()
}
