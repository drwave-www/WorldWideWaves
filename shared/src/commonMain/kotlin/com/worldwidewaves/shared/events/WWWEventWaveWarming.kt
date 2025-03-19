package com.worldwidewaves.shared.events

import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_WARMING_DURATION
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_WARN_BEFORE_HIT
import com.worldwidewaves.shared.choreographies.ChoreographyManager
import com.worldwidewaves.shared.choreographies.ChoreographyManager.DisplayableSequence
import com.worldwidewaves.shared.choreographies.SoundChoreographyManager
import com.worldwidewaves.shared.events.utils.IClock
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.DrawableResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration

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

class WWWEventWaveWarming(val event: IWWWEvent) : KoinComponent {

    private val clock: IClock by inject()
    private val choreographyManager: ChoreographyManager<DrawableResource> by inject()
    private val soundChoreographyManager: SoundChoreographyManager by inject()

    fun getWarmingDuration(): Duration = WAVE_WARMING_DURATION

    suspend fun userWarmingStartDateTime(): Instant? {
        return event.wave.userHitDateTime()?.let { hitDateTime ->
            hitDateTime - getWarmingDuration() - WAVE_WARN_BEFORE_HIT
        }
    }

    suspend fun isUserWarmingStarted(): Boolean = userWarmingStartDateTime()?.let { clock.now() >= it } ?: false

    fun getCurrentChoregraphySequence(): DisplayableSequence<DrawableResource>? =
        choreographyManager.getCurrentWarmingSequence(event.getStartDateTime())

    suspend fun playCurrentSoundChoreographyTone() =
        soundChoreographyManager.playCurrentSoundTone(event.getStartDateTime())

}