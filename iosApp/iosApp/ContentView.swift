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
    private let wwwEvents: WWWEvents

    // KMM - Koin Call
    init() {
        HelperKt.doInitKoin()
        self.wwwEvents = WWWEvents()
    }
    
    var body: some View {
        VStack {
            if isLoading {
                ProgressView("Loading events…")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = errorMessage {
                VStack(spacing: 8) {
                    Text("Failed to load events")
                        .font(.headline)
                    Text(error)
                        .font(.caption)
                        .multilineTextAlignment(.center)
                }
                .padding()
            } else {
                List(events, id: \.id) { event in
                    Text(event.getLocation().localized())
                }
            }
        }
        .onAppear {
            // Trigger loading only if we have no events yet and we're not already loading.
            if events.isEmpty && !isLoading {
                loadEvents()
            }
        }
    }
    
    // MARK: - Private state & helpers

    @State
    private var events: [any IWWWEvent] = []
    
    @State
    private var isLoading: Bool = false
    
    @State
    private var errorMessage: String? = nil
    
    /// Loads events using the shared `WWWEvents` instance.
    private func loadEvents() {
        // Set initial loading state
        isLoading = true
        errorMessage = nil
        
        _ = wwwEvents.loadEvents(
            onLoaded: {
                DispatchQueue.main.async {
                    self.events = self.wwwEvents.list()
                    self.isLoading = false
                }
            },
            onLoadingError: { error in
                DispatchQueue.main.async {
                    self.errorMessage = error.message ?? "Unknown error"
                    self.isLoading = false
                }
            }
        )
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
