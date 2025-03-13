package com.worldwidewaves.shared.choreographies

import com.worldwidewaves.shared.WWWGlobals.Companion.FS_CHOREOGRAPHIES_SOUND_MIDIFILE
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DefaultCoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.sound.MidiParser
import com.worldwidewaves.shared.sound.MidiTrack
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.sound.WaveformGenerator
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.random.Random
import kotlin.time.Duration
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
 * allowing each device to play one note from a collective melody when hit arrives.
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
