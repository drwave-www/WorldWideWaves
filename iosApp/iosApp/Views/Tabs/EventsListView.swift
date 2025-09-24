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
import Combine

// iOS ViewModel wrapper for shared EventsViewModel
class EventsListViewModel: ObservableObject {
    @Published var events: [WWWEvent] = []
    @Published var isLoading: Bool = false
    @Published var hasLoadingError: Bool = false

    private let wwwEvents: WWWEvents

    init(wwwEvents: WWWEvents = WWWEvents()) {
        self.wwwEvents = wwwEvents
        loadEvents()
    }

    func loadEvents() {
        isLoading = true
        hasLoadingError = false

        // Use the existing WWWEvents for now to avoid integration complexity
        events = wwwEvents.events()
        isLoading = false
    }

    func refreshEvents() {
        wwwEvents.loadEvents {
            DispatchQueue.main.async {
                self.events = self.wwwEvents.events()
                self.isLoading = false
            }
        }
    }
}

struct EventsListView: View {
    @ObservedObject var viewModel: EventsListViewModel
    @State private var selectedEvent: WWWEvent?
    @State private var navigateToEventDetail = false

    var body: some View {
        NavigationView {
            if viewModel.isLoading && viewModel.events.isEmpty {
                // Loading state
                VStack {
                    ProgressView("Loading events...")
                        .padding()
                    Spacer()
                }
            } else if viewModel.hasLoadingError && viewModel.events.isEmpty {
                // Error state
                VStack {
                    Text("Failed to load events")
                        .font(.headline)
                        .foregroundColor(.red)
                    Text("Please try again")
                        .font(.caption)
                    Button("Retry") {
                        // Trigger reload - will be implemented
                    }
                    .padding()
                    Spacer()
                }
            } else {
                // Events list
                List {
                    ForEach(viewModel.events, id: \.id) { event in
                        EventRow(event: event)
                            .onTapGesture {
                                selectedEvent = event
                                navigateToEventDetail = true
                            }
                    }
                }
                .refreshable {
                    // Pull to refresh - will be implemented
                }
                .background(
                    NavigationLink(
                        destination: EventDetailView(event: selectedEvent),
                        isActive: $navigateToEventDetail
                    )                        { EmptyView() }
                    .hidden()
                )
            }
        }
        .navigationTitle("Events")
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

// Row item for each event in the list
struct EventRow: View {
    let event: WWWEvent

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(event.name)
                .font(.headline)

            Text(event.location)
                .font(.subheadline)
                .foregroundColor(.secondary)

            HStack {
                Text("Date: \(formattedDate(event.date))")
                    .font(.caption)
                Spacer()
                Text("Status: \(event.status.name)")
                    .font(.caption)
                    .padding(4)
                    .background(statusColor(event.status))
                    .cornerRadius(4)
            }
        }
        .padding(.vertical, 8)
    }

    // Helper function to format the date
    private func formattedDate(_ date: Kotlinx_datetimeLocalDateTime) -> String {
        "\(date.dayOfMonth)/\(date.monthNumber)/\(date.year) \(date.hour):\(String(format: "%02d", date.minute))"
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

// Placeholder for the event detail view
struct EventDetailView: View {
    let event: WWWEvent?

    var body: some View {
        if let event = event {
            VStack(alignment: .leading, spacing: 16) {
                // Event header
                VStack(alignment: .leading, spacing: 8) {
                    Text(event.id.replacingOccurrences(of: "_", with: " ").capitalized)
                        .font(.largeTitle)
                        .fontWeight(.bold)

                    Text("Wave Event Location")
                        .font(.title2)
                        .foregroundColor(.secondary)
                }

                Divider()

                // Event details section
                VStack(alignment: .leading, spacing: 12) {
                    Label("Event Information", systemImage: "info.circle")
                        .font(.headline)

                    Text("This is a WorldWideWaves event where participants create synchronized human waves across the city.")
                        .font(.body)

                    Text("Join thousands of others in this unique experience that transcends physical and cultural boundaries!")
                        .font(.body)
                        .italic()
                }

                Divider()

                // Action section placeholder
                VStack(alignment: .leading, spacing: 12) {
                    Label("Actions", systemImage: "hand.raised")
                        .font(.headline)

                    Button(action: {
                        // Wave action - to be implemented
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
                }

                Spacer()
            }
            .padding()
            .navigationTitle("Event Details")
            .navigationBarTitleDisplayMode(.inline)
        } else {
            VStack {
                Image(systemName: "exclamationmark.circle")
                    .font(.system(size: 50))
                    .foregroundColor(.gray)
                Text("No event selected")
                    .font(.title2)
                    .foregroundColor(.gray)
            }
        }
    }
}

struct EventsListView_Previews: PreviewProvider {
    static var previews: some View {
        let viewModel = EventsListViewModel()
        EventsListView(viewModel: viewModel)
    }
}
