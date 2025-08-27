package com.worldwidewaves.shared.choreographies

/*
 * Copyright 2025 DrWave
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
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.getChoreographyText
import com.worldwidewaves.shared.utils.ImageResolver
import dev.icerock.moko.resources.StringResource
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Manages choreography sequences for different phases of wave events.
 */
@OptIn(ExperimentalTime::class)
open class ChoreographyManager<T>(
    coroutineScopeProvider: CoroutineScopeProvider = DefaultCoroutineScopeProvider()
) : KoinComponent {

    val clock: IClock by inject()
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
        val text: StringResource,
        val resolvedImage: T?,
        val startTime: Duration,
        val endTime: Duration
    )

    data class DisplayableSequence<T>(
        val image: T?,
        val frameWidth: Int,
        val frameHeight: Int,
        val frameCount: Int,
        val timing: Duration,
        val duration: Duration,
        val text: StringResource,
        val loop: Boolean,
        val remainingDuration: Duration?
    )

    // ----------------------

    private fun ChoreographySequence.toResolved(
        startTime: Duration = Duration.ZERO,
        seqType: String,
        seqNumber: Int? = null
    ): ResolvedSequence<T> {
        val resolvedImages = resolveImageResources(imageResolver)
        Log.d("ChoreographyManager", "duration: ${duration?.inWholeSeconds}")
        Log.d("ChoreographyManager", "startTime: ${startTime.inWholeSeconds}")
        Log.d("ChoreographyManager", "endTime: ${(startTime + duration!!).inWholeSeconds}")
        return ResolvedSequence(
            sequence = this,
            text = getChoreographyText(seqType, seqNumber),
            resolvedImage = resolvedImages.firstOrNull(),
            startTime = startTime,
            endTime = startTime + duration
        )
    }

    private fun ResolvedSequence<T>.toDisplayable(
        remainingDuration: Duration? = null
    ): DisplayableSequence<T> = DisplayableSequence(
        image = resolvedImage,
        frameWidth = sequence.frameWidth,
        frameHeight = sequence.frameHeight,
        frameCount = sequence.frameCount,
        timing = sequence.timing,
        duration = sequence.duration!!,
        text = text,
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
        var startTime = Duration.ZERO

        val warmingSequences = definition.warmingSequences.mapIndexed { i, sequence ->
            sequence.toResolved(startTime, seqType ="warming", seqNumber = i + 1).also {
                startTime = it.endTime
            }
        }

        resolvedSequences = ResolvedChoreography(
            warmingSequences = warmingSequences,
            waitingSequence = definition.waitingSequence?.toResolved(seqType ="waiting"),
            hitSequence = definition.hitSequence?.toResolved(seqType = "hit")
        )
    }

    // ------------------------------------------------------------------------

    open fun getCurrentWarmingSequence(startTime: Instant): DisplayableSequence<T>? {
        val resolved = resolvedSequences ?: return null
        if (resolved.warmingSequences.isEmpty()) return null

        val totalTiming = resolved.warmingSequences.last().endTime
        val elapsedTime = clock.now() - startTime
        val wrappedElapsedTime = if (totalTiming.isPositive()) {
            (elapsedTime.inWholeNanoseconds % totalTiming.inWholeNanoseconds).nanoseconds
                .coerceAtLeast(Duration.ZERO)
        } else {
            Duration.ZERO
        }

        Log.v("ChoreographyManager", "wrappedElapsedTime: ${wrappedElapsedTime.inWholeSeconds} seconds")

        val sequence = resolved.warmingSequences.find {
            wrappedElapsedTime >= it.startTime && wrappedElapsedTime < it.endTime
        } ?: resolved.warmingSequences.first()

        return sequence.toDisplayable(sequence.endTime - wrappedElapsedTime)
    }

    open fun getWaitingSequence(): DisplayableSequence<T>? =
        resolvedSequences?.waitingSequence?.toDisplayable()

    open fun getHitSequence(): DisplayableSequence<T>? =
        resolvedSequences?.hitSequence?.toDisplayable()
}
