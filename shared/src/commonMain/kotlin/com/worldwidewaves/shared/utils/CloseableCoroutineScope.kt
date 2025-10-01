package com.worldwidewaves.shared.utils

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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * A lifecycle-aware coroutine scope with resource cleanup capabilities.
 *
 * ## Purpose
 * Provides structured concurrency with explicit lifecycle management and cleanup hooks:
 * - Cancels all launched coroutines when closed
 * - Executes registered cleanup actions before cancellation
 * - Uses Main dispatcher for UI-bound operations
 * - Uses SupervisorJob to prevent child failures from canceling siblings
 *
 * ## Usage Pattern
 * ```kotlin
 * class MyComponent {
 *     private val scope = CloseableCoroutineScope()
 *
 *     fun start() {
 *         // Launch coroutines on the scope
 *         scope.launch {
 *             updateUI()
 *         }
 *
 *         // Register cleanup actions
 *         scope.registerForCleanup {
 *             releaseResources()
 *             unregisterListeners()
 *         }
 *     }
 *
 *     fun stop() {
 *         // Cleanup actions run first, then coroutines are cancelled
 *         scope.close()
 *     }
 * }
 * ```
 *
 * ## Lifecycle
 * When [close] is called:
 * 1. All registered cleanup actions execute in registration order
 * 2. The SupervisorJob is cancelled, cancelling all child coroutines
 * 3. The scope becomes inactive (launching new coroutines will fail)
 *
 * ## Cleanup Actions
 * Use [registerForCleanup] to register actions that should run before cancellation:
 * - Releasing native resources (file handles, database connections)
 * - Unregistering listeners or callbacks
 * - Saving state
 * - Logging or telemetry
 *
 * **Important**: Cleanup actions are regular synchronous functions, not suspending.
 * For suspending cleanup, use `scope.launch` with `try-finally` instead.
 *
 * ## Thread Safety
 * - **Not thread-safe**: Should be used from a single thread (typically Main)
 * - **Main dispatcher**: All coroutines launched on this scope use Dispatchers.Main
 * - **Cleanup order**: Actions execute in registration order (FIFO)
 *
 * ## Comparison with Other Scopes
 * - **viewModelScope**: Tied to ViewModel lifecycle, auto-cancelled on clear
 * - **lifecycleScope**: Tied to Android Lifecycle, auto-cancelled on destroy
 * - **CloseableCoroutineScope**: Manual lifecycle, explicit cleanup hooks, platform-agnostic
 *
 * ## Testing
 * For tests, inject a test dispatcher:
 * ```kotlin
 * // Note: Current implementation hardcodes Dispatchers.Main
 * // Consider making dispatcher configurable for testing
 * @Test
 * fun testComponent() = runTest {
 *     val component = MyComponent()
 *     component.start()
 *     // Perform test actions
 *     component.stop()
 *     // Verify cleanup occurred
 * }
 * ```
 *
 * @see CoroutineScope for base scope interface
 * @see SupervisorJob for exception isolation
 */
class CloseableCoroutineScope : CoroutineScope {
    private val job = SupervisorJob()

    /**
     * Coroutine context using Main dispatcher and SupervisorJob.
     *
     * - **Main dispatcher**: Suitable for UI updates
     * - **SupervisorJob**: Child failures don't affect siblings
     */
    override val coroutineContext = job + Dispatchers.Main

    private val cleanupActions = mutableListOf<() -> Unit>()

    /**
     * Registers a cleanup action to be executed when [close] is called.
     *
     * Actions are executed in registration order (FIFO) before the scope is cancelled.
     * Use this for releasing resources, unregistering listeners, or saving state.
     *
     * **Note**: Actions must be synchronous. For suspending cleanup, use scope.launch
     * with try-finally instead.
     *
     * @param action Synchronous cleanup action to execute on close
     */
    fun registerForCleanup(action: () -> Unit) {
        cleanupActions.add(action)
    }

    /**
     * Executes all cleanup actions and cancels the scope.
     *
     * Execution order:
     * 1. All registered cleanup actions (in registration order)
     * 2. Cancellation of the SupervisorJob (cancels all child coroutines)
     *
     * After calling close(), the scope is inactive and should not be used further.
     * Launching new coroutines after close() will fail with CancellationException.
     */
    fun close() {
        cleanupActions.forEach { it() }
        job.cancel()
    }
}
