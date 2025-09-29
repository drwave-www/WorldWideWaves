package com.worldwidewaves.debug

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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.WWWGlobals.FileSystem
import com.worldwidewaves.shared.sound.AndroidSoundPlayer
import com.worldwidewaves.shared.sound.MidiParser
import com.worldwidewaves.shared.sound.MidiTrack
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.sound.WaveformGenerator
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Debug Activity for testing real audio playback on Android hardware.
 *
 * ⚠️ WARNING: This activity will produce ACTUAL SOUND through device speakers!
 *
 * This demonstrates:
 * - Real MIDI file parsing and playback
 * - AndroidSoundPlayer with actual audio output
 * - Crowd sound choreography simulation with audible results
 * - Wave progression timing with real sound feedback
 */
@OptIn(ExperimentalTime::class)
class AudioTestActivity : ComponentActivity() {
    companion object {
        private const val TAG = "AudioTest"
    }

    private lateinit var soundPlayer: AndroidSoundPlayer
    private var midiTrack: MidiTrack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        soundPlayer = AndroidSoundPlayer(this)

        setContent {
            MaterialTheme {
                AudioTestScreen()
            }
        }
    }

    @Composable
    @Suppress("FunctionName")
    fun AudioTestScreen() {
        var isLoading by remember { mutableStateOf(false) }
        var isPlaying by remember { mutableStateOf(false) }
        var statusMessage by remember { mutableStateOf("Ready to test audio") }

        val scope = rememberCoroutineScope()

        // Load MIDI track on first composition
        LaunchedEffect(Unit) {
            statusMessage = "Loading MIDI file..."
            midiTrack = loadMidiTrack()
            statusMessage =
                if (midiTrack != null) {
                    "MIDI loaded: ${midiTrack!!.notes.size} notes, ${midiTrack!!.totalDuration.inWholeSeconds}s"
                } else {
                    "Failed to load MIDI, using demo track"
                }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "🎵 Real Audio Crowd Simulation",
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    scope.launch {
                        isPlaying = true
                        statusMessage = "🔊 Playing single test note (A4 - 440Hz)..."
                        playTestNote()
                        statusMessage = "✅ Single note test completed"
                        isPlaying = false
                    }
                },
                enabled = !isLoading && !isPlaying,
            ) {
                Text("Play Test Note")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        isPlaying = true
                        statusMessage = "🎼 Playing MIDI notes sequentially..."
                        playMidiSequence()
                        statusMessage = "✅ MIDI sequence completed"
                        isPlaying = false
                    }
                },
                enabled = !isLoading && !isPlaying && midiTrack != null,
            ) {
                Text("Play MIDI Sequence")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        isPlaying = true
                        statusMessage = "👥 Starting crowd simulation (5 people)..."
                        playCrowdSimulation()
                        statusMessage = "✅ Crowd simulation completed"
                        isPlaying = false
                    }
                },
                enabled = !isLoading && !isPlaying && midiTrack != null,
            ) {
                Text("Play Crowd Simulation")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        isPlaying = true
                        statusMessage = "🌊 Playing full symphony - wave through crowd (10 per 100ms)..."
                        playWaveProgression()
                        statusMessage = "✅ Wave progression completed"
                        isPlaying = false
                    }
                },
                enabled = !isLoading && !isPlaying && midiTrack != null,
            ) {
                Text("🌊 Play Wave Progression")
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isPlaying) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("🔊 Audio playing...")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "⚠️ Make sure your device volume is turned up!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    /**
     * Load MIDI track, fallback to demo if needed
     */
    private suspend fun loadMidiTrack(): MidiTrack? =
        try {
            Log.d(TAG, "Loading MIDI file: ${FileSystem.CHOREOGRAPHIES_SOUND_MIDIFILE}")
            MidiParser.parseMidiFile(FileSystem.CHOREOGRAPHIES_SOUND_MIDIFILE)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load MIDI file, creating demo track: ${e.message}")
            createDemoMidiTrack()
        }

    /**
     * Create a simple demo MIDI track
     */
    private fun createDemoMidiTrack(): MidiTrack {
        // C major scale: C4, D4, E4, F4, G4, A4, B4, C5
        val scaleNotes =
            listOf(60, 62, 64, 65, 67, 69, 71, 72).mapIndexed { index, pitch ->
                com.worldwidewaves.shared.sound.MidiNote(
                    pitch = pitch,
                    velocity = 100,
                    startTime = (index * 800).milliseconds,
                    duration = 600.milliseconds,
                )
            }

        return MidiTrack(
            name = "Demo C Major Scale",
            notes = scaleNotes,
            totalDuration = 8.seconds,
            tempo = 120,
        )
    }

    /**
     * Play a single test note to verify audio works
     */
    private suspend fun playTestNote() {
        Log.d(TAG, "Playing test note: A4 (440Hz)")

        soundPlayer.playTone(
            frequency = 440.0, // A4
            amplitude = 0.7,
            duration = 2.seconds,
            waveform = SoundPlayer.Waveform.SINE,
        )

        // Wait for the tone to finish
        delay(2.5.seconds)
        Log.d(TAG, "Test note completed")
    }

    /**
     * Play MIDI notes sequentially
     */
    private suspend fun playMidiSequence() {
        val track = midiTrack ?: return
        Log.d(TAG, "Playing MIDI sequence: ${track.notes.size} notes")

        // Play first DEFAULT_OCTAVE notes to keep demo reasonable
        track.notes.take(com.worldwidewaves.shared.WWWGlobals.Midi.DEFAULT_OCTAVE).forEachIndexed { index, note ->
            val frequency = WaveformGenerator.midiPitchToFrequency(note.pitch)
            val amplitude = WaveformGenerator.midiVelocityToAmplitude(note.velocity) * 0.6

            Log.d(TAG, "Note ${index + 1}: MIDI ${note.pitch} -> ${frequency.toInt()}Hz")

            soundPlayer.playTone(
                frequency = frequency,
                amplitude = amplitude,
                duration = 800.milliseconds,
                waveform = SoundPlayer.Waveform.SINE,
            )

            delay(1000.milliseconds) // Gap between notes
        }

        Log.d(TAG, "MIDI sequence completed")
    }

    /**
     * Simulate crowd playing with overlapping notes
     */
    private suspend fun playCrowdSimulation() {
        val track = midiTrack ?: return
        Log.d(TAG, "Starting crowd simulation")

        val simulationDuration = 8.seconds
        val crowdSize = 5
        val playbackInterval = 600.milliseconds

        Instant.fromEpochMilliseconds(System.currentTimeMillis())
        var currentTime = Duration.ZERO

        while (currentTime < simulationDuration) {
            // Calculate position in MIDI track
            val trackPosition =
                if (track.totalDuration > Duration.ZERO) {
                    (currentTime.inWholeNanoseconds % track.totalDuration.inWholeNanoseconds).milliseconds
                } else {
                    currentTime
                }

            // Find active notes at this time
            val activeNotes = track.notes.filter { it.isActiveAt(trackPosition) }

            if (activeNotes.isNotEmpty()) {
                Log.d(TAG, "Time: ${currentTime.inWholeSeconds}s, Active notes: ${activeNotes.size}")

                // Simulate multiple people playing different notes
                repeat(crowdSize.coerceAtMost(activeNotes.size)) { personIndex ->
                    val note = activeNotes[personIndex % activeNotes.size]
                    val frequency = WaveformGenerator.midiPitchToFrequency(note.pitch)
                    val amplitude = WaveformGenerator.midiVelocityToAmplitude(note.velocity) * 0.3

                    // Each person uses a different waveform
                    val waveform =
                        when (personIndex % 3) {
                            0 -> SoundPlayer.Waveform.SINE
                            1 -> SoundPlayer.Waveform.SQUARE
                            else -> SoundPlayer.Waveform.SAWTOOTH
                        }

                    Log.d(TAG, "  Person ${personIndex + 1}: MIDI ${note.pitch} (${frequency.toInt()}Hz, ${waveform.name})")

                    // Play the note (this produces actual sound!)
                    soundPlayer.playTone(
                        frequency = frequency,
                        amplitude = amplitude,
                        duration = (playbackInterval.inWholeMilliseconds * 0.8).milliseconds,
                        waveform = waveform,
                    )
                }
            }

            // Wait before next interval
            delay(playbackInterval)
            currentTime = currentTime.plus(playbackInterval)
        }

        Log.d(TAG, "Crowd simulation completed")
    }

    /**
     * Simulate a wave passing through a crowd - using real WorldWideWaves approach
     *
     * This simulation mimics how the actual app works:
     * - Each person calls playCurrentSoundTone(waveStartTime) when hit by the wave
     * - SoundChoreographyManager calculates elapsed time and plays appropriate notes
     * - Multiple people are "hit" every 100ms to simulate wave propagation
     */
    private suspend fun playWaveProgression() {
        val track = midiTrack ?: return
        Log.d(TAG, "🌊 Starting realistic wave progression through crowd")
        Log.d(TAG, "🎼 Full symphony duration: ${track.totalDuration.inWholeSeconds}s")

        // Create a SoundChoreographyManager-like simulation
        val waveStartTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val waveSlotDurationMs = 100L // 100ms time slots
        val peoplePerSlot = 5 // Increased to 5 simultaneous people for better crowd effect
        val totalDurationMs = track.totalDuration.inWholeMilliseconds

        var currentSlotIndex = 0

        while (currentSlotIndex * waveSlotDurationMs < totalDurationMs) {
            val currentTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            val elapsedTime = currentTime - waveStartTime

            Log.d(TAG, "🌊 Slot $currentSlotIndex: Wave hits $peoplePerSlot people at ${elapsedTime.inWholeSeconds}s")

            // Simulate people getting hit by the wave at this time
            repeat(peoplePerSlot) { personIndex ->
                // Each person has random micro-timing within the 100ms slot
                val randomOffsetMs = kotlin.random.Random.nextInt(0, waveSlotDurationMs.toInt())

                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    delay(randomOffsetMs.milliseconds)

                    // Calculate what this person should play (like SoundChoreographyManager.playCurrentSoundTone)
                    val personHitTime = currentTime.plus(randomOffsetMs.milliseconds)
                    val elapsedSinceWaveStart = personHitTime - waveStartTime

                    // Calculate position in track with looping
                    val trackPosition =
                        if (track.totalDuration > Duration.ZERO) {
                            (elapsedSinceWaveStart.inWholeNanoseconds % track.totalDuration.inWholeNanoseconds).milliseconds
                        } else {
                            elapsedSinceWaveStart
                        }

                    // Find active notes at this position (like real SoundChoreographyManager)
                    val activeNotes = track.notes.filter { it.isActiveAt(trackPosition) }

                    if (activeNotes.isNotEmpty()) {
                        // Pick a random note from active ones (like real implementation)
                        val note = activeNotes[kotlin.random.Random.nextInt(activeNotes.size)]
                        val frequency = WaveformGenerator.midiPitchToFrequency(note.pitch)
                        val amplitude = WaveformGenerator.midiVelocityToAmplitude(note.velocity) * 0.15

                        // All people use SQUARE waveform (like real SoundChoreographyManager)
                        // SQUARE waveform has richer harmonics for better perceived loudness
                        val waveform = SoundPlayer.Waveform.SQUARE

                        Log.v(
                            TAG,
                            "   Person ${personIndex + 1} (+${randomOffsetMs}ms): " +
                                "MIDI ${note.pitch} at ${trackPosition.inWholeMilliseconds}ms",
                        )

                        soundPlayer.playTone(
                            frequency = frequency,
                            amplitude = amplitude,
                            duration = (waveSlotDurationMs * 0.8).milliseconds,
                            waveform = waveform,
                        )
                    }
                }
            }

            // Move to next 100ms slot
            delay(waveSlotDurationMs.milliseconds)
            currentSlotIndex++
        }

        Log.d(TAG, "🌊 Wave progression completed - played full ${track.totalDuration.inWholeSeconds}s symphony")
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPlayer.release()
    }
}
