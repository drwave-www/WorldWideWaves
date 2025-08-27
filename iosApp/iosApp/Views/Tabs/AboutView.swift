/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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

// View Model for AboutView
class AboutViewModel: ObservableObject {
    @Published var appVersion: String = "1.0.0"
    @Published var appDescription: String = "WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries, culminating in a global wave."
    
    // Placeholder for future implementation
    func loadAboutInformation() {
        // Will be implemented to load information from shared code or resources
    }
}

struct AboutView: View {
    @ObservedObject var viewModel: AboutViewModel
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // App logo
                    Image(uiImage: UIImage(named: "www_logo_transparent")!)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(maxWidth: 200)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.top, 20)
                    
                    // App description
                    Text(viewModel.appDescription)
                        .font(.body)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                        .frame(maxWidth: .infinity, alignment: .center)
                    
                    // Placeholder sections for future content
                    AboutSection(title: "About WorldWideWaves", content: "Placeholder for app description and mission statement.")
                    
                    AboutSection(title: "How It Works", content: "Placeholder for explanation of wave mechanics and participation.")
                    
                    AboutSection(title: "Our Team", content: "Placeholder for team information.")
                    
                    AboutSection(title: "Privacy & Data", content: "Placeholder for privacy information.")
                    
                    // Version information
                    Text("Version \(viewModel.appVersion)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.top, 20)
                }
                .padding()
            }
            .navigationTitle("About")
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

// Helper view for about sections
struct AboutSection: View {
    let title: String
    let content: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)
                .padding(.bottom, 4)
            
            Text(content)
                .font(.body)
                .foregroundColor(.secondary)
            
            Divider()
                .padding(.top, 8)
        }
        .padding(.horizontal)
    }
}

struct AboutView_Previews: PreviewProvider {
    static var previews: some View {
        let viewModel = AboutViewModel()
        AboutView(viewModel: viewModel)
    }
}
