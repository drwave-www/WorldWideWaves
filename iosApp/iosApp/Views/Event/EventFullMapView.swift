/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
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
import Shared
// MapLibre will be imported here when integrated
// import MapLibre

// ViewModel for EventFullMapView
class EventFullMapViewModel: ObservableObject {
    @Published var event: WWWEvent
    @Published var isWaveVisible: Bool = true
    @Published var mapZoomLevel: Double = 14.0
    @Published var isFollowingUser: Bool = false
    @Published var showWaveDetails: Bool = false
    
    // Properties for map interaction
    @Published var centerCoordinate: (Double, Double)
    
    init(event: WWWEvent) {
        self.event = event
        self.centerCoordinate = (event.coordinates.latitude, event.coordinates.longitude)
    }
    
    // Function to toggle wave visibility
    func toggleWaveVisibility() {
        isWaveVisible.toggle()
    }
    
    // Function to toggle user location tracking
    func toggleLocationTracking() {
        isFollowingUser.toggle()
    }
    
    // Function to zoom in
    func zoomIn() {
        mapZoomLevel = min(mapZoomLevel + 1.0, 18.0)
    }
    
    // Function to zoom out
    func zoomOut() {
        mapZoomLevel = max(mapZoomLevel - 1.0, 10.0)
    }
    
    // Function to reset map view to event location
    func resetMapView() {
        centerCoordinate = (event.coordinates.latitude, event.coordinates.longitude)
        mapZoomLevel = 14.0
    }
}

struct EventFullMapView: View {
    @ObservedObject var viewModel: EventFullMapViewModel
    @Environment(\.presentationMode) var presentationMode
    
    // State for UI interaction
    @State private var showLegend: Bool = false
    @State private var showSettings: Bool = false
    
    init(event: WWWEvent) {
        self.viewModel = EventFullMapViewModel(event: event)
    }
    
    var body: some View {
        ZStack(alignment: .top) {
            // Map placeholder - will be replaced with MapLibre implementation
            mapPlaceholder
                .edgesIgnoringSafeArea(.all)
            
            // Navigation bar
            VStack {
                HStack {
                    // Back button
                    Button(action: {
                        presentationMode.wrappedValue.dismiss()
                    }) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.primary)
                            .padding(12)
                            .background(Color.white.opacity(0.8))
                            .clipShape(Circle())
                            .shadow(radius: 2)
                    }
                    
                    Spacer()
                    
                    // Event title
                    Text(viewModel.event.name)
                        .font(.headline)
                        .padding(.vertical, 8)
                        .padding(.horizontal, 16)
                        .background(Color.white.opacity(0.8))
                        .cornerRadius(20)
                        .shadow(radius: 2)
                    
                    Spacer()
                    
                    // Settings button
                    Button(action: {
                        showSettings.toggle()
                    }) {
                        Image(systemName: "gear")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.primary)
                            .padding(12)
                            .background(Color.white.opacity(0.8))
                            .clipShape(Circle())
                            .shadow(radius: 2)
                    }
                }
                .padding()
                
                Spacer()
            }
            
            // Map controls overlay (right side)
            VStack {
                Spacer()
                
                VStack(spacing: 16) {
                    // Zoom in button
                    mapControlButton(icon: "plus", action: viewModel.zoomIn)
                    
                    // Zoom out button
                    mapControlButton(icon: "minus", action: viewModel.zoomOut)
                    
                    // Location tracking button
                    mapControlButton(
                        icon: viewModel.isFollowingUser ? "location.fill" : "location",
                        action: viewModel.toggleLocationTracking,
                        isActive: viewModel.isFollowingUser
                    )
                    
                    // Reset map view button
                    mapControlButton(icon: "arrow.counterclockwise", action: viewModel.resetMapView)
                    
                    // Toggle wave visibility button
                    mapControlButton(
                        icon: viewModel.isWaveVisible ? "eye" : "eye.slash",
                        action: viewModel.toggleWaveVisibility,
                        isActive: viewModel.isWaveVisible
                    )
                    
                    // Show legend button
                    mapControlButton(icon: "info.circle", action: { showLegend.toggle() })
                }
                .padding(.vertical, 12)
                .padding(.horizontal, 8)
                .background(Color.white.opacity(0.8))
                .cornerRadius(12)
                .shadow(radius: 2)
                .padding()
            }
            .frame(maxWidth: .infinity, alignment: .trailing)
            
            // Wave information overlay (bottom)
            VStack {
                Spacer()
                
                waveInfoOverlay
                    .padding(.bottom)
            }
            
            // Legend sheet
            if showLegend {
                legendOverlay
            }
            
            // Settings sheet
            if showSettings {
                mapSettingsOverlay
            }
        }
        .navigationBarHidden(true)
    }
    
    // MARK: - UI Components
    
    // Map placeholder (will be replaced with actual MapLibre implementation)
    private var mapPlaceholder: some View {
        ZStack {
            Color.gray.opacity(0.2)
            
            // Event location marker
            VStack {
                Image(systemName: "mappin.circle.fill")
                    .font(.system(size: 40))
                    .foregroundColor(.red)
                
                Text("Event Location")
                    .font(.caption)
                    .padding(6)
                    .background(Color.white.opacity(0.8))
                    .cornerRadius(8)
            }
            
            // Wave visualization placeholder
            if viewModel.isWaveVisible {
                Circle()
                    .stroke(Color.blue.opacity(0.5), lineWidth: 3)
                    .frame(width: 200, height: 200)
            }
            
            // Placeholder text
            Text("MapLibre integration pending")
                .font(.caption)
                .foregroundColor(.secondary)
                .position(x: 100, y: 30)
        }
    }
    
    // Map control button helper
    private func mapControlButton(icon: String, action: @escaping () -> Void, isActive: Bool = false) -> some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 18, weight: .medium))
                .foregroundColor(isActive ? .blue : .primary)
                .frame(width: 44, height: 44)
                .background(Color.white)
                .clipShape(Circle())
                .shadow(radius: 1)
        }
    }
    
    // Wave information overlay
    private var waveInfoOverlay: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(viewModel.event.name)
                    .font(.headline)
                
                Spacer()
                
                Button(action: {
                    viewModel.showWaveDetails.toggle()
                }) {
                    Image(systemName: "chevron.up")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(.secondary)
                        .rotationEffect(viewModel.showWaveDetails ? .degrees(180) : .degrees(0))
                }
            }
            
            if viewModel.showWaveDetails {
                Divider()
                
                HStack {
                    VStack(alignment: .leading) {
                        Text("Wave Radius")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("\(String(format: "%.1f", viewModel.event.waveRadius)) km")
                            .font(.subheadline)
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .leading) {
                        Text("Participants")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("\(viewModel.event.participants)")
                            .font(.subheadline)
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .leading) {
                        Text("Status")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(viewModel.event.status.name)
                            .font(.subheadline)
                    }
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(radius: 2)
        .padding(.horizontal)
    }
    
    // Legend overlay
    private var legendOverlay: some View {
        VStack {
            Spacer()
            
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Text("Map Legend")
                        .font(.headline)
                    
                    Spacer()
                    
                    Button(action: {
                        showLegend = false
                    }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.secondary)
                    }
                }
                
                Divider()
                
                legendItem(color: .red, symbol: "mappin.circle.fill", label: "Event Center")
                legendItem(color: .blue.opacity(0.5), symbol: "circle", label: "Wave Radius")
                legendItem(color: .green, symbol: "person.fill", label: "Participants")
                legendItem(color: .orange, symbol: "arrow.clockwise", label: "Wave Direction")
            }
            .padding()
            .background(Color.white)
            .cornerRadius(12)
            .shadow(radius: 4)
            .padding()
        }
        .background(Color.black.opacity(0.3).edgesIgnoringSafeArea(.all))
        .onTapGesture {
            showLegend = false
        }
    }
    
    // Legend item helper
    private func legendItem(color: Color, symbol: String, label: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .foregroundColor(color)
                .font(.system(size: 20))
            
            Text(label)
                .font(.body)
            
            Spacer()
        }
    }
    
    // Map settings overlay
    private var mapSettingsOverlay: some View {
        VStack {
            Spacer()
            
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Text("Map Settings")
                        .font(.headline)
                    
                    Spacer()
                    
                    Button(action: {
                        showSettings = false
                    }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.secondary)
                    }
                }
                
                Divider()
                
                Toggle("Show Wave Visualization", isOn: $viewModel.isWaveVisible)
                Toggle("Follow My Location", isOn: $viewModel.isFollowingUser)
                
                HStack {
                    Text("Map Zoom Level")
                    Spacer()
                    Text(String(format: "%.1f", viewModel.mapZoomLevel))
                        .foregroundColor(.secondary)
                }
                
                Slider(value: $viewModel.mapZoomLevel, in: 10...18, step: 0.5)
                
                Button(action: viewModel.resetMapView) {
                    Text("Reset Map View")
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }
            }
            .padding()
            .background(Color.white)
            .cornerRadius(12)
            .shadow(radius: 4)
            .padding()
        }
        .background(Color.black.opacity(0.3).edgesIgnoringSafeArea(.all))
        .onTapGesture {
            showSettings = false
        }
    }
}

struct EventFullMapView_Previews: PreviewProvider {
    static var previews: some View {
        // Create a sample event for preview
        let event = WWWEvent(
            id: "1",
            name: "Sample Wave Event",
            description_: "This is a sample wave event for preview purposes.",
            location: "San Francisco, USA",
            date: Kotlinx_datetimeLocalDateTime(
                year: 2025,
                monthNumber: 7,
                monthName: "July",
                dayOfMonth: 15,
                dayOfWeek: "Wednesday",
                hour: 18,
                minute: 30,
                second: 0,
                nanosecond: 0
            ),
            status: WWWEventStatus.upcoming,
            coordinates: WWWCoordinates(latitude: 37.7749, longitude: -122.4194),
            participants: 1500,
            waveRadius: 2.5
        )
        
        EventFullMapView(event: event)
    }
}
