package com.worldwidewaves.shared.choreographies.resources

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

import com.worldwidewaves.shared.MokoRes
import dev.icerock.moko.resources.StringResource

private object ChoreographyConstants {
    const val MAX_WARMING_SEQUENCE_NUMBER = 6
}

fun getChoreographyText(
    sequenceType: String,
    sequenceNumber: Int? = null,
): StringResource =
    when (sequenceType) {
        "warming" -> getChoreographyWarmingText(sequenceNumber)
        "waiting" -> getChoreographyWaitingText()
        "hit" -> getChoreographyHitText()
        else -> throw IllegalArgumentException("Invalid choreography type: $sequenceType")
    }

fun getChoreographyWarmingText(seq: Int?): StringResource =
    when (seq) {
        1 -> MokoRes.strings.choreography_warming_seq_1
        2 -> MokoRes.strings.choreography_warming_seq_2
        3 -> MokoRes.strings.choreography_warming_seq_3
        4 -> MokoRes.strings.choreography_warming_seq_4
        5 -> MokoRes.strings.choreography_warming_seq_5
        ChoreographyConstants.MAX_WARMING_SEQUENCE_NUMBER -> MokoRes.strings.choreography_warming_seq_6
        else -> throw IllegalArgumentException("Invalid choreography sequence number: $seq")
    }

fun getChoreographyWaitingText(): StringResource = MokoRes.strings.choreography_waiting

fun getChoreographyHitText(): StringResource = MokoRes.strings.choreography_hit
