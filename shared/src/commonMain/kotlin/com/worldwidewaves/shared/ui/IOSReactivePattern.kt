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

package com.worldwidewaves.shared.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Cross-platform reactive pattern bridge for iOS UI frameworks.
 *
 * This interface provides a bridge between Kotlin's StateFlow/Flow reactive patterns
 * and iOS UI frameworks like SwiftUI (with @Observable) and UIKit (with Combine).
 *
 * Architecture Impact:
 * - Enables seamless integration of Kotlin reactive state with iOS UI
 * - Provides iOS-compatible observation patterns for SharedViewModel state
 * - Supports iOS memory management patterns for reactive subscriptions
 * - Bridges Kotlin Coroutines with iOS Combine/AsyncSequence patterns
 *
 * Usage Pattern for iOS:
 * ```kotlin
 * // In shared code
 * val stateFlowWrapper = eventsViewModel.events.toIOSObservable()
 *
 * // Consumed in iOS Swift code via bridging
 * stateFlowWrapper.observe { events in
 *     // Update iOS UI
 * }
 * ```
 */
interface IOSObservable<T> {
    /**
     * Current value of the observable state
     * Compatible with iOS @State/@Published patterns
     */
    val value: T

    /**
     * Subscribe to state changes with iOS-compatible callback
     * Returns a subscription handle for cleanup
     */
    fun observe(callback: (T) -> Unit): IOSObservableSubscription

    /**
     * iOS-specific async observation
     * Compatible with Swift AsyncSequence/AsyncStream
     */
    suspend fun observeAsync(callback: suspend (T) -> Unit)
}

/**
 * Subscription handle for iOS-compatible cleanup
 * Follows iOS memory management patterns
 */
interface IOSObservableSubscription {
    /**
     * Dispose/cancel the subscription
     * Should be called in iOS deinit/onDisappear
     */
    fun dispose()

    /**
     * Check if subscription is still active
     */
    val isActive: Boolean
}

/**
 * Convert Kotlin StateFlow to iOS-compatible observable
 * This will be implemented in iOS-specific code
 */
expect fun <T> StateFlow<T>.toIOSObservable(): IOSObservable<T>

/**
 * Convert Kotlin Flow to iOS-compatible observable stream
 * This will be implemented in iOS-specific code
 */
expect fun <T> Flow<T>.toIOSObservableFlow(): IOSObservable<T>

/**
 * iOS-specific lifecycle observer for managing reactive subscriptions
 * Integrates with iOS ViewController/View lifecycle
 */
interface IOSLifecycleObserver {
    /**
     * Called when iOS View appears
     * Used to start reactive subscriptions
     */
    fun onViewDidAppear()

    /**
     * Called when iOS View disappears
     * Used to pause/cleanup reactive subscriptions
     */
    fun onViewDidDisappear()

    /**
     * Called when iOS View is deallocated
     * Used to fully cleanup all reactive resources
     */
    fun onViewDeinit()
}

/**
 * iOS-specific reactive subscription manager
 * Handles automatic cleanup based on iOS lifecycle
 */
interface IOSReactiveSubscriptionManager {
    /**
     * Register a subscription for automatic lifecycle management
     */
    fun addSubscription(subscription: IOSObservableSubscription)

    /**
     * Clean up all managed subscriptions
     */
    fun disposeAll()

    /**
     * Get count of active subscriptions for debugging
     */
    val activeSubscriptionCount: Int
}
