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

import com.worldwidewaves.shared.utils.ByteArrayReader

/**
 * Handles MIDI event processing including note events and other MIDI messages.
 *
 * Processes various MIDI events:
 * - Note On/Off events
 * - Program changes
 * - Control changes
 * - Other MIDI channel messages
 */
internal object MidiEventProcessor {
    // MIDI event constants
    private const val STATUS_MASK_0F = 0x0F
    private const val STATUS_MASK_E0 = 0xE0
    private const val PROGRAM_CHANGE_STATUS = 0xC0
    private const val SINGLE_DATA_BYTE_SKIP = 1
    private const val DOUBLE_DATA_BYTE_SKIP = 2

    /**
     * Internal representation of a MIDI note with tick-based timing.
     */
    data class MidiNoteInternal(
        val pitch: Int,
        val velocity: Int,
        val startTick: Long,
        val durationTicks: Long,
    )

    /**
     * Stores information about a note that has started but not yet ended.
     */
    data class NoteStartInfo(
        val startTick: Long,
        val velocity: Int,
    )

    /**
     * Handle a note-on MIDI event.
     */
    fun handleNoteOnEvent(
        statusByte: Int,
        reader: ByteArrayReader,
        activeNotes: MutableMap<Pair<Int, Int>, NoteStartInfo>,
        trackNotes: MutableList<MidiNoteInternal>,
        currentTick: Long,
    ) {
        val channel = statusByte and STATUS_MASK_0F
        val noteNumber = reader.readUInt8()
        val velocity = reader.readUInt8()

        if (velocity > 0) {
            // Start of note
            activeNotes[Pair(channel, noteNumber)] =
                NoteStartInfo(
                    startTick = currentTick,
                    velocity = velocity,
                )
        } else {
            // Note on with velocity 0 is equivalent to note off
            handleNoteOff(activeNotes, trackNotes, channel, noteNumber, currentTick)
        }
    }

    /**
     * Handle a note-off MIDI event.
     */
    fun handleNoteOffEvent(
        statusByte: Int,
        reader: ByteArrayReader,
        activeNotes: MutableMap<Pair<Int, Int>, NoteStartInfo>,
        trackNotes: MutableList<MidiNoteInternal>,
        currentTick: Long,
    ) {
        val channel = statusByte and STATUS_MASK_0F
        val noteNumber = reader.readUInt8()
        reader.readUInt8() // Velocity (ignored for note off)

        handleNoteOff(activeNotes, trackNotes, channel, noteNumber, currentTick)
    }

    /**
     * Handle other MIDI events (program change, control change, etc.).
     */
    fun handleOtherMidiEvent(
        statusByte: Int,
        reader: ByteArrayReader,
    ) {
        when {
            // Program change, channel pressure - 1 data byte
            (statusByte and STATUS_MASK_E0) == PROGRAM_CHANGE_STATUS -> reader.skip(SINGLE_DATA_BYTE_SKIP)
            // Most other events - 2 data bytes
            else -> reader.skip(DOUBLE_DATA_BYTE_SKIP)
        }
    }

    /**
     * Handle a note-off event (common logic for both note-off and note-on with velocity 0).
     */
    private fun handleNoteOff(
        activeNotes: MutableMap<Pair<Int, Int>, NoteStartInfo>,
        notes: MutableList<MidiNoteInternal>,
        channel: Int,
        pitch: Int,
        currentTick: Long,
    ) {
        val noteKey = Pair(channel, pitch)
        val startInfo = activeNotes.remove(noteKey) ?: return

        // Calculate duration in ticks
        val durationTicks = currentTick - startInfo.startTick

        // Only add notes with positive duration
        if (durationTicks > 0) {
            notes.add(
                MidiNoteInternal(
                    pitch = pitch,
                    velocity = startInfo.velocity,
                    startTick = startInfo.startTick,
                    durationTicks = durationTicks,
                ),
            )
        }
    }
}
