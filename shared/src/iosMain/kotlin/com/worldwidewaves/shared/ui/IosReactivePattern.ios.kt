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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * iOS implementation of StateFlow to IosObservable conversion
 */
actual fun <T> StateFlow<T>.toIosObservable(): IosObservable<T> = IosStateFlowObservable(this)

/**
 * iOS implementation of Flow to IosObservable conversion
 */
actual fun <T> Flow<T>.toIosObservableFlow(): IosObservable<T> = IosFlowObservable(this)

/**
 * iOS-specific StateFlow observable implementation with automatic cleanup
 *
 * Tracks all active subscriptions and provides cleanup mechanisms to prevent memory leaks.
 * Each subscription creates a coroutine scope that is tracked until disposal.
 *
 * Note: iOS runs on a single-threaded event loop model, so synchronization is not needed
 * for collection access as all operations happen on the main thread.
 */
private class IosStateFlowObservable<T>(
    private val stateFlow: StateFlow<T>,
) : IosObservable<T> {
    private val activeScopes = mutableSetOf<CoroutineScope>()

    override val value: T
        get() = stateFlow.value

    /**
     * Count of currently active subscriptions
     */
    override val activeSubscriptionCount: Int
        get() = activeScopes.size

    override fun observe(callback: (T) -> Unit): IosObservableSubscription {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        activeScopes.add(scope)

        stateFlow
            .onEach { value ->
                callback(value)
            }.launchIn(scope)

        return IosSubscription(
            scope = scope,
            onDispose = {
                activeScopes.remove(scope)
            },
        )
    }

    override suspend fun observeAsync(callback: suspend (T) -> Unit) {
        stateFlow.collect { value ->
            callback(value)
        }
    }

    /**
     * Cleanup all active subscriptions
     * Called by GC or explicitly when the observable is no longer needed
     */
    override fun cleanup() {
        activeScopes.forEach { it.cancel() }
        activeScopes.clear()
    }

    /**
     * Called by Kotlin/Native GC before object is deallocated
     * Ensures all subscriptions are properly cleaned up
     */
    @Suppress("unused")
    protected fun finalize() {
        cleanup()
    }
}

/**
 * iOS-specific Flow observable implementation with automatic cleanup
 *
 * Tracks all active subscriptions and provides cleanup mechanisms to prevent memory leaks.
 * Each subscription creates a coroutine scope that is tracked until disposal.
 *
 * Note: iOS runs on a single-threaded event loop model, so synchronization is not needed
 * for collection access as all operations happen on the main thread.
 */
private class IosFlowObservable<T>(
    private val flow: Flow<T>,
) : IosObservable<T> {
    private var _cachedValue: T? = null
    private val activeScopes = mutableSetOf<CoroutineScope>()

    override val value: T
        get() = _cachedValue ?: error("Flow has not emitted any values yet")

    /**
     * Count of currently active subscriptions
     */
    override val activeSubscriptionCount: Int
        get() = activeScopes.size

    override fun observe(callback: (T) -> Unit): IosObservableSubscription {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        activeScopes.add(scope)

        flow
            .onEach { value ->
                _cachedValue = value
                callback(value)
            }.launchIn(scope)

        return IosSubscription(
            scope = scope,
            onDispose = {
                activeScopes.remove(scope)
            },
        )
    }

    override suspend fun observeAsync(callback: suspend (T) -> Unit) {
        flow.collect { value ->
            _cachedValue = value
            callback(value)
        }
    }

    /**
     * Cleanup all active subscriptions
     * Called by GC or explicitly when the observable is no longer needed
     */
    override fun cleanup() {
        activeScopes.forEach { it.cancel() }
        activeScopes.clear()
    }

    /**
     * Called by Kotlin/Native GC before object is deallocated
     * Ensures all subscriptions are properly cleaned up
     */
    @Suppress("unused")
    protected fun finalize() {
        cleanup()
    }
}

/**
 * iOS-specific subscription implementation with cleanup callback support
 */
private class IosSubscription(
    private val scope: CoroutineScope,
    private val onDispose: (() -> Unit)? = null,
) : IosObservableSubscription {
    private var disposed = false

    override val isActive: Boolean
        get() = !disposed && scope.coroutineContext[kotlinx.coroutines.Job]?.isActive == true

    override fun dispose() {
        if (!disposed) {
            disposed = true
            scope.cancel()
            onDispose?.invoke()
        }
    }
}

/**
 * iOS-specific reactive subscription manager implementation
 */
class IosReactiveSubscriptionManagerImpl : IosReactiveSubscriptionManager {
    private val subscriptions = mutableListOf<IosObservableSubscription>()

    override fun addSubscription(subscription: IosObservableSubscription) {
        subscriptions.add(subscription)
    }

    override fun disposeAll() {
        subscriptions.forEach { it.dispose() }
        subscriptions.clear()
    }

    override val activeSubscriptionCount: Int
        get() = subscriptions.count { it.isActive }
}

/**
 * iOS-specific lifecycle observer implementation
 */
class IosLifecycleObserverImpl(
    private val subscriptionManager: IosReactiveSubscriptionManager,
) : IosLifecycleObserver {
    override fun onViewDidAppear() {
        // iOS View appeared - subscriptions should already be active
        // This can be used for logging or debugging
    }

    override fun onViewDidDisappear() {
        // iOS View disappeared - may want to pause heavy subscriptions
        // For now, keep subscriptions active for background updates
    }

    override fun onViewDeinit() {
        // iOS View is being deallocated - cleanup all subscriptions
        subscriptionManager.disposeAll()
    }
}
