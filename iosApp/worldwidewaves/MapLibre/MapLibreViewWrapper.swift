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
import Mapbox
import UIKit

/// Swift bridging layer for MapLibre Native iOS SDK
/// Provides Kotlin-friendly API for IOSMapLibreAdapter
@objc public class MapLibreViewWrapper: NSObject {
    private weak var mapView: MGLMapView?
    private var onStyleLoaded: (() -> Void)?
    private var onMapClick: ((Double, Double) -> Void)?
    private var onCameraIdle: (() -> Void)?
    private var cameraAnimationCallback: MapCameraCallbackWrapper?

    // Track layer and source IDs for cleanup
    private var waveLayerIds: [String] = []
    private var waveSourceIds: [String] = []

    @objc public override init() {
        super.init()
    }

    // MARK: - Map Setup

    @objc public func setMapView(_ mapView: MGLMapView) {
        self.mapView = mapView
        self.mapView?.delegate = self

        // Add tap gesture recognizer for map clicks
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleMapTap(_:)))
        self.mapView?.addGestureRecognizer(tapGesture)
    }

    @objc public func setStyle(styleURL: String, completion: @escaping () -> Void) {
        guard let mapView = mapView else {
            print("⚠️ iOS MapLibre: Cannot set style - mapView is nil")
            return
        }

        self.onStyleLoaded = completion

        if let url = URL(string: styleURL) {
            mapView.styleURL = url
        } else {
            print("❌ iOS MapLibre: Invalid style URL: \(styleURL)")
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

    @objc public func getCameraCenter() -> (latitude: Double, longitude: Double)? {
        guard let center = mapView?.centerCoordinate else { return nil }
        return (center.latitude, center.longitude)
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

    @objc public func moveCamera(latitude: Double, longitude: Double, zoom: Double?) {
        guard let mapView = mapView else { return }

        let coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        mapView.setCenter(coordinate, animated: false)

        if let zoom = zoom {
            mapView.zoomLevel = zoom
        }
    }

    @objc public func animateCamera(
        latitude: Double,
        longitude: Double,
        zoom: Double?,
        callback: MapCameraCallbackWrapper?
    ) {
        guard let mapView = mapView else {
            callback?.onCancel()
            return
        }

        self.cameraAnimationCallback = callback

        let coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        let targetZoom = zoom ?? mapView.zoomLevel

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

    @objc public func animateCameraToBounds(
        swLat: Double,
        swLng: Double,
        neLat: Double,
        neLng: Double,
        padding: Int,
        callback: MapCameraCallbackWrapper?
    ) {
        guard let mapView = mapView else {
            callback?.onCancel()
            return
        }

        self.cameraAnimationCallback = callback

        let southwest = CLLocationCoordinate2D(latitude: swLat, longitude: swLng)
        let northeast = CLLocationCoordinate2D(latitude: neLat, longitude: neLng)
        let bounds = MGLCoordinateBounds(sw: southwest, ne: northeast)

        let edgePadding = UIEdgeInsets(
            top: CGFloat(padding),
            left: CGFloat(padding),
            bottom: CGFloat(padding),
            right: CGFloat(padding)
        )

        let camera = mapView.cameraThatFitsCoordinateBounds(bounds, edgePadding: edgePadding)
        let timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)

        mapView.setCamera(camera, withDuration: 0.5, animationTimingFunction: timingFunction) { [weak self] in
            callback?.onFinish()
            self?.cameraAnimationCallback = nil
        }
    }

    // MARK: - Camera Constraints

    @objc public func setBoundsForCameraTarget(swLat: Double, swLng: Double, neLat: Double, neLng: Double) {
        guard let mapView = mapView else { return }

        let southwest = CLLocationCoordinate2D(latitude: swLat, longitude: swLng)
        let northeast = CLLocationCoordinate2D(latitude: neLat, longitude: neLng)
        let bounds = MGLCoordinateBounds(sw: southwest, ne: northeast)

        mapView.setVisibleCoordinateBounds(bounds, animated: false)
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
        guard let mapView = mapView, let style = mapView.style else {
            print("⚠️ iOS MapLibre: Cannot add polygons - style not loaded")
            return
        }

        // Clear existing polygons if requested
        if clearExisting {
            clearWavePolygons()
        }

        // Add each polygon as a separate source and layer
        for (index, coordinates) in polygons.enumerated() {
            let sourceId = "wave-polygons-source-\(index)-\(UUID().uuidString)"
            let layerId = "wave-polygons-layer-\(index)-\(UUID().uuidString)"

            // Create polygon shape
            let polygon = MGLPolygon(coordinates: coordinates, count: UInt(coordinates.count))

            // Create source
            let source = MGLShapeSource(identifier: sourceId, shape: polygon, options: nil)
            style.addSource(source)

            // Create fill layer with wave styling
            let fillLayer = MGLFillStyleLayer(identifier: layerId, source: source)
            fillLayer.fillColor = NSExpression(forConstantValue: UIColor(hex: "#00008B"))
            fillLayer.fillOpacity = NSExpression(forConstantValue: 0.20)

            style.addLayer(fillLayer)

            // Track for cleanup
            waveSourceIds.append(sourceId)
            waveLayerIds.append(layerId)
        }
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
    }

    // MARK: - Override BBox Drawing

    @objc public func drawOverrideBbox(swLat: Double, swLng: Double, neLat: Double, neLng: Double) {
        guard let style = mapView?.style else { return }

        let southwest = CLLocationCoordinate2D(latitude: swLat, longitude: swLng)
        let northeast = CLLocationCoordinate2D(latitude: neLat, longitude: neLng)
        let northwest = CLLocationCoordinate2D(latitude: neLat, longitude: swLng)
        let southeast = CLLocationCoordinate2D(latitude: swLat, longitude: neLng)

        var coordinates = [southwest, southeast, northeast, northwest, southwest]
        let polyline = MGLPolyline(coordinates: &coordinates, count: UInt(coordinates.count))

        let source = MGLShapeSource(identifier: "bbox-override-source", shape: polyline, options: nil)
        style.addSource(source)

        let lineLayer = MGLLineStyleLayer(identifier: "bbox-override-line", source: source)
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
        guard let mapView = mapView else { return }
        let point = gesture.location(in: mapView)
        let coordinate = mapView.convert(point, toCoordinateFrom: mapView)
        onMapClick?(coordinate.latitude, coordinate.longitude)
    }
}

// MARK: - MGLMapViewDelegate

extension MapLibreViewWrapper: MGLMapViewDelegate {
    public func mapView(_ mapView: MGLMapView, didFinishLoading style: MGLStyle) {
        print("✅ iOS MapLibre: Style loaded successfully")
        onStyleLoaded?()
        onStyleLoaded = nil
    }

    public func mapView(_ mapView: MGLMapView, regionDidChangeAnimated animated: Bool) {
        // Camera idle event
        onCameraIdle?()
    }

    public func mapViewDidFailLoadingMap(_ mapView: MGLMapView, withError error: Error) {
        print("❌ iOS MapLibre: Failed to load map: \(error.localizedDescription)")
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
