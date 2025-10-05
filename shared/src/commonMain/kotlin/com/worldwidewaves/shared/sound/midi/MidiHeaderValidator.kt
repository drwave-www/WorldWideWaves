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
import com.worldwidewaves.shared.utils.Log

/**
 * Handles MIDI header validation and parsing.
 *
 * Validates the MIDI file header structure and extracts format information:
 * - Format: MIDI file format (0, 1, or 2)
 * - Number of tracks: How many tracks are in the file
 * - Time division: Timing resolution (ticks per beat or SMPTE format)
 */
internal object MidiHeaderValidator {
    private const val TAG = "MidiHeaderValidator"

    // MIDI header constants
    private const val HEADER_CHUNK_ID = "MThd"
    private const val HEADER_CHUNK_LENGTH = 6
    private const val CHUNK_ID_LENGTH = 4
    private const val SMPTE_FRAME_FLAG = 0x8000
    private const val DEFAULT_TICKS_PER_BEAT = 24

    /**
     * Validate MIDI header and extract format information.
     *
     * @param reader ByteArrayReader positioned at the start of MIDI data
     * @return Triple of (format, numTracks, timeDivision)
     * @throws IllegalArgumentException if header is invalid or malformed
     */
    fun validateHeader(reader: ByteArrayReader): Triple<Int, Int, Int> {
        try {
            val headerChunkId = reader.readString(CHUNK_ID_LENGTH)
            Log.d(TAG, "Header chunk ID: $headerChunkId")
            require(headerChunkId == HEADER_CHUNK_ID) { "Not a valid MIDI file (missing MThd header)" }

            val headerLength = reader.readInt32()
            require(headerLength == HEADER_CHUNK_LENGTH) { "Invalid MIDI header length: $headerLength" }

            val format = reader.readInt16()
            val numTracks = reader.readInt16()
            val timeDivision = reader.readInt16()

            return Triple(format, numTracks, timeDivision)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid MIDI format: ${e.message}", throwable = e)
            throw IllegalArgumentException("Invalid MIDI format: ${e.message}", e)
        } catch (e: IndexOutOfBoundsException) {
            @Suppress("TooGenericExceptionCaught") // Catch IndexOutOfBounds as specific MIDI parsing error
            Log.e(TAG, "Malformed MIDI data: ${e.message}", throwable = e)
            throw IllegalArgumentException("Malformed MIDI data: ${e.message}", e)
        }
    }

    /**
     * Calculate ticks per beat from MIDI time division.
     *
     * @param timeDivision The time division value from MIDI header
     * @return Ticks per beat (quarter note)
     */
    fun calculateTicksPerBeat(timeDivision: Int): Int =
        if ((timeDivision and SMPTE_FRAME_FLAG) == 0) {
            // Ticks per quarter note
            timeDivision
        } else {
            // SMPTE frames - not supported in this simple implementation
            DEFAULT_TICKS_PER_BEAT
        }
}
