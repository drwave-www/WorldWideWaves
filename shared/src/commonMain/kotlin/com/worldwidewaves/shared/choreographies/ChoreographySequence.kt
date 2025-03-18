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

import com.worldwidewaves.shared.utils.ImageResolver
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Represents a sequence of images to be displayed in order with timing information
 * and associated text for wave choreography animations.
 */
@Serializable
data class ChoreographySequence(
    val images: List<String>, // Resource paths, will be resolved to actual resources
    val timing: Duration = 1.seconds, // Default timing is 1 second per image
    val text: String = "", // Text to display with the sequence
    val loop: Boolean = true, // Whether to loop the sequence
    val duration: Duration? = null // Optional total duration for this sequence
) {
    // Validate that the sequence contains at least one image
    init {
        require(images.isNotEmpty()) { "ChoreographySequence must contain at least one image" }
    }

    // Total duration of the sequence (for one complete cycle)
    val totalDuration: Duration get() = duration ?: (timing * images.size)

    // Returns resolved image resource IDs
    fun <T> resolveImageResources(resolver: ImageResolver<T>): List<T> {
        return images
            .mapNotNull { resolver.resolve(it) }
            .takeIf { it.isNotEmpty() }
            ?: listOfNotNull(resolver.resolve("transparent"))
    }

    companion object {
        // Create an empty placeholder sequence
        fun empty(): ChoreographySequence = ChoreographySequence(
            images = listOf("transparent"),
            timing = 1.seconds,
            text = "",
            loop = false
        )
    }
}

/**
 * JSON structure for choreography definitions
 */
@Serializable
data class ChoreographyDefinition(
    @SerialName("warming_sequences") val warmingSequences: List<ChoreographySequence> = emptyList(),
    @SerialName("waiting_sequence") val waitingSequence: ChoreographySequence? = null,
    @SerialName("hit_sequence") val hitSequence: ChoreographySequence? = null
)