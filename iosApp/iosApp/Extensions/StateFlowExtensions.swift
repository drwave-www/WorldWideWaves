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
import Combine

/// Extension to bridge KMM StateFlow to Swift Combine Publishers
extension Kotlinx_coroutines_coreStateFlow {
    /// Convert StateFlow to Combine Publisher for SwiftUI integration
    func toIOSObservable<T>() -> AnyPublisher<T, Never> {
        // Use the IOSReactivePattern bridge from shared module
        let bridge = IOSReactivePatternKt.toIOSObservable(self)

        return bridge.publisher
            .compactMap { $0 as? T }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()
    }
}

/// Extension to bridge KMM Flow to Swift Combine Publishers
extension Kotlinx_coroutines_coreFlow {
    /// Convert Flow to Combine Publisher for SwiftUI integration
    func toIOSObservableFlow<T>() -> AnyPublisher<T, Never> {
        // Use the IOSReactivePattern bridge from shared module
        let bridge = IOSReactivePatternKt.toIOSObservableFlow(self)

        return bridge.publisher
            .compactMap { $0 as? T }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()
    }
}