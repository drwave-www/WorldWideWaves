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
import Shared

/// Swift bridging layer for MapLibre Native iOS SDK
/// Provides @objc methods for controlling MapLibre from Kotlin or Swift
// swiftlint:disable type_body_length file_length
@objc public class MapLibreViewWrapper: NSObject {
    private static let tag = "MapLibreWrapper"
    private weak var mapView: MLNMapView?
    private var onStyleLoaded: (() -> Void)?
    private var onMapClick: ((Double, Double) -> Void)?
    private var onCameraIdle: (() -> Void)?
    private var cameraAnimationCallback: MapCameraCallbackWrapper?

    // Track layer and source IDs for cleanup
    private var waveLayerIds: [String] = []
    private var waveSourceIds: [String] = []

    // Timer for continuous polling of camera commands and polygons
    private var commandPollingTimer: Timer?
    private static let pollingInterval: TimeInterval = 0.1 // 100ms

    // Queue for polygons that arrive before style loads
    private var pendingPolygonQueue: [[CLLocationCoordinate2D]] = []
    private var styleIsLoaded: Bool = false

    // MARK: - Accessibility State

    /// Accessibility state tracking for VoiceOver
    private var currentUserPosition: CLLocationCoordinate2D?
    private var currentEventCenter: CLLocationCoordinate2D?
    private var currentEventRadius: Double = 0
    private var currentEventName: String?
    private var currentWavePolygons: [[CLLocationCoordinate2D]] = []

    @objc public override init() {
        super.init()
        WWWLog.d(Self.tag, "Initializing MapLibreViewWrapper")
    }

    deinit {
        WWWLog.d(Self.tag, "Deinitializing MapLibreViewWrapper for event: \(eventId ?? "unknown")")
        stopContinuousPolling()
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
        mapView.addGestureRecognizer(tapGesture)

        WWWLog.i(Self.tag, "👆 Tap gesture added to map view, gestureRecognizers: \(mapView.gestureRecognizers?.count ?? 0)")
        WWWLog.d(Self.tag, "Map view configured successfully for event: \(eventId ?? "nil")")
    }

    @objc public func setEventId(_ eventId: String) {
        self.eventId = eventId
        WWWLog.d(Self.tag, "Event ID set: \(eventId)")
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

        do {
            // cameraThatFitsCoordinateBounds can throw std::domain_error if bounds invalid
            let camera = mapView.cameraThatFitsCoordinateBounds(bounds, edgePadding: edgePadding)
            let timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)

            mapView.setCamera(camera, withDuration: 0.5, animationTimingFunction: timingFunction) { [weak self] in
                WWWLog.d(Self.tag, "✅ Camera animation to bounds completed")
                callback?.onFinish()
                self?.cameraAnimationCallback = nil
            }
        } catch let error as NSError {
            WWWLog.e(Self.tag, "❌ Error calculating camera for bounds: \(error.localizedDescription)")
            callback?.onCancel()
            self.cameraAnimationCallback = nil
        }
    }

    // MARK: - Camera Constraints

    @objc public func setBoundsForCameraTarget(swLat: Double, swLng: Double, neLat: Double, neLng: Double) {
        guard let mapView = mapView else {
            WWWLog.w(Self.tag, "Cannot set bounds - mapView is nil")
            return
        }

        // CRITICAL: Don't set constraint bounds if style not loaded
        // setVisibleCoordinateBounds throws std::domain_error if called before style loads
        guard styleIsLoaded, mapView.style != nil else {
            WWWLog.w(Self.tag, "Cannot set constraint bounds - style not loaded yet (will be set after style loads)")
            return
        }

        // Validate bounds before setting
        guard neLat > swLat else {
            WWWLog.e(Self.tag, "Invalid bounds: neLat (\(neLat)) must be > swLat (\(swLat))")
            return
        }

        // Validate coordinates are within valid ranges
        guard swLat >= -90 && swLat <= 90 && neLat >= -90 && neLat <= 90 &&
              swLng >= -180 && swLng <= 180 && neLng >= -180 && neLng <= 180 else {
            WWWLog.e(Self.tag, "Invalid coordinate values: SW(\(swLat),\(swLng)) NE(\(neLat),\(neLng))")
            return
        }

        WWWLog.i(Self.tag, "Setting camera constraint bounds: SW(\(swLat),\(swLng)) NE(\(neLat),\(neLng))")

        let southwest = CLLocationCoordinate2D(latitude: swLat, longitude: swLng)
        let northeast = CLLocationCoordinate2D(latitude: neLat, longitude: neLng)
        let bounds = MLNCoordinateBounds(sw: southwest, ne: northeast)

        // MapLibre's setVisibleCoordinateBounds can throw C++ std::domain_error
        // Swift can't catch C++ exceptions, so we prevent invalid calls instead
        mapView.setVisibleCoordinateBounds(bounds, animated: false)
        WWWLog.i(Self.tag, "✅ Camera constraint bounds set successfully")
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

    // MARK: - Attribution

    @objc public func setAttributionMargins(left: Int, top: Int, right: Int, bottom: Int) {
        // iOS MapLibre attribution positioning
        // Note: Attribution button position can be adjusted via logoView and attributionButton properties
        mapView?.logoView.isHidden = false
        mapView?.attributionButton.isHidden = false
    }

    // MARK: - Wave Polygons

    @objc public func addWavePolygons(polygons: [[CLLocationCoordinate2D]], clearExisting: Bool) {
        WWWLog.i(Self.tag, "addWavePolygons: \(polygons.count) polygons, clearExisting: \(clearExisting), styleLoaded: \(styleIsLoaded)")

        // If style not loaded yet, queue polygons for later
        guard styleIsLoaded, let mapView = mapView, let style = mapView.style else {
            let hasMap = mapView != nil
            let hasStyle = mapView?.style != nil
            WWWLog.w(Self.tag, "Style not ready - queueing \(polygons.count) polygons (mapView: \(hasMap), style: \(hasStyle))")

            // Queue polygons to render when style loads
            if clearExisting {
                pendingPolygonQueue.removeAll()
            }
            pendingPolygonQueue.append(contentsOf: polygons)
            WWWLog.d(Self.tag, "Polygon queue now contains \(pendingPolygonQueue.count) polygons")
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

        // Call coordinate callback if set
        if let onMapClick = onMapClick {
            WWWLog.d(Self.tag, "Calling onMapClick coordinate callback")
            onMapClick(coordinate.latitude, coordinate.longitude)
        } else {
            WWWLog.v(Self.tag, "No onMapClick coordinate callback set")
        }

        // Invoke map click callback from registry (for navigation)
        if let eventId = eventId {
            WWWLog.i(Self.tag, "Attempting to invoke map click callback for event: \(eventId)")
            let invoked = Shared.MapWrapperRegistry.shared.invokeMapClickCallback(eventId: eventId)
            if invoked {
                WWWLog.i(Self.tag, "✅ Map click callback invoked successfully for event: \(eventId)")
            } else {
                WWWLog.w(Self.tag, "⚠️ Map click callback NOT invoked for event: \(eventId)")
            }
        } else {
            WWWLog.w(Self.tag, "Cannot invoke click callback - eventId is nil")
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
            let distance = calculateDistance(from: userPos, to: eventCenter)
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
    private func calculateDistance(from: CLLocationCoordinate2D, to: CLLocationCoordinate2D) -> Double {
        let fromLocation = CLLocation(latitude: from.latitude, longitude: from.longitude)
        let toLocation = CLLocation(latitude: to.latitude, longitude: to.longitude)
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
        currentUserPosition = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        updateMapAccessibility()
        WWWLog.v(Self.tag, "User position updated for accessibility: \(latitude), \(longitude)")
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

    // MARK: - Continuous Command Polling

    /// Starts continuous polling for camera commands and wave polygons.
    /// Required because Kotlin code executes asynchronously and SwiftUI updateUIView()
    /// is not called frequently enough for immediate command execution.
    ///
    /// Polls every 100ms to:
    /// 1. Execute pending camera commands (auto-following, constraints)
    /// 2. Render pending wave polygons (real-time wave progression)
    private func startContinuousPolling() {
        guard let eventId = eventId else {
            WWWLog.w(Self.tag, "Cannot start polling - eventId is nil")
            return
        }

        WWWLog.i(Self.tag, "Starting continuous command polling for event: \(eventId)")

        commandPollingTimer = Timer.scheduledTimer(withTimeInterval: Self.pollingInterval, repeats: true) { [weak self] _ in
            guard let self = self, let eventId = self.eventId else { return }

            // Execute pending camera commands (constraints, auto-following)
            IOSMapBridge.executePendingCameraCommand(eventId: eventId)

            // Render pending wave polygons (real-time progression)
            IOSMapBridge.renderPendingPolygons(eventId: eventId)
        }

        WWWLog.d(Self.tag, "Polling timer started (interval: \(Self.pollingInterval * 1000)ms)")
    }

    /// Stops continuous polling and invalidates the timer.
    private func stopContinuousPolling() {
        if let timer = commandPollingTimer {
            timer.invalidate()
            commandPollingTimer = nil
            WWWLog.d(Self.tag, "Polling timer stopped for event: \(eventId ?? "unknown")")
        }
    }
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

        // IMMEDIATE EXECUTION: Execute all pending commands now that map is ready
        WWWLog.i(Self.tag, "⚡ Executing pending commands immediately after style load...")

        // 1. Render queued polygons that arrived before style loaded
        if !pendingPolygonQueue.isEmpty {
            WWWLog.i(Self.tag, "📦 Flushing polygon queue: \(pendingPolygonQueue.count) polygons")
            addWavePolygons(polygons: pendingPolygonQueue, clearExisting: true)
            pendingPolygonQueue.removeAll()
        }

        // 2. Execute pending camera commands (initial positioning, constraints)
        WWWLog.d(Self.tag, "Checking for pending camera commands...")
        IOSMapBridge.executePendingCameraCommand(eventId: eventId)

        // 3. Render pending wave polygons from registry (initial wave state)
        WWWLog.d(Self.tag, "Checking for pending polygons in registry...")
        IOSMapBridge.renderPendingPolygons(eventId: eventId)

        // START CONTINUOUS POLLING: For dynamic updates (auto-following, wave progression)
        WWWLog.i(Self.tag, "🔄 Starting continuous polling for dynamic updates...")
        startContinuousPolling()

        WWWLog.i(Self.tag, "✅ Map initialization complete for event: \(eventId)")
    }

    public func mapView(_ mapView: MLNMapView, regionDidChangeAnimated animated: Bool) {
        // Camera idle event
        WWWLog.v(Self.tag, "Region changed, camera idle")
        onCameraIdle?()

        // Update accessibility when map region changes
        updateMapAccessibility()
    }

    public func mapViewDidFailLoadingMap(_ mapView: MLNMapView, withError error: Error) {
        WWWLog.e(Self.tag, "Failed to load map", error: error)
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
