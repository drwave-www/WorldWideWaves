/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import Foundation
import UIKit
import Shared

/**
 * Swift implementation of NativeMapViewProvider.
 * This calls WWWMapViewBridge to create actual MapLibre map views.
 *
 * Register this in Koin to override the default placeholder implementation.
 */
class SwiftNativeMapViewProvider: NativeMapViewProvider {
    func createMapView(event: IWWWEvent, styleURL: String) -> Any {
        WWWLog.i("SwiftNativeMapViewProvider", "Creating MapLibre map view for: \(event.id)")
        WWWLog.d("SwiftNativeMapViewProvider", "Style URL: \(styleURL)")

        // Call ObjC bridge to create actual MapLibre map
        let viewController = WWWMapViewBridge.createMapViewController(
            withStyleURL: styleURL,
            latitude: 48.8566,  // TODO: Get from event
            longitude: 2.3522,
            zoom: 12.0
        )

        WWWLog.i("SwiftNativeMapViewProvider", "MapLibre view controller created successfully")
        return viewController
    }
}
