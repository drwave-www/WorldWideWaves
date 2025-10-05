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
import com.worldwidewaves.shared.sound.midi.MidiEventProcessor
import com.worldwidewaves.shared.sound.midi.MidiHeaderValidator
import com.worldwidewaves.shared.sound.midi.MidiTimeConverter
import com.worldwidewaves.shared.sound.midi.MidiTrackParser
import com.worldwidewaves.shared.utils.ByteArrayReader
import com.worldwidewaves.shared.utils.Log
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.time.Duration

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
 * Handles parsing of Standard MIDI File (SMF) format with global caching
 */
object MidiParser {
    // Logging tag
    private const val TAG = "MidiParser"

    // Global MIDI file cache - keyed by resource path
    // This ensures MIDI files are loaded only once for the entire application lifecycle
    private val midiCache = mutableMapOf<String, MidiTrack?>()

    // MIDI standard constants
    private const val DEFAULT_MICROSECONDS_PER_BEAT = 500_000L // 120 BPM
    private const val MICROSECONDS_PER_MINUTE = 60_000_000L
    private const val DEBUG_NOTES_LIMIT = 5

    // ------------------------------------------------------------------------

    /**
     * Parse a MIDI file into a MidiTrack
     */
    suspend fun parseMidiFile(midiResourcePath: String): MidiTrack? {
        // Check cache first
        if (midiCache.containsKey(midiResourcePath)) {
            Log.d(TAG, "Using cached MIDI file: $midiResourcePath")
            return midiCache[midiResourcePath]
        }

        // Load and parse MIDI file if not cached
        Log.d(TAG, "Loading MIDI file (not cached): $midiResourcePath")
        val track =
            try {
                val midiBytes = MidiResources.readMidiFile(midiResourcePath)
                val parsedTrack = parseMidiBytes(midiBytes)
                Log.i(TAG, "Successfully parsed and cached MIDI file: $midiResourcePath")
                parsedTrack
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid MIDI file format $midiResourcePath: ${e.message}")
                null
            } catch (e: org.jetbrains.compose.resources.MissingResourceException) {
                Log.e(TAG, "Resource not found $midiResourcePath: ${e.message}")
                null
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state reading MIDI file $midiResourcePath: ${e.message}")
                null
            } catch (e: RuntimeException) {
                Log.e(TAG, "Runtime error reading MIDI file $midiResourcePath: ${e.message}")
                null
            } catch (e: Exception) {
                @Suppress("TooGenericExceptionCaught") // Catch-all for unexpected errors during resource loading
                Log.e(TAG, "Unexpected error reading MIDI file $midiResourcePath: ${e.message}")
                null
            }

        // Cache the result (even if null to avoid repeated failed attempts)
        midiCache[midiResourcePath] = track
        return track
    }

    /**
     * Clear the MIDI cache (useful for testing or memory management)
     */
    fun clearCache() {
        Log.d(TAG, "Clearing MIDI cache (${midiCache.size} entries)")
        midiCache.clear()
    }

    /**
     * Get cache statistics for debugging
     */
    fun getCacheStats(): String = "MIDI cache: ${midiCache.size} files cached"

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

            // Validate header and get format information
            val (format, numTracks, timeDivision) = MidiHeaderValidator.validateHeader(reader)
            val ticksPerBeat = MidiHeaderValidator.calculateTicksPerBeat(timeDivision)

            // Memory optimization: Use streaming approach to reduce peak memory usage
            val finalNotes = mutableListOf<MidiEventProcessor.MidiNoteInternal>()
            val globalTempoChanges = mutableListOf<MidiTimeConverter.TempoChange>()

            // Default tempo (120 BPM)
            globalTempoChanges.add(MidiTimeConverter.TempoChange(0, DEFAULT_MICROSECONDS_PER_BEAT))

            // Process each track incrementally to minimize memory footprint
            for (i in 0 until numTracks) {
                Log.d(TAG, "Processing track $i of $numTracks (streaming mode)")

                // Parse track and collect results
                val trackResult = MidiTrackParser.parseTrack(reader, bytes, i)

                // Add track notes and tempo changes to global collections
                globalTempoChanges.addAll(trackResult.tempoChanges)
                finalNotes.addAll(trackResult.notes)

                Log.v(TAG, "Track $i processed: ${finalNotes.size} total notes so far")
            }

            // Sort tempo changes by tick position
            globalTempoChanges.sortBy { it.tick }

            // Convert internal note representation to final MidiNote objects
            val notes =
                finalNotes.map { internalNote ->
                    val startTimeSeconds =
                        MidiTimeConverter.ticksToRealTime(internalNote.startTick, ticksPerBeat, globalTempoChanges)
                    val endTimeSeconds =
                        MidiTimeConverter.ticksToRealTime(
                            internalNote.startTick + internalNote.durationTicks,
                            ticksPerBeat,
                            globalTempoChanges,
                        )

                    MidiNote(
                        pitch = internalNote.pitch,
                        velocity = internalNote.velocity,
                        startTime = startTimeSeconds,
                        duration = endTimeSeconds - startTimeSeconds,
                    )
                }

            // Calculate final tempo (use the last tempo change)
            val finalTempo = MICROSECONDS_PER_MINUTE / globalTempoChanges.last().microsecondsPerBeat

            // Calculate total duration
            val lastTick = finalNotes.maxOfOrNull { it.startTick + it.durationTicks } ?: 0L
            val totalDuration = MidiTimeConverter.ticksToRealTime(lastTick, ticksPerBeat, globalTempoChanges)

            Log.d(TAG, "MIDI file details:")
            Log.d(TAG, "Format: $format, Tracks: $numTracks, Ticks per beat: $ticksPerBeat")
            Log.d(TAG, "Initial tempo: ${MICROSECONDS_PER_MINUTE / globalTempoChanges.first().microsecondsPerBeat} BPM")
            Log.d(TAG, "Tempo changes: ${globalTempoChanges.size}")
            Log.d(TAG, "Notes found: ${notes.size}")
            Log.d(TAG, "Total duration: ${totalDuration.inWholeSeconds} seconds")

            // Dump first few notes for debugging
            notes.take(DEBUG_NOTES_LIMIT).forEachIndexed { index, note ->
                Log.d(
                    TAG,
                    "Note $index: Pitch=${note.pitch}, Start=${note.startTime.inWholeMilliseconds}ms, " +
                        "Duration=${note.duration.inWholeMilliseconds}ms",
                )
            }

            return MidiTrack(
                name = "Parsed MIDI Track",
                notes = notes,
                totalDuration = totalDuration,
                tempo = finalTempo.toInt(),
            )
        } catch (e: IllegalArgumentException) {
            // Already wrapped and logged in helper functions
            throw e
        } catch (e: Exception) {
            val runtimeException = IllegalArgumentException("Unexpected error parsing MIDI: ${e.message}", e)
            Log.e(TAG, "Unexpected error parsing MIDI: ${e.message}", throwable = e)
            throw runtimeException
        }
    }
}
