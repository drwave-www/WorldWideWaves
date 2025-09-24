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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlin.native.concurrent.ThreadLocal

/**
 * iOS implementation of StateFlow to IOSObservable conversion
 */
actual fun <T> StateFlow<T>.toIOSObservable(): IOSObservable<T> {
    return IOSStateFlowObservable(this)
}

/**
 * iOS implementation of Flow to IOSObservable conversion
 */
actual fun <T> Flow<T>.toIOSObservableFlow(): IOSObservable<T> {
    return IOSFlowObservable(this)
}

/**
 * iOS-specific StateFlow observable implementation
 */
private class IOSStateFlowObservable<T>(
    private val stateFlow: StateFlow<T>
) : IOSObservable<T> {

    override val value: T
        get() = stateFlow.value

    override fun observe(callback: (T) -> Unit): IOSObservableSubscription {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        val job = stateFlow
            .onEach { value ->
                callback(value)
            }
            .launchIn(scope)

        return IOSSubscription(scope, job.isActive)
    }

    override suspend fun observeAsync(callback: suspend (T) -> Unit) {
        stateFlow.collect { value ->
            callback(value)
        }
    }
}

/**
 * iOS-specific Flow observable implementation
 */
private class IOSFlowObservable<T>(
    private val flow: Flow<T>
) : IOSObservable<T> {

    @ThreadLocal
    private var _cachedValue: T? = null

    override val value: T
        get() = _cachedValue ?: throw IllegalStateException("Flow has not emitted any values yet")

    override fun observe(callback: (T) -> Unit): IOSObservableSubscription {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        val job = flow
            .onEach { value ->
                _cachedValue = value
                callback(value)
            }
            .launchIn(scope)

        return IOSSubscription(scope, job.isActive)
    }

    override suspend fun observeAsync(callback: suspend (T) -> Unit) {
        flow.collect { value ->
            _cachedValue = value
            callback(value)
        }
    }
}

/**
 * iOS-specific subscription implementation
 */
private class IOSSubscription(
    private val scope: CoroutineScope,
    private var _isActive: Boolean
) : IOSObservableSubscription {

    override val isActive: Boolean
        get() = _isActive

    override fun dispose() {
        if (_isActive) {
            scope.cancel()
            _isActive = false
        }
    }
}

/**
 * iOS-specific reactive subscription manager implementation
 */
class IOSReactiveSubscriptionManagerImpl : IOSReactiveSubscriptionManager {

    private val subscriptions = mutableListOf<IOSObservableSubscription>()

    override fun addSubscription(subscription: IOSObservableSubscription) {
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
class IOSLifecycleObserverImpl(
    private val subscriptionManager: IOSReactiveSubscriptionManager
) : IOSLifecycleObserver {

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