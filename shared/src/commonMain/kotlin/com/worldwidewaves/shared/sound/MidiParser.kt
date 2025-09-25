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
            val midiHeader = parseMidiHeader(reader)
            return processMidiTracks(reader, bytes, midiHeader)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing MIDI bytes: ${e.message}", throwable = e)
            throw e
        }
    }

    private data class MidiHeader(
        val format: Int,
        val numTracks: Int,
        val ticksPerBeat: Int
    )

    private fun parseMidiHeader(reader: ByteArrayReader): MidiHeader {
        val headerChunkId = reader.readString(4)
        Log.d("MidiParser", "Header chunk ID: $headerChunkId")
        if (headerChunkId != HEADER_CHUNK_ID) {
            throw IllegalArgumentException("Not a valid MIDI file (missing MThd header)")
        }

        val headerLength = reader.readInt32()
        if (headerLength != 6) {
            throw IllegalArgumentException("Invalid MIDI header length: $headerLength")
        }

        val format = reader.readInt16()
        val numTracks = reader.readInt16()
        val timeDivision = reader.readInt16()
        val ticksPerBeat = calculateTicksPerBeat(timeDivision)

        return MidiHeader(format, numTracks, ticksPerBeat)
    }

    private fun processMidiTracks(
        reader: ByteArrayReader,
        bytes: ByteArray,
        header: MidiHeader
    ): MidiTrack {

        val finalNotes = mutableListOf<MidiNoteInternal>()
        val globalTempoChanges = mutableListOf<TempoChange>()
        globalTempoChanges.add(TempoChange(0, 500000)) // Default tempo (120 BPM)

        for (i in 0 until header.numTracks) {
            Log.d("MidiParser", "Processing track $i of ${header.numTracks} (streaming mode)")
            val trackData = processTrack(reader, bytes, finalNotes, globalTempoChanges)
        }

        return createMidiTrackFromNotes(finalNotes, globalTempoChanges, header)
    }

    private data class TrackProcessingData(
        val notes: MutableList<MidiNoteInternal>,
        val tempoChanges: MutableList<TempoChange>
    )

    private fun processTrack(
        reader: ByteArrayReader,
        bytes: ByteArray,
        finalNotes: MutableList<MidiNoteInternal>,
        globalTempoChanges: MutableList<TempoChange>
    ): TrackProcessingData {
        val trackNotes = mutableListOf<MidiNoteInternal>()
        val activeNotes = mutableMapOf<Pair<Int, Int>, NoteStartInfo>()
        val trackTempoChanges = mutableListOf<TempoChange>()
        val trackHeader = readTrackHeader(reader, bytes)
        processTrackEvents(
            reader,
            trackHeader.endPosition,
            activeNotes,
            trackNotes,
            trackTempoChanges
        )

        globalTempoChanges.addAll(trackTempoChanges)
        finalNotes.addAll(trackNotes)

        return TrackProcessingData(trackNotes, trackTempoChanges)
    }

    private data class TrackHeader(val endPosition: Int)

    private fun readTrackHeader(reader: ByteArrayReader, bytes: ByteArray): TrackHeader {
        val trackChunkId = reader.readString(4)
        if (trackChunkId != TRACK_CHUNK_ID) {
            throw IllegalArgumentException("Invalid track chunk ID: $trackChunkId")
        }

        val trackLength = reader.readInt32()
        val remainingBytes = bytes.size - reader.position

        if (trackLength < 0) {
            throw IllegalArgumentException("Invalid track length: $trackLength (negative length)")
        }
        if (trackLength > remainingBytes) {
            throw IllegalStateException("Invalid track length: $trackLength bytes claimed but only $remainingBytes bytes available")
        }

        return TrackHeader(reader.position + trackLength)
    }

    private fun processTrackEvents(
        reader: ByteArrayReader,
        trackEndPosition: Int,
        activeNotes: MutableMap<Pair<Int, Int>, NoteStartInfo>,
        trackNotes: MutableList<MidiNoteInternal>,
        trackTempoChanges: MutableList<TempoChange>
    ) {
        var currentTick = 0L
        var runningStatus = 0

        while (reader.position < trackEndPosition) {
            val deltaTime = reader.readVariableLengthQuantity()
            currentTick += deltaTime

            var statusByte = reader.readUInt8()

            // Handle running status
            if ((statusByte and 0x80) == 0) {
                reader.position--
                statusByte = runningStatus
            } else {
                runningStatus = statusByte
            }

            processEvent(statusByte, reader, currentTick, activeNotes, trackNotes, trackTempoChanges)
        }
    }

    private fun processEvent(
        statusByte: Int,
        reader: ByteArrayReader,
        currentTick: Long,
        activeNotes: MutableMap<Pair<Int, Int>, NoteStartInfo>,
        trackNotes: MutableList<MidiNoteInternal>,
        trackTempoChanges: MutableList<TempoChange>
    ) {
        when {
            statusByte == META_EVENT -> {
                processMetaEvent(reader, currentTick, trackTempoChanges)
            }
            (statusByte and 0xF0) == NOTE_ON -> {
                processNoteOn(statusByte, reader, currentTick, activeNotes, trackNotes)
            }
            (statusByte and 0xF0) == NOTE_OFF -> {
                processNoteOff(statusByte, reader, currentTick, activeNotes, trackNotes)
            }
            (statusByte and 0x80) != 0 -> {
                skipOtherMidiEvents(statusByte, reader)
            }
        }
    }

    private fun processMetaEvent(
        reader: ByteArrayReader,
        currentTick: Long,
        trackTempoChanges: MutableList<TempoChange>
    ) {
        val metaType = reader.readUInt8()
        val metaLength = reader.readVariableLengthQuantity().toInt()

        if (metaType == TEMPO_META_TYPE && metaLength == 3) {
            val microsecondsPerBeat = (reader.readUInt8().toLong() shl 16) or
                (reader.readUInt8().toLong() shl 8) or
                reader.readUInt8().toLong()
            trackTempoChanges.add(TempoChange(currentTick, microsecondsPerBeat))
        } else {
            reader.skip(metaLength)
        }
    }

    private fun processNoteOn(
        statusByte: Int,
        reader: ByteArrayReader,
        currentTick: Long,
        activeNotes: MutableMap<Pair<Int, Int>, NoteStartInfo>,
        trackNotes: MutableList<MidiNoteInternal>
    ) {
        val channel = statusByte and 0x0F
        val noteNumber = reader.readUInt8()
        val velocity = reader.readUInt8()

        if (velocity > 0) {
            activeNotes[Pair(channel, noteNumber)] = NoteStartInfo(
                startTick = currentTick,
                velocity = velocity
            )
        } else {
            handleNoteOff(activeNotes, trackNotes, channel, noteNumber, currentTick)
        }
    }

    private fun processNoteOff(
        statusByte: Int,
        reader: ByteArrayReader,
        currentTick: Long,
        activeNotes: MutableMap<Pair<Int, Int>, NoteStartInfo>,
        trackNotes: MutableList<MidiNoteInternal>
    ) {
        val channel = statusByte and 0x0F
        val noteNumber = reader.readUInt8()
        reader.readUInt8() // Velocity (ignored for note off)

        handleNoteOff(activeNotes, trackNotes, channel, noteNumber, currentTick)
    }

    private fun skipOtherMidiEvents(statusByte: Int, reader: ByteArrayReader) {
        when {
            (statusByte and 0xE0) == 0xC0 -> reader.skip(1)
            else -> reader.skip(2)
        }
    }

    private fun createMidiTrackFromNotes(
        finalNotes: MutableList<MidiNoteInternal>,
        globalTempoChanges: MutableList<TempoChange>,
        header: MidiHeader
    ): MidiTrack {

        globalTempoChanges.sortBy { it.tick }

        val notes = convertInternalNotesToMidiNotes(finalNotes, header.ticksPerBeat, globalTempoChanges)
        val finalTempo = 60_000_000 / globalTempoChanges.last().microsecondsPerBeat
        val totalDuration = calculateTotalDuration(finalNotes, header.ticksPerBeat, globalTempoChanges)

        logMidiDetails(header, globalTempoChanges, notes, totalDuration)

        return MidiTrack(
            name = "Parsed MIDI Track",
            notes = notes,
            totalDuration = totalDuration,
            tempo = finalTempo.toInt(),
        )
    }

    private fun convertInternalNotesToMidiNotes(
        internalNotes: List<MidiNoteInternal>,
        ticksPerBeat: Int,
        tempoChanges: List<TempoChange>
    ): List<MidiNote> {
        return internalNotes.map { internalNote ->
            val startTimeSeconds = ticksToRealTime(internalNote.startTick, ticksPerBeat, tempoChanges)
            val endTimeSeconds = ticksToRealTime(
                internalNote.startTick + internalNote.durationTicks,
                ticksPerBeat,
                tempoChanges
            )

            MidiNote(
                pitch = internalNote.pitch,
                velocity = internalNote.velocity,
                startTime = startTimeSeconds,
                duration = endTimeSeconds - startTimeSeconds,
            )
        }
    }

    private fun calculateTotalDuration(
        notes: List<MidiNoteInternal>,
        ticksPerBeat: Int,
        tempoChanges: List<TempoChange>
    ): Duration {
        val lastTick = notes.maxOfOrNull { it.startTick + it.durationTicks } ?: 0L
        return ticksToRealTime(lastTick, ticksPerBeat, tempoChanges)
    }

    private fun logMidiDetails(
        header: MidiHeader,
        tempoChanges: List<TempoChange>,
        notes: List<MidiNote>,
        totalDuration: Duration
    ) {
        Log.d("MidiParser", "MIDI file details:")
        Log.d("MidiParser", "Format: ${header.format}, Tracks: ${header.numTracks}, Ticks per beat: ${header.ticksPerBeat}")
        Log.d("MidiParser", "Initial tempo: ${60_000_000 / tempoChanges.first().microsecondsPerBeat} BPM")
        Log.d("MidiParser", "Tempo changes: ${tempoChanges.size}")
        Log.d("MidiParser", "Notes found: ${notes.size}")
        Log.d("MidiParser", "Total duration: ${totalDuration.inWholeSeconds} seconds")

        notes.take(5).forEachIndexed { index, note ->
            Log.d(
                "MidiParser",
                "Note $index: Pitch=${note.pitch}, Start=${note.startTime.inWholeMilliseconds}ms, Duration=${note.duration.inWholeMilliseconds}ms"
            )
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
