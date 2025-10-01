package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.utils.Log
import platform.UIKit.UIViewController

/**
 * iOS implementation of native map view controller factory.
 *
 * Returns a placeholder UIViewController. The iOS app should call the Shared module's
 * map rendering components which will use this factory.
 *
 * The actual MapLibre integration happens in the iOS app via:
 * - iosApp/worldwidewaves/MapLibre/WWWMapViewBridge.m (compiled by Xcode)
 * - iosApp/worldwidewaves/MapLibre/MapViewBridge.swift (SwiftUI wrapper)
 * - iosApp/worldwidewaves/MapLibre/EventMapView.swift (SwiftUI map view)
 *
 * This placeholder allows the Kotlin code to compile and provides a visual
 * indicator that the iOS app needs to provide the actual implementation.
 */
actual fun createNativeMapViewController(
    event: IWWWEvent,
    styleURL: String,
): Any {
    Log.i("MapViewFactory", "Creating placeholder map view controller for: ${event.id}")
    Log.d("MapViewFactory", "Style URL: $styleURL")
    Log.w(
        "MapViewFactory",
        "Returning placeholder - iOS app should implement WWWMapViewBridge or use EventMapView directly",
    )

    // Return placeholder that shows visual feedback
    val viewController = UIViewController()
    // The placeholder will show a gray background
    // iOS app implementation will replace this with actual map

    return viewController
}
