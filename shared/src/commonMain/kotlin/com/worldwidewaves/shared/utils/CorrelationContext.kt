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
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

/**
 * Correlation context for distributed tracing in KMM applications.
 *
 * This utility enables tracking operations across multiple components and coroutines
 * by attaching a unique correlation ID to the coroutine context. All logs within the
 * same correlation context will automatically include the correlation ID.
 *
 * ## Architecture
 * - Uses [CorrelineContextElement] for proper KMM coroutine context propagation
 * - Works seamlessly across Android and iOS without ThreadLocal
 * - Automatically propagates through child coroutines
 * - Thread-safe and memory-efficient
 *
 * ## Usage Examples
 *
 * ### Automatic ID Generation
 * ```kotlin
 * suspend fun processEvent() = withCorrelation {
 *     Log.i("EventProcessor", "Starting event processing")
 *     // Logs will include auto-generated correlation ID: [CID-12345] Starting event processing
 *     processSteps()
 * }
 * ```
 *
 * ### Custom Correlation ID
 * ```kotlin
 * suspend fun handleRequest(requestId: String) = withCorrelation(requestId) {
 *     Log.i("RequestHandler", "Handling request")
 *     // Logs will include: [requestId] Handling request
 * }
 * ```
 *
 * ### Nested Operations
 * ```kotlin
 * suspend fun parentOperation() = withCorrelation("PARENT-123") {
 *     Log.i("Parent", "Starting parent")
 *     childOperation() // Inherits PARENT-123
 * }
 *
 * suspend fun childOperation() {
 *     Log.i("Child", "Starting child") // [PARENT-123] Starting child
 * }
 * ```
 *
 * ## Performance
 * - Zero overhead when not using correlation context
 * - Minimal memory footprint (single String per context)
 * - No synchronization needed (immutable context elements)
 *
 * ## Testing
 * ```kotlin
 * @Test
 * fun testCorrelationPropagation() = runTest {
 *     withCorrelation("TEST-001") {
 *         assertEquals("TEST-001", CorrelationContext.getCurrentId())
 *     }
 * }
 * ```
 */
object CorrelationContext {
    /**
     * Coroutine context element that holds the correlation ID.
     *
     * This element is automatically propagated to child coroutines and can be
     * accessed via [getCurrentId] from any suspend function.
     */
    private class CorrelationIdElement(
        val id: String,
    ) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<CorrelationIdElement>
    }

    /**
     * Execute a suspend block with a correlation ID.
     *
     * The correlation ID will be attached to the coroutine context and automatically
     * propagated to all child coroutines. All logs within this context will include
     * the correlation ID as a prefix.
     *
     * @param id Custom correlation ID. If null, a random ID will be generated (CID-XXXXX)
     * @param block Suspend block to execute with correlation context
     * @return Result of the block execution
     *
     * @sample
     * ```kotlin
     * suspend fun loadEvent(eventId: String) = withCorrelation("EVENT-$eventId") {
     *     val event = repository.loadEvent(eventId)
     *     Log.i("EventLoader", "Event loaded: ${event.name}")
     *     // Logs: [EVENT-abc123] Event loaded: Wave Event
     *     event
     * }
     * ```
     */
    suspend fun <T> withCorrelation(
        id: String? = null,
        block: suspend CoroutineScope.() -> T,
    ): T {
        val correlationId = id ?: generateCorrelationId()
        return withContext(CorrelationIdElement(correlationId)) {
            block()
        }
    }

    /**
     * Get the current correlation ID from the coroutine context.
     *
     * This function must be called from a suspend function or coroutine.
     * Returns null if no correlation context is active.
     *
     * @return Current correlation ID or null if not in a correlation context
     *
     * @sample
     * ```kotlin
     * suspend fun logCurrentContext() {
     *     val correlationId = CorrelationContext.getCurrentId()
     *     if (correlationId != null) {
     *         println("Operating in correlation context: $correlationId")
     *     }
     * }
     * ```
     */
    suspend fun getCurrentId(): String? = coroutineContext[CorrelationIdElement]?.id

    /**
     * Generate a random correlation ID in the format CID-XXXXX.
     *
     * Uses a 5-digit random number for readability and uniqueness within
     * a single session. For production systems requiring global uniqueness,
     * consider using UUID-based IDs.
     *
     * @return Generated correlation ID (e.g., "CID-42815")
     */
    private fun generateCorrelationId(): String = "CID-${Random.nextInt(10000, 99999)}"
}
