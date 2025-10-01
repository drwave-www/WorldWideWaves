package com.worldwidewaves.shared.events.utils

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

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Abstraction for managing coroutine scopes and dispatcher selection.
 *
 * ## Purpose
 * Provides centralized control over coroutine execution contexts, enabling:
 * - Consistent dispatcher selection (IO vs Default)
 * - Structured concurrency with proper lifecycle management
 * - Exception handling for all launched coroutines
 * - Testability through dependency injection of test dispatchers
 *
 * ## Threading Model
 * - **IO Dispatcher**: For I/O-bound operations (network, file system, database)
 * - **Default Dispatcher**: For CPU-bound operations (computation, parsing, algorithms)
 * - **SupervisorJob**: Failures in child coroutines don't cancel siblings
 * - **Exception Handler**: Catches and logs all unhandled exceptions to prevent app crashes
 *
 * ## Usage Patterns
 * ```kotlin
 * class MyRepository(
 *     private val scopeProvider: CoroutineScopeProvider
 * ) {
 *     // Launch fire-and-forget background task
 *     fun loadData() {
 *         scopeProvider.launchIO {
 *             val data = fetchFromNetwork()
 *             saveToDatabase(data)
 *         }
 *     }
 *
 *     // Structured concurrency with result
 *     suspend fun getData(): String {
 *         return scopeProvider.withIOContext {
 *             fetchFromNetwork()
 *         }
 *     }
 *
 *     // CPU-intensive work
 *     fun processData(input: List<Int>) {
 *         scopeProvider.launchDefault {
 *             val result = input.map { it * it }.sum()
 *             updateUI(result)
 *         }
 *     }
 * }
 * ```
 *
 * ## Lifecycle Management
 * Call [cancelAllCoroutines] when the component is destroyed:
 * ```kotlin
 * class MyViewModel(
 *     private val scopeProvider: CoroutineScopeProvider
 * ) {
 *     fun onCleared() {
 *         scopeProvider.cancelAllCoroutines()
 *     }
 * }
 * ```
 *
 * ## Testing
 * Inject test dispatchers for deterministic testing:
 * ```kotlin
 * @Test
 * fun testAsync() = runTest {
 *     val testProvider = DefaultCoroutineScopeProvider(
 *         ioDispatcher = StandardTestDispatcher(testScheduler),
 *         defaultDispatcher = StandardTestDispatcher(testScheduler)
 *     )
 *     // Test with controlled dispatcher
 * }
 * ```
 *
 * @see DefaultCoroutineScopeProvider for production implementation
 * @see SupervisorJob for exception isolation
 */
interface CoroutineScopeProvider {
    /**
     * Executes a suspending block on the IO dispatcher and returns the result.
     *
     * Use for I/O-bound operations: network calls, file I/O, database access.
     * Automatically switches to IO context and back to the caller's context.
     *
     * @param block The suspending operation to execute
     * @return The result of the block execution
     */
    suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T

    /**
     * Executes a suspending block on the Default dispatcher and returns the result.
     *
     * Use for CPU-bound operations: computation, parsing, data processing.
     * Automatically switches to Default context and back to the caller's context.
     *
     * @param block The suspending operation to execute
     * @return The result of the block execution
     */
    suspend fun <T> withDefaultContext(block: suspend CoroutineScope.() -> T): T

    /**
     * Launches a fire-and-forget coroutine on the IO dispatcher.
     *
     * Use for background I/O operations where you don't need the result.
     * Exceptions are caught by the global exception handler.
     *
     * @param block The suspending operation to launch
     * @return Job that can be used to cancel the operation
     */
    fun launchIO(block: suspend CoroutineScope.() -> Unit): Job

    /**
     * Launches a fire-and-forget coroutine on the Default dispatcher.
     *
     * Use for background CPU-intensive operations where you don't need the result.
     * Exceptions are caught by the global exception handler.
     *
     * @param block The suspending operation to launch
     * @return Job that can be used to cancel the operation
     */
    fun launchDefault(block: suspend CoroutineScope.() -> Unit): Job

    /**
     * Returns a CoroutineScope configured with the IO dispatcher.
     *
     * Use when you need direct access to the scope for advanced patterns.
     *
     * @return CoroutineScope with IO dispatcher
     */
    fun scopeIO(): CoroutineScope

    /**
     * Returns a CoroutineScope configured with the Default dispatcher.
     *
     * Use when you need direct access to the scope for advanced patterns.
     *
     * @return CoroutineScope with Default dispatcher
     */
    fun scopeDefault(): CoroutineScope

    /**
     * Cancels all active coroutines launched by this provider.
     *
     * Call this during cleanup/disposal to prevent memory leaks and ensure
     * all background work is properly cancelled. After calling this, the
     * provider should not be used again.
     */
    fun cancelAllCoroutines()
}

/**
 * Production implementation of [CoroutineScopeProvider] with proper exception handling.
 *
 * ## Exception Handling Strategy
 * Uses [CoroutineExceptionHandler] to catch and log all unhandled exceptions without
 * crashing the app. This is critical for production stability:
 * - Logs exception type, message, and stack trace via Napier
 * - Does NOT rethrow exceptions (prevents app crashes)
 * - Works with [SupervisorJob] to isolate failures between coroutines
 *
 * ## Dispatcher Configuration
 * Dispatchers can be customized via constructor for testing:
 * - Production: Uses [Dispatchers.IO] and [Dispatchers.Default]
 * - Testing: Inject [StandardTestDispatcher] for deterministic execution
 *
 * ## Lifecycle
 * - Shared [SupervisorJob] manages all scopes
 * - Call [cancelAllCoroutines] to cancel all launched work
 * - After cancellation, create a new instance for further work
 *
 * @param ioDispatcher Dispatcher for I/O-bound operations (default: Dispatchers.IO)
 * @param defaultDispatcher Dispatcher for CPU-bound operations (default: Dispatchers.Default)
 *
 * @see CoroutineScopeProvider for interface documentation
 */
class DefaultCoroutineScopeProvider(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : CoroutineScopeProvider {
    private val supervisorJob = SupervisorJob()

    /**
     * Global exception handler that logs all unhandled coroutine exceptions.
     *
     * **IMPORTANT**: Does not rethrow exceptions to prevent app crashes.
     * All exceptions are logged with full context for debugging.
     */
    private val exceptionHandler =
        CoroutineExceptionHandler { _, exception ->
            Napier.e("CoroutineExceptionHandler caught unhandled exception: $exception", exception)
            // Log additional context for debugging
            Napier.e("Exception type: ${exception::class.simpleName}")
            Napier.e("Exception message: ${exception.message}")
            // Don't rethrow - this prevents the app crash
        }

    // Create scopes with exception handler included
    private val ioScope = CoroutineScope(supervisorJob + ioDispatcher + exceptionHandler)
    private val defaultScope = CoroutineScope(supervisorJob + defaultDispatcher + exceptionHandler)

    // Note: Unused scope kept for potential future use
    @Suppress("unused")
    private val scope = CoroutineScope(supervisorJob + defaultDispatcher + exceptionHandler)

    override fun launchIO(block: suspend CoroutineScope.() -> Unit): Job = ioScope.launch(block = block)

    override fun launchDefault(block: suspend CoroutineScope.() -> Unit): Job = defaultScope.launch(block = block)

    override fun scopeIO(): CoroutineScope = ioScope

    override fun scopeDefault(): CoroutineScope = defaultScope

    override suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T = withContext(ioDispatcher) { block() }

    override suspend fun <T> withDefaultContext(block: suspend CoroutineScope.() -> T): T = withContext(defaultDispatcher) { block() }

    override fun cancelAllCoroutines() {
        supervisorJob.cancel()
    }
}
