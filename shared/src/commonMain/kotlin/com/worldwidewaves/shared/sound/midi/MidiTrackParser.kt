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
 * Handles MIDI track parsing including track validation and event processing.
 *
 * Parses individual MIDI tracks, extracting:
 * - Track events (notes, tempo changes, meta events)
 * - Timing information
 * - Track metadata
 */
internal object MidiTrackParser {
    private const val TAG = "MidiTrackParser"

    // Track constants
    private const val TRACK_CHUNK_ID = "MTrk"
    private const val META_EVENT = 0xFF
    private const val TEMPO_META_TYPE = 0x51
    private const val NOTE_ON = 0x90
    private const val NOTE_OFF = 0x80
    private const val RUNNING_STATUS_MASK = 0x80
    private const val STATUS_MASK_F0 = 0xF0
    private const val CHUNK_ID_LENGTH = 4
    private const val TEMPO_META_LENGTH = 3
    private const val BYTE_SHIFT_16 = 16
    private const val BYTE_SHIFT_8 = 8

    /**
     * Result of parsing a single track.
     */
    data class TrackParseResult(
        val notes: List<MidiEventProcessor.MidiNoteInternal>,
        val tempoChanges: List<MidiTimeConverter.TempoChange>,
    )

    /**
     * Validate and parse a single MIDI track.
     *
     * @param reader ByteArrayReader positioned at track start
     * @param bytes Complete MIDI file bytes (for bounds checking)
     * @param trackIndex Track number (for error reporting)
     * @return TrackParseResult containing notes and tempo changes
     * @throws IllegalArgumentException if track is invalid or malformed
     */
    @Suppress("ThrowsCount") // Multiple exception types for comprehensive track validation
    fun parseTrack(
        reader: ByteArrayReader,
        bytes: ByteArray,
        trackIndex: Int,
    ): TrackParseResult {
        // Validate track header
        val trackEndPosition = validateTrackHeader(reader, bytes, trackIndex)

        // Track-local storage
        val trackNotes = mutableListOf<MidiEventProcessor.MidiNoteInternal>()
        val activeNotes = mutableMapOf<Pair<Int, Int>, MidiEventProcessor.NoteStartInfo>()
        val trackTempoChanges = mutableListOf<MidiTimeConverter.TempoChange>()

        var currentTick = 0L
        var runningStatus = 0

        // Process track events
        while (reader.position < trackEndPosition) {
            // Read delta time
            val deltaTime = reader.readVariableLengthQuantity()
            currentTick += deltaTime

            // Read event
            var statusByte = reader.readUInt8()

            // Handle running status
            if ((statusByte and RUNNING_STATUS_MASK) == 0) {
                // This is not a status byte but a data byte - use running status
                reader.position-- // Move back to re-read this byte as data
                statusByte = runningStatus
            } else {
                runningStatus = statusByte
            }

            // Process based on event type
            when {
                statusByte == META_EVENT -> {
                    processMetaEvent(reader, trackTempoChanges, currentTick)
                }
                (statusByte and STATUS_MASK_F0) == NOTE_ON -> {
                    MidiEventProcessor.handleNoteOnEvent(statusByte, reader, activeNotes, trackNotes, currentTick)
                }
                (statusByte and STATUS_MASK_F0) == NOTE_OFF -> {
                    MidiEventProcessor.handleNoteOffEvent(statusByte, reader, activeNotes, trackNotes, currentTick)
                }
                (statusByte and RUNNING_STATUS_MASK) != 0 -> {
                    MidiEventProcessor.handleOtherMidiEvent(statusByte, reader)
                }
            }
        }

        Log.v(TAG, "Track $trackIndex processed: ${trackNotes.size} notes, ${trackTempoChanges.size} tempo changes")

        return TrackParseResult(
            notes = trackNotes,
            tempoChanges = trackTempoChanges,
        )
    }

    /**
     * Validate track header and return track end position.
     */
    @Suppress("ThrowsCount") // Multiple exception types for comprehensive track validation
    private fun validateTrackHeader(
        reader: ByteArrayReader,
        bytes: ByteArray,
        trackIndex: Int,
    ): Int {
        try {
            val trackChunkId = reader.readString(CHUNK_ID_LENGTH)
            require(trackChunkId == TRACK_CHUNK_ID) { "Invalid track chunk ID: $trackChunkId" }

            val trackLength = reader.readInt32()
            val remainingBytes = bytes.size - reader.position
            require(trackLength >= 0) { "Invalid track length: $trackLength (negative length)" }
            require(
                trackLength <= remainingBytes,
            ) { "Invalid track length: $trackLength bytes claimed but only $remainingBytes bytes available" }

            return reader.position + trackLength
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid track $trackIndex format: ${e.message}", throwable = e)
            throw IllegalArgumentException("Invalid track $trackIndex format: ${e.message}", e)
        } catch (e: IndexOutOfBoundsException) {
            @Suppress("TooGenericExceptionCaught") // IndexOutOfBoundsException is the most specific exception for malformed track data
            Log.e(TAG, "Malformed track $trackIndex data: ${e.message}", throwable = e)
            @Suppress("TooGenericExceptionCaught")
            throw IllegalArgumentException("Malformed track $trackIndex data: ${e.message}", e)
        } catch (e: NumberFormatException) {
            Log.e(TAG, "Invalid number format in track $trackIndex: ${e.message}", throwable = e)
            throw IllegalArgumentException("Invalid number format in track $trackIndex: ${e.message}", e)
        } catch (e: ArithmeticException) {
            Log.e(TAG, "Arithmetic error in track $trackIndex: ${e.message}", throwable = e)
            throw IllegalArgumentException("Arithmetic error in track $trackIndex: ${e.message}", e)
        } catch (e: Exception) {
            @Suppress("TooGenericExceptionCaught") // Catch-all for any unexpected track parsing errors
            Log.e(TAG, "Unexpected error in track $trackIndex: ${e.message}", throwable = e)
            @Suppress("TooGenericExceptionCaught")
            throw IllegalArgumentException("Unexpected error in track $trackIndex: ${e.message}", e)
        }
    }

    /**
     * Process a meta event (tempo, time signature, etc.).
     */
    private fun processMetaEvent(
        reader: ByteArrayReader,
        trackTempoChanges: MutableList<MidiTimeConverter.TempoChange>,
        currentTick: Long,
    ) {
        val metaType = reader.readUInt8()
        val metaLength = reader.readVariableLengthQuantity().toInt()

        if (metaType == TEMPO_META_TYPE && metaLength == TEMPO_META_LENGTH) {
            // Tempo change event
            val microsecondsPerBeat =
                (reader.readUInt8().toLong() shl BYTE_SHIFT_16) or
                    (reader.readUInt8().toLong() shl BYTE_SHIFT_8) or
                    reader.readUInt8().toLong()
            // Store tempo change with its tick position
            trackTempoChanges.add(MidiTimeConverter.TempoChange(currentTick, microsecondsPerBeat))
        } else {
            // Skip other meta events
            reader.skip(metaLength)
        }
    }
}
