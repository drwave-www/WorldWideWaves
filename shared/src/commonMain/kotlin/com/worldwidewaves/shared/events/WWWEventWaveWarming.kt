package com.worldwidewaves.shared.events

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

import com.worldwidewaves.shared.WWWGlobals.Companion.WaveTiming
import com.worldwidewaves.shared.choreographies.ChoreographyManager
import com.worldwidewaves.shared.choreographies.ChoreographyManager.DisplayableSequence
import com.worldwidewaves.shared.choreographies.SoundChoreographyManager
import com.worldwidewaves.shared.events.utils.IClock
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.DrawableResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class WWWEventWaveWarming(
    val event: IWWWEvent,
) : KoinComponent {
    private val clock: IClock by inject()
    private val choreographyManager: ChoreographyManager<DrawableResource> by inject()
    val soundChoreographyManager: SoundChoreographyManager by inject()

    fun getWarmingDuration(): Duration = WaveTiming.WARMING_DURATION

    suspend fun userWarmingStartDateTime(): Instant? =
        event.wave.userHitDateTime()?.let { hitDateTime ->
            hitDateTime - getWarmingDuration() - WaveTiming.WARN_BEFORE_HIT
        }

    suspend fun isUserWarmingStarted(): Boolean = userWarmingStartDateTime()?.let { clock.now() >= it } ?: false

    fun getCurrentChoregraphySequence(): DisplayableSequence<DrawableResource>? =
        choreographyManager.getCurrentWarmingSequenceImmediate(event.getStartDateTime())

    /**
     * Play a tone from the choreography that is active **now** and return the MIDI pitch
     * (or `null` if nothing was played).
     *
     * In debug builds we try to forward the played note information to the
     * Sound-Choreography test-mode overlay (if present) via reflection, so that
     * no production-code dependency is introduced.
     */
    suspend fun playCurrentSoundChoreographyTone(): Int? {
        val note = soundChoreographyManager.playCurrentSoundTone(event.getStartDateTime())
        notifyDebug(note)
        return note
    }

    /**
     * Same as [playCurrentSoundChoreographyTone] but allows forcing a custom
     * `startTime` (useful for unit / UI tests that need deterministic playback).
     */
    suspend fun playCurrentSoundChoreographyTone(forceStartTime: Instant? = null): Int? {
        val startTime = forceStartTime ?: event.getStartDateTime()
        val note = soundChoreographyManager.playCurrentSoundTone(startTime)
        notifyDebug(note)
        return note
    }

    /**
     * Try to inform the optional debug overlay that a note has been played.
     *
     * In a multiplatform context we cannot rely on JVM-only reflection APIs.  Instead
     * we simply output the played note to the debug logger (Napier).  Platform
     * modules that want to react to this information can intercept logs or
     * implement their own hook in the platform-specific `SoundChoreographyManager`
     * actual implementations.
     */
    private fun notifyDebug(note: Int?) {
        note?.let {
            Napier.d(tag = "WaveWarming", message = "Sound note played: $it")
        }
    }
}
