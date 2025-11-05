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

import SwiftUI
import MapLibre
import Shared

/// SwiftUI wrapper for MapLibre Native map view
/// Integrates with Kotlin business logic from IOSEventMap
struct EventMapView: UIViewRepresentable {
    private static let tag = "EventMapView"

    let eventId: String
    let styleURL: String
    let enableGestures: Bool  // Whether to enable zoom/scroll gestures (matches Android activateMapGestures)

    @Binding var wrapper: MapLibreViewWrapper?

    func makeUIView(context: Context) -> MLNMapView {
        WWWLog.i(Self.tag, "makeUIView - Creating map view")
        WWWLog.d(Self.tag, "Style URL: \(styleURL)")
        WWWLog.d(Self.tag, "Initial camera position will be set by AbstractEventMap.setupMap()")

        let mapView = MLNMapView(frame: .zero)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]

        // Ensure no content insets (prevents borders/margins in map content)
        mapView.contentInset = .zero
        mapView.automaticallyAdjustsContentInset = false

        WWWLog.d(Self.tag, "Map view created, frame: \(mapView.frame)")

        // Configure gestures (matches Android behavior)
        configureGestures(for: mapView)

        // NOTE: Initial camera position is NOT set here (was hard-coded to Paris)
        // AbstractEventMap.setupMap() will handle initial positioning via moveToWindowBounds(),
        // moveToMapBounds(), or moveToCenter() based on mapConfig.initialCameraPosition
        // This ensures iOS matches Android behavior (event-specific positioning, not hard-coded)

        // Configure style URL
        configureStyleURL(for: mapView)

        // Create and configure wrapper
        _ = createAndConfigureWrapper(for: mapView)

        return mapView
    }

    private func configureGestures(for mapView: MLNMapView) {
        WWWLog.i(Self.tag, "Configuring gestures, enableGestures: \(enableGestures)")

        // Conditional gesture activation (matches Android)
        // Gestures are only enabled when mapConfig.initialCameraPosition == MapCameraPosition.WINDOW
        // CRITICAL: Use correct MLNMapView property names (isZoomEnabled, isScrollEnabled, etc.)
        // Must match MapLibreViewWrapper.swift setGesturesEnabled callback for consistency
        mapView.isZoomEnabled = enableGestures
        mapView.isScrollEnabled = enableGestures

        // Always disable rotation and tilt (matches Android)
        mapView.isRotateEnabled = false
        mapView.isPitchEnabled = false

        // Note: Double-tap to zoom is automatically enabled/disabled with zoomEnabled

        // Remove rotation gesture recognizers if they exist
        if let gestureRecognizers = mapView.gestureRecognizers {
            for recognizer in gestureRecognizers {
                if recognizer is UIRotationGestureRecognizer || recognizer is UIPinchGestureRecognizer {
                    if recognizer is UIRotationGestureRecognizer {
                        mapView.removeGestureRecognizer(recognizer)
                        WWWLog.d(Self.tag, "Removed UIRotationGestureRecognizer")
                    }
                }
            }
        }

        let gestureStatus = "Gestures configured: zoom=\(mapView.isZoomEnabled), " +
            "scroll=\(mapView.isScrollEnabled), " +
            "rotate=\(mapView.isRotateEnabled), " +
            "pitch=\(mapView.isPitchEnabled)"
        WWWLog.i(Self.tag, gestureStatus)
    }

    private func configureStyleURL(for mapView: MLNMapView) {
        let url: URL
        if styleURL.hasPrefix("http://") || styleURL.hasPrefix("https://") {
            guard let remoteURL = URL(string: styleURL) else {
                WWWLog.e(Self.tag, "[ERROR] Invalid remote style URL: \(styleURL)")
                // Fallback to local default style if remote URL is malformed
                url = URL(fileURLWithPath: styleURL)
                WWWLog.w(Self.tag, "Falling back to local style path")
                mapView.styleURL = url
                return
            }
            url = remoteURL
            WWWLog.d(Self.tag, "Using remote style URL: \(url)")
        } else {
            url = URL(fileURLWithPath: styleURL)
            WWWLog.i(Self.tag, "Local file path: \(styleURL)")
            validateStyleFile(at: styleURL)
        }
        mapView.styleURL = url
        WWWLog.i(Self.tag, "[SUCCESS] Style URL set on map view: \(url.absoluteString)")
    }

    private func validateStyleFile(at path: String) {
        let fileManager = FileManager.default
        if fileManager.fileExists(atPath: path) {
            WWWLog.i(Self.tag, "[SUCCESS] Style file EXISTS at path: \(path)")
            if let attributes = try? fileManager.attributesOfItem(atPath: path),
               let fileSize = attributes[.size] as? UInt64 {
                WWWLog.d(Self.tag, "Style file size: \(fileSize) bytes")
            }
        } else {
            WWWLog.e(Self.tag, "[ERROR] Style file DOES NOT EXIST at path: \(path)")
        }
    }

    private func createAndConfigureWrapper(for mapView: MLNMapView) -> MapLibreViewWrapper {
        let mapWrapper = MapLibreViewWrapper()
        mapWrapper.setEventId(eventId)
        mapWrapper.setMapView(mapView)
        WWWLog.d(Self.tag, "Wrapper bound to map view")

        Shared.MapWrapperRegistry.shared.registerWrapper(eventId: eventId, wrapper: mapWrapper)
        WWWLog.d(Self.tag, "Wrapper registered in MapWrapperRegistry for event: \(eventId)")

        DispatchQueue.main.async {
            self.wrapper = mapWrapper
            WWWLog.d(Self.tag, "Wrapper binding updated in main thread")
        }

        return mapWrapper
    }

    func updateUIView(_ mapView: MLNMapView, context: Context) {
        // Check for pending polygons and render them
        _ = IOSMapBridge.renderPendingPolygons(eventId: eventId)

        // Check for pending bbox draw and render it
        _ = IOSMapBridge.renderPendingBbox(eventId: eventId)

        // Check for pending camera commands and execute them
        IOSMapBridge.executePendingCameraCommand(eventId: eventId)
    }
}

/// Preview provider for SwiftUI canvas
struct EventMapView_Previews: PreviewProvider {
    @State static var wrapper: MapLibreViewWrapper?

    static var previews: some View {
        EventMapView(
            eventId: "preview_event",
            styleURL: "https://demotiles.maplibre.org/style.json",
            enableGestures: true,
            wrapper: $wrapper
        )
    }
}
