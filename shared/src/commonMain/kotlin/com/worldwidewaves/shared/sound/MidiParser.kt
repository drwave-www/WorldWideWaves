package com.worldwidewaves.shared.sound

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

import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.utils.ByteArrayReader
import com.worldwidewaves.shared.utils.Log
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Helper for accessing MIDI resources
 */
object MidiResources {
    /**
     * Read a MIDI file from resources
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun readMidiFile(path: String): ByteArray {
        // Implementation could use platform-specific resource loading
        // For Compose Multiplatform:
        return Res.readBytes(path)
    }
}

/**
 * Represents a musical note with timing information from a MIDI file
 */
data class MidiNote(
    val pitch: Int, // MIDI note number (0-127)
    val velocity: Int, // Note velocity/volume (0-127)
    val startTime: Duration, // When the note starts (from song beginning)
    val duration: Duration, // How long the note lasts
) {
    // Calculate when the note ends
    private val endTime: Duration get() = startTime + duration

    // Check if this note is active at a given time
    fun isActiveAt(timePosition: Duration): Boolean = timePosition >= startTime && timePosition < endTime
}

/**
 * Represents a MIDI track or song with metadata and notes
 */
data class MidiTrack(
    val name: String,
    val notes: List<MidiNote>,
    val totalDuration: Duration,
    val tempo: Int = 120, // BPM
)

// ----------------------------------------------------------------------------

/**
 * Handles parsing of Standard MIDI File (SMF) format
 */
object MidiParser {
    // Logging tag
    private const val TAG = "MidiParser"

    // Constants for MIDI file parsing
    private const val HEADER_CHUNK_ID = "MThd"
    private const val TRACK_CHUNK_ID = "MTrk"
    private const val META_EVENT = 0xFF
    private const val TEMPO_META_TYPE = 0x51
    private const val NOTE_ON = 0x90
    private const val NOTE_OFF = 0x80
    private const val SMPTE_FRAME_FLAG = 0x8000
    private const val CHUNK_ID_LENGTH = 4

    // ------------------------------------------------------------------------

    private data class TempoChange(
        val tick: Long,
        val microsecondsPerBeat: Long,
    )

    private data class MidiNoteInternal(
        val pitch: Int,
        val velocity: Int,
        val startTick: Long,
        val durationTicks: Long,
    )

    private data class NoteStartInfo(
        val startTick: Long,
        val velocity: Int,
    )

    // ------------------------------------------------------------------------

    /**
     * Calculate ticks per beat from MIDI time division
     */
    private fun calculateTicksPerBeat(timeDivision: Int): Int =
        if ((timeDivision and SMPTE_FRAME_FLAG) == 0) {
            // Ticks per quarter note
            timeDivision
        } else {
            // SMPTE frames - not supported in this simple implementation
            24 // WWWGlobals.Midi.DEFAULT_TICKS_PER_BEAT
        }

    /**
     * Validate MIDI header and return format information
     */
    private fun readMidiHeader(reader: ByteArrayReader): Triple<Int, Int, Int> {
        val headerChunkId = reader.readString(CHUNK_ID_LENGTH)
        Log.d("MidiParser", "Header chunk ID: $headerChunkId")
        if (headerChunkId != HEADER_CHUNK_ID) {
            throw IllegalArgumentException("Not a valid MIDI file (missing MThd header)")
        }

        val headerLength = reader.readInt32()
        if (headerLength != 6) { // WWWGlobals.Midi.HEADER_CHUNK_LENGTH
            throw IllegalArgumentException("Invalid MIDI header length: $headerLength")
        }

        val format = reader.readInt16()
        val numTracks = reader.readInt16()
        val timeDivision = reader.readInt16()

        return Triple(format, numTracks, timeDivision)
    }

    // ------------------------------------------------------------------------

    /**
     * Parse a MIDI file into a MidiTrack
     */
    suspend fun parseMidiFile(midiResourcePath: String): MidiTrack =
        try {
            val midiBytes = MidiResources.readMidiFile(midiResourcePath)
            parseMidiBytes(midiBytes)
        } catch (e: Exception) {
            Log.e("MidiParser", "Failed to parse MIDI file: ${e.message}")
            throw e
        }

    // ------------------------------------------------------------------------

    /**
     * Parse raw MIDI file bytes using a memory-optimized streaming parser.
     *
     * Performance optimization: Processes tracks incrementally to reduce memory footprint
     * while preserving millisecond-precise timing for wave synchronization.
     */
    fun parseMidiBytes(bytes: ByteArray): MidiTrack {
        try {
            val reader = ByteArrayReader(bytes)

            // Read header chunk
            val headerChunkId = reader.readString(4)
            Log.d("MidiParser", "Header chunk ID: $headerChunkId")
            if (headerChunkId != HEADER_CHUNK_ID) {
                throw Exception("Not a valid MIDI file (missing MThd header)")
            }

            // Read header length (should be 6)
            val headerLength = reader.readInt32()
            if (headerLength != 6) { // WWWGlobals.Midi.HEADER_CHUNK_LENGTH
                throw IllegalArgumentException("Invalid MIDI header length: $headerLength")
            }

            // Read format type (0 = single track, 1 = multiple tracks, same timing, 2 = multiple tracks, independent timing)
            val format = reader.readInt16()

            // Read number of tracks
            val numTracks = reader.readInt16()

            // Read time division
            val timeDivision = reader.readInt16()
            val ticksPerBeat = calculateTicksPerBeat(timeDivision)

            // Memory optimization: Use streaming approach to reduce peak memory usage
            val finalNotes = mutableListOf<MidiNoteInternal>()
            val globalTempoChanges = mutableListOf<TempoChange>()

            // Default tempo (120 BPM)
            globalTempoChanges.add(TempoChange(0, 500000)) // WWWGlobals.Midi.DEFAULT_MICROSECONDS_PER_BEAT

            // Process each track incrementally to minimize memory footprint
            for (i in 0 until numTracks) {
                Log.d("MidiParser", "Processing track $i of $numTracks (streaming mode)")

                // Track-local storage (cleared after each track)
                val trackNotes = mutableListOf<MidiNoteInternal>()
                val activeNotes = mutableMapOf<Pair<Int, Int>, NoteStartInfo>()
                val trackTempoChanges = mutableListOf<TempoChange>()
                // Read track chunk
                val trackChunkId = reader.readString(4)
                if (trackChunkId != TRACK_CHUNK_ID) {
                    throw Exception("Invalid track chunk ID: $trackChunkId")
                }

                // Read track length
                val trackLength = reader.readInt32()

                // Validate track length against available data
                val remainingBytes = bytes.size - reader.position
                if (trackLength < 0) {
                    throw Exception("Invalid track length: $trackLength (negative length)")
                }
                if (trackLength > remainingBytes) {
                    throw Exception("Invalid track length: $trackLength bytes claimed but only $remainingBytes bytes available")
                }

                val trackEndPosition = reader.position + trackLength

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
                    if ((statusByte and 0x80) == 0) {
                        // This is not a status byte but a data byte - use running status
                        reader.position-- // Move back to re-read this byte as data
                        statusByte = runningStatus
                    } else {
                        runningStatus = statusByte
                    }

                    // Process based on event type
                    when {
                        statusByte == META_EVENT -> {
                            // Meta event
                            val metaType = reader.readUInt8()
                            val metaLength = reader.readVariableLengthQuantity().toInt()

                            if (metaType == TEMPO_META_TYPE && metaLength == 3) {
                                // Tempo change event
                                val microsecondsPerBeat =
                                    (reader.readUInt8().toLong() shl 16) or
                                        (reader.readUInt8().toLong() shl 8) or
                                        reader.readUInt8().toLong()
                                // Store tempo change with its tick position
                                trackTempoChanges.add(TempoChange(currentTick, microsecondsPerBeat))
                            } else {
                                // Skip other meta events
                                reader.skip(metaLength)
                            }
                        }
                        (statusByte and 0xF0) == NOTE_ON -> {
                            // Note on event
                            val channel = statusByte and 0x0F
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
                        (statusByte and 0xF0) == NOTE_OFF -> {
                            // Note off event
                            val channel = statusByte and 0x0F
                            val noteNumber = reader.readUInt8()
                            reader.readUInt8() // Velocity (ignored for note off)

                            handleNoteOff(activeNotes, trackNotes, channel, noteNumber, currentTick)
                        }
                        (statusByte and 0x80) != 0 -> {
                            // Other MIDI events - skip data bytes
                            when {
                                (statusByte and 0xE0) == 0xC0 -> reader.skip(1) // Program change, channel pressure - 1 data byte
                                else -> reader.skip(2) // Most other events - 2 data bytes
                            }
                        }
                    }
                }

                // Convert track notes to final format and add to global collections
                // Memory optimization: Process track notes immediately and discard local storage
                globalTempoChanges.addAll(trackTempoChanges)

                // Add track notes to global collection (will be converted later)
                finalNotes.addAll(trackNotes)

                // Clear track-local collections to free memory immediately
                trackNotes.clear()
                trackTempoChanges.clear()
                activeNotes.clear()

                Log.v("MidiParser", "Track $i processed: ${finalNotes.size} total notes so far")
            }

            // Sort tempo changes by tick position
            globalTempoChanges.sortBy { it.tick }

            // Convert internal note representation to final MidiNote objects
            val notes =
                finalNotes.map { internalNote ->
                    val startTimeSeconds = ticksToRealTime(internalNote.startTick, ticksPerBeat, globalTempoChanges)
                    val endTimeSeconds =
                        ticksToRealTime(internalNote.startTick + internalNote.durationTicks, ticksPerBeat, globalTempoChanges)

                    MidiNote(
                        pitch = internalNote.pitch,
                        velocity = internalNote.velocity,
                        startTime = startTimeSeconds,
                        duration = endTimeSeconds - startTimeSeconds,
                    )
                }

            // Calculate final tempo (use the last tempo change)
            val finalTempo = 60_000_000 / globalTempoChanges.last().microsecondsPerBeat

            // Calculate total duration
            val lastTick = finalNotes.maxOfOrNull { it.startTick + it.durationTicks } ?: 0L
            val totalDuration = ticksToRealTime(lastTick, ticksPerBeat, globalTempoChanges)

            Log.d("MidiParser", "MIDI file details:")
            Log.d("MidiParser", "Format: $format, Tracks: $numTracks, Ticks per beat: $ticksPerBeat")
            Log.d("MidiParser", "Initial tempo: ${60_000_000 / globalTempoChanges.first().microsecondsPerBeat} BPM")
            Log.d("MidiParser", "Tempo changes: ${globalTempoChanges.size}")
            Log.d("MidiParser", "Notes found: ${notes.size}")
            Log.d("MidiParser", "Total duration: ${totalDuration.inWholeSeconds} seconds")

            // Dump first few notes for debugging
            notes.take(5).forEachIndexed { index, note ->
                Log.d(
                    "MidiParser",
                    "Note $index: Pitch=${note.pitch}, Start=${note.startTime.inWholeMilliseconds}ms, Duration=${note.duration.inWholeMilliseconds}ms",
                )
            }

            return MidiTrack(
                name = "Parsed MIDI Track",
                notes = notes,
                totalDuration = totalDuration,
                tempo = finalTempo.toInt(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing MIDI bytes: ${e.message}", throwable = e)
            throw e
        }
    }

    // ------------------------------------------------------------------------

    /**
     * Convert ticks to real time accounting for tempo changes
     */
    private fun ticksToRealTime(
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
                    (lastTempoMPB.toDouble() / 1_000_000)
                break
            } else {
                // Calculate segment up to this tempo change
                val ticksInThisTempo = currentChange.tick - lastTempoTick
                elapsedSeconds += (ticksInThisTempo.toDouble() / ticksPerBeat) *
                    (lastTempoMPB.toDouble() / 1_000_000)

                // Move to next tempo
                lastTempoTick = currentChange.tick
                lastTempoMPB = currentChange.microsecondsPerBeat
            }
        }

        // If we went through all tempo changes, calculate the remainder
        if (ticks > lastTempoTick) {
            val ticksInLastTempo = ticks - lastTempoTick
            elapsedSeconds += (ticksInLastTempo.toDouble() / ticksPerBeat) *
                (lastTempoMPB.toDouble() / 1_000_000)
        }

        return elapsedSeconds.seconds
    }

    // ------------------------------------------------------------------------

    /**
     * Handle a note-off event
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
