package com.worldwidewaves.compose.choreographies

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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.worldwidewaves.shared.WWWGlobals.Companion.FileSystem
import com.worldwidewaves.shared.choreographies.SoundChoreographyManager
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.sound.AndroidSoundPlayer
import com.worldwidewaves.shared.sound.MidiParser
import com.worldwidewaves.shared.sound.MidiTrack
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.sound.WaveformGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * REAL AUDIO test for crowd sound choreography on Android hardware.
 *
 * ⚠️ WARNING: This test will produce ACTUAL SOUND through device speakers!
 *
 * This test demonstrates:
 * - Real audio playback through Android AudioTrack
 * - MIDI file parsing and note extraction
 * - Crowd simulation with actual sound output
 * - Wave progression timing with real audio feedback
 */
@OptIn(ExperimentalTime::class)
@RunWith(AndroidJUnit4::class)
class RealAudioCrowdSimulationTest {

    companion object {
        private const val TAG = "RealAudioCrowd"
        private const val DEMO_PEOPLE_COUNT = 5 // Reduced for demo
        private const val DEMO_DURATION_SECONDS = 10L // Shorter demo
        private const val PLAYBACK_INTERVAL_MS = 500L // Slower for audibility

        // Wave progression constants for realistic simulation
        private const val WAVE_PEOPLE_PER_SLOT = 10 // 10 people per 100ms slot
        private const val WAVE_SLOT_DURATION_MS = 100L // 100ms time slots
        private const val WAVE_TOTAL_DURATION_SECONDS = 60L // Play full MIDI for 60 seconds
    }

    @Test
    fun playActualAudioDemo() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Log.d(TAG, "🎵 Starting REAL AUDIO Crowd Simulation Demo")
        Log.d(TAG, "📱 Running on Android device with actual speakers")

        try {
            // Load real MIDI track
            val midiTrack = loadMidiTrack()
            Log.d(TAG, "🎼 MIDI loaded: ${midiTrack.name}")
            Log.d(TAG, "   Duration: ${midiTrack.totalDuration.inWholeSeconds}s")
            Log.d(TAG, "   Notes: ${midiTrack.notes.size}")

            // Create real Android sound player (will produce actual audio!)
            val realSoundPlayer = AndroidSoundPlayer(context)

            Log.d(TAG, "🔊 AUDIO WARNING: Sound will play through speakers in 3 seconds...")
            delay(3.seconds)

            // Demonstrate single note playback first
            demonstrateSingleNote(realSoundPlayer)

            delay(2.seconds)

            // Demonstrate crowd simulation with real audio
            demonstrateCrowdSimulation(realSoundPlayer, midiTrack)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Audio demo failed: ${e.message}")
            throw e
        }

        Log.d(TAG, "✅ Real audio demo completed!")
    }

    @Test
    fun playSimpleMIDIDemo() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Log.d(TAG, "🎼 Playing Simple MIDI Demo")

        try {
            val midiTrack = loadMidiTrack()
            val soundPlayer = AndroidSoundPlayer(context)

            Log.d(TAG, "🔊 Playing first 5 notes from MIDI file...")
            delay(1.seconds)

            // Play first 5 notes sequentially
            midiTrack.notes.take(5).forEach { note ->
                val frequency = WaveformGenerator.midiPitchToFrequency(note.pitch)
                val amplitude = WaveformGenerator.midiVelocityToAmplitude(note.velocity)

                Log.d(TAG, "🎵 Playing: MIDI ${note.pitch} -> ${frequency.toInt()}Hz")

                soundPlayer.playTone(
                    frequency = frequency,
                    amplitude = amplitude * 0.3, // Quieter volume
                    duration = 800.milliseconds,
                    waveform = SoundPlayer.Waveform.SINE
                )

                delay(1.seconds)
            }

            soundPlayer.release()

        } catch (e: Exception) {
            Log.e(TAG, "❌ MIDI demo failed: ${e.message}")
            throw e
        }

        Log.d(TAG, "✅ MIDI demo completed!")
    }

    @Test
    fun playWaveProgressionThroughCrowd() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Log.d(TAG, "🌊 Starting WAVE PROGRESSION through crowd simulation")
        Log.d(TAG, "👥 Configuration: $WAVE_PEOPLE_PER_SLOT people per ${WAVE_SLOT_DURATION_MS}ms slot")

        try {
            val midiTrack = loadMidiTrack()
            val soundPlayer = AndroidSoundPlayer(context)

            Log.d(TAG, "🎼 MIDI loaded: ${midiTrack.name}")
            Log.d(TAG, "   Duration: ${midiTrack.totalDuration.inWholeSeconds}s")
            Log.d(TAG, "   Notes: ${midiTrack.notes.size}")

            Log.d(TAG, "🔊 AUDIO WARNING: Full MIDI wave simulation starting in 3 seconds...")
            delay(3.seconds)

            // Run the wave progression simulation
            simulateWaveProgressionThroughCrowd(soundPlayer, midiTrack)

            soundPlayer.release()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Wave progression demo failed: ${e.message}")
            throw e
        }

        Log.d(TAG, "✅ Wave progression demo completed!")
    }

    /**
     * Load the MIDI track, fallback to mock if needed
     */
    private suspend fun loadMidiTrack(): MidiTrack {
        return try {
            MidiParser.parseMidiFile(FileSystem.CHOREOGRAPHIES_SOUND_MIDIFILE)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to load MIDI file, creating demo track: ${e.message}")
            createDemoMidiTrack()
        }
    }

    /**
     * Create a demo MIDI track for audio testing
     */
    private fun createDemoMidiTrack(): MidiTrack {
        // Create a simple scale: C4, D4, E4, F4, G4, A4, B4, C5
        val scaleNotes = listOf(60, 62, 64, 65, 67, 69, 71, 72).mapIndexed { index, pitch ->
            com.worldwidewaves.shared.sound.MidiNote(
                pitch = pitch,
                velocity = 100,
                startTime = (index * 1000).milliseconds,
                duration = 800.milliseconds
            )
        }

        return MidiTrack(
            name = "Demo Scale Track",
            notes = scaleNotes,
            totalDuration = 10.seconds,
            tempo = 120
        )
    }

    /**
     * Demonstrate single note playback to verify audio works
     */
    private suspend fun demonstrateSingleNote(soundPlayer: AndroidSoundPlayer) {
        Log.d(TAG, "🎵 Testing single note: A4 (440Hz)")

        soundPlayer.playTone(
            frequency = 440.0, // A4
            amplitude = 0.5,
            duration = 2.seconds,
            waveform = SoundPlayer.Waveform.SINE
        )

        delay(2.5.seconds)
        Log.d(TAG, "✅ Single note test completed")
    }

    /**
     * Demonstrate crowd simulation with real audio output
     */
    private suspend fun demonstrateCrowdSimulation(
        soundPlayer: AndroidSoundPlayer,
        midiTrack: MidiTrack
    ) {
        Log.d(TAG, "👥 Starting crowd simulation with $DEMO_PEOPLE_COUNT people")
        Log.d(TAG, "⏱️ Each person plays every ${PLAYBACK_INTERVAL_MS}ms")

        val waveStartTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val endTime = waveStartTime.plus(DEMO_DURATION_SECONDS.seconds)
        var currentTime = waveStartTime

        while (currentTime < endTime) {
            val elapsedTime = currentTime - waveStartTime

            // Calculate position in MIDI track
            val trackPosition = if (midiTrack.totalDuration > Duration.ZERO) {
                (elapsedTime.inWholeNanoseconds % midiTrack.totalDuration.inWholeNanoseconds).milliseconds
            } else {
                elapsedTime
            }

            // Find active notes at this time
            val activeNotes = midiTrack.notes.filter { it.isActiveAt(trackPosition) }

            if (activeNotes.isNotEmpty()) {
                Log.d(TAG, "🎼 Time: ${elapsedTime.inWholeSeconds}s, Active notes: ${activeNotes.size}")

                // Simulate multiple people playing different notes
                repeat(DEMO_PEOPLE_COUNT.coerceAtMost(activeNotes.size)) { personIndex ->
                    val note = activeNotes[personIndex % activeNotes.size]
                    val frequency = WaveformGenerator.midiPitchToFrequency(note.pitch)
                    val amplitude = WaveformGenerator.midiVelocityToAmplitude(note.velocity) * 0.2 // Quieter

                    // Each person uses a different waveform for variety
                    val waveform = when (personIndex % 3) {
                        0 -> SoundPlayer.Waveform.SINE
                        1 -> SoundPlayer.Waveform.SQUARE
                        else -> SoundPlayer.Waveform.SAWTOOTH
                    }

                    Log.d(TAG, "   Person ${personIndex + 1}: MIDI ${note.pitch} (${frequency.toInt()}Hz)")

                    // Play the note (this will actually produce sound!)
                    soundPlayer.playTone(
                        frequency = frequency,
                        amplitude = amplitude,
                        duration = (PLAYBACK_INTERVAL_MS * 0.8).milliseconds,
                        waveform = waveform
                    )
                }
            }

            // Wait before next playback interval
            delay(PLAYBACK_INTERVAL_MS.milliseconds)
            currentTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        }

        Log.d(TAG, "👥 Crowd simulation completed")
        soundPlayer.release()
    }

    /**
     * Simulate a wave passing through a crowd with realistic timing:
     * - 10 people per 100ms time slot
     * - Each group plays as the wave reaches them
     * - Full MIDI file duration
     * - Random spacing within each time slot
     */
    private suspend fun simulateWaveProgressionThroughCrowd(
        soundPlayer: AndroidSoundPlayer,
        midiTrack: MidiTrack
    ) {
        Log.d(TAG, "🌊 Wave passing through crowd - playing full MIDI track")
        Log.d(TAG, "⏰ Each ${WAVE_SLOT_DURATION_MS}ms: $WAVE_PEOPLE_PER_SLOT people play together")

        val waveStartTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val totalDurationMs = WAVE_TOTAL_DURATION_SECONDS * 1000
        var currentSlotIndex = 0

        while (currentSlotIndex * WAVE_SLOT_DURATION_MS < totalDurationMs) {
            val slotStartTime = currentSlotIndex * WAVE_SLOT_DURATION_MS
            val midiPositionMs = slotStartTime % midiTrack.totalDuration.inWholeMilliseconds

            // Find all active notes at this MIDI position
            val midiPosition = midiPositionMs.milliseconds
            val activeNotes = midiTrack.notes.filter { it.isActiveAt(midiPosition) }

            if (activeNotes.isNotEmpty()) {
                Log.d(TAG, "🎵 Slot ${currentSlotIndex}: ${activeNotes.size} active notes at ${midiPositionMs}ms")

                // Create 10 people for this time slot with random micro-timing
                repeat(WAVE_PEOPLE_PER_SLOT) { personIndex ->
                    // Random offset within the 100ms slot (0-99ms)
                    val randomOffsetMs = Random.nextInt(0, WAVE_SLOT_DURATION_MS.toInt())

                    // Each person gets a different note if available
                    val note = activeNotes[personIndex % activeNotes.size]
                    val frequency = WaveformGenerator.midiPitchToFrequency(note.pitch)
                    val amplitude = WaveformGenerator.midiVelocityToAmplitude(note.velocity) * 0.15 // Quieter for crowd

                    // Vary waveforms for realistic human sound variation
                    val waveform = when (personIndex % 4) {
                        0 -> SoundPlayer.Waveform.SINE
                        1 -> SoundPlayer.Waveform.SQUARE
                        2 -> SoundPlayer.Waveform.SAWTOOTH
                        else -> SoundPlayer.Waveform.SINE
                    }

                    // Launch each person's note with their random timing offset
                    launch {
                        delay(randomOffsetMs.milliseconds)

                        Log.v(TAG, "   Person ${personIndex + 1} (+${randomOffsetMs}ms): MIDI ${note.pitch} (${frequency.toInt()}Hz)")

                        soundPlayer.playTone(
                            frequency = frequency,
                            amplitude = amplitude,
                            duration = (WAVE_SLOT_DURATION_MS * 0.9).milliseconds, // Slight overlap
                            waveform = waveform
                        )
                    }
                }
            } else {
                Log.d(TAG, "🔇 Slot ${currentSlotIndex}: No active notes at ${midiPositionMs}ms")
            }

            // Move to next 100ms slot
            delay(WAVE_SLOT_DURATION_MS.milliseconds)
            currentSlotIndex++
        }

        Log.d(TAG, "🌊 Wave progression completed - ${currentSlotIndex} time slots processed")
    }
}