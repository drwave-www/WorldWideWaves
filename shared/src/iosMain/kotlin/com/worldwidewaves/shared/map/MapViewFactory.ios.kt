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
 * NOTE: This returns a placeholder. The actual map is provided by the iOS app's
 * WWWMapViewBridge.m which is compiled by Xcode and called at runtime.
 *
 * The challenge: Kotlin/Native can't easily call ObjC class methods with complex
 * signatures via objc_msgSend. Instead, the iOS app should:
 * 1. Compile WWWMapViewBridge.m (which creates actual MapLibre maps)
 * 2. The Xcode linker will resolve the symbol at app link time
 * 3. At runtime, the bridge will be available
 *
 * For now, returning placeholder to allow development to continue.
 * Full integration requires either:
 * - Simplified bridge interface
 * - Or app-layer override of map creation
 */
actual fun createNativeMapViewController(
    event: IWWWEvent,
    styleURL: String,
): Any {
    Log.i("MapViewFactory", "Creating map view controller for: ${event.id}")
    Log.d("MapViewFactory", "Style URL: $styleURL")
    Log.w("MapViewFactory", "Placeholder mode - WWWMapViewBridge.m needs runtime integration")

    // Return placeholder
    // iOS app's WWWMapViewBridge.m exists and compiles, but calling it from Kotlin
    // requires complex objc_msgSend usage that's error-prone
    return UIViewController()
}
