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
    // Step 3: Add proper loading states and event loading
    @State private var selectedTab = 0
    @State private var eventCount: Int = 0
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
                        Text("Found \(eventCount) events from shared module")
                            .font(.subheadline)
                            .foregroundColor(.blue)
                            .padding(.bottom)

                        List {
                            ForEach(0..<max(1, eventCount)) { index in
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("Event \(index + 1)")
                                        .font(.headline)
                                    Text("Real event from shared module")
                                        .font(.subheadline)
                                        .foregroundColor(.secondary)
                                    HStack {
                                        Text("Status: Loaded from KMM")
                                            .font(.caption)
                                            .padding(4)
                                            .background(Color.green.opacity(0.3))
                                            .cornerRadius(4)
                                        Spacer()
                                    }
                                }
                                .padding(.vertical, 4)
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
                        self.eventCount = self.wwwEvents.list().count
                        self.isLoading = false
                        print("iOS: Successfully loaded \(self.eventCount) events from shared module")
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
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
