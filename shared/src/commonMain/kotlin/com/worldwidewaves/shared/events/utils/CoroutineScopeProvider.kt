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

interface CoroutineScopeProvider {
    suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T

    suspend fun <T> withDefaultContext(block: suspend CoroutineScope.() -> T): T

    fun launchIO(block: suspend CoroutineScope.() -> Unit): Job

    fun launchDefault(block: suspend CoroutineScope.() -> Unit): Job

    fun scopeIO(): CoroutineScope

    fun scopeDefault(): CoroutineScope

    fun cancelAllCoroutines()
}

class DefaultCoroutineScopeProvider(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : CoroutineScopeProvider {
    private val supervisorJob = SupervisorJob()
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
