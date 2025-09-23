package com.worldwidewaves.shared

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

import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.sound.MidiParser
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.sound.WaveformGenerator
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * Comprehensive input validation tests addressing TODO items:
 * - Add MIDI file size limits and malformed header tests
 * - Add waveform generation bounds checking tests
 * - Add platform simulation parameter validation tests
 *
 * This test validates proper input validation and bounds checking across MIDI parsing,
 * waveform generation, and platform simulation parameters to prevent crashes and ensure
 * proper error handling.
 */
@OptIn(ExperimentalTime::class)
class InputValidationTest {

    @Test
    fun `should reject MIDI files with malformed headers`() {
        // GIVEN: MIDI file with invalid header chunk ID
        val invalidHeaderBytes = byteArrayOf(
            // Wrong header - should be "MThd" but using "XXXX"
            0x58.toByte(), 0x58.toByte(), 0x58.toByte(), 0x58.toByte(), // "XXXX" instead of "MThd"
            0x00, 0x00, 0x00, 0x06, // Header length (6)
            0x00, 0x00, // Format type 0
            0x00, 0x01, // 1 track
            0x00, 0x60  // 96 ticks per quarter note
        )

        // WHEN/THEN: Should throw exception for invalid header
        val exception = assertFailsWith<Exception> {
            MidiParser.parseMidiBytes(invalidHeaderBytes)
        }
        assertContains(exception.message!!, "Not a valid MIDI file", ignoreCase = true)
    }

    @Test
    fun `should reject MIDI files with invalid header length`() {
        // GIVEN: MIDI file with wrong header length
        val invalidLengthBytes = byteArrayOf(
            0x4D, 0x54, 0x68, 0x64, // "MThd"
            0x00, 0x00, 0x00, 0x08, // Header length (8 instead of 6)
            0x00, 0x00, // Format type 0
            0x00, 0x01, // 1 track
            0x00, 0x60, // 96 ticks per quarter note
            0x00, 0x00  // Extra bytes
        )

        // WHEN/THEN: Should throw exception for invalid header length
        val exception = assertFailsWith<Exception> {
            MidiParser.parseMidiBytes(invalidLengthBytes)
        }
        assertContains(exception.message!!, "Invalid MIDI header length", ignoreCase = true)
    }

    @Test
    fun `should reject MIDI files with oversized track length claims`() {
        // GIVEN: Valid minimal MIDI header but claims huge track size that exceeds available data
        val oversizedTrackBytes = byteArrayOf(
            0x4D, 0x54, 0x68, 0x64, // "MThd"
            0x00, 0x00, 0x00, 0x06, // Header length (6)
            0x00, 0x00, // Format type 0
            0x00, 0x01, // 1 track
            0x00, 0x60, // 96 ticks per quarter note

            0x4D, 0x54, 0x72, 0x6B, // "MTrk"
            0x7F.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // Track length (very large: 2147483647 bytes)
            0x00, // Delta time 0
            0xFF.toByte(), 0x2F, 0x00 // End of track meta event (only 3 bytes of track data)
        )

        // WHEN/THEN: Should reject files with track length claims that exceed available data
        val exception = assertFailsWith<Exception> {
            MidiParser.parseMidiBytes(oversizedTrackBytes)
        }
        assertContains(exception.message!!, "Invalid track length", ignoreCase = true)
        assertContains(exception.message!!, "bytes claimed", ignoreCase = true)
    }

    @Test
    fun `should reject MIDI files with negative track length`() {
        // GIVEN: Valid MIDI header but negative track length
        val negativeTrackLengthBytes = byteArrayOf(
            0x4D, 0x54, 0x68, 0x64, // "MThd"
            0x00, 0x00, 0x00, 0x06, // Header length (6)
            0x00, 0x00, // Format type 0
            0x00, 0x01, // 1 track
            0x00, 0x60, // 96 ticks per quarter note

            0x4D, 0x54, 0x72, 0x6B, // "MTrk"
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // Track length (negative: -1)
        )

        // WHEN/THEN: Should reject files with negative track length
        val exception = assertFailsWith<Exception> {
            MidiParser.parseMidiBytes(negativeTrackLengthBytes)
        }
        assertContains(exception.message!!, "Invalid track length", ignoreCase = true)
        assertContains(exception.message!!, "negative length", ignoreCase = true)
    }

    @Test
    fun `should reject MIDI files with no track data`() {
        // GIVEN: MIDI header claiming tracks but no track data follows
        val noTrackDataBytes = byteArrayOf(
            0x4D, 0x54, 0x68, 0x64, // "MThd"
            0x00, 0x00, 0x00, 0x06, // Header length (6)
            0x00, 0x00, // Format type 0
            0x00, 0x01, // 1 track
            0x00, 0x60  // 96 ticks per quarter note
            // Missing track data
        )

        // WHEN/THEN: Should throw exception for missing track data
        val exception = assertFailsWith<Exception> {
            MidiParser.parseMidiBytes(noTrackDataBytes)
        }
        assertNotNull(exception.message)
    }

    @Test
    fun `should reject empty MIDI file data`() {
        // GIVEN: Empty byte array
        val emptyBytes = byteArrayOf()

        // WHEN/THEN: Should throw exception for empty data
        val exception = assertFailsWith<Exception> {
            MidiParser.parseMidiBytes(emptyBytes)
        }
        assertNotNull(exception.message)
    }

    @Test
    fun `should reject waveform generation with invalid sample rate`() {
        // GIVEN: Invalid sample rates
        val invalidSampleRates = listOf(0, -1, -44100)

        invalidSampleRates.forEach { sampleRate ->
            // WHEN/THEN: Should throw IllegalArgumentException with proper validation
            val exception = assertFailsWith<IllegalArgumentException> {
                WaveformGenerator.generateWaveform(
                    sampleRate = sampleRate,
                    frequency = 440.0,
                    amplitude = 0.5,
                    duration = 1.seconds,
                    waveform = SoundPlayer.Waveform.SINE
                )
            }
            assertContains(exception.message!!, "Sample rate must be positive", ignoreCase = true)
        }
    }

    @Test
    fun `should reject waveform generation with invalid frequency`() {
        // GIVEN: Invalid frequencies
        val invalidFrequencies = listOf(-1.0, 0.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)

        invalidFrequencies.forEach { frequency ->
            // WHEN/THEN: Should throw IllegalArgumentException with proper validation
            val exception = assertFailsWith<IllegalArgumentException> {
                WaveformGenerator.generateWaveform(
                    sampleRate = 44100,
                    frequency = frequency,
                    amplitude = 0.5,
                    duration = 1.seconds,
                    waveform = SoundPlayer.Waveform.SINE
                )
            }
            assertContains(exception.message!!, "Frequency must be positive and finite", ignoreCase = true)
        }
    }

    @Test
    fun `should reject waveform generation with invalid amplitude`() {
        // GIVEN: Invalid amplitudes
        val invalidAmplitudes = listOf(-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)

        invalidAmplitudes.forEach { amplitude ->
            // WHEN/THEN: Should throw IllegalArgumentException with proper validation
            val exception = assertFailsWith<IllegalArgumentException> {
                WaveformGenerator.generateWaveform(
                    sampleRate = 44100,
                    frequency = 440.0,
                    amplitude = amplitude,
                    duration = 1.seconds,
                    waveform = SoundPlayer.Waveform.SINE
                )
            }
            assertContains(exception.message!!, "Amplitude must be between 0.0 and 1.0", ignoreCase = true)
        }
    }

    @Test
    fun `should reject waveform generation with invalid duration`() {
        // GIVEN: Invalid durations
        val invalidDurations = listOf(
            (-1).seconds,   // Negative duration
            (-100).milliseconds, // Negative duration
        )
        val validDurations = listOf(
            Duration.ZERO,  // Zero duration (valid)
            1.milliseconds, // Very short but positive
            1.seconds,      // Normal duration
        )

        // WHEN/THEN: Invalid durations should be rejected
        invalidDurations.forEach { duration ->
            val exception = assertFailsWith<IllegalArgumentException> {
                WaveformGenerator.generateWaveform(
                    sampleRate = 44100,
                    frequency = 440.0,
                    amplitude = 0.5,
                    duration = duration,
                    waveform = SoundPlayer.Waveform.SINE
                )
            }
            assertContains(exception.message!!, "Duration must be non-negative", ignoreCase = true)
        }

        // WHEN/THEN: Valid durations should work
        validDurations.forEach { duration ->
            val samples = WaveformGenerator.generateWaveform(
                sampleRate = 44100,
                frequency = 440.0,
                amplitude = 0.5,
                duration = duration,
                waveform = SoundPlayer.Waveform.SINE
            )
            assertNotNull(samples, "Samples should not be null for duration $duration")
            if (duration > Duration.ZERO) {
                assertTrue(samples.isNotEmpty(), "Should generate at least one sample for positive duration $duration")
            } else {
                assertTrue(samples.isEmpty(), "Should generate empty array for zero duration")
            }
        }
    }

    @Test
    fun `should validate MIDI pitch conversion bounds properly`() {
        // GIVEN: Valid and invalid MIDI pitch values
        val validPitches = listOf(0, 60, 127) // Valid MIDI range
        val invalidPitches = listOf(-1, 128, 255, Int.MIN_VALUE, Int.MAX_VALUE) // Outside valid range

        // WHEN/THEN: Valid pitches should work correctly
        validPitches.forEach { pitch ->
            val frequency = WaveformGenerator.midiPitchToFrequency(pitch)
            assertTrue(frequency.isFinite(), "Frequency should be finite for valid pitch $pitch")
            assertTrue(frequency > 0, "Frequency should be positive for valid pitch $pitch")
        }

        // WHEN/THEN: Invalid pitches should be clamped to valid range and return finite results
        invalidPitches.forEach { pitch ->
            val frequency = WaveformGenerator.midiPitchToFrequency(pitch)
            assertTrue(frequency.isFinite(), "Frequency should be finite even for invalid pitch $pitch (should be clamped)")
            assertTrue(frequency > 0, "Frequency should be positive even for invalid pitch $pitch")

            // Verify the result is within reasonable audio frequency range
            assertTrue(frequency >= 8.0 && frequency <= 20000.0,
                "Frequency should be within audible range for pitch $pitch, got $frequency Hz")
        }
    }

    @Test
    fun `should validate MIDI velocity conversion bounds`() {
        // GIVEN: Invalid MIDI velocity values (outside 0-127 range)
        val validVelocities = listOf(0, 1, 64, 127)
        val invalidVelocities = listOf(-1, 128, 255, Int.MIN_VALUE, Int.MAX_VALUE)

        // WHEN/THEN: Valid velocities should work correctly
        validVelocities.forEach { velocity ->
            val amplitude = WaveformGenerator.midiVelocityToAmplitude(velocity)
            assertTrue(amplitude in 0.0..1.0, "Amplitude should be normalized for velocity $velocity")
        }

        // WHEN/THEN: Invalid velocities should be clamped
        invalidVelocities.forEach { velocity ->
            val amplitude = WaveformGenerator.midiVelocityToAmplitude(velocity)
            assertTrue(amplitude in 0.0..1.0, "Amplitude should be clamped for invalid velocity $velocity")
        }
    }

    @Test
    fun `should reject WWWSimulation with invalid speed parameters`() {
        // GIVEN: Valid position and start time
        val validPosition = Position(lat = 40.7128, lng = -74.0060)
        val validStartTime = kotlin.time.Clock.System.now()

        // WHEN/THEN: Should reject speeds outside valid range
        val invalidSpeeds = listOf(0, -1, 601, 1000, Int.MIN_VALUE, Int.MAX_VALUE)

        invalidSpeeds.forEach { speed ->
            val exception = assertFailsWith<IllegalArgumentException> {
                WWWSimulation(
                    startDateTime = validStartTime,
                    userPosition = validPosition,
                    initialSpeed = speed
                )
            }
            assertContains(exception.message!!, "Speed must be between", ignoreCase = true)
        }
    }

    @Test
    fun `should accept WWWSimulation with valid speed parameters`() {
        // GIVEN: Valid position and start time
        val validPosition = Position(lat = 40.7128, lng = -74.0060)
        val validStartTime = kotlin.time.Clock.System.now()

        // WHEN/THEN: Should accept speeds within valid range
        val validSpeeds = listOf(1, 50, 100, 250, 300)

        validSpeeds.forEach { speed ->
            val simulation = WWWSimulation(
                startDateTime = validStartTime,
                userPosition = validPosition,
                initialSpeed = speed
            )
            assertEquals(speed, simulation.speed, "Speed should be set correctly")
        }
    }

    @Test
    fun `should validate WWWSimulation speed changes`() {
        // GIVEN: Valid simulation
        val validPosition = Position(lat = 40.7128, lng = -74.0060)
        val validStartTime = kotlin.time.Clock.System.now()
        val simulation = WWWSimulation(
            startDateTime = validStartTime,
            userPosition = validPosition,
            initialSpeed = 50
        )

        // WHEN/THEN: Should reject invalid speed changes
        val invalidSpeeds = listOf(0, -1, 301, 1000)

        invalidSpeeds.forEach { speed ->
            val exception = assertFailsWith<IllegalArgumentException> {
                simulation.setSpeed(speed)
            }
            assertContains(exception.message!!, "Speed must be between", ignoreCase = true)
        }

        // AND: Should accept valid speed changes
        val validSpeeds = listOf(1, 100, 300)
        validSpeeds.forEach { speed ->
            val result = simulation.setSpeed(speed)
            assertEquals(speed, result, "Speed change should return new speed")
            assertEquals(speed, simulation.speed, "Speed should be updated")
        }
    }

    @Test
    fun `should validate WWWSimulation resume speed parameters`() {
        // GIVEN: Paused simulation
        val validPosition = Position(lat = 40.7128, lng = -74.0060)
        val validStartTime = kotlin.time.Clock.System.now()
        val simulation = WWWSimulation(
            startDateTime = validStartTime,
            userPosition = validPosition,
            initialSpeed = 50
        )
        simulation.pause()

        // WHEN/THEN: Should reject invalid resume speeds
        val invalidSpeeds = listOf(0, -1, 301, 1000)

        invalidSpeeds.forEach { speed ->
            val exception = assertFailsWith<IllegalArgumentException> {
                simulation.resume(speed)
            }
            assertContains(exception.message!!, "Speed must be between", ignoreCase = true)
        }

        // AND: Should accept valid resume speeds
        val validSpeeds = listOf(1, 100, 300)
        validSpeeds.forEach { speed ->
            simulation.pause() // Re-pause for each test
            simulation.resume(speed)
            assertEquals(speed, simulation.speed, "Resume speed should be set correctly")
        }
    }

    @Test
    fun `should validate platform simulation parameter boundary conditions`() {
        // GIVEN: Edge case positions
        val extremePositions = listOf(
            Position(lat = 90.0, lng = 180.0),    // North Pole, Date Line
            Position(lat = -90.0, lng = -180.0),  // South Pole, Date Line
            Position(lat = 0.0, lng = 0.0),       // Null Island
            Position(lat = 85.0, lng = 179.9),    // Near polar/antimeridian
            Position(lat = -85.0, lng = -179.9)   // Near polar/antimeridian
        )

        val validStartTime = kotlin.time.Clock.System.now()

        // WHEN/THEN: Should handle extreme but valid positions
        extremePositions.forEach { position ->
            val simulation = WWWSimulation(
                startDateTime = validStartTime,
                userPosition = position,
                initialSpeed = 100
            )
            assertEquals(position, simulation.getUserPosition(), "Position should be preserved")
        }
    }
}