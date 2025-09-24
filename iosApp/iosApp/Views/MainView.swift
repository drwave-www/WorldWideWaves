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

struct MainView: View {
    // Tab selection state
    @State private var selectedTab = 0
    
    private let eventsListView: EventsListView
    private let aboutView: AboutView
    private let settingsView: SettingsView
    
    init() {
        // Initialize Koin DI if not already done
        if KoinKt.getKoin() == nil {
            HelperKt.doInitKoin()
        }

        // Create view-models using enhanced architecture
        let eventsListViewModel = EventsListViewModel()  // Now uses shared EventsViewModel
        let aboutViewModel      = AboutViewModel()
        let settingsViewModel   = SettingsViewModel()

        // Create tab views with their respective view-models
        self.eventsListView = EventsListView(viewModel: eventsListViewModel)
        self.aboutView      = AboutView(viewModel: aboutViewModel)
        self.settingsView   = SettingsView(viewModel: settingsViewModel)
    }
    
    var body: some View {
        TabView(selection: $selectedTab) {
            // Events Tab
            eventsListView
                .tabItem {
                    TabBarIcon(
                        isSelected: selectedTab == 0,
                        normalIcon: "waves_icon",
                        selectedIcon: "waves_icon_selected"
                    )
                    Text("Events")
                }
                .tag(0)
            
            // About Tab
            aboutView
                .tabItem {
                    TabBarIcon(
                        isSelected: selectedTab == 1,
                        normalIcon: "about_icon",
                        selectedIcon: "about_icon_selected"
                    )
                    Text("About")
                }
                .tag(1)
            
            // Settings Tab
            settingsView
                .tabItem {
                    TabBarIcon(
                        isSelected: selectedTab == 2,
                        normalIcon: "settings_icon",
                        selectedIcon: "settings_icon_selected"
                    )
                    Text("Settings")
                }
                .tag(2)
        }
        .accentColor(.blue) // Set the accent color for selected tab
        .onAppear {
            // Configure tab bar appearance
            let appearance = UITabBarAppearance()
            appearance.configureWithOpaqueBackground()
            UITabBar.appearance().standardAppearance = appearance
            UITabBar.appearance().scrollEdgeAppearance = appearance
        }
    }
}

// Helper view for tab bar icons
struct TabBarIcon: View {
    let isSelected: Bool
    let normalIcon: String
    let selectedIcon: String
    
    var body: some View {
        // Use the appropriate icon based on selection state
        Image(isSelected ? selectedIcon : normalIcon)
            .resizable()
            .aspectRatio(contentMode: .fit)
            .frame(height: CGFloat(WWWGlobalsCompanion().DIM_EXT_TABBAR_HEIGHT))
    }
}

// Preview provider
struct MainView_Previews: PreviewProvider {
    static var previews: some View {
        MainView()
    }
}
