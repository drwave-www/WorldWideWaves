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
// swiftlint:disable file_length type_body_length

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

    // DEPRECATED: Polling timer removed - now using direct dispatch callbacks
    // private var commandPollingTimer: Timer?
    // private static let pollingInterval: TimeInterval = 0.1 // 100ms

    // Queue for polygons that arrive before style loads
    // Only stores the most recent set since wave progression contains all previous circles
    private var pendingPolygons: [[CLLocationCoordinate2D]]?
    private var styleIsLoaded: Bool = false

    // Queue for constraint bounds that arrive before style loads
    private var pendingConstraintBounds: MLNCoordinateBounds?

    // Current constraint bounds for gesture clamping (matches Android behavior)
    private var currentConstraintBounds: MLNCoordinateBounds?

    // MARK: - Accessibility State

    /// Accessibility state tracking for VoiceOver
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
        WWWLog.w(Self.tag, "⚠️ Deinitializing MapLibreViewWrapper for event: \(eventId ?? "unknown")")
        WWWLog.w(Self.tag, "This will stop polygon updates! Wrapper should stay alive during wave screen.")
        // DEPRECATED: No longer using polling timer
        // stopContinuousPolling()
    }

    // MARK: - Map Setup

    private var eventId: String?

    @objc public func setMapView(_ mapView: MLNMapView) {
        WWWLog.i(Self.tag, "setMapView called for event: \(eventId ?? "nil"), bounds: \(mapView.bounds)")
        self.mapView = mapView
        self.mapView?.delegate = self

        // Ensure user interaction is enabled
        mapView.isUserInteractionEnabled = true
        WWWLog.d(Self.tag, "User interaction enabled: \(mapView.isUserInteractionEnabled)")

        // Configure accessibility for VoiceOver
        configureMapAccessibility()

        // Add tap gesture recognizer for map clicks
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleMapTap(_:)))
        tapGesture.numberOfTapsRequired = 1
        tapGesture.numberOfTouchesRequired = 1
        tapGesture.delegate = self  // Set delegate to handle conflicts with MapLibre gestures
        mapView.addGestureRecognizer(tapGesture)

        WWWLog.i(Self.tag, "👆 Tap gesture added to map view")
        WWWLog.i(Self.tag, "Total gesture recognizers on map: \(mapView.gestureRecognizers?.count ?? 0)")

        // Log all gesture recognizers to debug conflicts
        mapView.gestureRecognizers?.enumerated().forEach { index, recognizer in
            WWWLog.d(Self.tag, "Gesture \(index): \(type(of: recognizer))")
        }
        WWWLog.d(Self.tag, "Map view configured successfully for event: \(eventId ?? "nil")")
    }

    @objc public func setEventId(_ eventId: String) {
        self.eventId = eventId
        WWWLog.d(Self.tag, "Event ID set: \(eventId)")

        // Register immediate render callback (eliminates polling delay for polygons)
        Shared.MapWrapperRegistry.shared.setRenderCallback(eventId: eventId) { [weak self] in
            guard self != nil else { return }
            WWWLog.i(Self.tag, "🚀 Immediate render callback triggered for: \(eventId)")
            _ = IOSMapBridge.renderPendingPolygons(eventId: eventId)
        }
        WWWLog.d(Self.tag, "Immediate render callback registered for: \(eventId)")

        // Register immediate camera callback (eliminates polling delay for camera commands)
        Shared.MapWrapperRegistry.shared.setCameraCallback(eventId: eventId) { [weak self] in
            guard self != nil else { return }
            WWWLog.i(Self.tag, "📸 Immediate camera callback triggered for: \(eventId)")
            IOSMapBridge.executePendingCameraCommand(eventId: eventId)
        }
        WWWLog.d(Self.tag, "Immediate camera callback registered for: \(eventId)")

        // Register map click callback handler (direct callback storage)
        Shared.MapWrapperRegistry.shared.setMapClickRegistrationCallback(
            eventId: eventId
        ) { [weak self] clickCallback in
            guard let self = self else { return }
            WWWLog.i(Self.tag, "👆 Registering map click navigation callback directly on wrapper")
            // Wrap Kotlin Unit-returning function to Swift Void
            self.setOnMapClickNavigationListener {
                _ = clickCallback()
            }
        }
        WWWLog.d(Self.tag, "Map click registration callback registered for: \(eventId)")

        // Register getActualMinZoom callback (provides real-time min zoom to Kotlin)
        Shared.MapWrapperRegistry.shared.setGetActualMinZoomCallback(eventId: eventId) { [weak self] in
            guard let self = self, let mapView = self.mapView else { return }
            let actualMinZoom = mapView.minimumZoomLevel
            WWWLog.v(Self.tag, "getActualMinZoom callback: \(eventId) -> \(actualMinZoom)")
            Shared.MapWrapperRegistry.shared.updateActualMinZoom(eventId: eventId, actualMinZoom: actualMinZoom)
        }
        WWWLog.d(Self.tag, "getActualMinZoom callback registered for: \(eventId)")

        // Register location component callback (enables user position marker control)
        Shared.MapWrapperRegistry.shared.setLocationComponentCallback(eventId: eventId) { [weak self] enabled in
            guard let self = self else { return }
            let swiftEnabled = enabled.boolValue  // Convert KotlinBoolean to Bool
            WWWLog.i(Self.tag, "Location component callback: \(swiftEnabled) for: \(eventId)")
            self.enableLocationComponent(swiftEnabled)
        }
        WWWLog.d(Self.tag, "Location component callback registered for: \(eventId)")

        // Register setUserPosition callback (receives position updates from PositionManager)
        Shared.MapWrapperRegistry.shared.setUserPositionCallback(eventId: eventId) { [weak self] latitude, longitude in
            guard let self = self else { return }
            let swiftLat = latitude.doubleValue  // Convert KotlinDouble to Double
            let swiftLng = longitude.doubleValue
            self.setUserPosition(latitude: swiftLat, longitude: swiftLng)
        }
        WWWLog.d(Self.tag, "User position callback registered for: \(eventId)")
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
        let timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)

        mapView.setCamera(camera, withDuration: 0.5, animationTimingFunction: timingFunction) { [weak self] in
            WWWLog.d(Self.tag, "✅ Camera animation to bounds completed")
            callback?.onFinish()
            self?.cameraAnimationCallback = nil
        }
    }

    // MARK: - Camera Constraints

    @objc public func setBoundsForCameraTarget(swLat: Double, swLng: Double, neLat: Double, neLng: Double) -> Bool {
        guard let mapView = mapView else {
            WWWLog.w(Self.tag, "Cannot set bounds - mapView is nil")
            return false
        }

        // Validate bounds before setting or queuing
        guard neLat > swLat else {
            WWWLog.e(Self.tag, "Invalid bounds: neLat (\(neLat)) must be > swLat (\(swLat))")
            return false
        }

        // Validate coordinates are within valid ranges
        guard swLat >= -90 && swLat <= 90 && neLat >= -90 && neLat <= 90 &&
              swLng >= -180 && swLng <= 180 && neLng >= -180 && neLng <= 180 else {
            WWWLog.e(Self.tag, "Invalid coordinate values: SW(\(swLat),\(swLng)) NE(\(neLat),\(neLng))")
            return false
        }

        let southwest = CLLocationCoordinate2D(latitude: swLat, longitude: swLng)
        let northeast = CLLocationCoordinate2D(latitude: neLat, longitude: neLng)
        let bounds = MLNCoordinateBounds(sw: southwest, ne: northeast)

        // If style not loaded yet, queue bounds for later application
        guard styleIsLoaded, mapView.style != nil else {
            WWWLog.i(
                Self.tag,
                "Style not loaded - queueing constraint bounds: SW(\(swLat),\(swLng)) NE(\(neLat),\(neLng))"
            )
            pendingConstraintBounds = bounds
            return true  // Return true - will be applied when style loads
        }

        // Style is loaded - apply bounds immediately
        WWWLog.i(Self.tag, "Setting camera constraint bounds: SW(\(swLat),\(swLng)) NE(\(neLat),\(neLng))")
        mapView.setVisibleCoordinateBounds(bounds, animated: false)

        // Store constraint bounds for gesture clamping (matches Android setLatLngBoundsForCameraTarget behavior)
        currentConstraintBounds = bounds
        WWWLog.i(Self.tag, "Constraint bounds stored for gesture clamping")

        pendingConstraintBounds = nil  // Clear pending bounds
        WWWLog.i(Self.tag, "✅ Camera constraint bounds set successfully")
        return true
    }

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

        WWWLog.i(Self.tag, "✅ Attribution margins applied successfully")
    }

    // MARK: - Wave Polygons

    @objc public func addWavePolygons(polygons: [[CLLocationCoordinate2D]], clearExisting: Bool) {
        WWWLog.i(
            Self.tag,
            """
            addWavePolygons: \(polygons.count) polygons, \
            clearExisting: \(clearExisting), styleLoaded: \(styleIsLoaded)
            """
        )

        // If style not loaded yet, store most recent polygons for later
        // Only the most recent set matters (wave progression contains all previous circles)
        guard styleIsLoaded, let mapView = mapView, let style = mapView.style else {
            let hasMap = mapView != nil
            let hasStyle = mapView?.style != nil
            let message = "Style not ready - storing \(polygons.count) polygons " +
                "(most recent, mapView: \(hasMap), style: \(hasStyle))"
            WWWLog.w(Self.tag, message)
            pendingPolygons = polygons
            return
        }

        // Style is loaded - render immediately
        // Clear existing polygons if requested
        if clearExisting {
            WWWLog.d(Self.tag, "Clearing existing wave polygons before rendering")
            clearWavePolygons()
        }

        // Add each polygon as a separate source and layer
        for (index, coordinates) in polygons.enumerated() {
            let sourceId = "wave-polygons-source-\(index)-\(UUID().uuidString)"
            let layerId = "wave-polygons-layer-\(index)-\(UUID().uuidString)"

            // Create polygon shape
            let polygon = MLNPolygon(coordinates: coordinates, count: UInt(coordinates.count))

            // Create source
            let source = MLNShapeSource(identifier: sourceId, shape: polygon, options: nil)
            style.addSource(source)

            // Create fill layer with wave styling
            let fillLayer = MLNFillStyleLayer(identifier: layerId, source: source)
            fillLayer.fillColor = NSExpression(forConstantValue: UIColor(hex: "#00008B"))
            fillLayer.fillOpacity = NSExpression(forConstantValue: 0.20)

            style.addLayer(fillLayer)

            // Track for cleanup
            waveSourceIds.append(sourceId)
            waveLayerIds.append(layerId)
        }

        WWWLog.i(Self.tag, "✅ Rendered \(polygons.count) wave polygons to map, total layers: \(waveLayerIds.count)")

        // Update accessibility state with new polygons
        currentWavePolygons = polygons
        updateMapAccessibility()
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
        WWWLog.d(Self.tag, "Setting map click navigation listener")
        self.onMapClickNavigation = listener
    }

    @objc public func setOnCameraIdleListener(_ listener: @escaping () -> Void) {
        self.onCameraIdle = listener
    }

    @objc private func handleMapTap(_ gesture: UITapGestureRecognizer) {
        WWWLog.i(Self.tag, "👆 Map tap detected!")

        guard let mapView = mapView else {
            WWWLog.w(Self.tag, "Cannot handle tap - mapView is nil")
            return
        }

        let point = gesture.location(in: mapView)
        let coordinate = mapView.convert(point, toCoordinateFrom: mapView)
        WWWLog.d(Self.tag, "Tap coordinates: \(coordinate.latitude), \(coordinate.longitude)")

        // Call coordinate callback if set (local)
        if let onMapClick = onMapClick {
            WWWLog.d(Self.tag, "Calling onMapClick coordinate callback")
            onMapClick(coordinate.latitude, coordinate.longitude)
        } else {
            WWWLog.v(Self.tag, "No onMapClick coordinate callback set")
        }

        // Call coordinate listener from registry (for setOnMapClickListener)
        if let eventId = eventId {
            Shared.MapWrapperRegistry.shared.invokeMapClickCoordinateListener(
                eventId: eventId,
                latitude: coordinate.latitude,
                longitude: coordinate.longitude
            )
        }

        // Call navigation callback directly (no registry lookup)
        if let onMapClickNavigation = onMapClickNavigation {
            WWWLog.i(Self.tag, "✅ Calling map click navigation callback directly")
            onMapClickNavigation()
        } else {
            WWWLog.v(Self.tag, "No map click navigation callback set")
        }
    }

    // MARK: - Accessibility Configuration

    /// Configures the map container for VoiceOver accessibility.
    /// The map itself is not an accessibility element, but a container for accessible elements.
    private func configureMapAccessibility() {
        guard let mapView = mapView else { return }

        // Map container is not a leaf element (contains accessible elements)
        mapView.isAccessibilityElement = false
        mapView.accessibilityNavigationStyle = .combined

        WWWLog.d(Self.tag, "Map accessibility configured")
    }

    /// Updates accessibility elements based on current map state.
    /// Called when user position, wave polygons, or event data changes.
    private func updateMapAccessibility() {
        guard let mapView = mapView else { return }

        var accessibilityElements: [Any] = []

        // Map summary element (always first)
        let summaryElement = createMapSummaryElement()
        accessibilityElements.append(summaryElement)

        // User position marker (if available)
        if let userElement = createUserPositionElement() {
            accessibilityElements.append(userElement)
        }

        // Event area boundary (if available)
        if let areaElement = createEventAreaElement() {
            accessibilityElements.append(areaElement)
        }

        // Wave progression circles (if available)
        let waveElements = createWaveProgressionElements()
        accessibilityElements.append(contentsOf: waveElements)

        // Update map view accessibility elements
        mapView.accessibilityElements = accessibilityElements

        WWWLog.v(Self.tag, "Map accessibility updated with \(accessibilityElements.count) elements")
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

    /// Updates user position for accessibility.
    /// Call this when user location changes.
    @objc public func setUserPosition(latitude: Double, longitude: Double) {
        WWWLog.i(
            Self.tag,
            "📍 setUserPosition called: (\(latitude), \(longitude)), locationEnabled=\(isLocationComponentEnabled)"
        )

        currentUserPosition = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        updateMapAccessibility()

        // Update location marker if enabled
        if isLocationComponentEnabled {
            WWWLog.d(Self.tag, "Updating location marker for: \(eventId ?? "unknown")")
            updateUserLocationMarker(coordinate: currentUserPosition!)
        } else {
            WWWLog.w(Self.tag, "⚠️ Location component NOT enabled, marker not updated")
        }
    }

    // MARK: - Location Component

    /// Enable/disable user location marker with custom annotation.
    /// Uses custom annotation because we're using PositionManager (not native CLLocationManager).
    @objc public func enableLocationComponent(_ enabled: Bool) {
        guard let mapView = mapView else { return }

        isLocationComponentEnabled = enabled

        if enabled {
            WWWLog.i(Self.tag, "Enabling location component with custom annotation")

            // Create user location annotation if it doesn't exist
            if userLocationAnnotation == nil {
                let annotation = MLNPointAnnotation()
                annotation.title = "Your Location"
                userLocationAnnotation = annotation

                // Add annotation to map if we have a current position
                if let currentPos = currentUserPosition {
                    annotation.coordinate = currentPos
                    mapView.addAnnotation(annotation)
                    WWWLog.i(
                        Self.tag,
                        "Added user location annotation at: \(currentPos.latitude), \(currentPos.longitude)"
                    )
                } else {
                    WWWLog.w(Self.tag, "No current position available for annotation")
                }
            }

            WWWLog.i(Self.tag, "✅ Location component enabled (custom annotation)")
        } else {
            WWWLog.i(Self.tag, "Disabling location component")

            // Remove annotation from map
            if let annotation = userLocationAnnotation {
                mapView.removeAnnotation(annotation)
                userLocationAnnotation = nil
            }
        }
    }

    /// Update user location marker position.
    /// Manually updates the custom annotation position when using PositionManager.
    private func updateUserLocationMarker(coordinate: CLLocationCoordinate2D) {
        guard let mapView = mapView else {
            WWWLog.w(Self.tag, "Cannot update user location - mapView is nil")
            return
        }

        // Update the annotation coordinate
        if let annotation = userLocationAnnotation {
            // Remove old annotation
            mapView.removeAnnotation(annotation)

            // Update coordinate
            annotation.coordinate = coordinate

            // Add back to map
            mapView.addAnnotation(annotation)

            WWWLog.v(Self.tag, "User location marker updated: \(coordinate.latitude), \(coordinate.longitude)")
        } else {
            WWWLog.w(Self.tag, "User location annotation not created yet, creating now")
            // Create annotation if it doesn't exist (shouldn't happen if enableLocationComponent called first)
            let annotation = MLNPointAnnotation()
            annotation.title = "Your Location"
            annotation.coordinate = coordinate
            userLocationAnnotation = annotation
            mapView.addAnnotation(annotation)
            WWWLog.i(
                Self.tag,
                "Created and added user location annotation at: \(coordinate.latitude), \(coordinate.longitude)"
            )
        }
    }

    /// Updates event metadata for accessibility.
    /// Call this when event data is loaded.
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
        WWWLog.v(Self.tag, "Event info updated for accessibility: \(eventName ?? "unknown")")
    }

    // MARK: - DEPRECATED: Continuous Command Polling
    // NOTE: Polling has been replaced with direct dispatch callbacks for better performance
    // The immediate render and camera callbacks registered in setEventId() now handle updates
    // This eliminates 100ms+ polling delay and reduces CPU/battery usage

    /*
    /// DEPRECATED: Replaced with direct dispatch callbacks
    private func startContinuousPolling() {
        // No longer needed - using MapWrapperRegistry.setRenderCallback and setCameraCallback
    }

    /// DEPRECATED: Replaced with direct dispatch callbacks
    private func stopContinuousPolling() {
        // No longer needed - callbacks are automatically cleaned up
    }
    */
}

// MARK: - MLNMapViewDelegate

extension MapLibreViewWrapper: MLNMapViewDelegate {
    public func mapView(_ mapView: MLNMapView, didFinishLoading style: MLNStyle) {
        WWWLog.i(Self.tag, "🎨 Style loaded successfully for event: \(eventId ?? "unknown")")
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

        // IMMEDIATE EXECUTION: Execute all pending commands now that map is ready
        WWWLog.i(Self.tag, "⚡ Executing pending commands immediately after style load...")

        // 1. Render pending polygons that arrived before style loaded
        // Only the most recent set matters (wave progression contains all previous circles)
        if let polygons = pendingPolygons {
            WWWLog.i(Self.tag, "📦 Rendering pending polygons: \(polygons.count) polygons")
            addWavePolygons(polygons: polygons, clearExisting: true)
            pendingPolygons = nil
        }

        // 2. Execute pending camera commands (initial positioning, constraints)
        WWWLog.d(Self.tag, "Checking for pending camera commands...")
        IOSMapBridge.executePendingCameraCommand(eventId: eventId)

        // 3. Render pending wave polygons from registry (initial wave state)
        WWWLog.d(Self.tag, "Checking for pending polygons in registry...")
        _ = IOSMapBridge.renderPendingPolygons(eventId: eventId)

        // 4. Apply queued constraint bounds if any
        if let bounds = pendingConstraintBounds {
            WWWLog.i(Self.tag, "Applying queued constraint bounds after style load")
            mapView.setVisibleCoordinateBounds(bounds, animated: false)
            currentConstraintBounds = bounds  // Store for gesture clamping
            pendingConstraintBounds = nil
            WWWLog.i(Self.tag, "✅ Constraint bounds applied successfully with gesture clamping")
        }

        // 5. Invoke map ready callbacks (enables wave polygon rendering and other operations)
        WWWLog.i(Self.tag, "Invoking map ready callbacks...")
        IOSMapBridge.invokeMapReadyCallbacks(eventId: eventId)

        // NOTE: Continuous polling REMOVED - now using direct dispatch callbacks
        // Callbacks were registered in setEventId() and provide immediate updates (<16ms vs 100ms+)
        WWWLog.i(Self.tag, "✅ Map initialization complete with direct dispatch callbacks for event: \(eventId)")
    }

    public func mapView(
        _ mapView: MLNMapView,
        shouldChangeFrom oldCamera: MLNMapCamera,
        to newCamera: MLNMapCamera,
        reason: MLNCameraChangeReason
    ) -> Bool {
        // Enforce constraint bounds if set (matches Android setLatLngBoundsForCameraTarget behavior)
        guard let bounds = currentConstraintBounds else {
            return true  // No constraints, allow all movements
        }

        // Get the center coordinate of the new camera
        let newCenter = newCamera.centerCoordinate

        // Check if new center is within constraint bounds
        let withinLatBounds = newCenter.latitude >= bounds.sw.latitude && newCenter.latitude <= bounds.ne.latitude
        let withinLngBounds = newCenter.longitude >= bounds.sw.longitude && newCenter.longitude <= bounds.ne.longitude

        if withinLatBounds && withinLngBounds {
            return true  // Movement is within bounds, allow it
        }

        // Camera would move outside bounds - clamp to bounds
        var clampedLat = newCenter.latitude
        var clampedLng = newCenter.longitude

        // Clamp latitude
        if newCenter.latitude < bounds.sw.latitude {
            clampedLat = bounds.sw.latitude
        } else if newCenter.latitude > bounds.ne.latitude {
            clampedLat = bounds.ne.latitude
        }

        // Clamp longitude
        if newCenter.longitude < bounds.sw.longitude {
            clampedLng = bounds.sw.longitude
        } else if newCenter.longitude > bounds.ne.longitude {
            clampedLng = bounds.ne.longitude
        }

        // Apply clamped position
        let clampedCenter = CLLocationCoordinate2D(latitude: clampedLat, longitude: clampedLng)
        mapView.setCenter(clampedCenter, animated: false)

        WWWLog.v(
            Self.tag,
            "Clamped camera from (\(newCenter.latitude),\(newCenter.longitude)) to (\(clampedLat),\(clampedLng))"
        )

        return false  // Prevent the original movement, use clamped position instead
    }

    public func mapView(_ mapView: MLNMapView, regionDidChangeAnimated animated: Bool) {
        // Camera idle event
        WWWLog.v(Self.tag, "Region changed, camera idle")
        onCameraIdle?()

        // Invoke camera idle listener from registry (for adapter's addOnCameraIdleListener)
        if let eventId = eventId {
            Shared.MapWrapperRegistry.shared.invokeCameraIdleListener(eventId: eventId)

            // Update camera position and zoom in adapter (for StateFlow reactive updates)
            Shared.MapWrapperRegistry.shared.updateCameraPosition(
                eventId: eventId,
                latitude: mapView.centerCoordinate.latitude,
                longitude: mapView.centerCoordinate.longitude
            )
            Shared.MapWrapperRegistry.shared.updateCameraZoom(
                eventId: eventId,
                zoom: mapView.zoomLevel
            )

            // Update visible region in registry (for getVisibleRegion calls)
            let bounds = mapView.visibleCoordinateBounds
            let bbox = BoundingBox(
                swLat: bounds.sw.latitude,
                swLng: bounds.sw.longitude,
                neLat: bounds.ne.latitude,
                neLng: bounds.ne.longitude
            )
            Shared.MapWrapperRegistry.shared.updateVisibleRegion(eventId: eventId, bbox: bbox)

            // Update min zoom in registry
            Shared.MapWrapperRegistry.shared.updateMinZoom(eventId: eventId, minZoom: mapView.minimumZoomLevel)

            // Update map dimensions in registry (for padding/bounds calculations)
            Shared.MapWrapperRegistry.shared.updateMapWidth(
                eventId: eventId,
                width: Double(mapView.bounds.size.width)
            )
            Shared.MapWrapperRegistry.shared.updateMapHeight(
                eventId: eventId,
                height: Double(mapView.bounds.size.height)
            )
        }

        // Update accessibility when map region changes
        updateMapAccessibility()
    }

    public func mapViewDidFailLoadingMap(_ mapView: MLNMapView, withError error: Error) {
        WWWLog.e(Self.tag, "❌ FAILED to load map", error: error)
        WWWLog.e(Self.tag, "Error domain: \((error as NSError).domain), code: \((error as NSError).code)")
        WWWLog.e(Self.tag, "Error description: \(error.localizedDescription)")
        WWWLog.e(Self.tag, "Event: \(eventId ?? "unknown")")
    }

    @objc public func mapView(_ mapView: MLNMapView, didFailToLoadImage imageName: String) -> UIImage? {
        WWWLog.w(Self.tag, "⚠️ Failed to load image: \(imageName)")
        WWWLog.w(Self.tag, "Event: \(eventId ?? "unknown")")
        return nil // Return nil to let MapLibre use default/fallback
    }

    public func mapViewWillStartLoadingMap(_ mapView: MLNMapView) {
        WWWLog.i(Self.tag, "🔄 Map WILL START loading for event: \(eventId ?? "unknown")")
        WWWLog.d(Self.tag, "Style URL: \(mapView.styleURL?.absoluteString ?? "nil")")
    }

    public func mapViewDidFinishLoadingMap(_ mapView: MLNMapView) {
        WWWLog.i(Self.tag, "🗺️ Map DID FINISH loading (tiles, layers) for event: \(eventId ?? "unknown")")
        // Note: This is different from didFinishLoading style:
        // - didFinishLoadingMap = all tiles and resources loaded
        // - didFinishLoading style: = style JSON parsed and layers created
    }

    public func mapViewWillStartRenderingFrame(_ mapView: MLNMapView) {
        // Called frequently - only log once
        if !styleIsLoaded {
            WWWLog.d(
                Self.tag,
                "🎬 Map started rendering frames (first frame) for event: \(eventId ?? "unknown")"
            )
        }
    }

    public func mapViewDidFinishRenderingFrame(_ mapView: MLNMapView, fullyRendered: Bool) {
        // Called frequently - only log once
        if !styleIsLoaded {
            WWWLog.i(
                Self.tag,
                "🖼️ Map finished rendering frame, fullyRendered: \(fullyRendered), event: \(eventId ?? "unknown")"
            )
        }
    }

    public func mapViewDidBecomeIdle(_ mapView: MLNMapView) {
        WWWLog.d(Self.tag, "💤 Map became idle for event: \(eventId ?? "unknown")")
    }

    public func mapView(_ mapView: MLNMapView, didSelect annotation: MLNAnnotation) {
        WWWLog.d(Self.tag, "📍 Annotation selected: \(annotation)")
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
    /// Allow our tap gesture to work simultaneously with MapLibre's gestures
    public func gestureRecognizer(
        _ gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
    ) -> Bool {
        // Allow our tap gesture to work alongside MapLibre's gestures
        WWWLog.v(
            Self.tag,
            "Gesture conflict: \(type(of: gestureRecognizer)) vs \(type(of: otherGestureRecognizer))"
        )
        return true
    }

    /// Ensure tap gesture receives touch events
    public func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
        WWWLog.v(Self.tag, "Gesture should receive touch: \(type(of: gestureRecognizer))")
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

// swiftlint:enable file_length type_body_length
