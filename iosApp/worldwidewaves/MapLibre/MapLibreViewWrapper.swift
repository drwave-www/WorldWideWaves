/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Foundation
import MapLibre
import UIKit
import CoreLocation
import MapKit
import Shared

// Note: File exceeds 1000 lines due to comprehensive MapLibre feature set
// Includes: camera control, wave polygons, accessibility, callbacks, location component
// swiftlint:disable type_body_length

/// Swift bridging layer for MapLibre Native iOS SDK.
/// Provides @objc methods for controlling MapLibre from Kotlin or Swift.
@objc public class MapLibreViewWrapper: NSObject {
    private static let tag = "MapLibreWrapper"
    private weak var mapView: MLNMapView?
    private var onStyleLoaded: (() -> Void)?
    private var onMapClick: ((Double, Double) -> Void)?
    private var onMapClickNavigation: (() -> Void)?  // Navigation callback (for full-screen map)
    private var onCameraIdle: (() -> Void)?
    private var cameraAnimationCallback: MapCameraCallbackWrapper?

    // Track layer and source IDs for cleanup
    private var waveLayerIds: [String] = []
    private var waveSourceIds: [String] = []

    // Queue for polygons that arrive before style loads
    // Wave progression is cumulative - only most recent set needed
    private var pendingPolygons: [[CLLocationCoordinate2D]]?
    private var styleIsLoaded: Bool = false

    // Queue for constraint bounds that arrive before style loads
    private var pendingConstraintBounds: MLNCoordinateBounds?
    private var pendingEventBounds: MLNCoordinateBounds?
    private var pendingIsWindowMode: Bool = false

    // Constraint bounds for gesture clamping
    private var currentConstraintBounds: MLNCoordinateBounds?
    private var currentEventBounds: MLNCoordinateBounds?

    // Min zoom locking (matches Android minZoomLocked mechanism)
    private var minZoomLocked: Bool = false
    private var lockedMinZoom: Double = 0.0
    private var minZoomUsedInvalidDimensions: Bool = false

    // MARK: - Accessibility State
    private var currentUserPosition: CLLocationCoordinate2D?
    private var currentEventCenter: CLLocationCoordinate2D?
    private var currentEventRadius: Double = 0
    private var currentEventName: String?
    private var currentWavePolygons: [[CLLocationCoordinate2D]] = []

    // MARK: - Location Component

    /// User location annotation (matches Android styling)
    private var userLocationAnnotation: MLNPointAnnotation?
    private var isLocationComponentEnabled: Bool = false
    private var userLocationAnnotationView: MKAnnotationView?
    private var pulseLayer: CAShapeLayer?

    @objc public override init() {
        super.init()
        WWWLog.d(Self.tag, "Initializing MapLibreViewWrapper")
    }

    deinit {
        WWWLog.w(Self.tag, "[WARNING] Deinitializing MapLibreViewWrapper for event: \(eventId ?? "unknown")")
    }

    // MARK: - Map Setup

    private var eventId: String?

    @objc public func setMapView(_ mapView: MLNMapView) {
        WWWLog.i(Self.tag, "setMapView called for event: \(eventId ?? "nil"), bounds: \(mapView.bounds)")
        self.mapView = mapView
        self.mapView?.delegate = self

        // Ensure user interaction is enabled
        mapView.isUserInteractionEnabled = true

        // Configure accessibility for VoiceOver
        configureMapAccessibility()

        // Add tap gesture recognizer for map clicks
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleMapTap(_:)))
        tapGesture.numberOfTapsRequired = 1
        tapGesture.numberOfTouchesRequired = 1
        tapGesture.delegate = self  // Allow simultaneous recognition with MapLibre gestures
        mapView.addGestureRecognizer(tapGesture)

        WWWLog.d(Self.tag, "Map view configured successfully for event: \(eventId ?? "nil")")
    }

    // Justified: Initial setup with multiple callbacks registration (hard to split without breaking flow)
    @objc public func setEventId(_ eventId: String) {
        self.eventId = eventId

        // Register render callback - invoked immediately when polygons update
        Shared.MapWrapperRegistry.shared.setRenderCallback(eventId: eventId) { [weak self] in
            guard self != nil else { return }
            _ = IOSMapBridge.renderPendingPolygons(eventId: eventId)
        }

        // Register camera callback - invoked immediately when camera commands arrive
        Shared.MapWrapperRegistry.shared.setCameraCallback(eventId: eventId) { [weak self] in
            guard self != nil else { return }
            IOSMapBridge.executePendingCameraCommand(eventId: eventId)
        }

        // Register map click callback - stores callback directly on wrapper (no registry lookup)
        Shared.MapWrapperRegistry.shared.setMapClickRegistrationCallback(
            eventId: eventId
        ) { [weak self] clickCallback in
            guard let self = self else { return }
            self.setOnMapClickNavigationListener {
                _ = clickCallback()
            }
        }

        // Register min zoom callback - provides real-time min zoom to Kotlin
        Shared.MapWrapperRegistry.shared.setGetActualMinZoomCallback(eventId: eventId) { [weak self] in
            guard let self = self, let mapView = self.mapView else { return }
            let actualMinZoom = mapView.minimumZoomLevel
            Shared.MapWrapperRegistry.shared.updateActualMinZoom(eventId: eventId, actualMinZoom: actualMinZoom)
        }

        // Register location component callback - controls user position marker
        Shared.MapWrapperRegistry.shared.setLocationComponentCallback(eventId: eventId) { [weak self] enabled in
            guard let self = self else { return }
            let swiftEnabled = enabled.boolValue
            self.enableLocationComponent(swiftEnabled)
        }

        // Register position update callback - receives updates from PositionManager
        Shared.MapWrapperRegistry.shared.setUserPositionCallback(eventId: eventId) { [weak self] latitude, longitude in
            guard let self = self else { return }
            let swiftLat = latitude.doubleValue
            let swiftLng = longitude.doubleValue
            self.setUserPosition(latitude: swiftLat, longitude: swiftLng)
        }

        // Register gestures callback - controls map interaction (pan/zoom/rotate/pitch)
        Shared.MapWrapperRegistry.shared.setGesturesEnabledCallback(eventId: eventId) { [weak self] enabled in
            guard let self = self, let mapView = self.mapView else { return }
            let swiftEnabled = enabled.boolValue

            // Control MapLibre gestures only - preserve isUserInteractionEnabled for tap gestures
            mapView.isScrollEnabled = swiftEnabled
            mapView.isZoomEnabled = swiftEnabled
            mapView.isRotateEnabled = swiftEnabled
            mapView.isPitchEnabled = swiftEnabled
        }
    }

    @objc public func setStyle(styleURL: String, completion: @escaping () -> Void) {
        WWWLog.d(Self.tag, "setStyle called with URL: \(styleURL)")
        guard let mapView = mapView else {
            WWWLog.e(Self.tag, "Cannot set style - mapView is nil")
            return
        }

        self.onStyleLoaded = completion

        if let url = URL(string: styleURL) {
            WWWLog.d(Self.tag, "Setting style URL on map view")
            mapView.styleURL = url
        } else {
            WWWLog.e(Self.tag, "Invalid style URL: \(styleURL)")
        }
    }

    // MARK: - Dimensions

    @objc public func getWidth() -> Double {
        return Double(mapView?.bounds.size.width ?? 0)
    }

    @objc public func getHeight() -> Double {
        return Double(mapView?.bounds.size.height ?? 0)
    }

    // MARK: - Camera Position

    @objc public func getCameraCenterLatitude() -> Double {
        return mapView?.centerCoordinate.latitude ?? 0.0
    }

    @objc public func getCameraCenterLongitude() -> Double {
        return mapView?.centerCoordinate.longitude ?? 0.0
    }

    @objc public func getCameraZoom() -> Double {
        return mapView?.zoomLevel ?? 12.0
    }

    @objc public func getVisibleBounds() -> [Double] {
        guard let bounds = mapView?.visibleCoordinateBounds else {
            return [0, 0, 0, 0]
        }
        return [bounds.sw.latitude, bounds.sw.longitude, bounds.ne.latitude, bounds.ne.longitude]
    }

    // MARK: - Camera Movement

    @objc public func moveCamera(latitude: Double, longitude: Double, zoom: NSNumber?) {
        guard let mapView = mapView else { return }

        let coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        mapView.setCenter(coordinate, animated: false)

        if let zoom = zoom {
            mapView.zoomLevel = zoom.doubleValue
        }
    }

    @objc public func animateCamera(
        latitude: Double,
        longitude: Double,
        zoom: NSNumber?,
        callback: MapCameraCallbackWrapper?
    ) {
        guard let mapView = mapView else {
            callback?.onCancel()
            return
        }

        self.cameraAnimationCallback = callback

        let coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        let targetZoom = zoom?.doubleValue ?? mapView.zoomLevel

        UIView.animate(
            withDuration: 0.5,
            delay: 0,
            options: [.curveEaseInOut],
            animations: {
                mapView.setCenter(coordinate, zoomLevel: targetZoom, animated: false)
            },
            completion: { finished in
                if finished {
                    callback?.onFinish()
                } else {
                    callback?.onCancel()
                }
                self.cameraAnimationCallback = nil
            }
        )
    }

    // swiftlint:disable:next function_parameter_count
    @objc public func animateCameraToBounds(
        swLat: Double,
        swLng: Double,
        neLat: Double,
        neLng: Double,
        padding: Int,
        callback: MapCameraCallbackWrapper?
    ) {
        guard let mapView = mapView else {
            WWWLog.w(Self.tag, "Cannot animate to bounds - mapView is nil")
            callback?.onCancel()
            return
        }

        // Validate bounds before attempting camera calculation
        guard neLat > swLat else {
            WWWLog.e(Self.tag, "Invalid bounds for animation: neLat (\(neLat)) must be > swLat (\(swLat))")
            callback?.onCancel()
            return
        }

        WWWLog.i(Self.tag, "Animating camera to bounds: SW(\(swLat),\(swLng)) NE(\(neLat),\(neLng)) padding=\(padding)")

        self.cameraAnimationCallback = callback

        let southwest = CLLocationCoordinate2D(latitude: swLat, longitude: swLng)
        let northeast = CLLocationCoordinate2D(latitude: neLat, longitude: neLng)
        let bounds = MLNCoordinateBounds(sw: southwest, ne: northeast)

        let edgePadding = UIEdgeInsets(
            top: CGFloat(padding),
            left: CGFloat(padding),
            bottom: CGFloat(padding),
            right: CGFloat(padding)
        )

        // Note: cameraThatFitsCoordinateBounds can throw C++ std::domain_error, but Swift can't catch C++ exceptions
        // We validate bounds above to prevent invalid calls
        let camera = mapView.cameraThatFitsCoordinateBounds(bounds, edgePadding: edgePadding)

        // Calculate zoom from camera altitude using MapLibre's standard formula
        // Formula: altitude = earthCircumference * cos(latitude) / (2^(zoom + 1))
        // Solving for zoom: zoom = log2(earthCircumference * cos(latitude) / altitude) - 1
        let earthCircumference = 40_075_016.686  // Earth's equatorial circumference in meters
        let centerLat = camera.centerCoordinate.latitude
        let latRadians = centerLat * .pi / 180.0
        let calculatedZoom = log2(earthCircumference * cos(latRadians) / camera.altitude) - 1.0

        WWWLog.d(
            Self.tag,
            """
            Camera calculated: center=(\(camera.centerCoordinate.latitude), \
            \(camera.centerCoordinate.longitude)), altitude=\(camera.altitude), zoom=\(calculatedZoom)
            """
        )

        let timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)

        mapView.setCamera(camera, withDuration: 0.5, animationTimingFunction: timingFunction) { [weak self] in
            WWWLog.d(Self.tag, "[SUCCESS] Camera animation to bounds completed")
            callback?.onFinish()
            self?.cameraAnimationCallback = nil
        }
    }

    // MARK: - Camera Constraints

    // swiftlint:disable function_parameter_count function_body_length cyclomatic_complexity
    // Justified: Camera constraint logic requires validation, bounds setup, and multiple conditionals
    @objc public func setBoundsForCameraTarget(
        constraintSwLat: Double,
        constraintSwLng: Double,
        constraintNeLat: Double,
        constraintNeLng: Double,
        eventSwLat: Double,
        eventSwLng: Double,
        eventNeLat: Double,
        eventNeLng: Double,
        isWindowMode: Bool
    ) -> Bool {
        guard let mapView = mapView else {
            WWWLog.w(Self.tag, "Cannot set bounds - mapView is nil")
            return false
        }

        // Validate constraint bounds
        guard constraintNeLat > constraintSwLat else {
            WWWLog.e(
                Self.tag,
                "Invalid constraint bounds: neLat (\(constraintNeLat)) must be > swLat (\(constraintSwLat))"
            )
            return false
        }

        // Validate coordinates are within valid ranges
        guard constraintSwLat >= -90 && constraintSwLat <= 90 &&
              constraintNeLat >= -90 && constraintNeLat <= 90 &&
              constraintSwLng >= -180 && constraintSwLng <= 180 &&
              constraintNeLng >= -180 && constraintNeLng <= 180 else {
            WWWLog.e(
                Self.tag,
                "Invalid constraint coordinate values: " +
                "SW(\(constraintSwLat),\(constraintSwLng)) NE(\(constraintNeLat),\(constraintNeLng))"
            )
            return false
        }

        // Create constraint bounds (shrunk bounds for gesture enforcement)
        let constraintSouthwest = CLLocationCoordinate2D(latitude: constraintSwLat, longitude: constraintSwLng)
        let constraintNortheast = CLLocationCoordinate2D(latitude: constraintNeLat, longitude: constraintNeLng)
        let constraintBounds = MLNCoordinateBounds(sw: constraintSouthwest, ne: constraintNortheast)

        // Create event bounds (original bounds for min zoom calculation)
        let eventSouthwest = CLLocationCoordinate2D(latitude: eventSwLat, longitude: eventSwLng)
        let eventNortheast = CLLocationCoordinate2D(latitude: eventNeLat, longitude: eventNeLng)
        let eventBounds = MLNCoordinateBounds(sw: eventSouthwest, ne: eventNortheast)

        // If style not loaded yet, queue bounds for later application
        guard styleIsLoaded, mapView.style != nil else {
            WWWLog.i(
                Self.tag,
                "Style not loaded - queueing constraint bounds and event bounds"
            )
            pendingConstraintBounds = constraintBounds
            pendingEventBounds = eventBounds
            pendingIsWindowMode = isWindowMode
            return true  // Return true - will be applied when style loads
        }

        // Check if min zoom already locked (matches Android minZoomLocked mechanism)
        // Allow recalculation only if previously used invalid dimensions
        if minZoomLocked {
            let currentScreenWidth = Double(mapView.bounds.size.width)
            let currentScreenHeight = Double(mapView.bounds.size.height)
            let nowHaveValidDimensions = currentScreenWidth > 0 && currentScreenHeight > 0

            if minZoomUsedInvalidDimensions && nowHaveValidDimensions {
                // Unlock and recalculate with real dimensions
                minZoomLocked = false
                minZoomUsedInvalidDimensions = false
            } else {
                currentConstraintBounds = constraintBounds
                return true
            }
        }

        // Style is loaded - apply bounds and calculate min zoom
        WWWLog.i(
            Self.tag,
            "Setting camera constraint bounds: " +
            "SW(\(constraintSwLat),\(constraintSwLng)) NE(\(constraintNeLat),\(constraintNeLng))"
        )
        WWWLog.i(
            Self.tag,
            "Using event bounds for min zoom: SW(\(eventSwLat),\(eventSwLng)) NE(\(eventNeLat),\(eventNeLng))"
        )

        // CRITICAL: Use EVENT bounds (not constraint bounds) for min zoom calculation
        // Constraint bounds may be shrunk by viewport padding, giving incorrect min zoom
        let eventWidth = eventBounds.ne.longitude - eventBounds.sw.longitude
        let eventHeight = eventBounds.ne.latitude - eventBounds.sw.latitude

        let screenWidth = Double(mapView.bounds.size.width)
        let screenHeight = Double(mapView.bounds.size.height)

        // Detect if MapView hasn't been laid out yet
        let hasInvalidDimensions = screenWidth <= 0 || screenHeight <= 0
        if hasInvalidDimensions {
            WWWLog.w(Self.tag, "[WARNING] MapView not laid out yet - min zoom may need recalculation")
        }

        // Calculate min zoom differently for BOUNDS vs WINDOW mode (matches Android)
        let baseMinZoom: Double

        if isWindowMode {
            // WINDOW MODE: Fit the smallest event dimension (prevents showing pixels outside event area)

            // Validate dimensions before calculation (prevent division by zero)
            guard screenWidth > 0 && screenHeight > 0 && eventWidth > 0 && eventHeight > 0 else {
                WWWLog.w(
                    Self.tag,
                    "Invalid dimensions: screen=\(screenWidth)x\(screenHeight), event=\(eventWidth)x\(eventHeight), " +
                    "queueing bounds for retry"
                )
                pendingConstraintBounds = constraintBounds
                pendingEventBounds = eventBounds
                pendingIsWindowMode = isWindowMode
                return true
            }

            let eventAspect = eventWidth / eventHeight
            let screenAspect = screenWidth / screenHeight

            // Create bounds matching the constraining dimension
            let constrainingBounds: MLNCoordinateBounds
            if eventAspect > screenAspect {
                // Event wider than screen → constrained by HEIGHT
                let constrainedWidth = eventHeight * screenAspect
                let centerLng = (eventBounds.sw.longitude + eventBounds.ne.longitude) / 2.0
                constrainingBounds = MLNCoordinateBounds(
                    sw: CLLocationCoordinate2D(
                        latitude: eventBounds.sw.latitude,
                        longitude: centerLng - constrainedWidth / 2
                    ),
                    ne: CLLocationCoordinate2D(
                        latitude: eventBounds.ne.latitude,
                        longitude: centerLng + constrainedWidth / 2
                    )
                )
                WWWLog.d(
                    Self.tag,
                    "Constraining bounds (HEIGHT-constrained): \(eventHeight)° x \(constrainedWidth)° " +
                    "(event width \(eventWidth)° reduced to \(constrainedWidth)°)"
                )
            } else {
                // Event taller than screen → constrained by WIDTH
                let constrainedHeight = eventWidth / screenAspect
                let centerLat = (eventBounds.sw.latitude + eventBounds.ne.latitude) / 2.0
                constrainingBounds = MLNCoordinateBounds(
                    sw: CLLocationCoordinate2D(
                        latitude: centerLat - constrainedHeight / 2,
                        longitude: eventBounds.sw.longitude
                    ),
                    ne: CLLocationCoordinate2D(
                        latitude: centerLat + constrainedHeight / 2,
                        longitude: eventBounds.ne.longitude
                    )
                )
                WWWLog.d(
                    Self.tag,
                    "Constraining bounds (WIDTH-constrained): \(constrainedHeight)° x \(eventWidth)° " +
                    "(event height \(eventHeight)° reduced to \(constrainedHeight)°)"
                )
            }

            // Calculate zoom directly (cameraThatFitsCoordinateBounds produces incorrect altitude)
            let centerLat = (constrainingBounds.sw.latitude + constrainingBounds.ne.latitude) / 2.0
            let latRadians = centerLat * .pi / 180.0

            // For wide event (fit by HEIGHT): calculate zoom from HEIGHT dimension only
            // For tall event (fit by WIDTH): calculate zoom from WIDTH dimension only
            let boundsHeight = constrainingBounds.ne.latitude - constrainingBounds.sw.latitude
            let boundsWidth = constrainingBounds.ne.longitude - constrainingBounds.sw.longitude

            // Web Mercator: degrees per point at zoom level Z
            // MapLibre uses 512px tiles (not 256px) per web search results
            // degreesPerPoint = (360 / (512 * 2^Z)) for latitude (no cos adjustment)
            // degreesPerPoint = (360 / (512 * 2^Z)) * cos(lat) for longitude
            //
            // Rearranged to solve for Z:
            // 2^Z = (screenPoints * 360) / (bounds * 512) for latitude
            // 2^Z = (screenPoints * 360 * cos(lat)) / (bounds * 512) for longitude

            let zoomForHeight = log2((screenHeight * 360.0) / (boundsHeight * 512.0))
            let zoomForWidth = log2((screenWidth * 360.0 * cos(latRadians)) / (boundsWidth * 512.0))

            // Use the constraining dimension zoom (ensures that dimension fills screen)
            if eventAspect > screenAspect {
                // Wide event → HEIGHT constrains
                baseMinZoom = zoomForHeight
                WWWLog.d(
                    Self.tag,
                    "WINDOW (HEIGHT): zoomH=\(zoomForHeight), zoomW=\(zoomForWidth), using HEIGHT zoom"
                )
            } else {
                // Tall event → WIDTH constrains
                baseMinZoom = zoomForWidth
                WWWLog.d(
                    Self.tag,
                    "WINDOW (WIDTH): zoomH=\(zoomForHeight), zoomW=\(zoomForWidth), using WIDTH zoom"
                )
            }

            WWWLog.i(
                Self.tag,
                "[AIM] WINDOW mode: eventAspect=\(eventAspect), screenAspect=\(screenAspect), " +
                "constrainedBy=\(eventAspect > screenAspect ? "HEIGHT" : "WIDTH"), minZoom=\(baseMinZoom)"
            )
        } else {
            // BOUNDS MODE: Use MapLibre's calculation (shows entire event)
            let camera = mapView.cameraThatFitsCoordinateBounds(eventBounds, edgePadding: .zero)

            let centerLat = (eventBounds.sw.latitude + eventBounds.ne.latitude) / 2.0
            let latRadians = centerLat * .pi / 180.0
            baseMinZoom = log2(40_075_016.686 * cos(latRadians) / camera.altitude) - 1.0

            WWWLog.i(Self.tag, "[AIM] BOUNDS mode: base=\(baseMinZoom) (entire event visible)")
        }

        let finalMinZoom = baseMinZoom

        // Lock min zoom to prevent recalculation
        lockedMinZoom = finalMinZoom
        minZoomLocked = true
        minZoomUsedInvalidDimensions = hasInvalidDimensions

        // Set minimum zoom to prevent zooming out beyond event bounds
        mapView.minimumZoomLevel = max(0, finalMinZoom)
        WWWLog.i(Self.tag, "[SUCCESS] Min zoom set to \(finalMinZoom) (prevents showing pixels outside event area)")

        // Store constraint bounds for gesture enforcement
        currentConstraintBounds = constraintBounds
        currentEventBounds = eventBounds

        pendingConstraintBounds = nil
        pendingEventBounds = nil
        pendingIsWindowMode = false
        return true
    }
    // swiftlint:enable function_parameter_count function_body_length cyclomatic_complexity

    @objc public func setMinZoom(_ minZoom: Double) {
        mapView?.minimumZoomLevel = minZoom
    }

    @objc public func setMaxZoom(_ maxZoom: Double) {
        mapView?.maximumZoomLevel = maxZoom
    }

    @objc public func getMinZoom() -> Double {
        return mapView?.minimumZoomLevel ?? 0
    }

    @objc public func getVisibleRegionBounds() -> [String: Double]? {
        guard let mapView = mapView else { return nil }

        let visibleBounds = mapView.visibleCoordinateBounds
        return [
            "minLat": visibleBounds.sw.latitude,
            "minLng": visibleBounds.sw.longitude,
            "maxLat": visibleBounds.ne.latitude,
            "maxLng": visibleBounds.ne.longitude
        ]
    }

    // MARK: - Attribution

    @objc public func setAttributionMargins(left: Int, top: Int, right: Int, bottom: Int) {
        guard let mapView = mapView else {
            WWWLog.w(Self.tag, "Cannot set attribution margins - mapView is nil")
            return
        }

        WWWLog.d(Self.tag, "Setting attribution margins: left=\(left), top=\(top), right=\(right), bottom=\(bottom)")

        // Ensure logo and attribution are visible
        mapView.logoView.isHidden = false
        mapView.attributionButton.isHidden = false

        // Enable Auto Layout for logo and attribution
        mapView.logoView.translatesAutoresizingMaskIntoConstraints = false
        mapView.attributionButton.translatesAutoresizingMaskIntoConstraints = false

        // Remove existing attribution constraints
        let logoConstraints = mapView.constraints.filter { constraint in
            constraint.firstItem as? UIView == mapView.logoView ||
            constraint.secondItem as? UIView == mapView.logoView
        }
        let attrConstraints = mapView.constraints.filter { constraint in
            constraint.firstItem as? UIView == mapView.attributionButton ||
            constraint.secondItem as? UIView == mapView.attributionButton
        }
        NSLayoutConstraint.deactivate(logoConstraints + attrConstraints)

        // Apply new constraints for logo (bottom-left position with margins)
        NSLayoutConstraint.activate([
            mapView.logoView.leadingAnchor.constraint(
                equalTo: mapView.leadingAnchor,
                constant: CGFloat(left)
            ),
            mapView.logoView.bottomAnchor.constraint(
                equalTo: mapView.bottomAnchor,
                constant: -CGFloat(bottom)
            )
        ])

        // Apply new constraints for attribution button (bottom-right position with margins)
        NSLayoutConstraint.activate([
            mapView.attributionButton.trailingAnchor.constraint(
                equalTo: mapView.trailingAnchor,
                constant: -CGFloat(right)
            ),
            mapView.attributionButton.bottomAnchor.constraint(
                equalTo: mapView.bottomAnchor,
                constant: -CGFloat(bottom)
            )
        ])

        WWWLog.i(Self.tag, "[SUCCESS] Attribution margins applied successfully")
    }

    // MARK: - Wave Polygons

    @objc public func addWavePolygons(polygons: [[CLLocationCoordinate2D]], clearExisting: Bool) {
        // Queue polygons if style not loaded yet
        guard styleIsLoaded, let mapView = mapView, let style = mapView.style else {
            pendingPolygons = polygons
            return
        }

        // Render polygons - updates existing layers to prevent flickering
        updatePolygonLayers(polygons: polygons, style: style)

        // Update accessibility state
        currentWavePolygons = polygons
        updateMapAccessibility()
    }

    /// Updates polygon layers by reusing existing layers (prevents flickering).
    private func updatePolygonLayers(polygons: [[CLLocationCoordinate2D]], style: MLNStyle) {
        // Remove excess layers if polygon count decreased
        if polygons.count < waveLayerIds.count {
            for index in polygons.count..<waveLayerIds.count {
                if let layer = style.layer(withIdentifier: waveLayerIds[index]) {
                    style.removeLayer(layer)
                }
                if let source = style.source(withIdentifier: waveSourceIds[index]) {
                    style.removeSource(source)
                }
            }
            waveLayerIds.removeSubrange(polygons.count...)
            waveSourceIds.removeSubrange(polygons.count...)
        }

        // Update or create each polygon layer
        for (index, coordinates) in polygons.enumerated() {
            let sourceId = index < waveSourceIds.count ? waveSourceIds[index] : "wave-polygons-source-\(index)"
            let layerId = index < waveLayerIds.count ? waveLayerIds[index] : "wave-polygons-layer-\(index)"

            // Create polygon shape
            let polygon = MLNPolygon(coordinates: coordinates, count: UInt(coordinates.count))

            if index < waveSourceIds.count {
                // Update existing source (no flickering)
                updateExistingPolygon(
                    index: index,
                    sourceId: sourceId,
                    layerId: layerId,
                    polygon: polygon,
                    style: style
                )
            } else {
                // Add new polygon (first time or expansion)
                addNewPolygon(index: index, sourceId: sourceId, layerId: layerId, polygon: polygon, style: style)
            }
        }
    }

    /// Updates existing polygon source (prevents flickering).
    private func updateExistingPolygon(
        index: Int,
        sourceId: String,
        layerId: String,
        polygon: MLNPolygon,
        style: MLNStyle
    ) {
        if let existingSource = style.source(withIdentifier: sourceId) as? MLNShapeSource {
            existingSource.shape = polygon
        } else {
            // Recreate missing source
            let source = MLNShapeSource(identifier: sourceId, shape: polygon, options: nil)
            style.addSource(source)

            let fillLayer = MLNFillStyleLayer(identifier: layerId, source: source)
            fillLayer.fillColor = NSExpression(forConstantValue: UIColor(hex: "#00008B"))
            fillLayer.fillOpacity = NSExpression(forConstantValue: 0.20)
            style.addLayer(fillLayer)
        }
    }

    /// Adds new polygon layer (wave expansion).
    private func addNewPolygon(
        index: Int,
        sourceId: String,
        layerId: String,
        polygon: MLNPolygon,
        style: MLNStyle
    ) {
        let source = MLNShapeSource(identifier: sourceId, shape: polygon, options: nil)
        style.addSource(source)

        let fillLayer = MLNFillStyleLayer(identifier: layerId, source: source)
        fillLayer.fillColor = NSExpression(forConstantValue: UIColor(hex: "#00008B"))
        fillLayer.fillOpacity = NSExpression(forConstantValue: 0.20)
        style.addLayer(fillLayer)

        waveSourceIds.append(sourceId)
        waveLayerIds.append(layerId)
    }

    @objc public func clearWavePolygons() {
        guard let style = mapView?.style else { return }

        // Remove all tracked wave layers and sources
        for layerId in waveLayerIds {
            if let layer = style.layer(withIdentifier: layerId) {
                style.removeLayer(layer)
            }
        }

        for sourceId in waveSourceIds {
            if let source = style.source(withIdentifier: sourceId) {
                style.removeSource(source)
            }
        }

        waveLayerIds.removeAll()
        waveSourceIds.removeAll()

        // Update accessibility state (no more polygons)
        currentWavePolygons.removeAll()
        updateMapAccessibility()
    }

    // MARK: - Override BBox Drawing

    @objc public func drawOverrideBbox(swLat: Double, swLng: Double, neLat: Double, neLng: Double) {
        guard let style = mapView?.style else { return }

        let southwest = CLLocationCoordinate2D(latitude: swLat, longitude: swLng)
        let northeast = CLLocationCoordinate2D(latitude: neLat, longitude: neLng)
        let northwest = CLLocationCoordinate2D(latitude: neLat, longitude: swLng)
        let southeast = CLLocationCoordinate2D(latitude: swLat, longitude: neLng)

        var coordinates = [southwest, southeast, northeast, northwest, southwest]
        let polyline = MLNPolyline(coordinates: &coordinates, count: UInt(coordinates.count))

        let source = MLNShapeSource(identifier: "bbox-override-source", shape: polyline, options: nil)
        style.addSource(source)

        let lineLayer = MLNLineStyleLayer(identifier: "bbox-override-line", source: source)
        lineLayer.lineColor = NSExpression(forConstantValue: UIColor.red)
        lineLayer.lineWidth = NSExpression(forConstantValue: 1.0)
        lineLayer.lineOpacity = NSExpression(forConstantValue: 1.0)
        lineLayer.lineDashPattern = NSExpression(forConstantValue: [5, 2])

        style.addLayer(lineLayer)
    }

    // MARK: - Event Listeners

    @objc public func setOnMapClickListener(_ listener: @escaping (Double, Double) -> Void) {
        self.onMapClick = listener
    }

    @objc public func setOnMapClickNavigationListener(_ listener: @escaping () -> Void) {
        self.onMapClickNavigation = listener
    }

    @objc public func setOnCameraIdleListener(_ listener: @escaping () -> Void) {
        self.onCameraIdle = listener
    }

    @objc private func handleMapTap(_ gesture: UITapGestureRecognizer) {
        guard let mapView = mapView else { return }

        let point = gesture.location(in: mapView)
        let coordinate = mapView.convert(point, toCoordinateFrom: mapView)

        // Invoke coordinate callback (local)
        onMapClick?(coordinate.latitude, coordinate.longitude)

        // Invoke coordinate listener (registry)
        if let eventId = eventId {
            Shared.MapWrapperRegistry.shared.invokeMapClickCoordinateListener(
                eventId: eventId,
                latitude: coordinate.latitude,
                longitude: coordinate.longitude
            )
        }

        // Invoke navigation callback (direct)
        onMapClickNavigation?()
    }

    // MARK: - Accessibility Configuration

    /// Configures map as accessibility container (not a leaf element).
    private func configureMapAccessibility() {
        guard let mapView = mapView else { return }
        mapView.isAccessibilityElement = false
        mapView.accessibilityNavigationStyle = .combined
    }

    /// Updates accessibility elements based on current map state.
    private func updateMapAccessibility() {
        guard let mapView = mapView else { return }

        var accessibilityElements: [Any] = []
        accessibilityElements.append(createMapSummaryElement())

        if let userElement = createUserPositionElement() {
            accessibilityElements.append(userElement)
        }

        if let areaElement = createEventAreaElement() {
            accessibilityElements.append(areaElement)
        }

        accessibilityElements.append(contentsOf: createWaveProgressionElements())
        mapView.accessibilityElements = accessibilityElements
    }

    /// Creates a summary element describing the overall map state.
    private func createMapSummaryElement() -> UIAccessibilityElement {
        guard let mapView = mapView else {
            return UIAccessibilityElement(accessibilityContainer: self)
        }

        let summaryElement = UIAccessibilityElement(accessibilityContainer: mapView)
        summaryElement.accessibilityTraits = .staticText

        // Build summary text
        var summaryText = "Map showing "

        if let eventName = currentEventName {
            summaryText += "\(eventName) event area"
        } else {
            summaryText += "event area"
        }

        if let userPos = currentUserPosition, let eventCenter = currentEventCenter {
            let distance = calculateDistance(from: userPos, destination: eventCenter)
            let distanceMeters = Int(distance)
            summaryText += ". You are \(distanceMeters) meters from event center"
        }

        if !currentWavePolygons.isEmpty {
            summaryText += ". \(currentWavePolygons.count) wave progression circle"
            if currentWavePolygons.count > 1 {
                summaryText += "s"
            }
            summaryText += " visible"
        }

        summaryElement.accessibilityLabel = summaryText

        // Position at top of map
        summaryElement.accessibilityFrame = CGRect(
            x: mapView.frame.origin.x,
            y: mapView.frame.origin.y,
            width: mapView.frame.width,
            height: 44
        )

        return summaryElement
    }

    /// Creates accessibility element for user position marker.
    private func createUserPositionElement() -> UIAccessibilityElement? {
        guard let mapView = mapView, let userPos = currentUserPosition else {
            return nil
        }

        let userElement = UIAccessibilityElement(accessibilityContainer: mapView)
        userElement.accessibilityLabel = "Your current position"
        userElement.accessibilityTraits = .updatesFrequently

        // Calculate screen position for coordinate
        userElement.accessibilityFrame = calculateFrameForCoordinate(userPos, in: mapView)

        return userElement
    }

    /// Creates accessibility element for event area boundary.
    private func createEventAreaElement() -> UIAccessibilityElement? {
        guard let mapView = mapView, let eventCenter = currentEventCenter else {
            return nil
        }

        let areaElement = UIAccessibilityElement(accessibilityContainer: mapView)

        var label = "Event area boundary"
        if let eventName = currentEventName {
            label += " for \(eventName)"
        }
        if currentEventRadius > 0 {
            let radiusKm = currentEventRadius / 1000.0
            label += ", radius \(String(format: "%.1f", radiusKm)) kilometers"
        }

        areaElement.accessibilityLabel = label
        areaElement.accessibilityTraits = .staticText

        // Position at event center
        areaElement.accessibilityFrame = calculateFrameForCoordinate(eventCenter, in: mapView)

        return areaElement
    }

    /// Creates accessibility elements for wave progression circles.
    private func createWaveProgressionElements() -> [UIAccessibilityElement] {
        guard let mapView = mapView else { return [] }

        var elements: [UIAccessibilityElement] = []

        for (index, polygon) in currentWavePolygons.enumerated() {
            guard let centerCoord = calculatePolygonCenter(polygon) else { continue }

            let circleElement = UIAccessibilityElement(accessibilityContainer: mapView)
            circleElement.accessibilityLabel = "Wave progression circle \(index + 1) of \(currentWavePolygons.count)"
            circleElement.accessibilityTraits = .updatesFrequently

            circleElement.accessibilityFrame = calculateFrameForCoordinate(centerCoord, in: mapView)

            elements.append(circleElement)
        }

        return elements
    }

    // MARK: - Accessibility Helpers

    /// Calculates the screen frame for a geographic coordinate.
    /// Returns a 44x44pt frame centered on the coordinate (iOS touch target size).
    private func calculateFrameForCoordinate(_ coordinate: CLLocationCoordinate2D, in mapView: MLNMapView) -> CGRect {
        let point = mapView.convert(coordinate, toPointTo: mapView)
        return CGRect(x: point.x - 22, y: point.y - 22, width: 44, height: 44)
    }

    /// Calculates distance in meters between two coordinates.
    private func calculateDistance(from: CLLocationCoordinate2D, destination: CLLocationCoordinate2D) -> Double {
        let fromLocation = CLLocation(latitude: from.latitude, longitude: from.longitude)
        let toLocation = CLLocation(latitude: destination.latitude, longitude: destination.longitude)
        return fromLocation.distance(from: toLocation)
    }

    /// Calculates the center coordinate of a polygon.
    private func calculatePolygonCenter(_ polygon: [CLLocationCoordinate2D]) -> CLLocationCoordinate2D? {
        guard !polygon.isEmpty else { return nil }

        var totalLat: Double = 0
        var totalLng: Double = 0

        for coord in polygon {
            totalLat += coord.latitude
            totalLng += coord.longitude
        }

        let count = Double(polygon.count)
        return CLLocationCoordinate2D(
            latitude: totalLat / count,
            longitude: totalLng / count
        )
    }

    // MARK: - Accessibility State Updates

    /// Updates user position and location marker.
    @objc public func setUserPosition(latitude: Double, longitude: Double) {
        currentUserPosition = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        updateMapAccessibility()

        if isLocationComponentEnabled {
            updateUserLocationMarker(coordinate: currentUserPosition!)
        }
    }

    // MARK: - Location Component

    /// Enable/disable user location marker (uses custom annotation with PositionManager).
    @objc public func enableLocationComponent(_ enabled: Bool) {
        guard let mapView = mapView else { return }

        isLocationComponentEnabled = enabled

        if enabled {
            if userLocationAnnotation == nil {
                let annotation = MLNPointAnnotation()
                annotation.title = "Your Location"
                userLocationAnnotation = annotation

                if let currentPos = currentUserPosition {
                    annotation.coordinate = currentPos
                    mapView.addAnnotation(annotation)
                }
            } else if let currentPos = currentUserPosition {
                updateUserLocationMarker(coordinate: currentPos)
            }
        } else {
            if let annotation = userLocationAnnotation {
                mapView.removeAnnotation(annotation)
                userLocationAnnotation = nil
            }
        }
    }

    /// Updates user location marker position (remove and re-add for coordinate update).
    private func updateUserLocationMarker(coordinate: CLLocationCoordinate2D) {
        guard let mapView = mapView else { return }

        if let annotation = userLocationAnnotation {
            mapView.removeAnnotation(annotation)
            annotation.coordinate = coordinate
            mapView.addAnnotation(annotation)
        } else {
            // Create if missing
            let annotation = MLNPointAnnotation()
            annotation.title = "Your Location"
            annotation.coordinate = coordinate
            userLocationAnnotation = annotation
            mapView.addAnnotation(annotation)
        }
    }

    /// Updates event metadata for accessibility.
    @objc public func setEventInfo(
        centerLatitude: Double,
        centerLongitude: Double,
        radius: Double,
        eventName: String?
    ) {
        currentEventCenter = CLLocationCoordinate2D(latitude: centerLatitude, longitude: centerLongitude)
        currentEventRadius = radius
        currentEventName = eventName
        updateMapAccessibility()
    }
}

// MARK: - MLNMapViewDelegate

extension MapLibreViewWrapper: MLNMapViewDelegate {
    public func mapView(_ mapView: MLNMapView, didFinishLoading style: MLNStyle) {
        WWWLog.i(Self.tag, "[STYLE] Style loaded successfully for event: \(eventId ?? "unknown")")
        styleIsLoaded = true
        onStyleLoaded?()
        onStyleLoaded = nil

        guard let eventId = eventId else {
            WWWLog.e(Self.tag, "Cannot execute commands - eventId is nil")
            return
        }

        // Mark style as loaded in registry (enables immediate callback invocation)
        Shared.MapWrapperRegistry.shared.setStyleLoaded(eventId: eventId, loaded: true)

        // Update map dimensions in registry for padding/bounds calculations
        Shared.MapWrapperRegistry.shared.updateMapWidth(
            eventId: eventId,
            width: Double(mapView.bounds.size.width)
        )
        Shared.MapWrapperRegistry.shared.updateMapHeight(
            eventId: eventId,
            height: Double(mapView.bounds.size.height)
        )
        WWWLog.d(Self.tag, "Map dimensions updated: \(mapView.bounds.size.width) x \(mapView.bounds.size.height)")

        // Execute pending commands in order: constraints first, then camera movements (matches Android)
        if let bounds = pendingConstraintBounds {

            // Use MapLibre's native camera calculation to get accurate zoom level
            let camera = mapView.cameraThatFitsCoordinateBounds(bounds, edgePadding: .zero)

            // Convert camera altitude to zoom level using MapLibre's formula
            let earthCircumference = 40_075_016.686
            let centerLat = (bounds.sw.latitude + bounds.ne.latitude) / 2.0
            let latRadians = centerLat * .pi / 180.0
            let calculatedMinZoom = log2(earthCircumference * cos(latRadians) / camera.altitude) - 1.0

            mapView.minimumZoomLevel = max(0, calculatedMinZoom)
            currentConstraintBounds = bounds
            pendingConstraintBounds = nil
        }

        if let polygons = pendingPolygons {
            addWavePolygons(polygons: polygons, clearExisting: true)
            pendingPolygons = nil
        }

        IOSMapBridge.executePendingCameraCommand(eventId: eventId)
        _ = IOSMapBridge.renderPendingPolygons(eventId: eventId)
        IOSMapBridge.invokeMapReadyCallbacks(eventId: eventId)
    }

    public func mapView(
        _ mapView: MLNMapView,
        shouldChangeFrom oldCamera: MLNMapCamera,
        to newCamera: MLNMapCamera,
        reason: MLNCameraChangeReason
    ) -> Bool {
        // Validate camera position against constraint bounds (matches Android setLatLngBoundsForCameraTarget)
        // Constrains camera CENTER to padded bounds, allows viewport to extend beyond for full event edges
        guard let constraintBounds = currentConstraintBounds else {
            return true
        }

        // Tolerance for smooth gesture handling at boundaries (~55 meters)
        // Increased from 0.00001 to prevent sticky/blocking behavior when panning at edges
        let epsilon = 0.0005

        // Check if camera center exceeds constraint bounds
        let cameraPosition = newCamera.centerCoordinate
        let cameraWithinBounds = cameraPosition.latitude >= (constraintBounds.sw.latitude - epsilon) &&
                                  cameraPosition.latitude <= (constraintBounds.ne.latitude + epsilon) &&
                                  cameraPosition.longitude >= (constraintBounds.sw.longitude - epsilon) &&
                                  cameraPosition.longitude <= (constraintBounds.ne.longitude + epsilon)

        return cameraWithinBounds
    }

    public func mapView(_ mapView: MLNMapView, regionDidChangeAnimated animated: Bool) {
        onCameraIdle?()

        if let eventId = eventId {
            Shared.MapWrapperRegistry.shared.invokeCameraIdleListener(eventId: eventId)

            Shared.MapWrapperRegistry.shared.updateCameraPosition(
                eventId: eventId,
                latitude: mapView.centerCoordinate.latitude,
                longitude: mapView.centerCoordinate.longitude
            )
            Shared.MapWrapperRegistry.shared.updateCameraZoom(
                eventId: eventId,
                zoom: mapView.zoomLevel
            )

            let bounds = mapView.visibleCoordinateBounds
            let latSpan = bounds.ne.latitude - bounds.sw.latitude
            let lngSpan = bounds.ne.longitude - bounds.sw.longitude

            // Only update if bounds are reasonable (< 10°) to prevent fallback world bounds
            if latSpan < 10.0 && lngSpan < 10.0 {
                let bbox = BoundingBox(
                    swLat: bounds.sw.latitude,
                    swLng: bounds.sw.longitude,
                    neLat: bounds.ne.latitude,
                    neLng: bounds.ne.longitude
                )
                Shared.MapWrapperRegistry.shared.updateVisibleRegion(eventId: eventId, bbox: bbox)
            }

            Shared.MapWrapperRegistry.shared.updateMinZoom(eventId: eventId, minZoom: mapView.minimumZoomLevel)
            Shared.MapWrapperRegistry.shared.updateMapWidth(eventId: eventId, width: Double(mapView.bounds.size.width))
            Shared.MapWrapperRegistry.shared.updateMapHeight(
                eventId: eventId,
                height: Double(mapView.bounds.size.height)
            )
        }

        updateMapAccessibility()
    }

    public func mapViewDidFailLoadingMap(_ mapView: MLNMapView, withError error: Error) {
        WWWLog.e(Self.tag, "[ERROR] FAILED to load map", error: error)
        WWWLog.e(Self.tag, "Error domain: \((error as NSError).domain), code: \((error as NSError).code)")
        WWWLog.e(Self.tag, "Error description: \(error.localizedDescription)")
        WWWLog.e(Self.tag, "Event: \(eventId ?? "unknown")")
    }

    @objc public func mapView(_ mapView: MLNMapView, didFailToLoadImage imageName: String) -> UIImage? {
        WWWLog.w(Self.tag, "[WARNING] Failed to load image: \(imageName)")
        WWWLog.w(Self.tag, "Event: \(eventId ?? "unknown")")
        return nil // Return nil to let MapLibre use default/fallback
    }

    public func mapViewWillStartLoadingMap(_ mapView: MLNMapView) {
        WWWLog.i(Self.tag, "[REFRESH] Map WILL START loading for event: \(eventId ?? "unknown")")
        WWWLog.d(Self.tag, "Style URL: \(mapView.styleURL?.absoluteString ?? "nil")")
    }

    public func mapViewDidFinishLoadingMap(_ mapView: MLNMapView) {
        WWWLog.i(Self.tag, "[MAP] Map DID FINISH loading (tiles, layers) for event: \(eventId ?? "unknown")")
        // Note: This is different from didFinishLoading style:
        // - didFinishLoadingMap = all tiles and resources loaded
        // - didFinishLoading style: = style JSON parsed and layers created
    }

    public func mapViewDidBecomeIdle(_ mapView: MLNMapView) {
        WWWLog.d(Self.tag, "[IDLE] Map became idle for event: \(eventId ?? "unknown")")
    }

    public func mapView(_ mapView: MLNMapView, didSelect annotation: MLNAnnotation) {
        WWWLog.d(Self.tag, "[LOCATION] Annotation selected: \(annotation)")
    }

    /// Add pulse animation to match Android's pulsing location marker.
    /// Creates a repeating scale animation on the pulse circle.
    private func addPulseAnimation(to view: UIView) {
        let pulseAnimation = CABasicAnimation(keyPath: "transform.scale")
        pulseAnimation.duration = 1.5
        pulseAnimation.fromValue = 1.0
        pulseAnimation.toValue = 1.3
        pulseAnimation.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)
        pulseAnimation.autoreverses = true
        pulseAnimation.repeatCount = .infinity
        view.layer.add(pulseAnimation, forKey: "pulse")
    }

    public func mapView(_ mapView: MLNMapView, viewFor annotation: MLNAnnotation) -> MLNAnnotationView? {
        // Customize user location annotation to match Android style
        if let userAnnotation = userLocationAnnotation, annotation === userAnnotation {
            let reuseIdentifier = "userLocation"

            // Try to reuse existing annotation view
            var annotationView = mapView.dequeueReusableAnnotationView(withIdentifier: reuseIdentifier)

            if annotationView == nil {
                // Create new annotation view - matches Android style
                annotationView = MLNAnnotationView(reuseIdentifier: reuseIdentifier)
                annotationView?.frame = CGRect(x: 0, y: 0, width: 40, height: 40)

                // Red pulse circle (matches Android pulseColor with full opacity)
                let pulseView = UIView(frame: CGRect(x: 0, y: 0, width: 40, height: 40))
                pulseView.backgroundColor = UIColor(red: 1.0, green: 0.0, blue: 0.0, alpha: 1.0)
                pulseView.layer.cornerRadius = 20
                pulseView.tag = 100 // Tag for animation

                // Black center dot (matches Android foregroundTintColor)
                let dotView = UIView(frame: CGRect(x: 15, y: 15, width: 10, height: 10))
                dotView.backgroundColor = UIColor.black
                dotView.layer.cornerRadius = 5
                dotView.layer.borderWidth = 2
                dotView.layer.borderColor = UIColor.white.cgColor

                pulseView.addSubview(dotView)
                annotationView?.addSubview(pulseView)

                // Center the annotation on the coordinate
                annotationView?.centerOffset = CGVector(dx: 0, dy: 0)

                // Add pulse animation (matches Android pulse effect)
                addPulseAnimation(to: pulseView)
            }

            return annotationView
        }

        return nil
    }
}

// MARK: - Camera Callback Wrapper

@objc public class MapCameraCallbackWrapper: NSObject {
    private let onFinishBlock: () -> Void
    private let onCancelBlock: () -> Void

    @objc public init(onFinish: @escaping () -> Void, onCancel: @escaping () -> Void) {
        self.onFinishBlock = onFinish
        self.onCancelBlock = onCancel
        super.init()
    }

    @objc public func onFinish() {
        onFinishBlock()
    }

    @objc public func onCancel() {
        onCancelBlock()
    }
}

// MARK: - UIGestureRecognizerDelegate

extension MapLibreViewWrapper: UIGestureRecognizerDelegate {
    /// Allow tap gesture to work simultaneously with MapLibre's gestures.
    public func gestureRecognizer(
        _ gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
    ) -> Bool {
        return true
    }

    public func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
        return true
    }
}

// MARK: - UIColor Hex Extension

extension UIColor {
    convenience init(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")

        var rgb: UInt64 = 0
        Scanner(string: hexSanitized).scanHexInt64(&rgb)

        let red = CGFloat((rgb & 0xFF0000) >> 16) / 255.0
        let green = CGFloat((rgb & 0x00FF00) >> 8) / 255.0
        let blue = CGFloat(rgb & 0x0000FF) / 255.0

        self.init(red: red, green: green, blue: blue, alpha: 1.0)
    }
}

// swiftlint:enable type_body_length
