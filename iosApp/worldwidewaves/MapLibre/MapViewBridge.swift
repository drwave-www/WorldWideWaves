/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import Foundation
import SwiftUI
import Shared

/**
 * Bridge to create UIViewController containing SwiftUI EventMapView.
 * This allows Kotlin Compose IOSEventMap to embed the native map via UIKitView.
 *
 * Architecture:
 * Kotlin Compose (IOSEventMap) → UIKitView → UIHostingController → EventMapView (SwiftUI) → MapLibre
 */
@objc public class MapViewBridge: NSObject {
    /**
     * Creates a UIViewController wrapping the EventMapView for the given event.
     * This controller can be embedded in Compose via UIKitView.
     */
    @objc public static func createMapViewController(
        for event: IWWWEvent,
        styleURL: String,
        wrapperRef: UnsafeMutablePointer<MapLibreViewWrapper?>?
    ) -> UIViewController {
        WWWLog.i("MapViewBridge", "Creating map view controller for event: \(event.id)")

        // Create the SwiftUI map view
        let mapView = EventMapView(
            styleURL: styleURL,
            initialLatitude: event.map.center.lat,
            initialLongitude: event.map.center.lng,
            initialZoom: 12.0,
            wrapper: .constant(nil) // Will be bound via EventMapView's own State
        )

        // Wrap in UIHostingController
        let hostingController = UIHostingController(rootView: mapView)
        hostingController.view.backgroundColor = .clear

        WWWLog.d("MapViewBridge", "Map view controller created successfully")
        return hostingController
    }

    /**
     * Creates a simple map view controller with direct wrapper access.
     * Allows Kotlin to get the MapLibreViewWrapper for direct control.
     */
    @objc public static func createMapViewControllerWithWrapper(
        for event: IWWWEvent,
        styleURL: String
    ) -> (controller: UIViewController, wrapper: MapLibreViewWrapper) {
        WWWLog.i("MapViewBridge", "Creating map view with wrapper for: \(event.id)")

        var wrapperInstance: MapLibreViewWrapper?

        let mapView = EventMapView(
            styleURL: styleURL,
            initialLatitude: event.map.center.lat,
            initialLongitude: event.map.center.lng,
            initialZoom: 12.0,
            wrapper: Binding(
                get: { wrapperInstance },
                set: { wrapperInstance = $0 }
            )
        )

        let controller = UIHostingController(rootView: mapView)
        controller.view.backgroundColor = .clear

        // Wait briefly for wrapper to be created
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            if let wrapper = wrapperInstance {
                WWWLog.d("MapViewBridge", "Wrapper available: \(wrapper)")
            } else {
                WWWLog.w("MapViewBridge", "Wrapper not yet created")
            }
        }

        return (controller, wrapperInstance ?? MapLibreViewWrapper())
    }
}
