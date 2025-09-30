package com.worldwidewaves.shared.choreographies

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

import com.worldwidewaves.shared.WWWGlobals.FileSystem
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.sound.MidiParser
import com.worldwidewaves.shared.sound.MidiTrack
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.sound.WaveformGenerator
import com.worldwidewaves.shared.utils.Log
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Manages the **musical choreography** that accompanies a wave.
 *
 * Workflow:
 * • At start-up, the manager pre-loads a MIDI file located at
 *   [FileSystem.CHOREOGRAPHIES_SOUND_MIDIFILE] (or any custom path via
 *   [preloadMidiFile]). The file is parsed into a single [MidiTrack] that
 *   stores note, timing and velocity information.
 * • **MIDI files are cached globally** - once a MIDI file is loaded, it's reused
 *   across all events and manager instances for the entire application lifecycle.
 *   This ensures optimal performance and memory usage.
 * • When a device gets "hit" by the wave the UI calls [playCurrentSoundTone]
 *   passing the *wave start* timestamp. The manager maps the current
 *   `clock.now() – waveStartTime` to a position inside the track (with optional
 *   looping) and fetches all notes whose `[start,end]` window contains that
 *   position.
 * • One of those active notes is randomly selected so each device contributes
 *   a different tone, creating a crowd-sourced chord.
 * • The selected note's pitch / velocity are converted to
 *   `frequency` / `amplitude` using helpers in [WaveformGenerator] and finally
 *   played through the platform-specific [SoundPlayer] with the currently
 *   chosen [Waveform][SoundPlayer.Waveform] (default *sine*).
 *
 * Public knobs:
 * • [setWaveform] lets callers choose another synthesis waveform.
 * • [setLooping] controls whether the track should wrap when reaching its end.
 * • [getTotalDuration] exposes the track length for progress UI.
 * • [release] frees audio resources when the enclosing screen is disposed.
 */
@OptIn(ExperimentalTime::class)
class SoundChoreographyPlayer : KoinComponent {
    private val clock: IClock by inject()
    private val soundPlayer: SoundPlayer by inject()

    // MIDI track data
    private var currentTrack: MidiTrack? = null
    private var looping: Boolean = true
    private var isInitialized: Boolean = false

    // Selected instrument settings - SQUARE waveform has richer harmonics for better perceived loudness
    private var selectedWaveform = SoundPlayer.Waveform.SQUARE

    // iOS FIX: Removed init{} block to prevent coroutine deadlocks
    // MIDI preloading now must be triggered from @Composable LaunchedEffect

    /**
     * ⚠️ iOS CRITICAL: Initialize manager by preloading default MIDI file.
     * Must be called from @Composable LaunchedEffect, never from init{} or constructor.
     *
     * This method is idempotent - it will only initialize once per manager instance.
     * Subsequent calls are no-ops that return immediately.
     */
    suspend fun initialize() {
        if (isInitialized) {
            Log.d("SoundChoreographyManager", "Already initialized, skipping")
            return
        }

        val success = preloadMidiFile(FileSystem.CHOREOGRAPHIES_SOUND_MIDIFILE)
        if (success) {
            isInitialized = true
        }
    }

    /**
     * Preload a MIDI file for later playback
     */
    suspend fun preloadMidiFile(midiResourcePath: String): Boolean {
        Log.d("SoundChoreographyManager", "Attempting to preload MIDI file: $midiResourcePath")
        try {
            currentTrack = MidiParser.parseMidiFile(midiResourcePath)
            val success = currentTrack != null
            if (success) {
                Log.d("SoundChoreographyManager", "Successfully preloaded MIDI file: $midiResourcePath")
            } else {
                Log.w("SoundChoreographyManager", "MIDI file returned null: $midiResourcePath")
            }
            return success
        } catch (e: Exception) {
            Log.e("SoundChoreographyManager", "Failed to load MIDI file $midiResourcePath: ${e.message}")
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
        val track = currentTrack
        if (track == null) {
            return null
        }

        // Calculate elapsed time since wave start
        val elapsedTime = clock.now() - waveStartTime

        // Calculate position in the track, with looping
        val trackPosition =
            if (looping && track.totalDuration > Duration.ZERO) {
                (elapsedTime.inWholeNanoseconds % track.totalDuration.inWholeNanoseconds).nanoseconds
            } else {
                elapsedTime
            }

        // Find all notes that are active at this position
        val activeNotes = track.notes.filter { it.isActiveAt(trackPosition) }

        if (activeNotes.isEmpty()) {
            return null
        }

        // Select a random note from active notes
        val selectedNote = activeNotes[Random.nextInt(activeNotes.size)]

        // Convert MIDI pitch to frequency using shared utility
        val frequency = WaveformGenerator.midiPitchToFrequency(selectedNote.pitch)

        // Convert MIDI velocity to amplitude using shared utility - use full amplitude for maximum loudness
        val amplitude = WaveformGenerator.midiVelocityToAmplitude(selectedNote.velocity)

        // Play the tone using the platform-specific implementation
        soundPlayer.playTone(
            frequency = frequency,
            amplitude = amplitude,
            duration = selectedNote.duration.coerceAtMost(2.seconds),
            waveform = selectedWaveform,
        )

        val result = selectedNote.pitch
        return result
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
    fun getTotalDuration(): Duration = currentTrack?.totalDuration ?: Duration.ZERO

    /**
     * Clear loaded resources
     */
    fun release() {
        soundPlayer.release()
        currentTrack = null
    }
}
