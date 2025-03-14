package com.worldwidewaves.shared.events

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

import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

// ---------------------------

@Serializable
data class WWWEventWaveDeep(
    override val speed: Double,
    override val direction: Direction,
    override val approxDuration: Int
) : KoinComponent, WWWEventWave() {

    override suspend fun getWavePolygons(
        lastWaveState: WavePolygons?,
        mode: WaveMode
    ): WavePolygons {
        TODO("Not yet implemented")
    }

    override suspend fun getWaveDuration(): Duration {
        return 2.hours // TODO
    }

    override suspend fun hasUserBeenHitInCurrentPosition(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun timeBeforeHit(): Duration? {
        TODO("Not yet implemented")
    }

    // ---------------------------

    override fun validationErrors() : List<String>? {
        val superValid = super.validationErrors()
        val errors = superValid?.toMutableList() ?: mutableListOf()

        // TODO

        return errors.takeIf { it.isNotEmpty() }?.map { "${WWWEventWaveDeep::class.simpleName}: $it" }
    }

}