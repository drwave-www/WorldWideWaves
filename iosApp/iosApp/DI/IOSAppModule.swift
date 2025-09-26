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

import Foundation
import Shared

// MARK: - iOS App Module

/// Manages dependency injection for iOS-specific components
class IOSAppModule {
    /// Singleton instance of the module
    static let shared = IOSAppModule()

    private init() {
        // Private initializer to enforce singleton pattern
    }

    /// Initialize the iOS-specific dependencies and register them with Koin
    func initialize() {
        // Ensure Koin is initialized from the shared module first
        if KoinKt.getKoin() == nil {
            HelperKt.doInitKoin()
        }

        // Register iOS-specific dependencies with Koin
        registerIOSViewModels()
    }

    /// Register iOS-specific view models with Koin
    private func registerIOSViewModels() {
        let koin = KoinKt.getKoin()

        // Register EventsListViewModel
        koin.registerFactory { koinInstance -> AnyObject in
            guard let wwwEvents = koinInstance.get(objCClass: WWWEvents.self) as? WWWEvents else {
                fatalError("Failed to resolve WWWEvents from Koin")
            }
            return EventsListViewModel(wwwEvents: wwwEvents)
        }

        // Register AboutViewModel
        koin.registerFactory { _ -> AnyObject in
            AboutViewModel()
        }

        // Register SettingsViewModel
        koin.registerFactory { _ -> AnyObject in
            SettingsViewModel()
        }
    }

    // MARK: - Convenience Accessors

    /// Get the EventsListViewModel instance
    func getEventsListViewModel() -> EventsListViewModel {
        guard let viewModel = KoinKt.getKoin().get(objCClass: EventsListViewModel.self) as? EventsListViewModel else {
            fatalError("Failed to resolve EventsListViewModel from Koin")
        }
        return viewModel
    }

    /// Get the AboutViewModel instance
    func getAboutViewModel() -> AboutViewModel {
        guard let viewModel = KoinKt.getKoin().get(objCClass: AboutViewModel.self) as? AboutViewModel else {
            fatalError("Failed to resolve AboutViewModel from Koin")
        }
        return viewModel
    }

    /// Get the SettingsViewModel instance
    func getSettingsViewModel() -> SettingsViewModel {
        guard let viewModel = KoinKt.getKoin().get(objCClass: SettingsViewModel.self) as? SettingsViewModel else {
            fatalError("Failed to resolve SettingsViewModel from Koin")
        }
        return viewModel
    }
}

// MARK: - Koin Extensions

/// Extension to provide Swift-friendly registration methods for Koin
extension Koin {
    /// Register a factory with Koin
    func registerFactory<T: AnyObject>(_ factory: @escaping (Koin) -> T) {
        _koin.registerFactory(createdAtStart: false, qualifier: nil) { koin in
            guard let koinInstance = koin as? Koin else {
                fatalError("Invalid Koin instance")
            }
            return factory(koinInstance)
        }
    }
}
