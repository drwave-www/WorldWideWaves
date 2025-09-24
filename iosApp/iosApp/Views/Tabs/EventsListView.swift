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
    @Published var events: [any IWWWEvent] = []
    @Published var isLoading: Bool = false
    @Published var hasLoadingError: Bool = false
    @Published var hasFavorites: Bool = false

    private let sharedViewModel: EventsViewModel
    private var cancellables: Set<AnyCancellable> = []

    init() {
        // Initialize Koin DI if not already done
        if KoinKt.getKoin() == nil {
            HelperKt.doInitKoin()
        }

        // Initialize shared EventsViewModel with DI
        self.sharedViewModel = DIContainer.shared.eventsViewModel
        setupStateObservers()
    }

    private func setupStateObservers() {
        // Observe shared ViewModel StateFlows using iOS reactive bridge
        sharedViewModel.events.toIOSObservable().sink { [weak self] events in
            DispatchQueue.main.async {
                self?.events = events
            }
        }.store(in: &cancellables)

        sharedViewModel.isLoading.toIOSObservable().sink { [weak self] loading in
            DispatchQueue.main.async {
                self?.isLoading = loading
            }
        }.store(in: &cancellables)

        sharedViewModel.hasLoadingError.toIOSObservable().sink { [weak self] error in
            DispatchQueue.main.async {
                self?.hasLoadingError = error
            }
        }.store(in: &cancellables)

        sharedViewModel.hasFavorites.toIOSObservable().sink { [weak self] favorites in
            DispatchQueue.main.async {
                self?.hasFavorites = favorites
            }
        }.store(in: &cancellables)
    }

    func filterEvents(onlyFavorites: Bool = false, onlyDownloaded: Bool = false) {
        sharedViewModel.filterEvents(onlyFavorites: onlyFavorites, onlyDownloaded: onlyDownloaded)
    }

    deinit {
        cancellables.removeAll()
    }
}

struct EventsListView: View {
    @ObservedObject var viewModel: EventsListViewModel
    @State private var selectedEvent: (any IWWWEvent)?
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
    let event: any IWWWEvent

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Event title (using ID for now, will be enhanced)
            Text(event.id.replacingOccurrences(of: "_", with: " ").capitalized)
                .font(.headline)

            // Event location/description placeholder
            Text("Wave event in \(event.id.replacingOccurrences(of: "_", with: " "))")
                .font(.subheadline)
                .foregroundColor(.secondary)

            HStack {
                // Status indicator
                Text("Status: \(statusText(event.status))")
                    .font(.caption)
                    .padding(4)
                    .background(statusColor(event.status))
                    .cornerRadius(4)
                Spacer()

                // Event type indicator
                Text("Wave Event")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 8)
    }

    // Helper function to get status text
    private func statusText(_ status: IWWWEventStatus?) -> String {
        guard let status = status else { return "Unknown" }

        switch status {
        case .upcomingValue:
            return "Upcoming"
        case .soonValue:
            return "Soon"
        case .runningValue:
            return "Running"
        case .doneValue:
            return "Done"
        default:
            return "Unknown"
        }
    }

    // Helper function to determine status color
    private func statusColor(_ status: IWWWEventStatus?) -> Color {
        guard let status = status else { return Color.gray.opacity(0.3) }

        switch status {
        case .upcomingValue:
            return Color.blue.opacity(0.3)
        case .soonValue:
            return Color.orange.opacity(0.3)
        case .runningValue:
            return Color.green.opacity(0.3)
        case .doneValue:
            return Color.gray.opacity(0.3)
        default:
            return Color.gray.opacity(0.3)
        }
    }
}

// Placeholder for the event detail view
struct EventDetailView: View {
    let event: (any IWWWEvent)?

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
