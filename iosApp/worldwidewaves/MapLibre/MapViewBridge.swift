/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import Foundation
import SwiftUI
import Shared

/// Factory for creating UIViewControllers that wrap SwiftUI MapLibre maps for Kotlin Compose integration.
///
/// ## Purpose
/// MapViewBridge is the glue layer between Kotlin Compose UI and SwiftUI EventMapView.
/// Provides factory methods that create UIHostingControllers wrapping MapLibre maps,
/// enabling Kotlin code to embed native iOS maps via UIKitView.
///
/// ## Architecture Pattern
/// ```
/// Kotlin Compose (@Composable IOSEventMap)
///   └── UIKitView { ... } [Kotlin Compose UIKit integration]
///       └── UIHostingController [UIKit wrapper for SwiftUI]
///           └── EventMapView [SwiftUI MapLibre map component]
///               └── MapLibre SDK [Native iOS map rendering]
/// ```
///
/// ## Why This Bridge Is Needed
/// - **Kotlin Compose**: Cannot directly embed SwiftUI views
/// - **UIKitView**: Kotlin Compose can embed UIViewControllers
/// - **UIHostingController**: UIKit wrapper for SwiftUI views
/// - **Solution**: Bridge creates UIHostingController<EventMapView> for Compose
///
/// ## Factory Methods
/// Two methods for creating map view controllers:
/// 1. **createMapViewController(for:styleURL:wrapperRef:)** - @objc version for Kotlin
/// 2. **createMapViewControllerWithWrapper(for:styleURL:)** - Swift-only version with wrapper binding
///
/// ## Threading Model
/// Can be called from any thread (returns UIViewController for later presentation)
///
/// ## Initial Coordinates
/// Maps are initialized with default Paris coordinates (48.8566°N, 2.3522°E, zoom 12).
/// Actual event positioning happens after map style loads via event location data.
///
/// ## Memory Management
/// - UIHostingController retains EventMapView (SwiftUI view)
/// - EventMapView registers MapLibreViewWrapper in MapWrapperRegistry (weak reference)
/// - Wrapper is automatically removed when view is deallocated
///
/// - Important: All map operations (polygon rendering, etc.) must use main thread
/// - Note: Uses @objc to be callable from Kotlin/Native
@objc public class MapViewBridge: NSObject {
    /// Creates a UIViewController wrapping EventMapView for embedding in Kotlin Compose UI.
    ///
    /// ## Purpose
    /// Primary factory method for creating native iOS maps from Kotlin Compose.
    /// Creates UIHostingController wrapping SwiftUI EventMapView, enabling Compose to embed
    /// the map via UIKitView.
    ///
    /// ## Usage from Kotlin
    /// ```kotlin
    /// @Composable
    /// fun IOSEventMap(event: IWWWEvent, styleURL: String) {
    ///     val viewController = remember {
    ///         MapViewBridge.createMapViewController(
    ///             for = event,
    ///             styleURL = styleURL,
    ///             wrapperRef = null
    ///         )
    ///     }
    ///     UIKitView(factory = { viewController.view })
    /// }
    /// ```
    ///
    /// ## Wrapper Reference Parameter
    /// `wrapperRef` parameter is currently unused (passed as nil):
    /// - **Original intent**: Return MapLibreViewWrapper to Kotlin
    /// - **Current approach**: Use MapWrapperRegistry instead (cleaner)
    /// - **Future**: May remove this parameter
    ///
    /// ## Initial Position
    /// Map initializes at Paris coordinates (48.8566°N, 2.3522°E, zoom 12).
    /// EventMapView re-centers to event location after style loads.
    ///
    /// ## Wrapper Binding
    /// Uses `.constant(nil)` binding - EventMapView manages its own wrapper state.
    /// Wrapper is registered in MapWrapperRegistry when map initializes.
    ///
    /// ## Threading Model
    /// Can be called from any thread (returns UIViewController for later presentation)
    ///
    /// - Parameters:
    ///   - event: Event model (IWWWEvent) containing event ID and metadata
    ///   - styleURL: MapLibre style JSON URL (defines map appearance)
    ///   - wrapperRef: Unused wrapper reference pointer (pass nil, kept for API compatibility)
    /// - Returns: UIHostingController<EventMapView> wrapping SwiftUI map (as UIViewController)
    /// - Note: Called from Kotlin via @objc bridge (SwiftNativeMapViewProvider)
    /// - Note: Clear background to let Compose handle background rendering
    @objc public static func createMapViewController(
        for event: IWWWEvent,
        styleURL: String,
        enableGestures: Bool,
        wrapperRef: UnsafeMutablePointer<MapLibreViewWrapper?>?
    ) -> UIViewController {
        WWWLog.i(
            "MapViewBridge",
            "Creating map view controller for event: \(event.id), enableGestures: \(enableGestures)"
        )

        // Create the SwiftUI map view
        // NOTE: Using default Paris coordinates as initial position
        // The map will be re-centered based on event data after style loads
        let mapView = EventMapView(
            eventId: event.id,
            styleURL: styleURL,
            initialLatitude: 48.8566,
            initialLongitude: 2.3522,
            initialZoom: 12.0,
            enableGestures: enableGestures,
            wrapper: .constant(nil) // Will be bound via EventMapView's own State
        )

        // Wrap in UIHostingController
        let hostingController = UIHostingController(rootView: mapView)
        hostingController.view.backgroundColor = UIColor.clear

        WWWLog.d("MapViewBridge", "Map view controller created successfully")
        return hostingController
    }

    /// Creates a map view controller with mutable wrapper binding (Swift-only version).
    ///
    /// ## Purpose
    /// Alternative factory method for Swift callers that need direct access to MapLibreViewWrapper.
    /// Not used by Kotlin (cannot be @objc due to complex return type).
    ///
    /// ## Difference from createMapViewController
    /// - **createMapViewController**: @objc, uses constant nil binding, wrapper accessed via registry
    /// - **createMapViewControllerWithWrapper**: Swift-only, uses mutable binding, captures wrapper
    ///
    /// ## Wrapper Binding
    /// Uses mutable `Binding` that captures wrapper in closure:
    /// - `wrapperInstance` is local variable capturing wrapper reference
    /// - EventMapView can set wrapper via binding
    /// - Swift caller can access `wrapperInstance` after map initializes
    ///
    /// ## Current Usage
    /// **Not currently used** - all callers use createMapViewController + registry approach.
    /// Kept for potential future Swift-only use cases.
    ///
    /// ## Initial Position
    /// Map initializes at Paris coordinates (48.8566°N, 2.3522°E, zoom 12).
    /// EventMapView re-centers to event location after style loads.
    ///
    /// ## Threading Model
    /// Can be called from any thread (returns UIViewController for later presentation)
    ///
    /// ## When to Use This Method
    /// Use this instead of createMapViewController when:
    /// - Caller is pure Swift (not Kotlin)
    /// - Need immediate synchronous access to wrapper
    /// - Don't want to use MapWrapperRegistry
    ///
    /// - Parameters:
    ///   - event: Event model (IWWWEvent) containing event ID and metadata
    ///   - styleURL: MapLibre style JSON URL (defines map appearance)
    /// - Returns: UIHostingController<EventMapView> wrapping SwiftUI map
    /// - Note: Not @objc (cannot be called from Kotlin due to complex return type)
    /// - Note: Clear background to let parent view control background
    public static func createMapViewControllerWithWrapper(
        for event: IWWWEvent,
        styleURL: String
    ) -> UIViewController {
        WWWLog.i("MapViewBridge", "Creating map view with wrapper for: \(event.id)")

        var wrapperInstance: MapLibreViewWrapper?

        // NOTE: Using default Paris coordinates as initial position
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
