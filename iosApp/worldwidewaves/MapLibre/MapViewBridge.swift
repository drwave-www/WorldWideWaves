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
        // TODO: Get actual center from event.map (WWWEventMap doesn't have center property)
        // Using Paris as default for now
        let mapView = EventMapView(
            eventId: event.id,
            styleURL: styleURL,
            initialLatitude: 48.8566,
            initialLongitude: 2.3522,
            initialZoom: 12.0,
            wrapper: .constant(nil) // Will be bound via EventMapView's own State
        )

        // Wrap in UIHostingController
        let hostingController = UIHostingController(rootView: mapView)
        hostingController.view.backgroundColor = UIColor.clear

        WWWLog.d("MapViewBridge", "Map view controller created successfully")
        return hostingController
    }

    /**
     * Creates a simple map view controller.
     * Non-@objc version without tuple return (not needed for current implementation).
     */
    public static func createMapViewControllerWithWrapper(
        for event: IWWWEvent,
        styleURL: String
    ) -> UIViewController {
        WWWLog.i("MapViewBridge", "Creating map view with wrapper for: \(event.id)")

        var wrapperInstance: MapLibreViewWrapper?

        let mapView = EventMapView(
            eventId: event.id,
            styleURL: styleURL,
            initialLatitude: 48.8566,
            initialLongitude: 2.3522,
            initialZoom: 12.0,
            wrapper: Binding(
                get: { wrapperInstance },
                set: { wrapperInstance = $0 }
            )
        )

        let controller = UIHostingController(rootView: mapView)
        controller.view.backgroundColor = UIColor.clear

        WWWLog.d("MapViewBridge", "Map view controller created")
        return controller
    }
}
