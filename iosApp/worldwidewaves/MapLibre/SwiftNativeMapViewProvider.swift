/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import Foundation
import UIKit
import Shared

/// Swift implementation of Kotlin NativeMapViewProvider protocol for iOS MapLibre integration.
///
/// ## Purpose
/// Provides the bridge between Kotlin Compose UI and native iOS MapLibre maps.
/// Registered in Koin DI during platform initialization (SceneDelegate.installPlatform).
///
/// ## Architecture Role
/// This class is the entry point for Kotlin code to create native iOS map views:
/// ```
/// Kotlin Compose (@Composable IOSEventMap)
///   └── NativeMapViewProvider.createMapView() [Kotlin interface]
///       └── SwiftNativeMapViewProvider.createMapView() [Swift implementation]
///           └── MapViewBridge.createMapViewController() [Swift factory]
///               └── UIHostingController<EventMapView> [SwiftUI wrapper]
///                   └── EventMapView [SwiftUI MapLibre map]
///                       └── MapLibre SDK [Native iOS maps]
/// ```
///
/// ## Koin Registration
/// Registered in SceneDelegate.installPlatform() via:
/// ```swift
/// NativeMapViewProviderRegistrationKt.registerNativeMapViewProvider(
///     provider: SwiftNativeMapViewProvider()
/// )
/// ```
///
/// ## Protocol Implementation
/// Implements Kotlin interface `NativeMapViewProvider` with two methods:
/// 1. **createMapView()**: Creates UIViewController containing MapLibre map
/// 2. **getMapWrapper()**: Retrieves MapLibreViewWrapper from registry
///
/// ## Threading Model
/// - **createMapView()**: Can be called from any thread (returns UIViewController)
/// - **getMapWrapper()**: Thread-safe (registry uses concurrent access)
///
/// ## Memory Management
/// - Provider instance is singleton (registered once in Koin)
/// - Map view controllers are created on-demand per event
/// - Wrappers are weakly referenced in MapWrapperRegistry
///
/// - Important: Must be registered in Koin before any map views are created
/// - Important: Registered after Koin initialization in SceneDelegate
/// - Note: Replaces default no-op implementation from shared module
public class SwiftNativeMapViewProvider: NativeMapViewProvider {
    public init() {}

    /// Creates a UIViewController containing a native MapLibre map for the given event.
    ///
    /// ## Purpose
    /// Factory method called from Kotlin Compose UI to create native iOS map views.
    /// Delegates to MapViewBridge to create UIHostingController wrapping SwiftUI EventMapView.
    ///
    /// ## Return Type
    /// Returns `Any` (Kotlin requirement for platform-specific objects):
    /// - **Actual type**: UIViewController containing SwiftUI EventMapView
    /// - **Kotlin usage**: Cast to UIViewController, embed in UIKitView
    ///
    /// ## Style URL
    /// MapLibre style JSON URL defining map appearance:
    /// - **Format**: `https://example.com/style.json` or `asset://local-style.json`
    /// - **Content**: Defines layers, sources, sprites, glyphs (Mapbox GL style spec)
    /// - **Example**: `https://demotiles.maplibre.org/style.json`
    ///
    /// ## Map Initialization
    /// Map view is initialized with default Paris coordinates (48.8566, 2.3522).
    /// Actual positioning is handled after style loads based on event location data.
    ///
    /// ## View Hierarchy Created
    /// ```
    /// UIViewController (returned)
    ///   └── UIHostingController.view
    ///       └── EventMapView (SwiftUI)
    ///           └── MapLibre native map
    /// ```
    ///
    /// ## Threading Model
    /// Can be called from any thread (returns UIViewController for later presentation)
    ///
    /// - Parameters:
    ///   - event: Event model (IWWWEvent) containing event ID and metadata
    ///   - styleURL: MapLibre style JSON URL (defines map appearance)
    /// - Returns: UIViewController containing MapLibre map (type-erased to `Any` for Kotlin)
    /// - Note: Called from Kotlin Compose IOSEventMap composable
    public func createMapView(event: IWWWEvent, styleURL: String, enableGestures: Bool) -> Any {
        WWWLog.i("SwiftNativeMapViewProvider", "Creating MapLibre map view for: \(event.id)")
        WWWLog.d("SwiftNativeMapViewProvider", "Style URL: \(styleURL), enableGestures: \(enableGestures)")

        // Use Swift MapViewBridge instead of ObjC WWWMapViewBridge
        let viewController = MapViewBridge.createMapViewController(
            for: event,
            styleURL: styleURL,
            enableGestures: enableGestures,
            wrapperRef: nil
        )

        WWWLog.i("SwiftNativeMapViewProvider", "MapLibre view controller created successfully")
        return viewController
    }

    /// Retrieves the MapLibreViewWrapper for a given event from the registry.
    ///
    /// ## Purpose
    /// Allows Kotlin code to retrieve the Swift MapLibreViewWrapper instance for direct manipulation.
    /// Used for advanced map operations not exposed through IOSMapBridge.
    ///
    /// ## Return Type
    /// Returns `Any?` (Kotlin requirement for platform-specific objects):
    /// - **Actual type**: MapLibreViewWrapper? (Swift class)
    /// - **nil**: If no wrapper is registered for the eventId
    ///
    /// ## Registry Pattern
    /// MapWrapperRegistry stores weak references keyed by eventId:
    /// - **Registration**: Happens when EventMapView initializes
    /// - **Deregistration**: Automatic when map view is deallocated (weak reference)
    /// - **Thread-safety**: Registry supports concurrent access
    ///
    /// ## Use Cases
    /// - Direct map manipulation from Kotlin (rare, prefer IOSMapBridge)
    /// - Debugging and inspection
    /// - Advanced map operations not yet exposed via bridge
    ///
    /// ## Threading Model
    /// Thread-safe (registry uses concurrent data structures)
    ///
    /// - Parameters:
    ///   - eventId: Unique event identifier (registry key)
    /// - Returns: MapLibreViewWrapper instance if registered, nil otherwise (type-erased to `Any?` for Kotlin)
    /// - Note: Returns nil if map has not been created yet or has been deallocated
    public func getMapWrapper(eventId: String) -> Any? {
        WWWLog.v("SwiftNativeMapViewProvider", "Getting map wrapper for event: \(eventId)")
        return Shared.MapWrapperRegistry.shared.getWrapper(eventId: eventId)
    }
}
