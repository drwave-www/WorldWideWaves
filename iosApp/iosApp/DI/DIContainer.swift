/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import Foundation
import Shared

/// iOS DI Container to access shared KMM ViewModels and services
class DIContainer {
    static let shared = DIContainer()

    private init() {}

    /// Access the shared EventsViewModel
    lazy var eventsViewModel: EventsViewModel = {
        // Get the shared EventsViewModel from Koin DI
        let koin = KoinKt.getKoin()
        return koin.get(objCClass: EventsViewModel.self) as! EventsViewModel
    }()
}