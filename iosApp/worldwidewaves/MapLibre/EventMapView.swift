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
    let initialLatitude: Double
    let initialLongitude: Double
    let initialZoom: Double

    @Binding var wrapper: MapLibreViewWrapper?

    func makeUIView(context: Context) -> MLNMapView {
        WWWLog.i(Self.tag, "makeUIView - Creating map view")
        WWWLog.d(Self.tag, "Style URL: \(styleURL)")
        WWWLog.d(Self.tag, "Initial position: lat=\(initialLatitude), lng=\(initialLongitude), zoom=\(initialZoom)")

        let mapView = MLNMapView(frame: .zero)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]

        WWWLog.d(Self.tag, "Map view created, frame: \(mapView.frame)")

        // Set initial camera position
        let coordinate = CLLocationCoordinate2D(latitude: initialLatitude, longitude: initialLongitude)
        mapView.setCenter(coordinate, zoomLevel: initialZoom, animated: false)
        WWWLog.d(Self.tag, "Camera position set")

        // Set style URL
        if let url = URL(string: styleURL) {
            mapView.styleURL = url
            WWWLog.d(Self.tag, "Style URL set on map view")
        } else {
            WWWLog.e(Self.tag, "Invalid style URL: \(styleURL)")
        }

        // Create wrapper and bind to the map view
        let mapWrapper = MapLibreViewWrapper()
        mapWrapper.setEventId(eventId)
        mapWrapper.setMapView(mapView)
        WWWLog.d(Self.tag, "Wrapper bound to map view")

        // Register wrapper in Kotlin registry for later access
        Shared.MapWrapperRegistry.shared.registerWrapper(eventId: eventId, wrapper: mapWrapper)
        WWWLog.d(Self.tag, "Wrapper registered in MapWrapperRegistry for event: \(eventId)")

        // Update binding
        DispatchQueue.main.async {
            self.wrapper = mapWrapper
            WWWLog.d(Self.tag, "Wrapper binding updated in main thread")
        }

        return mapView
    }

    func updateUIView(_ mapView: MLNMapView, context: Context) {
        // Check for pending polygons and render them
        IOSMapBridge.renderPendingPolygons(eventId: eventId)
    }
}

/// Preview provider for SwiftUI canvas
struct EventMapView_Previews: PreviewProvider {
    @State static var wrapper: MapLibreViewWrapper?

    static var previews: some View {
        EventMapView(
            eventId: "preview_event",
            styleURL: "https://demotiles.maplibre.org/style.json",
            initialLatitude: 48.8566,
            initialLongitude: 2.3522,
            initialZoom: 12.0,
            wrapper: $wrapper
        )
    }
}
