/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

// Note: File exceeds limits due to comprehensive camera command handling
// swiftlint:disable file_length

import Foundation
import Shared
import CoreLocation

/// Bridge between Kotlin shared module and Swift MapLibre implementation.
///
/// ## Purpose
/// IOSMapBridge solves the architectural challenge of MapLibre being in the iosApp target
/// (Swift) while wave rendering logic is in the shared module (Kotlin). Provides @objc methods
/// that Kotlin code can call to interact with MapLibre maps.
///
/// ## Architecture Pattern
/// ```
/// Kotlin (shared module)
///   └── IOSMapBridge (@objc methods)
///       └── MapWrapperRegistry (Kotlin/Native singleton)
///           └── MapLibreViewWrapper (Swift class)
///               └── MapLibre SDK (native iOS maps)
/// ```
///
/// ## Why This Bridge Is Needed
/// - **MapLibre SDK**: Pure Swift/iOS framework, cannot be accessed from Kotlin/Native shared code
/// - **MapLibreViewWrapper**: Swift class managing MapLibre instance, lives in iosApp target
/// - **Wave Rendering Logic**: Lives in Kotlin shared module, needs to draw polygons on map
/// - **Solution**: Bridge with @objc methods callable from Kotlin
///
/// ## Threading Model
/// - **Main thread only**: All MapLibre operations must occur on main thread (UIKit requirement)
/// - Kotlin callers are responsible for dispatching to main thread before calling bridge methods
///
/// ## Registry Pattern
/// Uses MapWrapperRegistry (Kotlin/Native singleton) to store weak references to MapLibreViewWrapper:
/// - **Key**: eventId (String)
/// - **Value**: MapLibreViewWrapper instance (Swift object)
/// - **Lifecycle**: Registered when map is created, unregistered when map is destroyed
///
/// ## Polygon Rendering Flow
/// 1. Kotlin wave detector calculates wave polygons (lat/lng coordinates)
/// 2. Kotlin stores polygons in MapWrapperRegistry as "pending"
/// 3. Swift map view loads and registers wrapper in registry
/// 4. Swift calls renderPendingPolygons() periodically to check for polygons
/// 5. Bridge retrieves polygons from registry and renders on MapLibre map
/// 6. Bridge clears pending polygons after successful rendering
///
/// ## Memory Management
/// - MapWrapperRegistry holds **weak** references to MapLibreViewWrapper
/// - Wrappers are automatically removed when map views are deallocated
/// - No retain cycles between Kotlin and Swift
///
/// - Important: All methods must be called on main thread
/// - Important: MapLibreViewWrapper must be registered in MapWrapperRegistry before use
/// - Note: Uses WWWLog for consistent logging across Kotlin/Swift boundary
@objc public class IOSMapBridge: NSObject {
    /// Renders wave polygons on the MapLibre map for a specific event.
    ///
    /// ## Purpose
    /// Called from Kotlin shared module to draw wave visualization polygons on the native iOS map.
    /// Retrieves the MapLibreViewWrapper from the registry and delegates to its rendering methods.
    ///
    /// ## Rendering Behavior
    /// - **clearExisting=true**: Removes all previous polygons before adding new ones (full refresh)
    /// - **clearExisting=false**: Adds new polygons to existing ones (incremental update)
    ///
    /// ## Threading Model
    /// Main thread only (MapLibre/UIKit requirement)
    ///
    /// ## Error Handling
    /// If no wrapper is registered for eventId:
    /// - Logs warning
    /// - Returns silently (no crash)
    /// - Polygons remain pending in registry for future retry
    ///
    /// ## Coordinate Format
    /// Polygons are arrays of CLLocationCoordinate2D (latitude/longitude pairs).
    /// Each polygon is a closed ring of coordinates defining the wave boundary.
    ///
    /// - Parameters:
    ///   - eventId: Unique event identifier (registry key)
    ///   - polygons: Array of polygon coordinate arrays (each polygon is an array of lat/lng points)
    ///   - clearExisting: Whether to clear existing polygons before adding new ones
    /// - Important: Must be called on main thread
    /// - Note: Called from Kotlin via @objc bridge
    @objc public static func renderWavePolygons(
        eventId: String,
        polygons: [[CLLocationCoordinate2D]],
        clearExisting: Bool
    ) {
        guard let wrapper = Shared.MapWrapperRegistry.shared.getWrapper(eventId: eventId) as? MapLibreViewWrapper else {
            WWWLog.w("IOSMapBridge", "No wrapper found for event: \(eventId)")
            return
        }

        WWWLog.i("IOSMapBridge", "Rendering \(polygons.count) wave polygons for event: \(eventId)")
        wrapper.addWavePolygons(polygons: polygons, clearExisting: clearExisting)
    }

    /// Checks for pending polygons in the registry and renders them if found.
    ///
    /// ## Purpose
    /// Solves the timing problem where Kotlin wave detection may finish before the Swift map view is ready.
    /// Kotlin stores polygons as "pending" in the registry, and Swift periodically checks for them.
    ///
    /// ## Timing Problem
    /// 1. **Problem**: Wave detector (Kotlin) calculates polygons immediately
    /// 2. **Problem**: Map view (Swift) takes time to load style and initialize
    /// 3. **Solution**: Store polygons as "pending" in registry until map is ready
    /// 4. **Solution**: Swift map view polls for pending polygons after initialization
    ///
    /// ## Rendering Flow
    /// 1. Check if MapLibreViewWrapper is registered (map must exist)
    /// 2. Check if pending polygons exist in registry
    /// 3. Retrieve polygon coordinate data from registry
    /// 4. Convert Kotlin coordinate pairs to Swift CLLocationCoordinate2D
    /// 5. Render polygons on map via wrapper
    /// 6. Clear pending polygons from registry (consumed)
    ///
    /// ## Threading Model
    /// Main thread only (MapLibre/UIKit requirement)
    ///
    /// ## Coordinate Conversion
    /// Converts Kotlin Pair<Double, Double> to Swift CLLocationCoordinate2D:
    /// - Kotlin: `Pair(latitude: Double, longitude: Double)`
    /// - Swift: `CLLocationCoordinate2D(latitude: Double, longitude: Double)`
    ///
    /// ## Error Handling
    /// - No wrapper registered: Logs warning, returns (polygons remain pending for retry)
    /// - No pending polygons: Logs verbose, returns (normal case)
    /// - Invalid coordinates: Filtered out via compactMap (nil coordinates dropped)
    ///
    /// - Parameters:
    ///   - eventId: Unique event identifier (registry key)
    /// - Important: Must be called on main thread
    /// - Important: Call periodically after map style loads to catch pending polygons
    /// - Note: Clears pending polygons after successful rendering (one-time consumption)
    @objc public static func renderPendingPolygons(eventId: String) -> Bool {
        WWWLog.v("IOSMapBridge", "renderPendingPolygons called for: \(eventId)")

        guard let wrapper = Shared.MapWrapperRegistry.shared.getWrapper(eventId: eventId) as? MapLibreViewWrapper else {
            return false
        }

        let hasPending = Shared.MapWrapperRegistry.shared.hasPendingPolygons(eventId: eventId)

        guard hasPending else {
            return false
        }

        guard let polygonData = Shared.MapWrapperRegistry.shared.getPendingPolygons(eventId: eventId) else {
            return false
        }

        WWWLog.i("IOSMapBridge", "🌊 Rendering \(polygonData.coordinates.count) pending polygons for event: \(eventId)")

        // Convert coordinate pairs to CLLocationCoordinate2D arrays
        let coordinateArrays: [[CLLocationCoordinate2D]] = polygonData.coordinates.map { polygon in
            polygon.compactMap { coordPair -> CLLocationCoordinate2D? in
                guard let lat = coordPair.first?.doubleValue, let lng = coordPair.second?.doubleValue else {
                    return nil
                }
                return CLLocationCoordinate2D(latitude: lat, longitude: lng)
            }
        }

        // Render on map
        wrapper.addWavePolygons(polygons: coordinateArrays, clearExisting: polygonData.clearExisting)

        // Clear after rendering - next update will overwrite with latest state
        // This is OK: we only need to show the LATEST wave state, intermediate updates can be skipped
        Shared.MapWrapperRegistry.shared.clearPendingPolygons(eventId: eventId)
        WWWLog.i("IOSMapBridge", "✅ Rendered \(polygonData.coordinates.count) polygons, cleared from registry")
        return true
    }

    /// Clears all wave polygons from the map.
    ///
    /// ## Purpose
    /// Removes all rendered wave visualization polygons from the MapLibre map.
    /// Called when wave state changes or screen transitions require clearing visualizations.
    ///
    /// ## Use Cases
    /// - Wave animation ends (clear old wave polygons)
    /// - User navigates away from wave screen
    /// - New wave detection cycle starts (clear before rendering new polygons)
    /// - Error recovery (clear corrupt or invalid polygon state)
    ///
    /// ## Threading Model
    /// Main thread only (MapLibre/UIKit requirement)
    ///
    /// ## Error Handling
    /// If no wrapper is registered:
    /// - Logs warning
    /// - Returns silently (no crash)
    /// - No-op if map doesn't exist
    ///
    /// - Parameters:
    ///   - eventId: Unique event identifier (registry key)
    /// - Important: Must be called on main thread
    /// - Note: Called from Kotlin via @objc bridge
    @objc public static func clearWavePolygons(eventId: String) {
        guard let wrapper = Shared.MapWrapperRegistry.shared.getWrapper(eventId: eventId) as? MapLibreViewWrapper else {
            WWWLog.w("IOSMapBridge", "No wrapper found for event: \(eventId)")
            return
        }

        WWWLog.d("IOSMapBridge", "Clearing wave polygons for event: \(eventId)")
        wrapper.clearWavePolygons()
    }

    // MARK: - Accessibility Support

    /// Updates user position for VoiceOver accessibility.
    ///
    /// ## Purpose
    /// Enables VoiceOver users to know their current position relative to the event.
    /// Creates accessible elements showing user location and distance from event center.
    ///
    /// ## Threading Model
    /// Main thread only (UIKit requirement)
    ///
    /// ## Use Cases
    /// - User location updates (GPS changes)
    /// - Simulation position updates (testing)
    /// - Initial map load with known user position
    ///
    /// - Parameters:
    ///   - eventId: Unique event identifier (registry key)
    ///   - latitude: User latitude coordinate
    ///   - longitude: User longitude coordinate
    /// - Important: Must be called on main thread
    /// - Note: Called from Kotlin via @objc bridge
    @objc public static func setUserPosition(eventId: String, latitude: Double, longitude: Double) {
        guard let wrapper = Shared.MapWrapperRegistry.shared.getWrapper(eventId: eventId) as? MapLibreViewWrapper else {
            WWWLog.w("IOSMapBridge", "No wrapper found for event: \(eventId)")
            return
        }

        wrapper.setUserPosition(latitude: latitude, longitude: longitude)
    }

    /// Updates event metadata for VoiceOver accessibility.
    ///
    /// ## Purpose
    /// Enables VoiceOver users to understand event boundaries and area information.
    /// Creates accessible elements describing event area, radius, and location.
    ///
    /// ## Threading Model
    /// Main thread only (UIKit requirement)
    ///
    /// ## Use Cases
    /// - Event data loads (initial screen setup)
    /// - Event data updates (real-time changes)
    ///
    /// - Parameters:
    ///   - eventId: Unique event identifier (registry key)
    ///   - centerLatitude: Event center latitude coordinate
    ///   - centerLongitude: Event center longitude coordinate
    ///   - radius: Event area radius in meters
    ///   - eventName: Optional human-readable event name
    /// - Important: Must be called on main thread
    /// - Note: Called from Kotlin via @objc bridge
    @objc public static func setEventInfo(
        eventId: String,
        centerLatitude: Double,
        centerLongitude: Double,
        radius: Double,
        eventName: String?
    ) {
        guard let wrapper = Shared.MapWrapperRegistry.shared.getWrapper(eventId: eventId) as? MapLibreViewWrapper else {
            WWWLog.w("IOSMapBridge", "No wrapper found for event: \(eventId)")
            return
        }

        wrapper.setEventInfo(
            centerLatitude: centerLatitude,
            centerLongitude: centerLongitude,
            radius: radius,
            eventName: eventName
        )
    }

    // MARK: - Camera Control

    /// Checks for pending camera commands and executes them if found.
    ///
    /// ## Purpose
    /// Solves the timing problem where Kotlin camera control logic may issue commands
    /// before the Swift map view is ready. Kotlin stores camera commands in the registry,
    /// and Swift periodically checks for and executes them.
    ///
    /// ## Supported Commands
    /// - **AnimateToPosition**: Animate to specific lat/lng with optional zoom
    /// - **AnimateToBounds**: Animate to fit bounding box with padding
    /// - **MoveToBounds**: Move (non-animated) to bounding box
    /// - **SetConstraintBounds**: Set camera movement constraints
    ///
    /// ## Threading Model
    /// Main thread only (MapLibre/UIKit requirement)
    ///
    /// ## Error Handling
    /// If no wrapper or command found:
    /// - Returns silently
    /// - Commands remain pending for future retry
    ///
    /// - Parameters:
    ///   - eventId: Unique event identifier (registry key)
    /// - Important: Must be called on main thread
    /// - Important: Call periodically after map initialization (e.g., from EventMapView.updateUIView)
    @objc public static func executePendingCameraCommand(eventId: String) {
        // Check wrapper exists
        guard let wrapper = Shared.MapWrapperRegistry.shared.getWrapper(eventId: eventId) as? MapLibreViewWrapper else {
            // Silent return - wrapper not registered yet (normal during initialization)
            return
        }

        // Check if command exists
        guard Shared.MapWrapperRegistry.shared.hasPendingCameraCommand(eventId: eventId) else {
            // Silent return - no command pending (normal case)
            return
        }

        guard let command = Shared.MapWrapperRegistry.shared.getPendingCameraCommand(eventId: eventId) else {
            WWWLog.w("IOSMapBridge", "hasPendingCameraCommand=true but getPendingCameraCommand returned nil")
            return
        }

        WWWLog.i("IOSMapBridge", "📸 Executing camera command for event: \(eventId), type: \(type(of: command))")
        let success = executeCommand(command, on: wrapper)

        // Only clear command if execution succeeded
        if success {
            Shared.MapWrapperRegistry.shared.clearPendingCameraCommand(eventId: eventId)
            WWWLog.i("IOSMapBridge", "✅ Camera command executed and cleared for event: \(eventId)")
        } else {
            WWWLog.w("IOSMapBridge", "⚠️ Camera command execution failed, will retry later for event: \(eventId)")
        }
    }

    /// Executes a specific camera command on the wrapper.
    /// - Returns: True if execution succeeded, false if it should be retried
    // swiftlint:disable:next function_body_length
    private static func executeCommand(_ command: CameraCommand, on wrapper: MapLibreViewWrapper) -> Bool {
        if let animateToPos = command as? CameraCommand.AnimateToPosition {
            let zoom = animateToPos.zoom?.doubleValue
            let pos = animateToPos.position
            let callbackId = animateToPos.callbackId
            WWWLog.i("IOSMapBridge", "Animating to position: \(pos.lat), \(pos.lng), zoom=\(zoom ?? -1)")

            // Create callback wrapper if callbackId provided
            let callbackWrapper: MapCameraCallbackWrapper? = callbackId != nil ?
                MapCameraCallbackWrapper(
                    onFinish: {
                        if let id = callbackId {
                            Shared.MapWrapperRegistry.shared.invokeCameraAnimationCallback(callbackId: id, success: true)
                        }
                    },
                    onCancel: {
                        if let id = callbackId {
                            Shared.MapWrapperRegistry.shared.invokeCameraAnimationCallback(callbackId: id, success: false)
                        }
                    }
                ) : nil

            wrapper.animateCamera(
                latitude: animateToPos.position.lat,
                longitude: animateToPos.position.lng,
                zoom: zoom as NSNumber?,
                callback: callbackWrapper
            )
            return true
        } else if let animateBounds = command as? CameraCommand.AnimateToBounds {
            let bbox = animateBounds.bounds
            let callbackId = animateBounds.callbackId
            WWWLog.i("IOSMapBridge", "Animating to bounds with padding: \(animateBounds.padding)")
            WWWLog.d(
                "IOSMapBridge",
                """
                Swift sees bbox: minLat=\(bbox.minLatitude), \
                minLng=\(bbox.minLongitude), maxLat=\(bbox.maxLatitude), \
                maxLng=\(bbox.maxLongitude)
                """
            )

            // Create callback wrapper if callbackId provided
            let callbackWrapper: MapCameraCallbackWrapper? = callbackId != nil ?
                MapCameraCallbackWrapper(
                    onFinish: {
                        if let id = callbackId {
                            Shared.MapWrapperRegistry.shared.invokeCameraAnimationCallback(callbackId: id, success: true)
                        }
                    },
                    onCancel: {
                        if let id = callbackId {
                            Shared.MapWrapperRegistry.shared.invokeCameraAnimationCallback(callbackId: id, success: false)
                        }
                    }
                ) : nil

            wrapper.animateCameraToBounds(
                swLat: bbox.minLatitude,
                swLng: bbox.minLongitude,
                neLat: bbox.maxLatitude,
                neLng: bbox.maxLongitude,
                padding: Int(animateBounds.padding),
                callback: callbackWrapper
            )
            return true
        } else if let moveBounds = command as? CameraCommand.MoveToBounds {
            let bbox = moveBounds.bounds
            WWWLog.i("IOSMapBridge", "Moving to bounds")
            let center = CLLocationCoordinate2D(
                latitude: (bbox.minLatitude + bbox.maxLatitude) / 2,
                longitude: (bbox.minLongitude + bbox.maxLongitude) / 2
            )
            wrapper.moveCamera(latitude: center.latitude, longitude: center.longitude, zoom: nil)
            return true  // Move commands always succeed
        } else if let constraintBounds = command as? CameraCommand.SetConstraintBounds {
            let bbox = constraintBounds.bounds
            WWWLog.i("IOSMapBridge", "Setting camera constraint bounds")
            WWWLog.d(
                "IOSMapBridge",
                """
                Swift sees bbox: minLat=\(bbox.minLatitude), \
                minLng=\(bbox.minLongitude), maxLat=\(bbox.maxLatitude), \
                maxLng=\(bbox.maxLongitude)
                """
            )
            let success = wrapper.setBoundsForCameraTarget(
                swLat: bbox.minLatitude,
                swLng: bbox.minLongitude,
                neLat: bbox.maxLatitude,
                neLng: bbox.maxLongitude
            )
            return success  // Return actual success/failure from setBoundsForCameraTarget
        } else if let setMinZoom = command as? CameraCommand.SetMinZoom {
            WWWLog.i("IOSMapBridge", "Setting min zoom: \(setMinZoom.minZoom)")
            wrapper.setMinZoom(setMinZoom.minZoom)
            return true
        } else if let setMaxZoom = command as? CameraCommand.SetMaxZoom {
            WWWLog.i("IOSMapBridge", "Setting max zoom: \(setMaxZoom.maxZoom)")
            wrapper.setMaxZoom(setMaxZoom.maxZoom)
            return true
        }
        return true  // Unknown command type, don't retry
    }
}
