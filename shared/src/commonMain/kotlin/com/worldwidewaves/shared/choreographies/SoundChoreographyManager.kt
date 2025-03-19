package com.worldwidewaves.shared.choreographies

import com.worldwidewaves.shared.WWWGlobals.Companion.FS_CHOREOGRAPHIES_SOUND_MIDIFILE
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DefaultCoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.generated.resources.Res
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

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

/**
 * Manages musical choreography for the wave experience,
 * allowing each device to play one note from a collective melody.
 */
class SoundChoreographyManager(
    coroutineScopeProvider: CoroutineScopeProvider = DefaultCoroutineScopeProvider()
) : KoinComponent {
    private val clock: IClock by inject()
    private val soundPlayer: SoundPlayer by inject()

    // MIDI track data
    private var currentTrack: MidiTrack? = null
    private var looping: Boolean = true

    // Selected instrument settings
    private var selectedWaveform = SoundPlayer.Waveform.SINE

    init {
        coroutineScopeProvider.launchIO {
            preloadMidiFile(FS_CHOREOGRAPHIES_SOUND_MIDIFILE)
        }
    }

    /**
     * Preload a MIDI file for later playback
     */
    suspend fun preloadMidiFile(midiResourcePath: String): Boolean {
        try {
            currentTrack = MidiParser.parseMidiFile(midiResourcePath)
            return currentTrack != null
        } catch (e: Exception) {
            Log.e(::preloadMidiFile.name, "Failed to load MIDI file: ${e.message}")
            return false
        }
    }

    fun setCurrentTrack(track: MidiTrack) {
        currentTrack = track
    }

    /**
     * Play a random tone from the notes that would be active at the current position
     * in the MIDI track based on elapsed time since the given start time.
     */
    suspend fun playCurrentSoundTone(waveStartTime: Instant): Int? {
        val track = currentTrack ?: return null

        // Calculate elapsed time since wave start
        val elapsedTime = clock.now() - waveStartTime

        // Calculate position in the track, with looping
        val trackPosition = if (looping && track.totalDuration > Duration.ZERO) {
            (elapsedTime.inWholeNanoseconds % track.totalDuration.inWholeNanoseconds).nanoseconds
        } else {
            elapsedTime
        }

        // Find all notes that are active at this position
        val activeNotes = track.notes.filter { it.isActiveAt(trackPosition) }

        if (activeNotes.isEmpty()) return null

        // Select a random note from active notes
        val selectedNote = activeNotes[Random.nextInt(activeNotes.size)]

        // Convert MIDI pitch to frequency using shared utility
        val frequency = WaveformGenerator.midiPitchToFrequency(selectedNote.pitch)

        // Convert MIDI velocity to amplitude using shared utility
        val amplitude = WaveformGenerator.midiVelocityToAmplitude(selectedNote.velocity) * 0.8

        // Play the tone using the platform-specific implementation
        soundPlayer.playTone(
            frequency = frequency,
            amplitude = amplitude,
            duration = selectedNote.duration.coerceAtMost(2.seconds),
            waveform = selectedWaveform
        )

        return selectedNote.pitch
    }

    /**
     * Set the waveform type for synthesis
     */
    fun setWaveform(waveform: SoundPlayer.Waveform) {
        selectedWaveform = waveform
    }

    /**
     * Set whether playback should loop when reaching the end of the track
     */
    fun setLooping(loop: Boolean) {
        looping = loop
    }

    /**
     * Get the total duration of the current track
     */
    fun getTotalDuration(): Duration {
        return currentTrack?.totalDuration ?: Duration.ZERO
    }

    /**
     * Clear loaded resources
     */
    fun release() {
        soundPlayer.release()
        currentTrack = null
    }

}

// ------------------------------

/**
 * Interface for platform-specific sound playback using synthesis
 */
interface SoundPlayer {
    /**
     * Play a synthesized tone
     * @param frequency Frequency in Hz (can be calculated from MIDI pitch)
     * @param amplitude Volume between 0.0 and 1.0
     * @param duration How long to play the tone
     * @param waveform Type of waveform (sine, square, etc.)
     */
    suspend fun playTone(frequency: Double, amplitude: Double, duration: Duration, waveform: Waveform = Waveform.SINE)

    /**
     * Release resources
     */
    fun release()

    /**
     * Available waveform types for synthesis
     */
    enum class Waveform {
        SINE,      // Smooth, pure tone
        SQUARE,    // Rich in harmonics, sounds "buzzy"
        TRIANGLE,  // Smoother than square, but with some harmonics
        SAWTOOTH   // Very rich in harmonics, sounds "brassy"
    }
}

/**
 * Interface for platform-specific volume control
 */
interface VolumeController {
    /**
     * Get the current volume level (0.0 to 1.0)
     */
    fun getCurrentVolume(): Float

    /**
     * Set the volume level (0.0 to 1.0)
     */
    fun setVolume(level: Float)

}

/**
 * Shared waveform generation algorithms for all platforms
 */
object WaveformGenerator {
    /**
     * Generate sample array for the specified waveform
     * @param sampleRate Sample rate in Hz (e.g., 44100)
     * @param frequency Tone frequency in Hz
     * @param amplitude Amplitude between 0.0 and 1.0
     * @param duration Duration of the tone
     * @param waveform Type of waveform to generate
     * @return Array of sound samples between -1.0 and 1.0
     */
    fun generateWaveform(
        sampleRate: Int,
        frequency: Double,
        amplitude: Double,
        duration: Duration,
        waveform: SoundPlayer.Waveform
    ): DoubleArray {
        // Calculate number of samples
        val numSamples = (sampleRate * duration.inWholeSeconds +
                (sampleRate * (duration.inWholeNanoseconds % 1_000_000_000) / 1_000_000_000.0)).toInt()

        // Create sample array
        val samples = DoubleArray(numSamples)

        // Generate the specified waveform
        for (i in 0 until numSamples) {
            val phase = 2.0 * PI * i / (sampleRate / frequency)
            samples[i] = when (waveform) {
                SoundPlayer.Waveform.SINE -> sin(phase)
                SoundPlayer.Waveform.SQUARE -> if (sin(phase) >= 0) 1.0 else -1.0
                SoundPlayer.Waveform.TRIANGLE -> {
                    val normPhase = (phase / (2.0 * PI)) % 1.0
                    when {
                        normPhase < 0.25 -> normPhase * 4.0
                        normPhase < 0.75 -> 2.0 - (normPhase * 4.0)
                        else -> (normPhase * 4.0) - 4.0
                    }
                }
                SoundPlayer.Waveform.SAWTOOTH -> {
                    val normPhase = (phase / (2.0 * PI)) % 1.0
                    2.0 * (normPhase - floor(0.5 + normPhase))
                }
            } * amplitude // Apply amplitude scaling
        }

        // Apply envelope to avoid clicks
        applyEnvelope(samples, sampleRate)

        return samples
    }

    /**
     * Apply a simple attack/release envelope to avoid clicks
     */
    private fun applyEnvelope(samples: DoubleArray, sampleRate: Int) {
        val attackTime = 0.01 // 10ms attack
        val releaseTime = 0.01 // 10ms release

        val attackSamples = (sampleRate * attackTime).toInt()
        val releaseSamples = (sampleRate * releaseTime).toInt()

        // Apply attack (fade in)
        for (i in 0 until attackSamples.coerceAtMost(samples.size)) {
            samples[i] *= i.toDouble() / attackSamples
        }

        // Apply release (fade out)
        val releaseStart = (samples.size - releaseSamples).coerceAtLeast(0)
        for (i in releaseStart until samples.size) {
            samples[i] *= (samples.size - i).toDouble() / releaseSamples
        }
    }

    /**
     * Convert MIDI pitch to frequency in Hz
     * A4 (MIDI note 69) = 440Hz
     */
    fun midiPitchToFrequency(pitch: Int): Double {
        return 440.0 * 2.0.pow((pitch - 69).toDouble() / 12.0)
    }

    /**
     * Convert MIDI velocity (0-127) to amplitude (0.0-1.0)
     */
    fun midiVelocityToAmplitude(velocity: Int): Double {
        return (velocity / 127.0).coerceIn(0.0, 1.0)
    }
}

/**
 * Base audio buffer interface for platform-specific implementations
 */
interface AudioBuffer {
    /**
     * Get the raw byte buffer for platform-specific audio APIs
     */
    fun getRawBuffer(): ByteArray

    /**
     * Number of samples in this buffer
     */
    val sampleCount: Int

    /**
     * Sample rate of this buffer
     */
    val sampleRate: Int
}

/**
 * Factory for creating platform-specific audio buffers
 */
expect object AudioBufferFactory {
    /**
     * Create platform-specific audio buffer from samples
     */
    fun createFromSamples(
        samples: DoubleArray,
        sampleRate: Int,
        bitsPerSample: Int = 16,
        channels: Int = 1
    ): AudioBuffer
}

/**
 * Helper for accessing MIDI resources
 */
object MidiResources {
    /**
     * Read a MIDI file from resources
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun readMidiFile(path: String): ByteArray {
        // Implementation would use platform-specific resource loading
        // For Compose Multiplatform:
        return Res.readBytes(path)
    }
}

// ------------------------------

/**
 * Represents a musical note with timing information from a MIDI file
 */
data class MidiNote(
    val pitch: Int,        // MIDI note number (0-127)
    val velocity: Int,     // Note velocity/volume (0-127)
    val startTime: Duration, // When the note starts (from song beginning)
    val duration: Duration   // How long the note lasts
) {
    // Calculate when the note ends
    private val endTime: Duration get() = startTime + duration

    // Check if this note is active at a given time
    fun isActiveAt(timePosition: Duration): Boolean {
        return timePosition >= startTime && timePosition < endTime
    }
}

/**
 * Represents a MIDI track or song with metadata and notes
 */
data class MidiTrack(
    val name: String,
    val notes: List<MidiNote>,
    val totalDuration: Duration,
    val tempo: Int = 120 // BPM
)

// ------------------------------

/**
 * Handles parsing of Standard MIDI File (SMF) format
 */
object MidiParser {
    // Constants for MIDI file parsing
    private const val HEADER_CHUNK_ID = "MThd"
    private const val TRACK_CHUNK_ID = "MTrk"
    private const val META_EVENT = 0xFF
    private const val TEMPO_META_TYPE = 0x51
    private const val NOTE_ON = 0x90
    private const val NOTE_OFF = 0x80

    /**
     * Parse a MIDI file into a MidiTrack
     */
    suspend fun parseMidiFile(midiResourcePath: String): MidiTrack {
        return try {
            val midiBytes = MidiResources.readMidiFile(midiResourcePath)
            parseMidiBytes(midiBytes)
        } catch (e: Exception) {
            Log.e("MidiParser", "Failed to parse MIDI file: ${e.message}")
            throw e
        }
    }

    /**
     * Parse raw MIDI file bytes using a custom SMF parser
     */
    private fun parseMidiBytes(bytes: ByteArray): MidiTrack {
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
            if (headerLength != 6) {
                throw Exception("Invalid MIDI header length: $headerLength")
            }

            // Read format type (0 = single track, 1 = multiple tracks, same timing, 2 = multiple tracks, independent timing)
            val format = reader.readInt16()

            // Read number of tracks
            val numTracks = reader.readInt16()

            // Read time division
            val timeDivision = reader.readInt16()
            val ticksPerBeat = if ((timeDivision and 0x8000) == 0) {
                // Ticks per quarter note
                timeDivision
            } else {
                // SMPTE frames - not supported in this simple implementation
                24 // Default to 24 ticks per beat
            }

            // Prepare to collect notes and tempo changes
            val internalNotes = mutableListOf<MidiNoteInternal>()
            val activeNotes = mutableMapOf<Pair<Int, Int>, NoteStartInfo>()
            val tempoChanges = mutableListOf<TempoChange>()

            // Default tempo (120 BPM)
            tempoChanges.add(TempoChange(0, 500000)) // 500,000 microseconds per beat = 120 BPM

            // Process each track
            for (i in 0 until numTracks) {
                // Read track chunk
                val trackChunkId = reader.readString(4)
                if (trackChunkId != TRACK_CHUNK_ID) {
                    throw Exception("Invalid track chunk ID: $trackChunkId")
                }

                // Read track length
                val trackLength = reader.readInt32()
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
                                val microsecondsPerBeat = (reader.readUInt8().toLong() shl 16) or
                                        (reader.readUInt8().toLong() shl 8) or
                                        reader.readUInt8().toLong()
                                // Store tempo change with its tick position
                                tempoChanges.add(TempoChange(currentTick, microsecondsPerBeat))
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
                                activeNotes[Pair(channel, noteNumber)] = NoteStartInfo(
                                    startTick = currentTick,
                                    velocity = velocity
                                )
                            } else {
                                // Note on with velocity 0 is equivalent to note off
                                handleNoteOff(activeNotes, internalNotes, channel, noteNumber, currentTick)
                            }
                        }
                        (statusByte and 0xF0) == NOTE_OFF -> {
                            // Note off event
                            val channel = statusByte and 0x0F
                            val noteNumber = reader.readUInt8()
                            reader.readUInt8() // Velocity (ignored for note off)

                            handleNoteOff(activeNotes, internalNotes, channel, noteNumber, currentTick)
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
            }

            // Sort tempo changes by tick position
            tempoChanges.sortBy { it.tick }

            // Convert internal note representation to final MidiNote objects
            val notes = internalNotes.map { internalNote ->
                val startTimeSeconds = ticksToRealTime(internalNote.startTick, ticksPerBeat, tempoChanges)
                val endTimeSeconds = ticksToRealTime(internalNote.startTick + internalNote.durationTicks, ticksPerBeat, tempoChanges)

                MidiNote(
                    pitch = internalNote.pitch,
                    velocity = internalNote.velocity,
                    startTime = startTimeSeconds,
                    duration = endTimeSeconds - startTimeSeconds
                )
            }

            // Calculate final tempo (use the last tempo change)
            val finalTempo = 60_000_000 / tempoChanges.last().microsecondsPerBeat

            // Calculate total duration
            val lastTick = internalNotes.maxOfOrNull { it.startTick + it.durationTicks } ?: 0
            val totalDuration = ticksToRealTime(lastTick, ticksPerBeat, tempoChanges)

            Log.d("MidiParser", "MIDI file details:")
            Log.d("MidiParser", "Format: $format, Tracks: $numTracks, Ticks per beat: $ticksPerBeat")
            Log.d("MidiParser", "Initial tempo: ${60_000_000 / tempoChanges.first().microsecondsPerBeat} BPM")
            Log.d("MidiParser", "Tempo changes: ${tempoChanges.size}")
            Log.d("MidiParser", "Notes found: ${notes.size}")
            Log.d("MidiParser", "Total duration: ${totalDuration.inWholeSeconds} seconds")

            // Dump first few notes for debugging
            notes.take(5).forEachIndexed { index, note ->
                Log.d("MidiParser", "Note $index: Pitch=${note.pitch}, Start=${note.startTime.inWholeMilliseconds}ms, Duration=${note.duration.inWholeMilliseconds}ms")
            }

            return MidiTrack(
                name = "Parsed MIDI Track",
                notes = notes,
                totalDuration = totalDuration,
                tempo = finalTempo.toInt()
            )
        } catch (e: Exception) {
            Log.e("MidiParser", "Error parsing MIDI bytes: ${e.message}")
            e.printStackTrace()
            return createDefaultTrack()
        }
    }

    /**
     * Data class to represent a tempo change
     */
    private data class TempoChange(val tick: Long, val microsecondsPerBeat: Long)

    /**
     * Convert ticks to real time accounting for tempo changes
     */
    private fun ticksToRealTime(ticks: Long, ticksPerBeat: Int, tempoChanges: List<TempoChange>): Duration {
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

    /**
     * Handle a note-off event
     */
    private fun handleNoteOff(
        activeNotes: MutableMap<Pair<Int, Int>, NoteStartInfo>,
        notes: MutableList<MidiNoteInternal>,
        channel: Int,
        pitch: Int,
        currentTick: Long
    ) {
        val noteKey = Pair(channel, pitch)
        val startInfo = activeNotes.remove(noteKey) ?: return

        // Calculate duration in ticks
        val durationTicks = currentTick - startInfo.startTick

        // Only add notes with positive duration
        if (durationTicks > 0) {
            notes.add(MidiNoteInternal(
                pitch = pitch,
                velocity = startInfo.velocity,
                startTick = startInfo.startTick,
                durationTicks = durationTicks
            ))
        }
    }

    /**
     * Internal representation of a MIDI note with tick timing
     */
    private data class MidiNoteInternal(
        val pitch: Int,
        val velocity: Int,
        val startTick: Long,
        val durationTicks: Long
    )

    /**
     * Helper class to track note start information
     */
    private data class NoteStartInfo(
        val startTick: Long,
        val velocity: Int
    )

    /**
     * Create a default track if parsing fails
     */
    private fun createDefaultTrack(): MidiTrack {
        val notes = mutableListOf<MidiNote>()

        // Create a simple C major scale
        var time = Duration.ZERO
        for (i in 0..7) {
            val pitch = 60 + when(i) {
                0 -> 0 // C
                1 -> 2 // D
                2 -> 4 // E
                3 -> 5 // F
                4 -> 7 // G
                5 -> 9 // A
                6 -> 11 // B
                else -> 12 // C
            }

            notes.add(MidiNote(
                pitch = pitch,
                velocity = 80,
                startTime = time,
                duration = 300.milliseconds
            ))

            time += 500.milliseconds
        }

        return MidiTrack(
            name = "Default C Major Scale",
            notes = notes,
            totalDuration = 4.seconds,
            tempo = 120
        )
    }

    /**
     * Helper class for reading byte arrays
     */
    private class ByteArrayReader(private val bytes: ByteArray) {
        var position: Int = 0

        fun readUInt8(): Int {
            return bytes[position++].toInt() and 0xFF
        }

        fun readInt16(): Int {
            val msb = readUInt8()
            val lsb = readUInt8()
            return (msb shl 8) or lsb
        }

        fun readInt32(): Int {
            val b1 = readUInt8()
            val b2 = readUInt8()
            val b3 = readUInt8()
            val b4 = readUInt8()
            return (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
        }

        fun readString(length: Int): String {
            val chars = CharArray(length)
            for (i in 0 until length) {
                chars[i] = bytes[position + i].toInt().toChar()
            }
            position += length
            return chars.concatToString()
        }

        fun readVariableLengthQuantity(): Long {
            var result: Long = 0
            var currentByte: Int

            do {
                currentByte = readUInt8()
                result = (result shl 7) or (currentByte and 0x7F).toLong()
            } while ((currentByte and 0x80) != 0)

            return result
        }

        fun skip(count: Int) {
            position += count
        }
    }
}