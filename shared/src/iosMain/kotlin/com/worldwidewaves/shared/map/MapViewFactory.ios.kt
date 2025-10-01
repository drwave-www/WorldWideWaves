package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import MapViewBridge.WWWMapViewBridge
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.utils.Log
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIViewController

/**
 * iOS implementation of native map view controller factory.
 * Creates UIViewController with MapLibre map via WWWMapViewBridge (ObjC cinterop).
 */
@OptIn(ExperimentalForeignApi::class)
actual fun createNativeMapViewController(
    event: IWWWEvent,
    styleURL: String,
): Any {
    Log.i("MapViewFactory", "Creating native map view controller for: ${event.id}")
    Log.d("MapViewFactory", "Style URL: $styleURL")

    // TODO: Get actual map center from event.map (need to determine correct property)
    // For now use Paris as default
    val defaultLat = 48.8566
    val defaultLng = 2.3522

    return try {
        // Call ObjC bridge via cinterop
        val viewController =
            WWWMapViewBridge.createMapViewControllerWithStyleURL(
                styleURL = styleURL,
                latitude = defaultLat,
                longitude = defaultLng,
                zoom = 12.0,
            ) ?: UIViewController()

        Log.i("MapViewFactory", "Map view controller created successfully via WWWMapViewBridge")
        viewController
    } catch (e: Exception) {
        Log.e("MapViewFactory", "Error creating map view controller: ${e.message}")
        // Return empty fallback controller
        UIViewController()
    }
}
