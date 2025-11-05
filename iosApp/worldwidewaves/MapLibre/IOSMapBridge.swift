/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

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
            WWWLog.w("IOSMapBridge", "[WARNING] No wrapper found for event: \(eventId)")
            return false
        }

        let hasPending = Shared.MapWrapperRegistry.shared.hasPendingPolygons(eventId: eventId)

        guard hasPending else {
            WWWLog.v("IOSMapBridge", "No pending polygons for event: \(eventId)")
            return false
        }

        guard let polygonData = Shared.MapWrapperRegistry.shared.getPendingPolygons(eventId: eventId) else {
            WWWLog.w(
                "IOSMapBridge",
                "[WARNING] hasPendingPolygons=true but getPendingPolygons returned nil for event: \(eventId)"
            )
            return false
        }

        WWWLog.i(
            "IOSMapBridge",
            "[WAVE] Rendering \(polygonData.coordinates.count) pending polygons for event: \(eventId)"
        )

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
        WWWLog.i("IOSMapBridge", "[SUCCESS] Rendered \(polygonData.coordinates.count) polygons, cleared from registry")
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

    // MARK: - Attribution

    /// Sets attribution and logo margins.
    ///
    /// ## Purpose
    /// Adjusts the position of MapLibre attribution button and logo view
    /// to avoid overlap with other UI elements (e.g., bottom navigation).
    ///
    /// ## Threading Model
    /// Main thread only (UIKit requirement)
    ///
    /// ## Coordinate System
    /// - left: Distance from left edge of map view
    /// - top: Distance from top edge of map view (currently unused for bottom-aligned elements)
    /// - right: Distance from right edge of map view
    /// - bottom: Distance from bottom edge of map view
    ///
    /// - Parameters:
    ///   - eventId: Unique event identifier (registry key)
    ///   - left: Left margin in points
    ///   - top: Top margin in points
    ///   - right: Right margin in points
    ///   - bottom: Bottom margin in points
    /// - Important: Must be called on main thread
    /// - Note: Called from Kotlin via @objc bridge
    @objc public static func setAttributionMargins(
        eventId: String,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        guard let wrapper = Shared.MapWrapperRegistry.shared.getWrapper(eventId: eventId) as? MapLibreViewWrapper else {
            WWWLog.w("IOSMapBridge", "No wrapper found for event: \(eventId)")
            return
        }

        WWWLog.d("IOSMapBridge", "Setting attribution margins for event: \(eventId)")
        wrapper.setAttributionMargins(left: left, top: top, right: right, bottom: bottom)
    }

    // MARK: - Camera Constraints

    /// Gets the actual minimum zoom level from the map view.
    ///
    /// ## Purpose
    /// Provides real-time minimum zoom level directly from mapView.minimumZoomLevel.
    /// This bypasses the registry cache to prevent race conditions where the cached
    /// value returns 0.0 while the map view has the correct constraint-based min zoom.
    ///
    /// ## Threading Model
    /// Main thread only (UIKit requirement)
    ///
    /// ## Use Cases
    /// - Called from IosMapLibreAdapter.getMinZoomLevel() before constraint enforcement
    /// - Prevents infinite unzoom when constraints are applied before registry updates
    ///
    /// - Parameters:
    ///   - eventId: Unique event identifier (registry key)
    /// - Returns: Actual minimum zoom level from map view, or 0.0 if wrapper not found
    /// - Important: Must be called on main thread
    /// - Note: Called from Kotlin via @objc bridge
    @objc public static func getActualMinZoomLevel(eventId: String) -> Double {
        guard let wrapper = Shared.MapWrapperRegistry.shared.getWrapper(eventId: eventId) as? MapLibreViewWrapper else {
            WWWLog.w("IOSMapBridge", "No wrapper found for event: \(eventId), returning 0.0")
            return 0.0
        }

        let actualMinZoom = wrapper.getMinZoom()
        WWWLog.v("IOSMapBridge", "getActualMinZoomLevel for \(eventId): \(actualMinZoom)")
        return actualMinZoom
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

    /// Enable or disable the user location component.
    ///
    /// ## Purpose
    /// Controls the visibility of the user position marker on the map.
    /// When enabled, displays the native MapLibre location marker.
    ///
    /// - Parameters:
    ///   - eventId: Event identifier to locate the wrapper
    ///   - enabled: Whether to enable the location component
    /// - Important: Must be called on main thread
    /// - Note: Called from Kotlin via @objc bridge
    @objc public static func enableLocationComponent(eventId: String, enabled: Bool) {
        guard let wrapper = Shared.MapWrapperRegistry.shared.getWrapper(eventId: eventId) as? MapLibreViewWrapper else {
            WWWLog.w("IOSMapBridge", "No wrapper found for event: \(eventId)")
            return
        }

        WWWLog.i("IOSMapBridge", "enableLocationComponent: \(enabled) for event: \(eventId)")
        wrapper.enableLocationComponent(enabled)
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

    // MARK: - Map Ready Callbacks

    /// Invokes map ready callbacks after style loads.
    ///
    /// ## Purpose
    /// Solves the timing problem where Kotlin may register onMapSet callbacks before the map is ready.
    /// Kotlin stores callbacks in the registry, and Swift invokes them after style loads.
    ///
    /// ## Use Cases
    /// - Wave polygon rendering that requires style to be loaded
    /// - Camera positioning that depends on map being initialized
    /// - Any operation that needs the map to be fully ready
    ///
    /// ## Threading Model
    /// Main thread only (MapLibre/UIKit requirement)
    ///
    /// ## Error Handling
    /// - No callbacks registered: Logs verbose, returns (normal case)
    /// - Callback throws exception: Logged but doesn't prevent other callbacks from executing
    ///
    /// - Parameters:
    ///   - eventId: Unique event identifier (registry key)
    /// - Important: Must be called on main thread
    /// - Important: Call after didFinishLoading style: to ensure map is ready
    /// - Note: Called from MapLibreViewWrapper after style loads
    @objc public static func invokeMapReadyCallbacks(eventId: String) {
        WWWLog.i("IOSMapBridge", "Invoking map ready callbacks for event: \(eventId)")
        Shared.MapWrapperRegistry.shared.invokeMapReadyCallbacks(eventId: eventId)
    }

    // MARK: - Camera Control

    /// Checks for pending camera commands and executes them if found.
    ///
    /// ## Purpose
    /// Solves the timing problem where Kotlin camera control logic may issue commands
    /// before the Swift map view is ready. Kotlin stores camera commands in the registry,
    /// and Swift periodically checks for and executes them.
    ///
    /// ## Command Execution Strategy
    /// - **Configuration commands** (SetConstraintBounds, SetMinZoom, SetMaxZoom, SetAttributionMargins):
    ///   Execute ALL queued commands in order until queue is empty.
    ///   These are fast synchronous operations that configure map constraints.
    /// - **Animation commands** (AnimateToPosition, AnimateToBounds, MoveToBounds): Execute ONE command per call.
    ///   Animations are asynchronous and should not be batched.
    ///
    /// ## Supported Commands
    /// - **AnimateToPosition**: Animate to specific lat/lng with optional zoom
    /// - **AnimateToBounds**: Animate to fit bounding box with padding
    /// - **MoveToBounds**: Move (non-animated) to bounding box
    /// - **SetConstraintBounds**: Set camera movement constraints
    /// - **SetMinZoom**: Set minimum zoom level
    /// - **SetMaxZoom**: Set maximum zoom level
    /// - **SetAttributionMargins**: Set attribution button and logo margins
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

        // Execute all pending commands until queue is empty or we hit an animation command
        // Configuration commands are batched, animation commands are executed one at a time
        var executedCount = 0
        let maxBatchSize = 10 // Safety limit to prevent infinite loops

        while Shared.MapWrapperRegistry.shared.hasPendingCameraCommand(eventId: eventId)
            && executedCount < maxBatchSize {
            guard let command = Shared.MapWrapperRegistry.shared.getPendingCameraCommand(eventId: eventId) else {
                WWWLog.w("IOSMapBridge", "hasPendingCameraCommand=true but getPendingCameraCommand returned nil")
                break
            }

            // Check if this is a configuration command (can batch) or animation command (execute one)
            let isConfigCommand = command is CameraCommand.SetConstraintBounds ||
                                 command is CameraCommand.SetMinZoom ||
                                 command is CameraCommand.SetMaxZoom ||
                                 command is CameraCommand.SetAttributionMargins

            WWWLog.i(
                "IOSMapBridge",
                "[CAMERA] Executing camera command for event: \(eventId), type: \(type(of: command))"
            )
            let success = executeCommand(command, on: wrapper)

            // Only clear command if execution succeeded
            if success {
                Shared.MapWrapperRegistry.shared.clearPendingCameraCommand(eventId: eventId)
                WWWLog.i(
                    "IOSMapBridge",
                    "[SUCCESS] Camera command executed and cleared for event: \(eventId)"
                )
                executedCount += 1

                // If this was an animation command, stop batching (animations are asynchronous)
                if !isConfigCommand {
                    WWWLog.d("IOSMapBridge", "Animation command executed, stopping batch execution")
                    break
                }
            } else {
                WWWLog.w(
                    "IOSMapBridge",
                    "[WARNING] Camera command execution failed, will retry later for event: \(eventId)"
                )
                break
            }
        }

        if executedCount > 0 {
            WWWLog.d("IOSMapBridge", "Batch execution complete: \(executedCount) command(s) executed")
        }
    }

    /// Creates a callback wrapper for camera animations that invokes registry callbacks.
    /// - Returns: Optional wrapper, nil if callbackId is nil
    private static func createCameraCallbackWrapper(callbackId: String?) -> MapCameraCallbackWrapper? {
        guard let id = callbackId else { return nil }

        return MapCameraCallbackWrapper(
            onFinish: {
                Shared.MapWrapperRegistry.shared.invokeCameraAnimationCallback(
                    callbackId: id,
                    success: true
                )
            },
            onCancel: {
                Shared.MapWrapperRegistry.shared.invokeCameraAnimationCallback(
                    callbackId: id,
                    success: false
                )
            }
        )
    }

    /// Executes a specific camera command on the wrapper.
    /// - Returns: True if execution succeeded, false if it should be retried
    private static func executeCommand(_ command: CameraCommand, on wrapper: MapLibreViewWrapper) -> Bool {
        switch command {
        case let animateToPos as CameraCommand.AnimateToPosition:
            return executeAnimateToPosition(animateToPos, on: wrapper)
        case let animateBounds as CameraCommand.AnimateToBounds:
            return executeAnimateToBounds(animateBounds, on: wrapper)
        case let moveBounds as CameraCommand.MoveToBounds:
            return executeMoveToBounds(moveBounds, on: wrapper)
        case let constraintBounds as CameraCommand.SetConstraintBounds:
            return executeSetConstraintBounds(constraintBounds, on: wrapper)
        case let setMinZoom as CameraCommand.SetMinZoom:
            return executeSetMinZoom(setMinZoom, on: wrapper)
        case let setMaxZoom as CameraCommand.SetMaxZoom:
            return executeSetMaxZoom(setMaxZoom, on: wrapper)
        case let setMargins as CameraCommand.SetAttributionMargins:
            return executeSetAttributionMargins(setMargins, on: wrapper)
        default:
            return true  // Unknown command type, don't retry
        }
    }

    private static func executeAnimateToPosition(
        _ command: CameraCommand.AnimateToPosition,
        on wrapper: MapLibreViewWrapper
    ) -> Bool {
        let zoom = command.zoom?.doubleValue
        let pos = command.position
        WWWLog.i("IOSMapBridge", "Animating to position: \(pos.lat), \(pos.lng), zoom=\(zoom ?? -1)")

        let callbackWrapper = createCameraCallbackWrapper(callbackId: command.callbackId)

        wrapper.animateCamera(
            latitude: command.position.lat,
            longitude: command.position.lng,
            zoom: zoom as NSNumber?,
            callback: callbackWrapper
        )
        return true
    }

    private static func executeAnimateToBounds(
        _ command: CameraCommand.AnimateToBounds,
        on wrapper: MapLibreViewWrapper
    ) -> Bool {
        let bbox = command.bounds
        WWWLog.i("IOSMapBridge", "Animating to bounds with padding: \(command.padding)")
        WWWLog.d(
            "IOSMapBridge",
            """
            Swift sees bbox: minLat=\(bbox.minLatitude), \
            minLng=\(bbox.minLongitude), maxLat=\(bbox.maxLatitude), \
            maxLng=\(bbox.maxLongitude)
            """
        )

        let callbackWrapper = createCameraCallbackWrapper(callbackId: command.callbackId)

        wrapper.animateCameraToBounds(
            swLat: bbox.minLatitude,
            swLng: bbox.minLongitude,
            neLat: bbox.maxLatitude,
            neLng: bbox.maxLongitude,
            padding: Int(command.padding),
            callback: callbackWrapper
        )
        return true
    }

    private static func executeMoveToBounds(
        _ command: CameraCommand.MoveToBounds,
        on wrapper: MapLibreViewWrapper
    ) -> Bool {
        let bbox = command.bounds
        WWWLog.i("IOSMapBridge", "Moving to bounds")
        let center = CLLocationCoordinate2D(
            latitude: (bbox.minLatitude + bbox.maxLatitude) / 2,
            longitude: (bbox.minLongitude + bbox.maxLongitude) / 2
        )
        wrapper.moveCamera(latitude: center.latitude, longitude: center.longitude, zoom: nil)
        return true
    }

    private static func executeSetConstraintBounds(
        _ command: CameraCommand.SetConstraintBounds,
        on wrapper: MapLibreViewWrapper
    ) -> Bool {
        let constraintBbox = command.constraintBounds
        let originalBbox = command.originalEventBounds
        let isWindowMode = command.applyZoomSafetyMargin

        WWWLog.i("IOSMapBridge", "Setting camera constraint bounds (WINDOW mode: \(isWindowMode))")
        WWWLog.d(
            "IOSMapBridge",
            """
            Constraint bounds: SW(\(constraintBbox.minLatitude),\(constraintBbox.minLongitude)) \
            NE(\(constraintBbox.maxLatitude),\(constraintBbox.maxLongitude))
            """
        )
        if let origBbox = originalBbox {
            WWWLog.d(
                "IOSMapBridge",
                """
                Original event bounds: SW(\(origBbox.minLatitude),\(origBbox.minLongitude)) \
                NE(\(origBbox.maxLatitude),\(origBbox.maxLongitude))
                """
            )
        }

        return wrapper.setBoundsForCameraTarget(
            constraintSwLat: constraintBbox.minLatitude,
            constraintSwLng: constraintBbox.minLongitude,
            constraintNeLat: constraintBbox.maxLatitude,
            constraintNeLng: constraintBbox.maxLongitude,
            eventSwLat: originalBbox?.minLatitude ?? constraintBbox.minLatitude,
            eventSwLng: originalBbox?.minLongitude ?? constraintBbox.minLongitude,
            eventNeLat: originalBbox?.maxLatitude ?? constraintBbox.maxLatitude,
            eventNeLng: originalBbox?.maxLongitude ?? constraintBbox.maxLongitude,
            isWindowMode: isWindowMode
        )
    }

    private static func executeSetMinZoom(
        _ command: CameraCommand.SetMinZoom,
        on wrapper: MapLibreViewWrapper
    ) -> Bool {
        WWWLog.i("IOSMapBridge", "Setting min zoom: \(command.minZoom)")
        wrapper.setMinZoom(command.minZoom)
        return true
    }

    private static func executeSetMaxZoom(
        _ command: CameraCommand.SetMaxZoom,
        on wrapper: MapLibreViewWrapper
    ) -> Bool {
        WWWLog.i("IOSMapBridge", "Setting max zoom: \(command.maxZoom)")
        wrapper.setMaxZoom(command.maxZoom)
        return true
    }

    private static func executeSetAttributionMargins(
        _ command: CameraCommand.SetAttributionMargins,
        on wrapper: MapLibreViewWrapper
    ) -> Bool {
        let margins = "(\(command.left),\(command.top),\(command.right),\(command.bottom))"
        WWWLog.i("IOSMapBridge", "Setting attribution margins: \(margins)")
        wrapper.setAttributionMargins(
            left: Int(command.left),
            top: Int(command.top),
            right: Int(command.right),
            bottom: Int(command.bottom)
        )
        return true
    }

    /// Renders pending bbox draw if one exists.
    ///
    /// ## Purpose
    /// Similar to renderPendingPolygons - checks for pending bbox draw requests from Kotlin
    /// and renders them on the map. Called periodically after map style loads.
    ///
    /// ## Timing Problem
    /// Kotlin may request bbox draw before Swift map view is ready, so request is stored
    /// as "pending" and Swift polls for it after initialization.
    ///
    /// ## Threading Model
    /// Main thread only (MapLibre/UIKit requirement)
    ///
    /// - Parameters:
    ///   - eventId: Unique event identifier (registry key)
    /// - Returns: True if bbox was rendered, false if no pending bbox or no wrapper
    /// - Important: Must be called on main thread
    /// - Important: Call after map style loads to catch pending bbox requests
    @objc public static func renderPendingBbox(eventId: String) -> Bool {
        guard Shared.MapWrapperRegistry.shared.hasPendingBboxDraw(eventId: eventId) else {
            return false
        }

        guard let wrapper = Shared.MapWrapperRegistry.shared.getWrapper(eventId: eventId) as? MapLibreViewWrapper else {
            WWWLog.w("IOSMapBridge", "No wrapper found for pending bbox draw: \(eventId)")
            return false
        }

        guard let bbox = Shared.MapWrapperRegistry.shared.getPendingBboxDraw(eventId: eventId) else {
            return false
        }

        WWWLog.i("IOSMapBridge", "Rendering pending bbox for event: \(eventId)")
        wrapper.drawOverrideBbox(
            swLat: bbox.sw.lat,
            swLng: bbox.sw.lng,
            neLat: bbox.ne.lat,
            neLng: bbox.ne.lng
        )
        return true
    }
}
