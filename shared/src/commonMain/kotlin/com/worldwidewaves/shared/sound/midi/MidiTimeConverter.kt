package com.worldwidewaves.shared.sound.midi

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

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Handles MIDI time conversion from ticks to real time.
 *
 * Manages tempo changes throughout the MIDI file and converts tick-based
 * timing to real-time durations accounting for tempo variations.
 */
internal object MidiTimeConverter {
    private const val MICROSECONDS_PER_SECOND = 1_000_000.0

    /**
     * Represents a tempo change event in the MIDI file.
     */
    data class TempoChange(
        val tick: Long,
        val microsecondsPerBeat: Long,
    )

    /**
     * Convert ticks to real time accounting for tempo changes.
     *
     * @param ticks The tick position to convert
     * @param ticksPerBeat Ticks per quarter note from MIDI header
     * @param tempoChanges List of tempo changes sorted by tick position
     * @return Real time duration from start of track
     */
    fun ticksToRealTime(
        ticks: Long,
        ticksPerBeat: Int,
        tempoChanges: List<TempoChange>,
    ): Duration {
        var elapsedSeconds = 0.0
        var lastTempoTick = 0L
        var lastTempoMPB = tempoChanges.first().microsecondsPerBeat

        // Find all tempo regions this time crosses
        for (i in 1 until tempoChanges.size) {
            val currentChange = tempoChanges[i]

            if (ticks <= currentChange.tick) {
                // This tick is before the current tempo change, so calculate with the previous tempo
                val ticksInThisTempo = ticks - lastTempoTick
                elapsedSeconds += (ticksInThisTempo.toDouble() / ticksPerBeat) *
                    (lastTempoMPB.toDouble() / MICROSECONDS_PER_SECOND)
                break
            } else {
                // Calculate segment up to this tempo change
                val ticksInThisTempo = currentChange.tick - lastTempoTick
                elapsedSeconds += (ticksInThisTempo.toDouble() / ticksPerBeat) *
                    (lastTempoMPB.toDouble() / MICROSECONDS_PER_SECOND)

                // Move to next tempo
                lastTempoTick = currentChange.tick
                lastTempoMPB = currentChange.microsecondsPerBeat
            }
        }

        // If we went through all tempo changes, calculate the remainder
        if (ticks > lastTempoTick) {
            val ticksInLastTempo = ticks - lastTempoTick
            elapsedSeconds += (ticksInLastTempo.toDouble() / ticksPerBeat) *
                (lastTempoMPB.toDouble() / MICROSECONDS_PER_SECOND)
        }

        return elapsedSeconds.seconds
    }
}
