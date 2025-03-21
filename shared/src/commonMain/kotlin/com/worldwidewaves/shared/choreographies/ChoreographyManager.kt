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
import io.github.aakira.napier.Napier
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/**
 * Manages choreography sequences for different phases of wave events.
 */
class ChoreographyManager<T>(
    coroutineScopeProvider: CoroutineScopeProvider = DefaultCoroutineScopeProvider()
) : KoinComponent {

    private val clock: IClock by inject()
    private val imageResolver: ImageResolver<T> by inject()
    private var definition: ChoreographyDefinition? = null
    private var resolvedSequences: ResolvedChoreography<T>? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ------------------------------------------------------------------------

    data class ResolvedChoreography<T>(
        val warmingSequences: List<ResolvedSequence<T>> = emptyList(),
        val waitingSequence: ResolvedSequence<T>? = null,
        val hitSequence: ResolvedSequence<T>? = null
    )

    data class ResolvedSequence<T>(
        val sequence: ChoreographySequence,
        val resolvedImages: List<T>,
        val startTime: Duration,
        val endTime: Duration
    )

    data class DisplayableSequence<T>(
        val images: List<T>,
        val timing: Duration,
        val text: String,
        val loop: Boolean,
        val remainingDuration: Duration?
    )

    // ----------------------

    private fun ChoreographySequence.toResolved(
        startTime: Duration = Duration.ZERO
    ): ResolvedSequence<T> = ResolvedSequence(
        sequence = this,
        resolvedImages = resolveImageResources(imageResolver),
        startTime = startTime,
        endTime = startTime + totalDuration
    )

    private fun ResolvedSequence<T>.toDisplayable(
        remainingDuration: Duration? = null
    ): DisplayableSequence<T> = DisplayableSequence(
        images = resolvedImages,
        timing = sequence.timing,
        text = sequence.text,
        loop = sequence.loop,
        remainingDuration = remainingDuration
    )

    // ------------------------------------------------------------------------

    init {
        // This could be moved outside and managed by Koin
        coroutineScopeProvider.launchIO {
            prepareChoreography(FS_CHOREOGRAPHIES_CONF)
        }
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
            Napier.e("Error loading choreography definition: ${e.message}")
            return ChoreographyDefinition()
        }
    }

    // ------------------------------------------------------------------------

    private suspend fun prepareChoreography(definitionResource: String) {
        val definition = loadDefinition(definitionResource)
        var currentOffset = Duration.ZERO

        val warmingSequences = definition.warmingSequences.map { sequence ->
            sequence.toResolved(currentOffset).also {
                currentOffset += sequence.totalDuration
            }
        }

        resolvedSequences = ResolvedChoreography(
            warmingSequences = warmingSequences,
            waitingSequence = definition.waitingSequence?.toResolved(),
            hitSequence = definition.hitSequence?.toResolved()
        )
    }

    // ------------------------------------------------------------------------

    fun getCurrentWarmingSequence(startTime: Instant): DisplayableSequence<T>? {
        val resolved = resolvedSequences ?: return null
        if (resolved.warmingSequences.isEmpty()) return null

        val totalDuration = resolved.warmingSequences.last().endTime
        val elapsedTime = clock.now() - startTime
        val wrappedElapsedTime = if (totalDuration.isPositive()) {
            (elapsedTime.inWholeNanoseconds % totalDuration.inWholeNanoseconds).nanoseconds
                .coerceAtLeast(Duration.ZERO)
        } else {
            Duration.ZERO
        }

        val sequence = resolved.warmingSequences.find {
            wrappedElapsedTime >= it.startTime && wrappedElapsedTime < it.endTime
        } ?: resolved.warmingSequences.first()

        return sequence.toDisplayable(sequence.endTime - wrappedElapsedTime)
    }

    fun getWaitingSequence(): DisplayableSequence<T>? =
        resolvedSequences?.waitingSequence?.toDisplayable()

    fun getHitSequence(): DisplayableSequence<T>? =
        resolvedSequences?.hitSequence?.toDisplayable()
}