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

struct ContentView: View {
    // Step 4: Show actual event data, not just count
    @State private var selectedTab = 0
    @State private var events: [String] = [] // Start with event IDs as strings
    @State private var isLoading: Bool = true
    @State private var hasError: Bool = false
    @State private var errorMessage: String = ""
    private let wwwEvents: WWWEvents

    // KMM - Koin Call
    init() {
        HelperKt.doInitKoin()
        self.wwwEvents = WWWEvents()
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            // Events Tab - Simple version first
            NavigationView {
                VStack {
                    Text("📋 Events List")
                        .font(.title)
                        .padding()

                    // Loading and error states
                    if isLoading {
                        VStack {
                            ProgressView("Loading events from KMM...")
                                .padding()
                            Text("Connecting to shared business logic...")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                    } else if hasError {
                        VStack {
                            Text("❌ Error Loading Events")
                                .font(.headline)
                                .foregroundColor(.red)
                            Text(errorMessage)
                                .font(.caption)
                                .foregroundColor(.gray)
                            Button("Retry") {
                                loadEventsWithStates()
                            }
                            .padding()
                        }
                    } else {
                        Text("Found \(events.count) real events from shared module")
                            .font(.subheadline)
                            .foregroundColor(.blue)
                            .padding(.bottom)

                        List {
                            ForEach(events.indices, id: \.self) { index in
                                NavigationLink(destination: EventDetailView(eventId: events[index])) {
                                    VStack(alignment: .leading, spacing: 12) {
                                        // Top row: Location (left) and Date (right) - matching Android
                                        HStack {
                                            Text(formatEventName(events[index]))
                                                .font(.title3)
                                                .fontWeight(.medium)
                                            Spacer()
                                            Text("Dec 24")
                                                .font(.headline)
                                                .fontWeight(.bold)
                                                .foregroundColor(.blue)
                                        }

                                        // Bottom row: Country / Community - matching Android
                                        HStack {
                                            Text(getCountryName(events[index]))
                                                .font(.subheadline)
                                                .foregroundColor(.secondary)
                                            Text(" / ")
                                                .font(.subheadline)
                                                .foregroundColor(.secondary)
                                            Text(getCommunityName(events[index]))
                                                .font(.subheadline)
                                                .foregroundColor(.secondary)
                                            Spacer()
                                        }
                                        .padding(.top, -8)
                                    }
                                    .padding(.vertical, 12)
                                }
                                .listRowBackground(Color.clear)
                            }
                        }
                    }
                }
                .navigationTitle("Events")
                .refreshable {
                    loadEventsWithStates()
                }
            }
            .tabItem {
                Image(systemName: "water.waves")
                Text("Events")
            }
            .tag(0)

            // About Tab - Simple
            NavigationView {
                VStack(spacing: 20) {
                    Text("ℹ️ About")
                        .font(.title)
                    Text("WorldWideWaves")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                    Text("Orchestrating human waves across cities and countries")
                        .font(.body)
                        .multilineTextAlignment(.center)
                        .padding()
                    Spacer()
                }
                .navigationTitle("About")
                .padding()
            }
            .tabItem {
                Image(systemName: "info.circle")
                Text("About")
            }
            .tag(1)

            // Settings Tab - Simple
            NavigationView {
                VStack(spacing: 20) {
                    Text("⚙️ Settings")
                        .font(.title)
                    Text("App Settings")
                        .font(.headline)
                    Text("Settings functionality will be implemented")
                        .font(.body)
                        .foregroundColor(.secondary)
                    Spacer()
                }
                .navigationTitle("Settings")
                .padding()
            }
            .tabItem {
                Image(systemName: "gear")
                Text("Settings")
            }
            .tag(2)
        }
        .accentColor(.blue)
        .onAppear {
            loadEventsWithStates()
        }
    }

    private func loadEventsWithStates() {
        isLoading = true
        hasError = false
        errorMessage = ""

        // Simulate loading time and then load events
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            // Trigger event loading from shared module with proper callbacks
            _ = self.wwwEvents.loadEvents(
                onLoaded: {
                    DispatchQueue.main.async {
                        // Extract actual event IDs from shared module
                        let eventList = self.wwwEvents.list()
                        self.events = eventList.map { $0.id }
                        self.isLoading = false
                        print("iOS: Successfully loaded \(self.events.count) real events: \(self.events.prefix(3).joined(separator: ", "))...")
                    }
                },
                onLoadingError: { error in
                    DispatchQueue.main.async {
                        self.hasError = true
                        self.errorMessage = error.message ?? "Unknown loading error"
                        self.isLoading = false
                        print("iOS: Error loading events - \(self.errorMessage)")
                    }
                }
            )
        }
    }

    private func formatEventName(_ id: String) -> String {
        return id.replacingOccurrences(of: "_", with: " ").capitalized
    }

    private func formatLocationName(_ id: String) -> String {
        return id.replacingOccurrences(of: "_", with: " ")
    }

    private func getCountryName(_ eventId: String) -> String {
        // Extract country from event ID (e.g., "new_york_usa" -> "USA")
        let components = eventId.components(separatedBy: "_")
        return components.last?.uppercased() ?? "Unknown"
    }

    private func getCommunityName(_ eventId: String) -> String {
        // Extract community name from event ID
        let components = eventId.components(separatedBy: "_")
        if components.count >= 2 {
            return components.dropLast().joined(separator: " ").capitalized
        }
        return "Community"
    }
}

// Event Detail Screen - Matching Android EventActivity design
struct EventDetailView: View {
    let eventId: String

    var body: some View {
        VStack(spacing: 0) {
            // Event overlays section (matching Android)
            ZStack {
                Rectangle()
                    .fill(Color.gray.opacity(0.1))
                    .frame(height: 200)

                VStack {
                    Text(formatEventName(eventId))
                        .font(.title)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)

                    Text("Wave Event")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }

            // Event description section (matching Android)
            VStack(alignment: .leading, spacing: 16) {
                Text("Event Description")
                    .font(.headline)
                    .padding(.horizontal)

                Text("Experience the wave in \(formatLocationName(eventId)). Join thousands of participants in this synchronized human wave event that transcends physical and cultural boundaries.")
                    .font(.body)
                    .padding(.horizontal)

                // Divider line (matching Android DividerLine)
                Rectangle()
                    .fill(Color.gray.opacity(0.3))
                    .frame(height: 1)
                    .padding(.horizontal)

                // Wave button section (matching Android ButtonWave)
                HStack {
                    Spacer()
                    Button(action: {
                        // Navigate to WaveActivity equivalent
                        print("Navigate to wave for \(eventId)")
                    }) {
                        HStack {
                            Image(systemName: "waveform.path.ecg")
                            Text("Wave Now")
                        }
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                    }
                    Spacer()
                }
                .padding()

                // Map button section with navigation
                NavigationLink(destination: EventMapView(eventId: eventId)) {
                    HStack {
                        Image(systemName: "map")
                        Text("View Map")
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color.green)
                    .foregroundColor(.white)
                    .cornerRadius(8)
                }
                .padding(.horizontal)

                Spacer()
            }
            .padding(.top)
        }
        .navigationTitle("Event")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func formatEventName(_ id: String) -> String {
        return id.replacingOccurrences(of: "_", with: " ").capitalized
    }

    private func formatLocationName(_ id: String) -> String {
        return id.replacingOccurrences(of: "_", with: " ")
    }
}

// Event Map Screen - Matching Android EventFullMapActivity
struct EventMapView: View {
    let eventId: String

    var body: some View {
        ZStack {
            // Map placeholder (iOS MapKit integration)
            Rectangle()
                .fill(
                    LinearGradient(
                        gradient: Gradient(colors: [Color.blue.opacity(0.3), Color.green.opacity(0.3)]),
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .ignoresSafeArea()

            VStack {
                // Map overlay with event info (matching Android)
                VStack(spacing: 8) {
                    Text("🗺️ Event Map")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)

                    Text(formatEventName(eventId))
                        .font(.headline)
                        .foregroundColor(.white)

                    Text("Event location will be displayed here")
                        .font(.subheadline)
                        .foregroundColor(.white.opacity(0.9))
                }
                .padding()
                .background(Color.black.opacity(0.7))
                .cornerRadius(12)
                .padding(.top, 50)

                Spacer()

                // Bottom action buttons (matching Android map actions)
                HStack(spacing: 16) {
                    Button(action: {
                        print("Center on event location for \(eventId)")
                    }) {
                        HStack {
                            Image(systemName: "location")
                            Text("Center")
                        }
                        .padding()
                        .background(Color.white)
                        .foregroundColor(.blue)
                        .cornerRadius(8)
                    }

                    Button(action: {
                        print("Start wave for \(eventId)")
                    }) {
                        HStack {
                            Image(systemName: "waveform.path.ecg")
                            Text("Join Wave")
                        }
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                    }
                }
                .padding(.bottom, 40)
            }
        }
        .navigationTitle("Map")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func formatEventName(_ id: String) -> String {
        return id.replacingOccurrences(of: "_", with: " ").capitalized
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
