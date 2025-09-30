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

/// SwiftUI wrapper for MapLibre Native map view
/// Integrates with Kotlin business logic from IOSEventMap
struct EventMapView: UIViewRepresentable {
    let styleURL: String
    let initialLatitude: Double
    let initialLongitude: Double
    let initialZoom: Double

    @Binding var wrapper: MapLibreViewWrapper?

    func makeUIView(context: Context) -> MLNMapView {
        let mapView = MLNMapView(frame: .zero)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]

        // Set initial camera position
        mapView.setCenter(
            CLLocationCoordinate2D(latitude: initialLatitude, longitude: initialLongitude),
            zoomLevel: initialZoom,
            animated: false
        )

        // Set style URL
        if let url = URL(string: styleURL) {
            mapView.styleURL = url
        }

        // Create wrapper and bind to the map view
        let mapWrapper = MapLibreViewWrapper()
        mapWrapper.setMapView(mapView)

        // Update binding
        DispatchQueue.main.async {
            self.wrapper = mapWrapper
        }

        return mapView
    }

    func updateUIView(_ mapView: MLNMapView, context: Context) {
        // Updates handled by wrapper methods called from Kotlin
    }
}

/// Preview provider for SwiftUI canvas
struct EventMapView_Previews: PreviewProvider {
    @State static var wrapper: MapLibreViewWrapper?

    static var previews: some View {
        EventMapView(
            styleURL: "https://demotiles.maplibre.org/style.json",
            initialLatitude: 48.8566,
            initialLongitude: 2.3522,
            initialZoom: 12.0,
            wrapper: $wrapper
        )
    }
}
