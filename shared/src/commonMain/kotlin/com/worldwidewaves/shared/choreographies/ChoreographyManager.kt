package com.worldwidewaves.shared.choreographies

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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

import com.worldwidewaves.shared.WWWGlobals.Companion.FS_CHOREOGRAPHIES_CONF
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DefaultCoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.utils.ImageResolver
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration

/**
 * Manages choreography sequences for different phases of wave events.
 * Loads sequences from JSON definition files.
 */
class ChoreographyManager<T>(
    coroutineScopeProvider: CoroutineScopeProvider = DefaultCoroutineScopeProvider()
) : KoinComponent {

    private val clock: IClock by inject()
    private val imageResolver: ImageResolver<T> by inject()

    private var definition : ChoreographyDefinition? = null
    private var resolvedSequences : ResolvedChoreography<T>? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Holds resolved choreography sequences with actual resource IDs
     */
    data class ResolvedChoreography<T>(
        val warmingSequences: List<ResolvedSequence<T>> = emptyList(),
        val waitingSequence: ResolvedSequence<T>? = null,
        val hitSequence: ResolvedSequence<T>? = null
    )

    /**
     * A sequence with resolved resource IDs and timing information
     */
    data class ResolvedSequence<T>(
        val sequence: ChoreographySequence,
        val resolvedImages: List<T>,
        val startTime: Duration, // Offset from the beginning of the warming phase
        val endTime: Duration    // When this sequence ends
    )

    init {
        coroutineScopeProvider.launchIO {
            prepareChoreography(FS_CHOREOGRAPHIES_CONF)
        }
    }

    /**
     * Load a choreography definition from a resource file
     * @param definitionResource Resource path to the JSON definition file
     * @return The loaded definition
     */
    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadDefinition(definitionResource: String): ChoreographyDefinition {
        definition?.let { return it }

        try {
            // Read the resource bytes
            val bytes = Res.readBytes(definitionResource)
            val jsonString = bytes.decodeToString()

            definition = json.decodeFromString<ChoreographyDefinition>(jsonString)
            return definition as ChoreographyDefinition
        } catch (e: Exception) {
            println("Error loading choreography definition: ${e.message}")
            return ChoreographyDefinition()
        }
    }

    /**
     * Prepare choreography sequences for an event
     * Resolves resource IDs and calculates timing information
     * @param definitionResource Resource path to the JSON definition
     */
    private suspend fun prepareChoreography(definitionResource: String) {
        val definition = loadDefinition(definitionResource)

        // Build the warmingSequences with timing information
        var currentOffset = Duration.ZERO
        val resolvedWarmingSequences = definition.warmingSequences.map { sequence ->
            val startTime = currentOffset
            val duration = sequence.totalDuration
            val endTime = startTime + duration
            currentOffset = endTime

            ResolvedSequence(
                sequence = sequence,
                resolvedImages = sequence.resolveImageResources(imageResolver),
                startTime = startTime,
                endTime = endTime
            )
        }

        // Resolve waiting and hit sequences
        val resolvedWaitingSequence = definition.waitingSequence?.let {
            ResolvedSequence(
                sequence = it,
                resolvedImages = it.resolveImageResources(imageResolver),
                startTime = Duration.ZERO,
                endTime = it.totalDuration
            )
        }

        val resolvedHitSequence = definition.hitSequence?.let {
            ResolvedSequence(
                sequence = it,
                resolvedImages = it.resolveImageResources(imageResolver),
                startTime = Duration.ZERO,
                endTime = it.totalDuration
            )
        }

        // Store the resolved choreography
        val resolved = ResolvedChoreography(
            warmingSequences = resolvedWarmingSequences,
            waitingSequence = resolvedWaitingSequence,
            hitSequence = resolvedHitSequence
        )

        resolvedSequences = resolved
    }

    /**
     * Get the appropriate warming sequence based on elapsed time since warming started
     * @param startTime When the warming phase started
     * @return The appropriate choreography sequence or null if none available
     */
    fun getCurrentWarmingSequence(startTime: Instant): DisplayableSequence<T>? {
        val resolved = resolvedSequences ?: return null
        if (resolved.warmingSequences.isEmpty()) return null

        // Calculate elapsed time since warming started
        val elapsedTime = clock.now() - startTime

        // Find the appropriate sequence
        val sequence = resolved.warmingSequences.find {
            elapsedTime >= it.startTime && elapsedTime < it.endTime
        } ?: run {
            // If we're past all sequences, return the last one
            if (elapsedTime >= resolved.warmingSequences.last().endTime) {
                resolved.warmingSequences.last()
            } else {
                // Or if we're before any sequence, return the first one
                resolved.warmingSequences.first()
            }
        }

        // Calculate remaining time in this sequence
        val remainingInSequence = sequence.endTime - elapsedTime

        return DisplayableSequence(
            images = sequence.resolvedImages,
            timing = sequence.sequence.timing,
            text = sequence.sequence.text,
            loop = sequence.sequence.loop,
            remainingDuration = remainingInSequence
        )
    }

    /**
     * Get the waiting sequence (when user is about to be hit)
     */
    fun getWaitingSequence(): DisplayableSequence<T>? {
        val resolved = resolvedSequences ?: return null
        val sequence = resolved.waitingSequence ?: return null

        return DisplayableSequence(
            images = sequence.resolvedImages,
            timing = sequence.sequence.timing,
            text = sequence.sequence.text,
            loop = sequence.sequence.loop,
            remainingDuration = null // Loop indefinitely
        )
    }

    /**
     * Get the hit sequence (when user has been hit)
     */
    fun getHitSequence(): DisplayableSequence<T>? {
        val resolved = resolvedSequences ?: return null
        val sequence = resolved.hitSequence ?: return null

        return DisplayableSequence(
            images = sequence.resolvedImages,
            timing = sequence.sequence.timing,
            text = sequence.sequence.text,
            loop = sequence.sequence.loop,
            remainingDuration = null // Loop indefinitely
        )
    }

    /**
     * A sequence ready for display with resolved resource IDs
     */
    data class DisplayableSequence<T>(
        val images: List<T>,
        val timing: Duration,
        val text: String,
        val loop: Boolean,
        val remainingDuration: Duration? // How long this sequence should be displayed
    )
}