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
import MapKit

// View Model for EventView
class EventViewModel: ObservableObject {
    @Published var event: WWWEvent
    @Published var isParticipating: Bool = false
    @Published var countdownString: String = "--:--:--"
    @Published var eventStatus: WWWEventStatus
    @Published var showWaveView: Bool = false
    
    init(event: WWWEvent) {
        self.event = event
        self.eventStatus = event.status
        updateCountdown()
    }
    
    func updateCountdown() {
        // Placeholder for countdown logic
        // Will be implemented to calculate time until event starts
        countdownString = "00:30:00"
    }
    
    func toggleParticipation() {
        isParticipating.toggle()
        // Will implement actual participation logic
    }
    
    func joinWave() {
        showWaveView = true
        // Will implement wave joining logic
    }
    
    func formattedDate(_ date: Kotlinx_datetimeLocalDateTime) -> String {
        return "\(date.dayOfMonth)/\(date.monthNumber)/\(date.year) \(date.hour):\(String(format: "%02d", date.minute))"
    }
}

struct EventView: View {
    @ObservedObject var viewModel: EventViewModel
    @State private var showFullMap: Bool = false
    @Environment(\.presentationMode) var presentationMode
    
    init(event: WWWEvent) {
        self.viewModel = EventViewModel(event: event)
    }
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Event header
                VStack(alignment: .leading, spacing: 8) {
                    Text(viewModel.event.name)
                        .font(.largeTitle)
                        .fontWeight(.bold)
                    
                    Text(viewModel.event.location)
                        .font(.headline)
                        .foregroundColor(.secondary)
                    
                    HStack {
                        Text("Date: \(viewModel.formattedDate(viewModel.event.date))")
                            .font(.subheadline)
                        
                        Spacer()
                        
                        Text(viewModel.event.status.name)
                            .font(.subheadline)
                            .padding(6)
                            .background(statusColor(viewModel.event.status))
                            .cornerRadius(4)
                    }
                }
                .padding()
                
                // Map preview (tappable to open full map)
                ZStack(alignment: .bottomTrailing) {
                    // Placeholder map view
                    Rectangle()
                        .fill(Color.gray.opacity(0.2))
                        .frame(height: 200)
                        .overlay(
                            Text("Event Map")
                                .font(.headline)
                        )
                        .onTapGesture {
                            showFullMap = true
                        }
                    
                    Button(action: {
                        showFullMap = true
                    }) {
                        Text("Full Map")
                            .font(.caption)
                            .padding(8)
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                    .padding(12)
                }
                
                // Countdown section
                VStack(spacing: 8) {
                    Text("Event starts in:")
                        .font(.headline)
                    
                    Text(viewModel.countdownString)
                        .font(.system(size: 36, weight: .bold, design: .monospaced))
                        .foregroundColor(.blue)
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.blue.opacity(0.1))
                .cornerRadius(12)
                .padding(.horizontal)
                
                // Event description
                VStack(alignment: .leading, spacing: 8) {
                    Text("About this event")
                        .font(.headline)
                    
                    Text(viewModel.event.description_)
                        .font(.body)
                        .foregroundColor(.secondary)
                }
                .padding()
                
                // Participation section
                VStack(spacing: 16) {
                    if viewModel.isParticipating {
                        Button(action: {
                            viewModel.joinWave()
                        }) {
                            Text("Join Wave Now")
                                .font(.headline)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.blue)
                                .cornerRadius(12)
                        }
                        .padding(.horizontal)
                        
                        Button(action: {
                            viewModel.toggleParticipation()
                        }) {
                            Text("Cancel Participation")
                                .font(.subheadline)
                                .foregroundColor(.red)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.red.opacity(0.1))
                                .cornerRadius(12)
                        }
                        .padding(.horizontal)
                    } else {
                        Button(action: {
                            viewModel.toggleParticipation()
                        }) {
                            Text("Participate in this Wave")
                                .font(.headline)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.blue)
                                .cornerRadius(12)
                        }
                        .padding(.horizontal)
                    }
                }
                .padding(.vertical)
                
                Spacer(minLength: 40)
            }
        }
        .navigationTitle("Event Details")
        .navigationBarTitleDisplayMode(.inline)
        .background(
            NavigationLink(
                destination: EventFullMapView(event: viewModel.event),
                isActive: $showFullMap,
                label: { EmptyView() }
            )
        )
        .background(
            NavigationLink(
                destination: WaveView(event: viewModel.event),
                isActive: $viewModel.showWaveView,
                label: { EmptyView() }
            )
        )
    }
    
    // Helper function to determine status color
    private func statusColor(_ status: WWWEventStatus) -> Color {
        switch status {
        case .upcoming:
            return Color.blue.opacity(0.3)
        case .active:
            return Color.green.opacity(0.3)
        case .completed:
            return Color.gray.opacity(0.3)
        default:
            return Color.gray.opacity(0.3)
        }
    }
}

// Placeholder for EventFullMapView
struct EventFullMapView: View {
    let event: WWWEvent
    
    var body: some View {
        Text("Full Map View - To be implemented")
            .navigationTitle("Event Map")
    }
}

// Placeholder for WaveView
struct WaveView: View {
    let event: WWWEvent
    
    var body: some View {
        Text("Wave View - To be implemented")
            .navigationTitle("Wave")
    }
}

struct EventView_Previews: PreviewProvider {
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
        
        NavigationView {
            EventView(event: event)
        }
    }
}
