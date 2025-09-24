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
                                    VStack(alignment: .leading, spacing: 8) {
                                        Text(formatEventName(events[index]))
                                            .font(.headline)
                                        Text("Wave event in \(formatLocationName(events[index]))")
                                            .font(.subheadline)
                                            .foregroundColor(.secondary)
                                        HStack {
                                            Text("Event ID: \(events[index])")
                                                .font(.caption)
                                                .padding(4)
                                                .background(Color.blue.opacity(0.3))
                                                .cornerRadius(4)
                                            Spacer()
                                            Image(systemName: "chevron.right")
                                                .foregroundColor(.gray)
                                        }
                                    }
                                    .padding(.vertical, 4)
                                }
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
}

// Event Detail Screen
struct EventDetailView: View {
    let eventId: String

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Event header
                VStack(alignment: .leading, spacing: 8) {
                    Text(formatEventName(eventId))
                        .font(.largeTitle)
                        .fontWeight(.bold)

                    Text("Wave Event Location")
                        .font(.title2)
                        .foregroundColor(.secondary)
                }
                .padding(.top)

                Divider()

                // Event information
                VStack(alignment: .leading, spacing: 12) {
                    Label("Event Information", systemImage: "info.circle")
                        .font(.headline)

                    Text("This is a WorldWideWaves event where participants create synchronized human waves across \(formatLocationName(eventId)).")
                        .font(.body)

                    Text("Join thousands of others in this unique experience that transcends physical and cultural boundaries!")
                        .font(.body)
                        .italic()
                }

                Divider()

                // Action section
                VStack(alignment: .leading, spacing: 12) {
                    Label("Participate", systemImage: "hand.raised")
                        .font(.headline)

                    Button(action: {
                        // Wave action - to be implemented
                        print("Join wave for \(eventId)")
                    }) {
                        HStack {
                            Image(systemName: "waveform")
                            Text("Join Wave")
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                    }

                    Button(action: {
                        // Map action - to be implemented
                        print("View map for \(eventId)")
                    }) {
                        HStack {
                            Image(systemName: "map")
                            Text("View on Map")
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                    }
                }

                Spacer()
            }
            .padding()
        }
        .navigationTitle("Event Details")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func formatEventName(_ id: String) -> String {
        return id.replacingOccurrences(of: "_", with: " ").capitalized
    }

    private func formatLocationName(_ id: String) -> String {
        return id.replacingOccurrences(of: "_", with: " ")
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
