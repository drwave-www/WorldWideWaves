package com.worldwidewaves.shared.choreographies

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

import com.worldwidewaves.shared.WWWGlobals.FileSystem
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.getChoreographyText
import com.worldwidewaves.shared.utils.ImageResolver
import dev.icerock.moko.resources.StringResource
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Manages visual choreography sequences for different phases of wave events.
 *
 * Core responsibilities:
 * • Loads JSON choreography definitions from [FileSystem.CHOREOGRAPHIES_CONF]
 * • Resolves sprite sheets into platform-specific image resources via [ImageResolver]
 * • Builds warming (progressive), waiting, and hit sequences with correct timing
 * • Provides [DisplayableSequence] objects with frame timing and remaining duration
 *
 * The manager handles sequence transitions, looping, and time-based frame selection
 * to create a synchronized visual experience across all wave phases.
 */
@OptIn(ExperimentalTime::class)
open class ChoreographyManager<T> : KoinComponent {
    val clock: IClock by inject()
    private val imageResolver: ImageResolver<T> by inject()

    // Performance optimization: Lazy loading and caching
    private var definition: ChoreographyDefinition? = null
    private var resolvedSequences: ResolvedChoreography<T>? = null
    private var isLoading = false
    private val resolvedImageCache = mutableMapOf<String, T?>()

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    // ------------------------------------------------------------------------

    /**
     * Holds all resolved choreography sequences for the three wave phases:
     * warming (progressive build-up), waiting (pre-hit), and hit (impact).
     */
    data class ResolvedChoreography<T>(
        val warmingSequences: List<ResolvedSequence<T>> = emptyList(),
        val waitingSequence: ResolvedSequence<T>? = null,
        val hitSequence: ResolvedSequence<T>? = null,
    )

    /**
     * Single choreography sequence with resolved platform-specific image resource
     * and timing boundaries (start/end time offsets).
     */
    data class ResolvedSequence<T>(
        val sequence: ChoreographySequence,
        val text: StringResource,
        val resolvedImage: T?,
        val startTime: Duration,
        val endTime: Duration,
    )

    /**
     * UI-ready choreography sequence with all parameters needed for rendering:
     * resolved image, frame dimensions, timing, text, and loop behavior.
     */
    data class DisplayableSequence<T>(
        val image: T?,
        val frameWidth: Int,
        val frameHeight: Int,
        val frameCount: Int,
        val timing: Duration,
        val duration: Duration,
        val text: StringResource,
        val loop: Boolean,
        val remainingDuration: Duration?,
    )

    // ----------------------

    /**
     * Optimized sequence resolution with image caching.
     * Images are cached by their path to avoid repeated resource loading.
     */
    private fun ChoreographySequence.toResolved(
        startTime: Duration = Duration.ZERO,
        seqType: String,
        seqNumber: Int? = null,
    ): ResolvedSequence<T> {
        // Use cached image if available, otherwise resolve and cache
        val imageKey = "${this.frames}_${this.frameWidth}_${this.frameHeight}"
        val resolvedImage = resolvedImageCache.getOrPut(imageKey) {
            try {
                resolveImageResources(imageResolver).firstOrNull()
            } catch (e: Exception) {
                Log.w("ChoreographyManager", "Failed to resolve image for $imageKey: ${e.message}")
                null
            }
        }

        return ResolvedSequence(
            sequence = this,
            text = getChoreographyText(seqType, seqNumber),
            resolvedImage = resolvedImage,
            startTime = startTime,
            endTime = startTime + (duration ?: 10.seconds),
        )
    }

    private fun ResolvedSequence<T>.toDisplayable(remainingDuration: Duration? = null): DisplayableSequence<T> =
        DisplayableSequence(
            image = resolvedImage,
            frameWidth = sequence.frameWidth,
            frameHeight = sequence.frameHeight,
            frameCount = sequence.frameCount,
            timing = sequence.timing,
            duration = sequence.duration!!,
            text = text,
            loop = sequence.loop,
            remainingDuration = remainingDuration,
        )

    // ------------------------------------------------------------------------

    /**
     * Lazy initialization: choreography is only loaded when first needed.
     * This improves app startup time by deferring resource loading until actual usage.
     */
    private suspend fun ensureChoreographyLoaded() {
        if (resolvedSequences == null && !isLoading) {
            isLoading = true
            try {
                Log.d("ChoreographyManager", "Lazy loading choreography resources")
                prepareChoreography(FileSystem.CHOREOGRAPHIES_CONF)
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Pre-loads choreography resources for timing-critical scenarios.
     *
     * This should be called proactively before wave events start to ensure
     * precise timing for sound emission and wave synchronization.
     * Critical for real-world symphony accuracy.
     */
    suspend fun preloadForWaveSync() {
        Log.d("ChoreographyManager", "Pre-loading choreography for wave synchronization")
        ensureChoreographyLoaded()
        Log.d("ChoreographyManager", "Choreography pre-loaded successfully")
    }

    // ------------------------------------------------------------------------

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadDefinition(definitionResource: String): ChoreographyDefinition {
        definition?.let { return it }

        try {
            val bytes = Res.readBytes(definitionResource)
            val jsonString = bytes.decodeToString()
            return json.decodeFromString<ChoreographyDefinition>(jsonString).also {
                definition = it
            }
        } catch (e: Exception) {
            Log.e("ChoreographyManager", "Error loading choreography definition: ${e.message}")
            return ChoreographyDefinition()
        }
    }

    // ------------------------------------------------------------------------

    private suspend fun prepareChoreography(definitionResource: String) {
        val definition = loadDefinition(definitionResource)
        var startTime = Duration.ZERO

        val warmingSequences =
            definition.warmingSequences.mapIndexed { i, sequence ->
                sequence.toResolved(startTime, seqType = "warming", seqNumber = i + 1).also {
                    startTime = it.endTime
                }
            }

        resolvedSequences =
            ResolvedChoreography(
                warmingSequences = warmingSequences,
                waitingSequence = definition.waitingSequence?.toResolved(seqType = "waiting"),
                hitSequence = definition.hitSequence?.toResolved(seqType = "hit"),
            )
    }

    // ------------------------------------------------------------------------

    /**
     * Returns the current warming sequence based on elapsed time since [startTime],
     * with remaining duration for smooth transitions.
     *
     * Performance optimized: Uses lazy loading to only load resources when needed.
     */
    open suspend fun getCurrentWarmingSequence(startTime: Instant): DisplayableSequence<T>? {
        ensureChoreographyLoaded()

        val resolved = resolvedSequences ?: return null
        if (resolved.warmingSequences.isEmpty()) return null

        val totalTiming = resolved.warmingSequences.last().endTime
        val elapsedTime = clock.now() - startTime
        val wrappedElapsedTime =
            if (totalTiming.isPositive()) {
                (elapsedTime.inWholeNanoseconds % totalTiming.inWholeNanoseconds)
                    .nanoseconds
                    .coerceAtLeast(Duration.ZERO)
            } else {
                Duration.ZERO
            }

        Log.v("ChoreographyManager", "wrappedElapsedTime: ${wrappedElapsedTime.inWholeSeconds} seconds")

        val sequence =
            resolved.warmingSequences.find {
                wrappedElapsedTime >= it.startTime && wrappedElapsedTime < it.endTime
            } ?: resolved.warmingSequences.first()

        return sequence.toDisplayable(sequence.endTime - wrappedElapsedTime)
    }

    /**
     * Returns the waiting sequence shown when a user is about to be hit by the wave.
     * Performance optimized: Uses lazy loading.
     */
    open suspend fun getWaitingSequence(): DisplayableSequence<T>? {
        ensureChoreographyLoaded()
        return resolvedSequences?.waitingSequence?.toDisplayable()
    }

    /**
     * Returns the hit sequence shown when a user has been hit by the wave.
     * Performance optimized: Uses lazy loading.
     */
    open suspend fun getHitSequence(): DisplayableSequence<T>? {
        ensureChoreographyLoaded()
        return resolvedSequences?.hitSequence?.toDisplayable()
    }

    /**
     * Clears the image cache to free memory.
     * Useful for memory management in resource-constrained scenarios.
     */
    fun clearImageCache() {
        Log.d("ChoreographyManager", "Clearing image cache (${resolvedImageCache.size} entries)")
        resolvedImageCache.clear()
    }

    // Backward compatibility: Non-suspend versions that return null if not loaded
    // These are deprecated and should be migrated to suspend versions for optimal performance

    /**
     * Legacy non-suspend version for timing-critical warming sequences.
     * Warming sequences are less timing-critical so this version is acceptable.
     */
    open fun getCurrentWarmingSequenceImmediate(startTime: Instant): DisplayableSequence<T>? {
        val resolved = resolvedSequences ?: return null
        if (resolved.warmingSequences.isEmpty()) return null

        val totalTiming = resolved.warmingSequences.last().endTime
        val elapsedTime = clock.now() - startTime
        val wrappedElapsedTime =
            if (totalTiming.isPositive()) {
                (elapsedTime.inWholeNanoseconds % totalTiming.inWholeNanoseconds)
                    .nanoseconds
                    .coerceAtLeast(Duration.ZERO)
            } else {
                Duration.ZERO
            }

        val sequence =
            resolved.warmingSequences.find {
                wrappedElapsedTime >= it.startTime && wrappedElapsedTime < it.endTime
            } ?: resolved.warmingSequences.first()

        return sequence.toDisplayable(sequence.endTime - wrappedElapsedTime)
    }

    /**
     * Immediate waiting sequence access - can be used for less timing-critical scenarios.
     * Returns null if choreography not yet loaded.
     */
    open fun getWaitingSequenceImmediate(): DisplayableSequence<T>? = resolvedSequences?.waitingSequence?.toDisplayable()

    /**
     * TIMING-CRITICAL: Immediate hit sequence access.
     * This is the most critical method for wave synchronization (~2s before hit).
     * Should be pre-loaded via preloadForWaveSync() to ensure zero-latency access.
     */
    open fun getHitSequenceImmediate(): DisplayableSequence<T>? {
        val resolved = resolvedSequences
        if (resolved == null) {
            Log.w("ChoreographyManager", "Hit sequence requested but choreography not pre-loaded! This may cause timing issues.")
        }
        return resolved?.hitSequence?.toDisplayable()
    }
}
