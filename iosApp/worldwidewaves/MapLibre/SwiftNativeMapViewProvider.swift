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
 * Uses MapViewBridge.swift instead of ObjC WWWMapViewBridge.
 *
 * Register this in Koin to override the default placeholder implementation.
 */
public class SwiftNativeMapViewProvider: NativeMapViewProvider {
    public init() {}

    public func createMapView(event: IWWWEvent, styleURL: String) -> Any {
        WWWLog.i("SwiftNativeMapViewProvider", "Creating MapLibre map view for: \(event.id)")
        WWWLog.d("SwiftNativeMapViewProvider", "Style URL: \(styleURL)")

        // Use Swift MapViewBridge instead of ObjC WWWMapViewBridge
        let viewController = MapViewBridge.createMapViewController(
            for: event,
            styleURL: styleURL,
            wrapperRef: nil
        )

        WWWLog.i("SwiftNativeMapViewProvider", "MapLibre view controller created successfully")
        return viewController
    }

    public func getMapWrapper(eventId: String) -> Any? {
        WWWLog.v("SwiftNativeMapViewProvider", "Getting map wrapper for event: \(eventId)")
        return MapWrapperRegistry.shared.getWrapper(eventId: eventId)
    }
}
