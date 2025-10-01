package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.utils.Log
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import platform.UIKit.UIViewController

// External C function from iOS app (MapViewFactoryHelper.m)
@OptIn(ExperimentalForeignApi::class)
private external fun WWW_createMapViewController(
    styleURL: CPointer<ByteVar>?,
    latitude: Double,
    longitude: Double,
    zoom: Double,
): UIViewController?

/**
 * iOS implementation of NativeMapViewProvider.
 * Calls C function wrapper in iOS app to create MapLibre map views.
 */
class IOSNativeMapViewProvider : NativeMapViewProvider {
    @OptIn(ExperimentalForeignApi::class)
    override fun createMapView(
        event: IWWWEvent,
        styleURL: String,
    ): Any {
        Log.i("IOSNativeMapViewProvider", "Creating map view for: ${event.id}")
        Log.d("IOSNativeMapViewProvider", "Style URL: $styleURL")

        return try {
            memScoped {
                // Call C function from iOS app
                val viewController =
                    WWW_createMapViewController(
                        styleURL = styleURL.cstr.ptr,
                        latitude = 48.8566, // TODO: Get from event
                        longitude = 2.3522,
                        zoom = 12.0,
                    )

                if (viewController != null) {
                    Log.i("IOSNativeMapViewProvider", "Map view created successfully")
                    viewController
                } else {
                    Log.e("IOSNativeMapViewProvider", "WWW_createMapViewController returned null")
                    UIViewController()
                }
            }
        } catch (e: Exception) {
            Log.e("IOSNativeMapViewProvider", "Error creating map view: ${e.message}")
            UIViewController()
        }
    }
}
