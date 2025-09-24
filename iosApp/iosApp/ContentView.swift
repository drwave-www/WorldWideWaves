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
                                    AndroidEventRow(eventId: events[index])
                                }
                                .listRowSeparator(.hidden)
                                .listRowBackground(Color.clear)
                            }
                        }
                        .refreshable {
                            loadEventsWithStates()
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

                // Wave button section with exact Android ButtonWave styling
                NavigationLink(destination: WaveView(eventId: eventId)) {
                    Text("Wave Now")
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(width: 120, height: 44)
                        .background(Color.blue)
                        .cornerRadius(6)
                        .overlay(
                            // Blinking animation matching Android
                            Rectangle()
                                .fill(Color.blue)
                                .opacity(0.3)
                                .cornerRadius(6)
                                .animation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true), value: UUID())
                        )
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

// Wave Participation Screen - Matching Android WaveActivity
struct WaveView: View {
    let eventId: String
    @State private var waveProgress: Double = 0.0
    @State private var isWaveActive: Bool = false
    @State private var userHit: Bool = false

    var body: some View {
        ZStack {
            // Background gradient matching Android
            LinearGradient(
                gradient: Gradient(colors: [Color.blue.opacity(0.8), Color.purple.opacity(0.6)]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: 30) {
                // Event title
                Text(formatEventName(eventId))
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .padding(.top, 20)

                // Wave progress indicator (matching Android)
                VStack(spacing: 16) {
                    Text("Wave Progress")
                        .font(.title2)
                        .foregroundColor(.white)

                    ZStack {
                        Circle()
                            .stroke(Color.white.opacity(0.3), lineWidth: 8)
                            .frame(width: 150, height: 150)

                        Circle()
                            .trim(from: 0, to: waveProgress)
                            .stroke(Color.white, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                            .frame(width: 150, height: 150)
                            .rotationEffect(.degrees(-90))
                            .animation(.easeInOut(duration: 0.5), value: waveProgress)

                        Text("\(Int(waveProgress * 100))%")
                            .font(.title)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                    }
                }

                // Wave status (matching Android choreography states)
                VStack(spacing: 12) {
                    if userHit {
                        Text("🎉 Wave Hit!")
                            .font(.title)
                            .fontWeight(.bold)
                            .foregroundColor(.yellow)
                    } else if isWaveActive {
                        Text("🌊 Wave In Progress")
                            .font(.title2)
                            .foregroundColor(.white)
                        Text("Get ready for the wave!")
                            .font(.body)
                            .foregroundColor(.white.opacity(0.9))
                    } else {
                        Text("⏳ Waiting for Wave")
                            .font(.title2)
                            .foregroundColor(.white)
                        Text("Wave will start soon...")
                            .font(.body)
                            .foregroundColor(.white.opacity(0.9))
                    }
                }

                Spacer()

                // Action buttons (matching Android)
                VStack(spacing: 16) {
                    Button(action: {
                        startWaveSimulation()
                    }) {
                        HStack {
                            Image(systemName: "play.circle.fill")
                            Text("Start Wave Demo")
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.white)
                        .foregroundColor(.blue)
                        .cornerRadius(12)
                    }
                    .padding(.horizontal)

                    Button(action: {
                        print("Leave wave for \(eventId)")
                    }) {
                        Text("Leave Wave")
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.red.opacity(0.8))
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }
                    .padding(.horizontal)
                }
                .padding(.bottom, 30)
            }
        }
        .navigationTitle("Wave")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            startWaveSimulation()
        }
    }

    private func formatEventName(_ id: String) -> String {
        return id.replacingOccurrences(of: "_", with: " ").capitalized
    }

    private func startWaveSimulation() {
        isWaveActive = true

        // Simulate wave progression
        Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { timer in
            waveProgress += 0.02

            if waveProgress >= 0.8 && !userHit {
                userHit = true
                print("iOS Wave hit for \(eventId)!")
            }

            if waveProgress >= 1.0 {
                timer.invalidate()
                isWaveActive = false
            }
        }
    }
}

// Android Event Row - Exact Android Event structure
struct AndroidEventRow: View {
    let eventId: String

    var body: some View {
        VStack(spacing: 0) {
            // EventOverlay - 160dp height with background image
            AndroidEventOverlay(eventId: eventId)
                .frame(height: 160) // OVERLAY_HEIGHT = 160dp

            // EventLocationAndDate - exact Android layout
            AndroidEventLocationAndDate(eventId: eventId)
        }
    }
}

// Android Event Overlay - Exact match to Android EventOverlay
struct AndroidEventOverlay: View {
    let eventId: String

    var body: some View {
        ZStack {
            // Background image matching Android getLocationImage()
            Rectangle()
                .fill(getEventGradientColor(eventId, 0))
                .overlay(
                    Rectangle()
                        .fill(getEventGradientColor(eventId, 1))
                        .opacity(0.6)
                )

            // Overlays matching Android structure
            VStack {
                HStack {
                    // Country flag (FLAG_WIDTH = 65dp)
                    Image(systemName: "flag.fill")
                        .foregroundColor(.white.opacity(0.8))
                        .frame(width: 65)
                        .padding(.leading, 8)

                    Spacer()

                    // Status overlay (SOON/RUNNING)
                    EventStatusOverlay(eventId: eventId)
                        .padding(.trailing, 8)
                }
                .padding(.top, 8)

                Spacer()
            }
        }
    }

    private func getEventGradientColor(_ eventId: String, _ index: Int) -> Color {
        switch eventId {
        case let id where id.contains("new_york"):
            return index == 0 ? Color.blue.opacity(0.8) : Color.purple.opacity(0.6)
        case let id where id.contains("los_angeles"):
            return index == 0 ? Color.orange.opacity(0.8) : Color.red.opacity(0.6)
        case let id where id.contains("mexico"):
            return index == 0 ? Color.green.opacity(0.8) : Color.yellow.opacity(0.6)
        default:
            return index == 0 ? Color.blue.opacity(0.7) : Color.purple.opacity(0.5)
        }
    }
}

// Android Event Location and Date - Exact match to Android EventLocationAndDate
struct AndroidEventLocationAndDate: View {
    let eventId: String

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Row 1: Location + Date - EXACT Android layout
            HStack {
                Text(formatEventName(eventId))
                    .font(.system(size: 26)) // EVENT_LOCATION_FONTSIZE = 26
                    .foregroundColor(.primary)
                Spacer()
                Text("Dec 24")
                    .font(.system(size: 30)) // EVENT_DATE_FONTSIZE = 30
                    .fontWeight(.bold)
                    .foregroundColor(.blue)
                    .padding(.trailing, 2) // end 2dp
            }

            // Row 2: Country / Community with -8dp offset - EXACT Android
            HStack(alignment: .center, spacing: 0) {
                Text(getCountryName(eventId))
                    .font(.system(size: 18)) // EVENT_COUNTRY_FONTSIZE = 18
                    .foregroundColor(.secondary)
                    .offset(y: -8) // -8dp offset
                    .padding(.leading, 2)

                Text(" / ")
                    .font(.system(size: 18))
                    .foregroundColor(.secondary)
                    .offset(y: -8)
                    .padding(.leading, 2)

                Text(getCommunityName(eventId))
                    .font(.system(size: 16)) // EVENT_COMMUNITY_FONTSIZE = 16
                    .foregroundColor(.quaternary)
                    .offset(y: -8)
                    .padding(.leading, 2)

                Spacer()
            }
            .padding(.top, 5) // 5dp top padding
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }

    private func formatEventName(_ id: String) -> String {
        return id.replacingOccurrences(of: "_", with: " ").capitalized
    }

    private func getCountryName(_ eventId: String) -> String {
        let components = eventId.components(separatedBy: "_")
        return components.last?.uppercased() ?? "Unknown"
    }

    private func getCommunityName(_ eventId: String) -> String {
        let components = eventId.components(separatedBy: "_")
        if components.count >= 2 {
            return components.dropLast().joined(separator: " ").capitalized
        }
        return "Community"
    }
}

// Event Status Overlay - Matching Android EventOverlaySoonOrRunning
struct EventStatusOverlay: View {
    let eventId: String

    var body: some View {
        // Simulate event status based on city (matching Android logic)
        let status = getEventStatus(eventId)

        if status != "none" {
            Text(status.uppercased())
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.white)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(getStatusColor(status))
                .cornerRadius(4)
                .padding(.top, 8)
                .padding(.trailing, 8)
        }
    }

    private func getEventStatus(_ eventId: String) -> String {
        // Simulate status based on event (matching Android behavior)
        switch eventId {
        case let id where id.contains("new_york"):
            return "soon"
        case let id where id.contains("paris"):
            return "running"
        case let id where id.contains("tokyo"):
            return "done"
        default:
            return "none"
        }
    }

    private func getStatusColor(_ status: String) -> Color {
        switch status {
        case "soon":
            return Color.orange
        case "running":
            return Color.green
        case "done":
            return Color.gray
        default:
            return Color.blue
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
