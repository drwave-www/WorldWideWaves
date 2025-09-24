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
    @State private var selectedEvent: WWWEvent? = nil
    @State private var navigateToEventDetail = false
    
    var body: some View {
        NavigationView {
            List {
                ForEach(viewModel.events, id: \.id) { event in
                    EventRow(event: event)
                        .onTapGesture {
                            selectedEvent = event
                            navigateToEventDetail = true
                        }
                }
            }
            .navigationTitle("Events")
            .refreshable {
                viewModel.refreshEvents()
            }
            .background(
                NavigationLink(
                    destination: EventDetailView(event: selectedEvent),
                    isActive: $navigateToEventDetail,
                    label: { EmptyView() }
                )
                .hidden()
            )
        }
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
        return "\(date.dayOfMonth)/\(date.monthNumber)/\(date.year) \(date.hour):\(String(format: "%02d", date.minute))"
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
            VStack {
                Text(event.name)
                    .font(.title)
                Text(event.location)
                    .font(.headline)
                
                // Placeholder for future implementation
                Text("Event details will be implemented")
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color.gray.opacity(0.1))
                    .cornerRadius(8)
                    .padding()
                
                Spacer()
            }
            .padding()
            .navigationTitle("Event Details")
        } else {
            Text("No event selected")
        }
    }
}

struct EventsListView_Previews: PreviewProvider {
    static var previews: some View {
        let viewModel = EventsListViewModel()
        EventsListView(viewModel: viewModel)
    }
}
