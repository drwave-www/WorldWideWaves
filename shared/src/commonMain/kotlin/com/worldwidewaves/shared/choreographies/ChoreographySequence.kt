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

import com.worldwidewaves.shared.utils.ImageResolver
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
data class ChoreographyDefinition(
    @SerialName("warming_sequences") val warmingSequences: List<ChoreographySequence> = emptyList(),
    @SerialName("waiting_sequence") val waitingSequence: ChoreographySequence? = null,
    @SerialName("hit_sequence") val hitSequence: ChoreographySequence? = null
)

/**
 * Represents a sequence of images to be displayed in order with timing information
 * and associated text for wave choreography animations.
 */
@Serializable
data class ChoreographySequence(
    /**
     * Path of the sprite-sheet containing all frames of the sequence.
     *
     * This must reference a single PNG placed in the resources that groups the frames
     * horizontally. (eg. 4 frames of 450×900px in a 1800×900px sheet).
     */
    val frames: String,

    /** Width in pixels of a single frame within [frames]. */
    @SerialName("frame_width")
    val frameWidth: Int,

    /** Height in pixels of a single frame within [frames]. */
    @SerialName("frame_height")
    val frameHeight: Int,

    /** Number of frames contained in [frames] (>= 1). */
    @SerialName("frame_count")
    val frameCount: Int = 1,

    /**
     * Timing per frame (size MUST equal [frameCount] or be empty).
     * If empty, a default of 1 second per frame will be assumed.
     */
    val timing: Duration,
    val text: String = "", // Text to display with the sequence
    val loop: Boolean = true, // Whether to loop the sequence
    val duration: Duration? = 10.seconds // Total duration for this sequence
) {
    init {
        require(frameCount > 0) { "frameCount must be > 0" }
        require(frameWidth > 0 && frameHeight > 0) { "frame dimensions must be > 0" }
    }

    // Returns resolved image resource IDs
    fun <T> resolveImageResources(resolver: ImageResolver<T>): List<T> {
        // Extract each frame from the sprite sheet
        val resolvedFrames = (0 until frameCount).mapNotNull { frameIndex ->
            resolver.resolve(frames)
        }
        
        return resolvedFrames.ifEmpty { emptyList() }
    }
}
