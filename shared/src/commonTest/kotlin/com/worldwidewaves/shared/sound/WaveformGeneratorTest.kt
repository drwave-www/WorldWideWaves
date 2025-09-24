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

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for WaveformGenerator audio waveform generation and conversion utilities.
 * These tests ensure accurate waveform generation, proper envelope application,
 * and correct MIDI-to-audio parameter conversions.
 */
class WaveformGeneratorTest {
    @Test
    fun `should convert MIDI pitch to frequency correctly`() {
        // GIVEN: Standard MIDI note values

        // WHEN/THEN: Testing standard reference notes
        val a4Frequency = WaveformGenerator.midiPitchToFrequency(69)
        assertEquals(440.0, a4Frequency, 0.01, "A4 (MIDI 69) should be 440Hz")

        val c4Frequency = WaveformGenerator.midiPitchToFrequency(60)
        assertEquals(261.63, c4Frequency, 0.01, "C4 (MIDI 60) should be ~261.63Hz")

        val c5Frequency = WaveformGenerator.midiPitchToFrequency(72)
        assertEquals(523.25, c5Frequency, 0.01, "C5 (MIDI 72) should be ~523.25Hz")
    }

    @Test
    fun `should handle extreme MIDI pitch values correctly`() {
        // GIVEN: Extreme MIDI note values

        // WHEN/THEN: Testing boundary values
        val lowNote = WaveformGenerator.midiPitchToFrequency(0)
        assertTrue(lowNote > 0.0, "MIDI note 0 should produce positive frequency")
        assertEquals(8.18, lowNote, 0.01, "MIDI note 0 should be ~8.18Hz")

        val highNote = WaveformGenerator.midiPitchToFrequency(127)
        assertTrue(highNote < 20000.0, "MIDI note 127 should be under 20kHz")
        assertEquals(12543.85, highNote, 0.1, "MIDI note 127 should be ~12.5kHz")
    }

    @Test
    fun `should maintain correct octave relationships`() {
        // GIVEN: Notes one octave apart
        val c3 = WaveformGenerator.midiPitchToFrequency(48)
        val c4 = WaveformGenerator.midiPitchToFrequency(60)
        val c5 = WaveformGenerator.midiPitchToFrequency(72)

        // WHEN/THEN: Each octave should double the frequency
        assertEquals(c3 * 2, c4, 0.01, "C4 should be exactly double C3")
        assertEquals(c4 * 2, c5, 0.01, "C5 should be exactly double C4")
    }

    @Test
    fun `should convert MIDI velocity to amplitude correctly`() {
        // GIVEN: Standard MIDI velocity values

        // WHEN/THEN: Testing standard conversions
        val fullAmplitude = WaveformGenerator.midiVelocityToAmplitude(127)
        assertEquals(1.0, fullAmplitude, 0.01, "Velocity 127 should give amplitude 1.0")

        val halfAmplitude = WaveformGenerator.midiVelocityToAmplitude(64)
        assertEquals(0.5, halfAmplitude, 0.04, "Velocity 64 should give amplitude ~0.5")

        val zeroAmplitude = WaveformGenerator.midiVelocityToAmplitude(0)
        assertEquals(0.0, zeroAmplitude, 0.01, "Velocity 0 should give amplitude 0.0")
    }

    @Test
    fun `should clamp velocity values to valid range`() {
        // GIVEN: Out-of-range velocity values

        // WHEN/THEN: Values should be clamped to valid range
        val overMax = WaveformGenerator.midiVelocityToAmplitude(200)
        assertEquals(1.0, overMax, 0.01, "Velocity over 127 should be clamped to 1.0")

        val negative = WaveformGenerator.midiVelocityToAmplitude(-10)
        assertEquals(0.0, negative, 0.01, "Negative velocity should be clamped to 0.0")

        val minValid = WaveformGenerator.midiVelocityToAmplitude(1)
        assertEquals(1.0 / 127.0, minValid, 0.01, "Velocity 1 should give minimal amplitude")
    }

    @Test
    fun `should generate correct number of samples for given duration`() {
        // GIVEN: Specific sample rate and duration

        // WHEN: Generating 1-second sample at 44100Hz
        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = 44100,
                frequency = 440.0,
                amplitude = 1.0,
                duration = 1.seconds,
                waveform = SoundPlayer.Waveform.SINE,
            )

        // THEN: Should have exactly 44100 samples
        assertEquals(44100, samples.size, "1 second at 44100Hz should have 44100 samples")
    }

    @Test
    fun `should handle fractional durations correctly`() {
        // GIVEN: Fractional duration

        // WHEN: Generating 500ms sample
        val samples500ms =
            WaveformGenerator.generateWaveform(
                sampleRate = 44100,
                frequency = 440.0,
                amplitude = 1.0,
                duration = 500.milliseconds,
                waveform = SoundPlayer.Waveform.SINE,
            )

        // THEN: Should have correct sample count
        assertEquals(22050, samples500ms.size, "500ms at 44100Hz should have 22050 samples")
    }

    @Test
    fun `should generate mathematically correct sine wave`() {
        // GIVEN: Simple parameters for easy verification
        val sampleRate = 1000
        val frequency = 1.0 // 1 cycle per second
        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = sampleRate,
                frequency = frequency,
                amplitude = 1.0,
                duration = 1.seconds,
                waveform = SoundPlayer.Waveform.SINE,
            )

        // WHEN/THEN: Testing sine wave properties
        assertEquals(1000, samples.size, "Should have 1000 samples for 1 second at 1000Hz")

        // Check general sine wave characteristics in the middle region (avoiding envelope effects)
        val attackSamples = (sampleRate * 0.01).toInt() // 10 samples attack envelope
        val releaseSamples = (sampleRate * 0.01).toInt() // 10 samples release envelope

        // Test in the stable middle region where envelope doesn't affect the waveform
        val stableStart = attackSamples + 100 // Well past attack envelope
        val stableEnd = samples.size - releaseSamples - 100 // Well before release envelope

        // Check that we have positive and negative values in the stable region
        val stableSamples = samples.sliceArray(stableStart until stableEnd)
        assertTrue(stableSamples.any { it > 0.5 }, "Should have positive peaks in stable region")
        assertTrue(stableSamples.any { it < -0.5 }, "Should have negative valleys in stable region")
    }

    @Test
    fun `should generate proper sine wave frequency content`() {
        // GIVEN: Parameters that avoid envelope effects
        val sampleRate = 8000
        val frequency = 100.0
        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = sampleRate,
                frequency = frequency,
                amplitude = 1.0,
                duration = 1.seconds,
                waveform = SoundPlayer.Waveform.SINE,
            )

        // WHEN: Checking middle portion away from envelope
        val attackSamples = (sampleRate * 0.01).toInt() // 80 samples
        val releaseSamples = (sampleRate * 0.01).toInt() // 80 samples
        val samplesPerPeriod = (sampleRate / frequency).toInt() // 80 samples per period

        // THEN: Check sine wave properties in stable region
        val middleStart = attackSamples + samplesPerPeriod
        val middleEnd = samples.size - releaseSamples - samplesPerPeriod

        for (i in middleStart until middleEnd step samplesPerPeriod) {
            if (i + samplesPerPeriod < middleEnd) {
                val expected = sin(2.0 * PI * frequency * i / sampleRate)
                assertEquals(expected, samples[i], 0.05, "Sine wave should match expected values")
            }
        }
    }

    @Test
    fun `should generate square wave with proper characteristics`() {
        // GIVEN: Square wave parameters
        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = 8000,
                frequency = 100.0,
                amplitude = 1.0,
                duration = 1.seconds,
                waveform = SoundPlayer.Waveform.SQUARE,
            )

        // WHEN/THEN: Square wave should alternate between +amplitude and -amplitude
        val samplesPerCycle = (8000 / 100.0).toInt()
        val halfCycle = samplesPerCycle / 2
        val attackSamples = (8000 * 0.01).toInt()

        // Check in stable region (away from envelope)
        for (i in attackSamples until (samples.size - attackSamples)) {
            val cyclePos = i % samplesPerCycle
            val isFirstHalf = cyclePos < halfCycle
            val expectedSign = if (isFirstHalf) 1.0 else -1.0
            assertTrue(samples[i] * expectedSign > 0, "Square wave should maintain proper polarity")
        }
    }

    @Test
    fun `should generate triangle wave with linear ramps`() {
        // GIVEN: Triangle wave parameters
        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = 8000,
                frequency = 100.0,
                amplitude = 1.0,
                duration = 1.seconds,
                waveform = SoundPlayer.Waveform.TRIANGLE,
            )

        // WHEN/THEN: Triangle wave should have linear segments
        val samplesPerCycle = (8000 / 100.0).toInt()
        val quarterCycle = samplesPerCycle / 4
        val attackSamples = (8000 * 0.01).toInt()

        // Check that triangle wave increases linearly in first quarter
        for (i in attackSamples until (attackSamples + quarterCycle)) {
            if (i + 1 < samples.size) {
                val slope = samples[i + 1] - samples[i]
                assertTrue(slope > 0, "Triangle wave should increase in first quarter")
            }
        }
    }

    @Test
    fun `should generate sawtooth wave with correct ramp`() {
        // GIVEN: Sawtooth wave parameters
        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = 8000,
                frequency = 100.0,
                amplitude = 1.0,
                duration = 1.seconds,
                waveform = SoundPlayer.Waveform.SAWTOOTH,
            )

        // WHEN/THEN: Sawtooth should have linear ramp from -amplitude to +amplitude
        val samplesPerCycle = (8000 / 100.0).toInt()
        val attackSamples = (8000 * 0.01).toInt()

        // Check a complete cycle in stable region
        val cycleStart = attackSamples + samplesPerCycle
        if (cycleStart + samplesPerCycle < samples.size) {
            var previousValue = samples[cycleStart]
            for (i in (cycleStart + 1) until (cycleStart + samplesPerCycle - 1)) {
                val currentValue = samples[i]
                assertTrue(currentValue > previousValue, "Sawtooth should increase monotonically")
                previousValue = currentValue
            }
        }
    }

    @Test
    fun `should respect amplitude bounds for all waveforms`() {
        // GIVEN: Various amplitude values
        val testAmplitudes = doubleArrayOf(0.1, 0.5, 0.8, 1.0)

        for (waveform in SoundPlayer.Waveform.entries) {
            for (amplitude in testAmplitudes) {
                // WHEN: Generating waveform with specific amplitude
                val samples =
                    WaveformGenerator.generateWaveform(
                        sampleRate = 44100,
                        frequency = 440.0,
                        amplitude = amplitude,
                        duration = 100.milliseconds,
                        waveform = waveform,
                    )

                // THEN: All samples should be within amplitude bounds
                for (sample in samples) {
                    assertTrue(
                        abs(sample) <= amplitude + 0.001, // Small tolerance for floating point
                        "Sample $sample should not exceed amplitude $amplitude for $waveform",
                    )
                }
            }
        }
    }

    @Test
    fun `should handle various frequencies correctly`() {
        // GIVEN: Range of frequencies
        val testFrequencies = doubleArrayOf(20.0, 440.0, 1000.0, 8000.0)

        for (frequency in testFrequencies) {
            for (waveform in SoundPlayer.Waveform.entries) {
                // WHEN: Generating waveform with specific frequency
                val samples =
                    WaveformGenerator.generateWaveform(
                        sampleRate = 44100,
                        frequency = frequency,
                        amplitude = 0.8,
                        duration = 100.milliseconds,
                        waveform = waveform,
                    )

                // THEN: Should produce valid samples
                assertTrue(samples.isNotEmpty(), "Should produce samples for frequency $frequency")
                assertTrue(samples.all { it.isFinite() }, "All samples should be finite for frequency $frequency")
            }
        }
    }

    @Test
    fun `should apply envelope to prevent audio clicks`() {
        // GIVEN: Short, high-amplitude tone that would cause clicks without envelope
        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = 44100,
                frequency = 1000.0,
                amplitude = 1.0,
                duration = 50.milliseconds,
                waveform = SoundPlayer.Waveform.SQUARE,
            )

        // WHEN/THEN: Envelope should fade in and out
        val attackSamples = (44100 * 0.01).toInt() // 10ms attack
        val releaseSamples = (44100 * 0.01).toInt() // 10ms release

        // Check attack envelope (fade in)
        if (samples.size > attackSamples) {
            assertTrue(abs(samples[0]) < abs(samples[attackSamples - 1]), "Should fade in at start")
        }

        // Check release envelope (fade out)
        if (samples.size > releaseSamples) {
            val releaseStart = samples.size - releaseSamples
            assertTrue(
                abs(samples[samples.size - 1]) < abs(samples[releaseStart]),
                "Should fade out at end",
            )
        }
    }

    @Test
    fun `should handle zero duration gracefully`() {
        // GIVEN: Zero duration

        // WHEN: Generating waveform with zero duration
        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = 44100,
                frequency = 440.0,
                amplitude = 1.0,
                duration = 0.milliseconds,
                waveform = SoundPlayer.Waveform.SINE,
            )

        // THEN: Should produce empty array
        assertEquals(0, samples.size, "Zero duration should produce empty array")
    }

    @Test
    fun `should handle extreme parameters without errors`() {
        // GIVEN: Extreme but valid parameters

        // WHEN/THEN: Testing very low frequency
        val lowFreqSamples =
            WaveformGenerator.generateWaveform(
                sampleRate = 44100,
                frequency = 1.0, // 1 Hz
                amplitude = 1.0,
                duration = 100.milliseconds,
                waveform = SoundPlayer.Waveform.SINE,
            )
        assertTrue(lowFreqSamples.isNotEmpty(), "Very low frequency should produce samples")

        // Testing high frequency (but reasonable)
        val highFreqSamples =
            WaveformGenerator.generateWaveform(
                sampleRate = 44100,
                frequency = 8000.0, // 8 kHz
                amplitude = 1.0,
                duration = 100.milliseconds,
                waveform = SoundPlayer.Waveform.SINE,
            )
        assertTrue(highFreqSamples.isNotEmpty(), "High frequency should produce samples")

        // Testing low sample rate
        val lowSampleRateSamples =
            WaveformGenerator.generateWaveform(
                sampleRate = 8000,
                frequency = 440.0,
                amplitude = 1.0,
                duration = 100.milliseconds,
                waveform = SoundPlayer.Waveform.SINE,
            )
        assertTrue(lowSampleRateSamples.isNotEmpty(), "Low sample rate should produce samples")
    }

    @Test
    fun `should generate all waveform types without errors`() {
        // GIVEN: All available waveform types

        // WHEN/THEN: All waveforms should generate without errors
        for (waveform in SoundPlayer.Waveform.entries) {
            val samples =
                WaveformGenerator.generateWaveform(
                    sampleRate = 44100,
                    frequency = 440.0,
                    amplitude = 0.8,
                    duration = 100.milliseconds,
                    waveform = waveform,
                )

            assertTrue(samples.isNotEmpty(), "Waveform $waveform should produce samples")
            assertTrue(samples.all { it.isFinite() }, "All samples should be finite for $waveform")
            assertTrue(samples.any { it != 0.0 }, "Should produce non-zero samples for $waveform")
        }
    }

    @Test
    fun `should maintain waveform characteristics across different sample rates`() {
        // GIVEN: Different sample rates
        val sampleRates = intArrayOf(8000, 22050, 44100, 48000)

        for (sampleRate in sampleRates) {
            // WHEN: Generating sine wave
            val samples =
                WaveformGenerator.generateWaveform(
                    sampleRate = sampleRate,
                    frequency = 100.0,
                    amplitude = 1.0,
                    duration = 100.milliseconds,
                    waveform = SoundPlayer.Waveform.SINE,
                )

            // THEN: Should maintain sine wave characteristics
            assertTrue(samples.isNotEmpty(), "Should produce samples at $sampleRate Hz")
            assertEquals(sampleRate / 10, samples.size, "Should have correct sample count for $sampleRate Hz")
        }
    }

    @Test
    fun `should handle very short durations correctly`() {
        // GIVEN: Very short duration

        // WHEN: Generating very short waveform
        val shortSamples =
            WaveformGenerator.generateWaveform(
                sampleRate = 44100,
                frequency = 440.0,
                amplitude = 1.0,
                duration = 1.milliseconds,
                waveform = SoundPlayer.Waveform.SINE,
            )

        // THEN: Should produce correct number of samples
        assertEquals(44, shortSamples.size, "1ms at 44100Hz should produce 44 samples")
        assertTrue(shortSamples.all { it.isFinite() }, "All samples should be finite")
    }
}
