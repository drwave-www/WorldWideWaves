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

// View Model for SettingsView
class SettingsViewModel: ObservableObject {
    @Published var notificationsEnabled: Bool = true
    @Published var locationPermissionGranted: Bool = false
    @Published var useDarkMode: Bool = false
    @Published var useHighQualityMaps: Bool = true
    @Published var useMetricUnits: Bool = true
    @Published var selectedLanguage: String = "English"
    @Published var availableLanguages: [String] = ["English", "Spanish", "French", "German", "Chinese"]

    // Placeholder for future implementation
    func toggleNotifications(_ enabled: Bool) {
        notificationsEnabled = enabled
        // Will implement actual notification settings
    }

    func toggleLocationPermission(_ enabled: Bool) {
        locationPermissionGranted = enabled
        // Will implement actual location permission handling
    }

    func toggleAppearanceMode(_ isDarkMode: Bool) {
        useDarkMode = isDarkMode
        // Will implement actual appearance settings
    }

    func toggleMapQuality(_ isHighQuality: Bool) {
        useHighQualityMaps = isHighQuality
        // Will implement actual map quality settings
    }

    func toggleUnitSystem(_ useMetric: Bool) {
        useMetricUnits = useMetric
        // Will implement actual unit system settings
    }

    func setLanguage(_ language: String) {
        selectedLanguage = language
        // Will implement actual language settings
    }

    func clearCachedData() {
        // Will implement cache clearing functionality
    }
}

struct SettingsView: View {
    @ObservedObject var viewModel: SettingsViewModel
    @State private var showLanguagePicker = false
    @State private var showClearCacheAlert = false

    var body: some View {
        NavigationView {
            List {
                // Notifications Section
                Section(header: Text("Notifications")) {
                    Toggle("Enable Notifications", isOn: $viewModel.notificationsEnabled)
                        .onChange(of: viewModel.notificationsEnabled) { newValue in
                            viewModel.toggleNotifications(newValue)
                        }

                    if viewModel.notificationsEnabled {
                        NavigationLink(destination: NotificationSettingsView()) {
                            Text("Notification Preferences")
                        }
                    }
                }

                // Location Section
                Section(header: Text("Location")) {
                    Toggle("Allow Location Access", isOn: $viewModel.locationPermissionGranted)
                        .onChange(of: viewModel.locationPermissionGranted) { newValue in
                            viewModel.toggleLocationPermission(newValue)
                        }

                    Text("Location access is required for wave participation")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                // Appearance Section
                Section(header: Text("Appearance")) {
                    Toggle("Dark Mode", isOn: $viewModel.useDarkMode)
                        .onChange(of: viewModel.useDarkMode) { newValue in
                            viewModel.toggleAppearanceMode(newValue)
                        }

                    HStack {
                        Text("Language")
                        Spacer()
                        Text(viewModel.selectedLanguage)
                            .foregroundColor(.secondary)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        showLanguagePicker = true
                    }
                }

                // Map Settings Section
                Section(header: Text("Map Settings")) {
                    Toggle("High Quality Maps", isOn: $viewModel.useHighQualityMaps)
                        .onChange(of: viewModel.useHighQualityMaps) { newValue in
                            viewModel.toggleMapQuality(newValue)
                        }

                    Toggle("Use Metric Units", isOn: $viewModel.useMetricUnits)
                        .onChange(of: viewModel.useMetricUnits) { newValue in
                            viewModel.toggleUnitSystem(newValue)
                        }

                    NavigationLink(destination: MapPreferencesView()) {
                        Text("Map Preferences")
                    }
                }

                // Data Management Section
                Section(header: Text("Data Management")) {
                    Button(action: {
                        showClearCacheAlert = true
                    }) {
                        Text("Clear Cached Data")
                            .foregroundColor(.red)
                    }

                    NavigationLink(destination: DownloadedMapsView()) {
                        Text("Manage Downloaded Maps")
                    }
                }

                // About Section
                Section(header: Text("About")) {
                    NavigationLink(destination: PrivacyPolicyView()) {
                        Text("Privacy Policy")
                    }

                    NavigationLink(destination: TermsOfServiceView()) {
                        Text("Terms of Service")
                    }

                    HStack {
                        Text("Version")
                        Spacer()
                        Text("1.0.0")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .listStyle(GroupedListStyle())
            .navigationTitle("Settings")
            .actionSheet(isPresented: $showLanguagePicker) {
                ActionSheet(
                    title: Text("Select Language"),
                    buttons: languageButtons()
                )
            }
            .alert(isPresented: $showClearCacheAlert) {
                Alert(
                    title: Text("Clear Cache"),
                    message: Text("Are you sure you want to clear all cached data? This will not affect your downloaded maps."),
                    primaryButton: .destructive(Text("Clear")) {
                        viewModel.clearCachedData()
                    },
                    secondaryButton: .cancel()
                )
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }

    // Helper function to generate language selection buttons
    private func languageButtons() -> [ActionSheet.Button] {
        var buttons = viewModel.availableLanguages.map { language in
            ActionSheet.Button.default(Text(language)) {
                viewModel.setLanguage(language)
            }
        }
        buttons.append(.cancel())
        return buttons
    }
}

// Placeholder views for navigation destinations
struct NotificationSettingsView: View {
    var body: some View {
        Text("Notification Settings Placeholder")
            .navigationTitle("Notification Settings")
    }
}

struct MapPreferencesView: View {
    var body: some View {
        Text("Map Preferences Placeholder")
            .navigationTitle("Map Preferences")
    }
}

struct DownloadedMapsView: View {
    var body: some View {
        Text("Downloaded Maps Placeholder")
            .navigationTitle("Downloaded Maps")
    }
}

struct PrivacyPolicyView: View {
    var body: some View {
        Text("Privacy Policy Placeholder")
            .navigationTitle("Privacy Policy")
    }
}

struct TermsOfServiceView: View {
    var body: some View {
        Text("Terms of Service Placeholder")
            .navigationTitle("Terms of Service")
    }
}

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        let viewModel = SettingsViewModel()
        SettingsView(viewModel: viewModel)
    }
}
