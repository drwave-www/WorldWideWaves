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
 * Default iOS implementation of NativeMapViewProvider.
 * Returns a placeholder UIViewController.
 *
 * The iOS app should override this by registering a custom implementation in Koin
 * that calls WWWMapViewBridge.m or EventMapView.swift.
 */
class IOSNativeMapViewProvider : NativeMapViewProvider {
    override fun createMapView(
        event: IWWWEvent,
        styleURL: String,
    ): Any {
        Log.i("IOSNativeMapViewProvider", "Creating placeholder map view for: ${event.id}")
        Log.d("IOSNativeMapViewProvider", "Style URL: $styleURL")
        Log.w(
            "IOSNativeMapViewProvider",
            "Default implementation - iOS app should register custom NativeMapViewProvider in Koin",
        )

        // Return placeholder
        // iOS app registers custom provider that creates actual MapLibre views
        return UIViewController()
    }
}
