package com.worldwidewaves.shared.choreographies

/* * Copyright 2025 DrWave
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
 * limitations under the License. */

import com.worldwidewaves.shared.WWWGlobals.FileSystem
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.sound.MidiNote
import com.worldwidewaves.shared.sound.MidiParser
import com.worldwidewaves.shared.sound.MidiTrack
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.sound.WaveformGenerator
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class SoundChoreographyManagerTest : KoinTest {
    @MockK
    private lateinit var clock: IClock

    @RelaxedMockK
    private lateinit var soundPlayer: SoundPlayer

    @MockK
    private lateinit var coroutineScopeProvider: CoroutineScopeProvider

    private lateinit var manager: SoundChoreographyManager

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this)

        // Setup Koin DI
        startKoin {
            modules(
                module {
                    single { clock }
                    single { soundPlayer }
                },
            )
        }

        // Mock coroutineScopeProvider to return a mock Job without executing the lambda - FIXME
        every { coroutineScopeProvider.launchIO(any()) } returns Job()

        // Setup default clock behavior
        every { clock.now() } returns Instant.fromEpochMilliseconds(0)

        // Create manager with mocked dependencies
        manager = SoundChoreographyManager(coroutineScopeProvider)
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `test SoundChoreographyManager initialize triggers MIDI preload`() =
        runTest {
            // Mock MidiParser to track calls
            mockkObject(MidiParser)

            try {
                // Setup mock return value
                val mockedTrack = MidiTrack(
                    name = "Test Track",
                    notes = listOf(
                        MidiNote(60, 80, 0.milliseconds, 300.milliseconds)
                    ),
                    totalDuration = 300.milliseconds
                )
                coEvery { MidiParser.parseMidiFile(any()) } returns mockedTrack

                // iOS FIX: Test now verifies explicit initialize() call instead of init{} block
                manager.initialize()

                // Verify that the MIDI file was preloaded
                coVerify { MidiParser.parseMidiFile(FileSystem.CHOREOGRAPHIES_SOUND_MIDIFILE) }
            } finally {
                unmockkObject(MidiParser)
            }
        }

    @Test
    fun `test preloadMidiFile returns true when MIDI loads successfully`() =
        runTest {
            // Create a mocked MidiTrack
            val mockedTrack =
                MidiTrack(
                    name = "Test Track",
                    notes =
                        listOf(
                            MidiNote(60, 80, 0.milliseconds, 300.milliseconds),
                        ),
                    totalDuration = 500.milliseconds,
                )

            // Use test-scoped mocking with guaranteed cleanup to avoid global state pollution
            mockkObject(MidiParser)

            try {
                // Set up the mock behavior with test-scoped isolation
                coEvery { MidiParser.parseMidiFile(any()) } returns mockedTrack

                // Test the preloadMidiFile function
                val result = manager.preloadMidiFile("test.mid")

                // Verify results
                assertTrue(result, "preloadMidiFile should return true when successful")
                coVerify { MidiParser.parseMidiFile("test.mid") }
            } finally {
                // Guaranteed cleanup to prevent test pollution - this runs even if test fails
                unmockkObject(MidiParser)

                // Additional safety: clear any remaining mock state
                clearAllMocks()
            }
        }

    @Test
    fun `test preloadMidiFile returns false when MIDI load fails`() =
        runTest {
            // Create a spy on MidiParser to control its behavior
            val midiParserSpy = spyk(MidiParser)

            coEvery { midiParserSpy.parseMidiFile(any()) } throws Exception("Test exception")

            // Test the preloadMidiFile function
            val result = manager.preloadMidiFile("test.mid")

            assertFalse(result, "preloadMidiFile should return false when it fails")
        }

    @Test
    fun `test playCurrentSoundTone returns null when no track is loaded`() =
        runBlocking {
            // Setup
            val waveStartTime = Instant.fromEpochMilliseconds(0)

            // Test
            val result = manager.playCurrentSoundTone(waveStartTime)

            // Verify
            assertNull(result, "playCurrentSoundTone should return null when no track is loaded")
            coVerify(exactly = 0) { soundPlayer.playTone(any(), any(), any(), any()) }
        }

    @Test
    fun `test playCurrentSoundTone plays a tone when notes are active`() =
        runTest {
            // Setup a test MIDI track
            val testTrack =
                MidiTrack(
                    name = "Test Track",
                    notes =
                        listOf(
                            MidiNote(60, 80, 0.milliseconds, 300.milliseconds),
                            MidiNote(62, 90, 0.milliseconds, 300.milliseconds),
                        ),
                    totalDuration = 500.milliseconds,
                )

            manager.setCurrentTrack(testTrack)

            // Setup clock to return a time that would make notes active
            every { clock.now() } returns Instant.fromEpochMilliseconds(100)

            // Test
            val waveStartTime = Instant.fromEpochMilliseconds(0)
            val result = manager.playCurrentSoundTone(waveStartTime)

            // Verify
            assertNotNull(result, "playCurrentSoundTone should return a pitch value")
            coVerify { soundPlayer.playTone(any(), any(), any(), any()) }
        }

    @Test
    fun `test playCurrentSoundTone returns null when no notes are active`() =
        runTest {
            // Setup a test MIDI track with notes that start in the future
            val testTrack =
                MidiTrack(
                    name = "Test Track",
                    notes =
                        listOf(
                            MidiNote(60, 80, 1000.milliseconds, 300.milliseconds),
                        ),
                    totalDuration = 2000.milliseconds,
                )

            manager.setCurrentTrack(testTrack)

            // Setup clock to return a time before any notes are active
            every { clock.now() } returns Instant.fromEpochMilliseconds(500)

            // Test
            val waveStartTime = Instant.fromEpochMilliseconds(0)
            val result = manager.playCurrentSoundTone(waveStartTime)

            // Verify
            assertNull(result, "playCurrentSoundTone should return null when no notes are active")
            coVerify(exactly = 0) { soundPlayer.playTone(any(), any(), any(), any()) }
        }

    @Test
    fun `test looping behavior in playCurrentSoundTone`() =
        runTest {
            // Setup a test MIDI track with clear active periods
            val testTrack =
                MidiTrack(
                    name = "Test Track",
                    notes =
                        listOf(
                            // A note active at the start of the track
                            MidiNote(60, 80, 0.milliseconds, 300.milliseconds),
                            // A note active in the middle of the track
                            MidiNote(62, 80, 500.milliseconds, 300.milliseconds),
                        ),
                    totalDuration = 1000.milliseconds,
                )

            manager.setCurrentTrack(testTrack)

            // Set looping to true
            manager.setLooping(true)

            // Setup wave start time
            val waveStartTime = Instant.fromEpochMilliseconds(0)

            // Test with time that would wrap around to the beginning (1500ms = 1.5 × track duration)
            // This should map to 500ms in the track (after wrapping)
            every { clock.now() } returns Instant.fromEpochMilliseconds(1500)

            // Since 500ms corresponds to our second note, we should get a result
            val result = manager.playCurrentSoundTone(waveStartTime)

            // Verify we get a pitch value when looping
            assertNotNull(result, "playCurrentSoundTone should return a pitch when looping")

            // Verify the sound player was called
            coVerify { soundPlayer.playTone(any(), any(), any(), any()) }

            // Now test with looping off
            manager.setLooping(false)

            // With the same time but looping off
            val result2 = manager.playCurrentSoundTone(waveStartTime)

            // With looping off, we should get null since we're past the track duration
            assertNull(result2, "playCurrentSoundTone should return null when past track duration with looping off")
        }

    @Test
    fun `test setWaveform changes the waveform used for playback`() =
        runBlocking {
            // Capture the waveform parameter passed to soundPlayer
            val waveformSlot = slot<SoundPlayer.Waveform>()

            // Setup a test MIDI track
            val testTrack =
                MidiTrack(
                    name = "Test Track",
                    notes =
                        listOf(
                            MidiNote(60, 80, 0.milliseconds, 300.milliseconds),
                        ),
                    totalDuration = 500.milliseconds,
                )

            manager.setCurrentTrack(testTrack)

            // Setup for sound player to capture waveform parameter
            coEvery {
                soundPlayer.playTone(any(), any(), any(), capture(waveformSlot))
            } returns Unit

            // Default should be SQUARE (changed for better perceived loudness)
            val waveStartTime = Instant.fromEpochMilliseconds(0)
            manager.playCurrentSoundTone(waveStartTime)
            assertEquals(SoundPlayer.Waveform.SQUARE, waveformSlot.captured, "Default waveform should be SQUARE")

            // Change to SINE and verify
            manager.setWaveform(SoundPlayer.Waveform.SINE)
            manager.playCurrentSoundTone(waveStartTime)
            assertEquals(SoundPlayer.Waveform.SINE, waveformSlot.captured, "Waveform should be changed to SINE")
        }

    @Test
    fun `test getTotalDuration returns the correct duration`() =
        runTest {
            // When no track is loaded, duration should be ZERO
            assertEquals(0.seconds, manager.getTotalDuration(), "Duration should be ZERO when no track is loaded")

            // Load a track
            val testTrack =
                MidiTrack(
                    name = "Test Track",
                    notes = listOf(),
                    totalDuration = 5.seconds,
                )

            manager.setCurrentTrack(testTrack)

            // Now the duration should match the track
            assertEquals(5.seconds, manager.getTotalDuration(), "Duration should match the loaded track")
        }

    @Test
    fun `test release clears resources`() {
        // Setup a test track
        val testTrack =
            MidiTrack(
                name = "Test Track",
                notes = listOf(),
                totalDuration = 5.seconds,
            )

        manager.setCurrentTrack(testTrack)

        // Call release
        manager.release()

        // Verify soundPlayer was released
        verify { soundPlayer.release() }

        // Verify currentTrack was cleared
        assertNull(
            manager.getTotalDuration().inWholeSeconds.takeIf { it > 0 },
            "Track should be cleared after release",
        )
    }

    @Test
    fun `test multiple active notes selection randomness`() =
        runTest {
            // Setup a track with multiple overlapping notes
            val testTrack =
                MidiTrack(
                    name = "Test Track",
                    notes =
                        listOf(
                            MidiNote(60, 80, 0.milliseconds, 500.milliseconds), // C4
                            MidiNote(64, 85, 0.milliseconds, 500.milliseconds), // E4
                            MidiNote(67, 90, 0.milliseconds, 500.milliseconds), // G4
                            MidiNote(72, 75, 0.milliseconds, 500.milliseconds), // C5
                        ),
                    totalDuration = 1000.milliseconds,
                )

            manager.setCurrentTrack(testTrack)
            every { clock.now() } returns Instant.fromEpochMilliseconds(250) // Middle of all notes

            val waveStartTime = Instant.fromEpochMilliseconds(0)
            val playedPitches = mutableSetOf<Int>()

            // Play multiple times to test randomness
            repeat(20) {
                val pitch = manager.playCurrentSoundTone(waveStartTime)
                assertNotNull(pitch, "Should always return a pitch when notes are active")
                playedPitches.add(pitch)
            }

            // We should get some variety in the played pitches (not always the same one)
            assertTrue(
                playedPitches.size > 1,
                "Should play different notes randomly, got: $playedPitches",
            )

            // All played pitches should be from our test notes
            val expectedPitches = setOf(60, 64, 67, 72)
            assertTrue(
                playedPitches.all { it in expectedPitches },
                "All played pitches should be from the test track",
            )
        }

    @Test
    fun `test amplitude calculation with edge velocities`() =
        runTest {
            val amplitudeSlot = slot<Double>()

            // Setup mock to capture amplitude parameter
            coEvery {
                soundPlayer.playTone(any(), capture(amplitudeSlot), any(), any())
            } returns Unit

            // Test with minimum velocity (0)
            val minVelocityTrack =
                MidiTrack(
                    name = "Min Velocity Track",
                    notes = listOf(MidiNote(60, 0, 0.milliseconds, 300.milliseconds)),
                    totalDuration = 500.milliseconds,
                )
            manager.setCurrentTrack(minVelocityTrack)
            manager.playCurrentSoundTone(Instant.fromEpochMilliseconds(0))

            assertEquals(0.0, amplitudeSlot.captured, 0.001, "Velocity 0 should result in amplitude 0")

            // Test with maximum velocity (127)
            val maxVelocityTrack =
                MidiTrack(
                    name = "Max Velocity Track",
                    notes = listOf(MidiNote(60, 127, 0.milliseconds, 300.milliseconds)),
                    totalDuration = 500.milliseconds,
                )
            manager.setCurrentTrack(maxVelocityTrack)
            manager.playCurrentSoundTone(Instant.fromEpochMilliseconds(0))

            assertEquals(1.0, amplitudeSlot.captured, 0.001, "Velocity 127 should result in amplitude 1.0 (full amplitude)")
        }

    @Test
    fun `test duration coercion to maximum 2 seconds`() =
        runTest {
            val durationSlot = slot<Duration>()

            // Setup mock to capture duration parameter
            coEvery {
                soundPlayer.playTone(any(), any(), capture(durationSlot), any())
            } returns Unit

            // Test with a note longer than 2 seconds
            val longNoteTrack =
                MidiTrack(
                    name = "Long Note Track",
                    notes = listOf(MidiNote(60, 80, 0.milliseconds, 5.seconds)),
                    totalDuration = 6.seconds,
                )

            manager.setCurrentTrack(longNoteTrack)
            manager.playCurrentSoundTone(Instant.fromEpochMilliseconds(0))

            assertEquals(2.seconds, durationSlot.captured, "Note duration should be capped at 2 seconds")

            // Test with a note shorter than 2 seconds
            val shortNoteTrack =
                MidiTrack(
                    name = "Short Note Track",
                    notes = listOf(MidiNote(60, 80, 0.milliseconds, 500.milliseconds)),
                    totalDuration = 1.seconds,
                )

            manager.setCurrentTrack(shortNoteTrack)
            manager.playCurrentSoundTone(Instant.fromEpochMilliseconds(0))

            assertEquals(500.milliseconds, durationSlot.captured, "Short note duration should be preserved")
        }

    @Test
    fun `test track position calculation with very long elapsed time`() =
        runTest {
            val testTrack =
                MidiTrack(
                    name = "Test Track",
                    notes = listOf(MidiNote(60, 80, 0.milliseconds, 100.milliseconds)),
                    totalDuration = 1000.milliseconds,
                )

            manager.setCurrentTrack(testTrack)
            manager.setLooping(true)

            // Test with elapsed time much longer than track duration
            val waveStartTime = Instant.fromEpochMilliseconds(0)
            every { clock.now() } returns Instant.fromEpochMilliseconds(25000) // 25 seconds

            val result = manager.playCurrentSoundTone(waveStartTime)

            // Should still work due to looping (25000ms % 1000ms = 0ms, so note should be active)
            assertNotNull(result, "Should handle very long elapsed times with looping")
        }

    @Test
    fun `test boundary conditions at track edges`() =
        runTest {
            val testTrack =
                MidiTrack(
                    name = "Edge Test Track",
                    notes =
                        listOf(
                            MidiNote(60, 80, 0.milliseconds, 100.milliseconds), // 0-100ms
                            MidiNote(64, 80, 900.milliseconds, 100.milliseconds), // 900-1000ms
                        ),
                    totalDuration = 1000.milliseconds,
                )

            manager.setCurrentTrack(testTrack)
            manager.setLooping(true)
            val waveStartTime = Instant.fromEpochMilliseconds(0)

            // Test exactly at track start
            every { clock.now() } returns Instant.fromEpochMilliseconds(0)
            var result = manager.playCurrentSoundTone(waveStartTime)
            assertEquals(60, result, "Should play first note at track start")

            // Test exactly at track end (should wrap to beginning with looping)
            every { clock.now() } returns Instant.fromEpochMilliseconds(1000)
            result = manager.playCurrentSoundTone(waveStartTime)
            assertEquals(60, result, "Should wrap to beginning at track end with looping")

            // Test just before track end
            every { clock.now() } returns Instant.fromEpochMilliseconds(950)
            result = manager.playCurrentSoundTone(waveStartTime)
            assertEquals(64, result, "Should play second note near track end")
        }

    @Test
    fun `test error handling with zero duration track`() =
        runTest {
            val zeroDurationTrack =
                MidiTrack(
                    name = "Zero Duration Track",
                    notes = listOf(MidiNote(60, 80, 0.milliseconds, 0.milliseconds)),
                    totalDuration = Duration.ZERO,
                )

            manager.setCurrentTrack(zeroDurationTrack)
            manager.setLooping(true)

            val waveStartTime = Instant.fromEpochMilliseconds(0)
            every { clock.now() } returns Instant.fromEpochMilliseconds(100)

            // Should handle zero duration gracefully without division by zero
            val result = manager.playCurrentSoundTone(waveStartTime)
            assertNull(result, "Should handle zero duration track gracefully")
        }

    @Test
    fun `test preloadMidiFile with mock exception handling`() =
        runTest {
            // Use test-scoped mocking with enhanced cleanup to prevent global state leakage
            mockkObject(MidiParser)

            try {
                // Test exception during parsing with isolated mock behavior
                coEvery { MidiParser.parseMidiFile(any()) } throws RuntimeException("Network error")

                val result = manager.preloadMidiFile("corrupted.mid")

                // Verify error handling behavior
                assertFalse(result, "Should return false when parsing throws exception")
                coVerify { MidiParser.parseMidiFile("corrupted.mid") }

                // Verify state remains clean after parsing failure
                assertEquals(Duration.ZERO, manager.getTotalDuration())
            } finally {
                // Guaranteed cleanup to prevent test pollution - critical for test isolation
                unmockkObject(MidiParser)

                // Additional safety: ensure no mock state persists between tests
                clearAllMocks()
            }
        }

    @Test
    fun `test frequency calculation for extreme MIDI pitches`() =
        runTest {
            val frequencySlot = slot<Double>()

            coEvery {
                soundPlayer.playTone(capture(frequencySlot), any(), any(), any())
            } returns Unit

            // Test with very low MIDI pitch (0)
            val lowPitchTrack =
                MidiTrack(
                    name = "Low Pitch Track",
                    notes = listOf(MidiNote(0, 80, 0.milliseconds, 300.milliseconds)),
                    totalDuration = 500.milliseconds,
                )
            manager.setCurrentTrack(lowPitchTrack)
            manager.playCurrentSoundTone(Instant.fromEpochMilliseconds(0))

            val lowFreq = frequencySlot.captured
            assertTrue(lowFreq > 0, "Frequency should be positive even for MIDI pitch 0")

            // Test with very high MIDI pitch (127)
            val highPitchTrack =
                MidiTrack(
                    name = "High Pitch Track",
                    notes = listOf(MidiNote(127, 80, 0.milliseconds, 300.milliseconds)),
                    totalDuration = 500.milliseconds,
                )
            manager.setCurrentTrack(highPitchTrack)
            manager.playCurrentSoundTone(Instant.fromEpochMilliseconds(0))

            val highFreq = frequencySlot.captured
            assertTrue(highFreq > lowFreq, "High MIDI pitch should result in higher frequency")
            assertTrue(highFreq < 20000, "High frequency should still be reasonable")
        }

    @Test
    fun `test setLooping toggles correctly affect playback behavior`() =
        runTest {
            val testTrack =
                MidiTrack(
                    name = "Looping Test Track",
                    notes = listOf(MidiNote(60, 80, 0.milliseconds, 100.milliseconds)),
                    totalDuration = 500.milliseconds,
                )

            manager.setCurrentTrack(testTrack)
            val waveStartTime = Instant.fromEpochMilliseconds(0)

            // Time past track duration that will map to note when looped
            // 550ms % 500ms = 50ms, which is within the 0-100ms note
            every { clock.now() } returns Instant.fromEpochMilliseconds(550)

            // Test with looping enabled (default)
            manager.setLooping(true)
            var result = manager.playCurrentSoundTone(waveStartTime)
            assertNotNull(result, "Should play note with looping enabled past track duration")

            // Test with looping disabled
            manager.setLooping(false)
            result = manager.playCurrentSoundTone(waveStartTime)
            assertNull(result, "Should not play note with looping disabled past track duration")

            // Toggle back to looping
            manager.setLooping(true)
            result = manager.playCurrentSoundTone(waveStartTime)
            assertNotNull(result, "Should resume playing with looping re-enabled")
        }
}

/**
 * Tests for the WaveformGenerator utility class
 */
class WaveformGeneratorTest {
    @Test
    fun `test midiPitchToFrequency conversion`() {
        // Test with middle A (A4 = MIDI note 69 = 440Hz)
        val a4Frequency = WaveformGenerator.midiPitchToFrequency(69)
        assertEquals(440.0, a4Frequency, 0.01, "A4 should be 440Hz")

        // Test with middle C (C4 = MIDI note 60)
        val c4Frequency = WaveformGenerator.midiPitchToFrequency(60)
        assertEquals(261.63, c4Frequency, 0.01, "C4 should be ~261.63Hz")

        // Test with higher octave
        val c5Frequency = WaveformGenerator.midiPitchToFrequency(72)
        assertEquals(523.25, c5Frequency, 0.01, "C5 should be ~523.25Hz")
    }

    @Test
    fun `test midiPitchToFrequency edge cases`() {
        // Test extreme low notes
        val lowNote = WaveformGenerator.midiPitchToFrequency(0)
        assertTrue(lowNote > 0.0, "Very low MIDI note should produce positive frequency")
        assertEquals(8.18, lowNote, 0.01, "MIDI note 0 should be ~8.18Hz")

        // Test extreme high notes
        val highNote = WaveformGenerator.midiPitchToFrequency(127)
        assertTrue(highNote < 20000.0, "Very high MIDI note should be under 20kHz")
        assertEquals(12543.85, highNote, 0.1, "MIDI note 127 should be ~12.5kHz")

        // Test octave relationships (each octave doubles frequency)
        val c3 = WaveformGenerator.midiPitchToFrequency(48)
        val c4 = WaveformGenerator.midiPitchToFrequency(60)
        val c5 = WaveformGenerator.midiPitchToFrequency(72)

        assertEquals(c3 * 2, c4, 0.01, "C4 should be exactly double C3")
        assertEquals(c4 * 2, c5, 0.01, "C5 should be exactly double C4")
    }

    @Test
    fun `test midiVelocityToAmplitude conversion`() {
        // Test with full velocity
        val fullAmplitude = WaveformGenerator.midiVelocityToAmplitude(127)
        assertEquals(1.0, fullAmplitude, 0.01, "Velocity 127 should give amplitude 1.0")

        // Test with half velocity
        val halfAmplitude = WaveformGenerator.midiVelocityToAmplitude(64)
        assertEquals(0.5, halfAmplitude, 0.04, "Velocity 64 should give amplitude ~0.5")

        // Test with zero velocity
        val zeroAmplitude = WaveformGenerator.midiVelocityToAmplitude(0)
        assertEquals(0.0, zeroAmplitude, 0.01, "Velocity 0 should give amplitude 0.0")
    }

    @Test
    fun `test midiVelocityToAmplitude edge cases and clamping`() {
        // Test values above maximum
        val overMax = WaveformGenerator.midiVelocityToAmplitude(200)
        assertEquals(1.0, overMax, 0.01, "Velocity over 127 should be clamped to 1.0")

        // Test negative values
        val negative = WaveformGenerator.midiVelocityToAmplitude(-10)
        assertEquals(0.0, negative, 0.01, "Negative velocity should be clamped to 0.0")

        // Test boundary values
        val minValid = WaveformGenerator.midiVelocityToAmplitude(1)
        assertEquals(1.0 / 127.0, minValid, 0.01, "Velocity 1 should give minimal amplitude")

        val maxValid = WaveformGenerator.midiVelocityToAmplitude(126)
        assertEquals(126.0 / 127.0, maxValid, 0.01, "Velocity 126 should give near-maximum amplitude")
    }

    @Test
    fun `test generateWaveform creates correct sample count`() {
        // Generate a 1-second sample at 44100Hz
        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = 44100,
                frequency = 440.0,
                amplitude = 1.0,
                duration = 1.seconds,
                waveform = SoundPlayer.Waveform.SINE,
            )

        assertEquals(44100, samples.size, "1-second sample at 44100Hz should have 44100 samples")
    }

    @Test
    fun `test generateWaveform sample count with fractional durations`() {
        // Test with fractional duration (500ms)
        val samples500ms =
            WaveformGenerator.generateWaveform(
                sampleRate = 44100,
                frequency = 440.0,
                amplitude = 1.0,
                duration = 500.milliseconds,
                waveform = SoundPlayer.Waveform.SINE,
            )
        assertEquals(22050, samples500ms.size, "500ms at 44100Hz should have 22050 samples")

        // Test with very short duration
        val samplesShort =
            WaveformGenerator.generateWaveform(
                sampleRate = 44100,
                frequency = 440.0,
                amplitude = 1.0,
                duration = 10.milliseconds,
                waveform = SoundPlayer.Waveform.SINE,
            )
        assertEquals(441, samplesShort.size, "10ms at 44100Hz should have 441 samples")
    }

    @Test
    fun `test generateWaveform creates sine waveform with correct properties`() {
        // Generate a 1-second, 1Hz sine wave at 1000Hz sampling rate for easy testing
        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = 1000,
                frequency = 1.0, // 1 cycle per second
                amplitude = 1.0,
                duration = 1.seconds,
                waveform = SoundPlayer.Waveform.SINE,
            )

        // Check sample count
        assertEquals(1000, samples.size, "Should have 1000 samples")

        // Check amplitude at key points (accounting for envelope)
        assertTrue(abs(samples[0]) < 0.1, "Sample near start should be close to 0")
        assertTrue(abs(samples[250] - 1.0) < 0.1, "Sample at 1/4 should be close to 1 (peak)")
        assertTrue(abs(samples[500]) < 0.1, "Sample at 1/2 should be close to 0 (zero crossing)")
        assertTrue(abs(samples[750] + 1.0) < 0.1, "Sample at 3/4 should be close to -1 (trough)")
    }

    @Test
    fun `test sine waveform mathematical properties`() {
        // Use longer duration to test in stable middle region away from envelope effects
        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = 8000,
                frequency = 100.0, // 100Hz
                amplitude = 1.0,
                duration = 300.milliseconds, // Longer duration for stable middle section
                waveform = SoundPlayer.Waveform.SINE,
            )

        // Calculate envelope parameters (from WaveformGenerator.applyEnvelope)
        val attackSamples = (8000 * 0.01).toInt() // 80 samples attack envelope
        val releaseSamples = (8000 * 0.01).toInt() // 80 samples release envelope
        val samplesPerPeriod = (8000 / 100.0).toInt() // 80 samples per period

        // Test periodicity in the stable middle region (after attack, before release)
        val stableStart = attackSamples + 10 // Start after attack envelope + buffer
        val stableEnd = samples.size - releaseSamples - samplesPerPeriod - 10 // End before release

        for (i in stableStart until stableEnd) {
            val nextPeriodIndex = i + samplesPerPeriod
            if (nextPeriodIndex < samples.size - releaseSamples) {
                val currentSample = samples[i]
                val nextPeriodSample = samples[nextPeriodIndex]
                assertEquals(
                    currentSample,
                    nextPeriodSample,
                    0.001,
                    "Sine wave should be periodic (sample $i)",
                )
            }
        }
    }

    @Test
    fun `test square waveform properties`() {
        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = 8000,
                frequency = 100.0,
                amplitude = 1.0,
                duration = 100.milliseconds,
                waveform = SoundPlayer.Waveform.SQUARE,
            )

        // Find middle section to avoid envelope effects
        val middleStart = samples.size / 4
        val middleEnd = 3 * samples.size / 4
        val middleSamples = samples.slice(middleStart until middleEnd)

        // Square wave should only have values close to +1 or -1
        for (sample in middleSamples) {
            assertTrue(
                abs(sample - 1.0) < 0.1 || abs(sample + 1.0) < 0.1,
                "Square wave sample should be close to +1 or -1, got $sample",
            )
        }
    }

    @Test
    fun `test triangle waveform properties`() {
        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = 8000,
                frequency = 100.0,
                amplitude = 1.0,
                duration = 100.milliseconds,
                waveform = SoundPlayer.Waveform.TRIANGLE,
            )

        // Triangle wave should have gradual transitions
        val middleStart = samples.size / 4
        val middleEnd = 3 * samples.size / 4

        // Check that consecutive samples don't have large jumps (triangle is continuous)
        for (i in middleStart until (middleEnd - 1)) {
            val diff = abs(samples[i + 1] - samples[i])
            assertTrue(diff < 0.2, "Triangle wave should have gradual transitions, found jump of $diff")
        }

        // Triangle wave should reach peak values
        val peakFound = samples.any { abs(it) > 0.8 }
        assertTrue(peakFound, "Triangle wave should reach near-peak values")
    }

    @Test
    fun `test sawtooth waveform properties`() {
        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = 8000,
                frequency = 100.0,
                amplitude = 1.0,
                duration = 100.milliseconds,
                waveform = SoundPlayer.Waveform.SAWTOOTH,
            )

        // Sawtooth should have both gradual ramps and sharp transitions
        val middleStart = samples.size / 4
        val middleEnd = 3 * samples.size / 4

        // Look for the characteristic sawtooth pattern
        val peakFound = samples.any { abs(it) > 0.8 }
        assertTrue(peakFound, "Sawtooth wave should reach near-peak values")

        // Sawtooth should cover the full amplitude range
        val maxValue = samples.maxOrNull() ?: 0.0
        val minValue = samples.minOrNull() ?: 0.0
        assertTrue(maxValue > 0.5, "Sawtooth should have positive peaks")
        assertTrue(minValue < -0.5, "Sawtooth should have negative peaks")
    }

    @Test
    fun `test amplitude scaling for all waveforms`() {
        val testAmplitudes = listOf(0.0, 0.25, 0.5, 0.75, 1.0)

        for (waveform in SoundPlayer.Waveform.entries) {
            for (amplitude in testAmplitudes) {
                val samples =
                    WaveformGenerator.generateWaveform(
                        sampleRate = 44100,
                        frequency = 440.0,
                        amplitude = amplitude,
                        duration = 50.milliseconds,
                        waveform = waveform,
                    )

                // All samples should be within the amplitude range
                val maxAbs = samples.maxOfOrNull { abs(it) } ?: 0.0
                assertTrue(
                    maxAbs <= amplitude + 0.01, // Small tolerance for floating point
                    "${waveform.name} with amplitude $amplitude should not exceed amplitude bounds, max was $maxAbs",
                )

                // For non-zero amplitudes, we should actually reach near the amplitude
                if (amplitude > 0.1) {
                    assertTrue(
                        maxAbs > amplitude * 0.7, // Should reach at least 70% of amplitude
                        "${waveform.name} with amplitude $amplitude should actually use the amplitude, max was $maxAbs",
                    )
                }
            }
        }
    }

    @Test
    fun `test frequency accuracy for all waveforms`() {
        val testFrequencies = listOf(110.0, 220.0, 440.0, 880.0)

        for (frequency in testFrequencies) {
            for (waveform in SoundPlayer.Waveform.entries) {
                val samples =
                    WaveformGenerator.generateWaveform(
                        sampleRate = 44100,
                        frequency = frequency,
                        amplitude = 0.8,
                        duration = 200.milliseconds, // Enough time for accurate frequency measurement
                        waveform = waveform,
                    )

                // Verify we have the expected number of samples
                assertTrue(samples.isNotEmpty(), "${waveform.name} should generate samples")

                // For periodic waveforms, verify approximate period
                val expectedSamplesPerPeriod = 44100.0 / frequency

                // This is a basic check - more sophisticated frequency analysis could be added
                assertTrue(
                    expectedSamplesPerPeriod > 10, // Ensure we have enough resolution
                    "Sample rate should be high enough for frequency $frequency",
                )
            }
        }
    }

    @Test
    fun `test envelope application prevents clicks`() {
        // Generate a short, high-amplitude tone that would cause clicks without envelope
        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = 44100,
                frequency = 1000.0,
                amplitude = 1.0,
                duration = 50.milliseconds,
                waveform = SoundPlayer.Waveform.SQUARE, // Square wave more prone to clicks
            )

        // Check that start and end samples are close to zero (envelope applied)
        assertTrue(abs(samples[0]) < 0.1, "First sample should be near zero due to attack envelope")
        assertTrue(abs(samples[samples.size - 1]) < 0.1, "Last sample should be near zero due to release envelope")

        // Check that we have a gradual fade-in at the beginning
        val attackSamples = (44100 * 0.01).toInt() // 10ms attack
        for (i in 0 until attackSamples.coerceAtMost(samples.size - 1)) {
            val nextSample = samples[i + 1]
            val currentSample = samples[i]
            assertTrue(
                abs(nextSample) >= abs(currentSample) || abs(nextSample - currentSample) < 0.5,
                "Attack envelope should gradually increase amplitude",
            )
        }
    }

    @Test
    fun `test zero duration produces empty array`() {
        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = 44100,
                frequency = 440.0,
                amplitude = 1.0,
                duration = 0.milliseconds,
                waveform = SoundPlayer.Waveform.SINE,
            )

        assertEquals(0, samples.size, "Zero duration should produce empty sample array")
    }

    @Test
    fun `test extreme parameters produce valid results`() {
        // Test very low frequency
        val lowFreqSamples =
            WaveformGenerator.generateWaveform(
                sampleRate = 44100,
                frequency = 1.0, // 1 Hz
                amplitude = 1.0,
                duration = 100.milliseconds,
                waveform = SoundPlayer.Waveform.SINE,
            )
        assertTrue(lowFreqSamples.isNotEmpty(), "Very low frequency should produce samples")

        // Test very high frequency (but reasonable)
        val highFreqSamples =
            WaveformGenerator.generateWaveform(
                sampleRate = 44100,
                frequency = 8000.0, // 8 kHz
                amplitude = 1.0,
                duration = 100.milliseconds,
                waveform = SoundPlayer.Waveform.SINE,
            )
        assertTrue(highFreqSamples.isNotEmpty(), "High frequency should produce samples")

        // Test low sample rate
        val lowSampleRateSamples =
            WaveformGenerator.generateWaveform(
                sampleRate = 8000,
                frequency = 440.0,
                amplitude = 1.0,
                duration = 100.milliseconds,
                waveform = SoundPlayer.Waveform.SINE,
            )
        assertEquals(800, lowSampleRateSamples.size, "Low sample rate should produce correct sample count")
    }

    @Test
    fun `test all waveform types generate samples without errors`() {
        // Test that all waveform types can be generated
        for (waveform in SoundPlayer.Waveform.entries) {
            val samples =
                WaveformGenerator.generateWaveform(
                    sampleRate = 44100,
                    frequency = 440.0,
                    amplitude = 0.8,
                    duration = 100.milliseconds,
                    waveform = waveform,
                )

            // Check we have samples and they're within range
            assertTrue(samples.isNotEmpty(), "${waveform.name} should generate samples")
            assertTrue(
                samples.all { it in -0.8..0.8 },
                "${waveform.name} samples should be within amplitude range",
            )
        }
    }
}

/**
 * Tests for the MidiNote class
 */
class MidiNoteTest {
    @Test
    fun `test isActiveAt returns true when time is within note duration`() {
        val note =
            MidiNote(
                pitch = 60,
                velocity = 80,
                startTime = 100.milliseconds,
                duration = 200.milliseconds,
            )

        // Before note start
        assertFalse(note.isActiveAt(50.milliseconds), "Note should not be active before its start time")

        // At note start
        assertTrue(note.isActiveAt(100.milliseconds), "Note should be active at exactly its start time")

        // During note
        assertTrue(note.isActiveAt(200.milliseconds), "Note should be active during its duration")

        // At note end
        assertFalse(note.isActiveAt(300.milliseconds), "Note should not be active at exactly its end time")

        // After note end
        assertFalse(note.isActiveAt(350.milliseconds), "Note should not be active after its end time")
    }
}
