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
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.time.Duration
import com.worldwidewaves.shared.WWWGlobals

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
        waveform: SoundPlayer.Waveform,
    ): DoubleArray {
        // Validate input parameters
        require(sampleRate > 0) { "Sample rate must be positive, got: $sampleRate" }
        require(frequency > 0.0 && frequency.isFinite()) { "Frequency must be positive and finite, got: $frequency" }
        require(amplitude >= 0.0 && amplitude <= 1.0 && amplitude.isFinite()) { "Amplitude must be between 0.0 and 1.0, got: $amplitude" }
        require(duration >= Duration.ZERO) { "Duration must be non-negative, got: $duration" }

        // Calculate number of samples
        val numSamples =
            (
                sampleRate * duration.inWholeSeconds +
                    (sampleRate * (duration.inWholeNanoseconds % 1_000_000_000) / 1_000_000_000.0)
            ).toInt()

        // Handle edge case of zero duration
        if (numSamples <= 0) {
            return doubleArrayOf()
        }

        // Create sample array
        val samples = DoubleArray(numSamples)

        // Pre-calculate constants for better performance
        val phaseIncrement = 2.0 * PI * frequency / sampleRate

        // Generate the specified waveform with optimized algorithms
        when (waveform) {
            SoundPlayer.Waveform.SINE -> {
                for (i in 0 until numSamples) {
                    samples[i] = sin(i * phaseIncrement) * amplitude
                }
            }
            SoundPlayer.Waveform.SQUARE -> {
                // Square wave is more efficient without sin() calculation
                val samplesPerCycle = (sampleRate / frequency).toInt()
                val halfCycle = samplesPerCycle / 2
                for (i in 0 until numSamples) {
                    samples[i] = if ((i % samplesPerCycle) < halfCycle) amplitude else -amplitude
                }
            }
            SoundPlayer.Waveform.TRIANGLE -> {
                val samplesPerCycle = (sampleRate / frequency).toInt()
                val quarterCycle = samplesPerCycle / 4
                for (i in 0 until numSamples) {
                    val cyclePos = i % samplesPerCycle
                    samples[i] = when {
                        cyclePos < quarterCycle -> (cyclePos.toDouble() / quarterCycle) * amplitude
                        cyclePos < 3 * quarterCycle -> (2.0 - cyclePos.toDouble() / quarterCycle) * amplitude
                        else -> ((cyclePos.toDouble() / quarterCycle) - 4.0) * amplitude
                    }
                }
            }
            SoundPlayer.Waveform.SAWTOOTH -> {
                val samplesPerCycle = (sampleRate / frequency).toInt()
                for (i in 0 until numSamples) {
                    val cyclePos = i % samplesPerCycle
                    samples[i] = (2.0 * cyclePos / samplesPerCycle - 1.0) * amplitude
                }
            }
        }

        // Apply envelope to avoid clicks
        applyEnvelope(samples, sampleRate)

        return samples
    }

    /**
     * Apply a simple attack/release envelope to avoid clicks
     */
    private fun applyEnvelope(
        samples: DoubleArray,
        sampleRate: Int,
    ) {
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
     * Valid MIDI pitch range: 0-127
     */
    fun midiPitchToFrequency(pitch: Int): Double {
        // Clamp pitch to valid MIDI range to prevent mathematical overflow
        val clampedPitch = pitch.coerceIn(0, 127)
        val frequency = 440.0 * 2.0.pow((clampedPitch - 69).toDouble() / 12.0)

        // Ensure result is finite and positive
        return if (frequency.isFinite() && frequency > 0.0) {
            frequency
        } else {
            // Fallback to middle C (MIDI note 60)
            WWWGlobals.Midi.A4_FREQUENCY * 2.0.pow((WWWGlobals.Midi.MIDDLE_C_MIDI_NOTE - WWWGlobals.Midi.A4_MIDI_NOTE).toDouble() / WWWGlobals.Midi.OCTAVE_DIVISOR.toDouble())
        }
    }

    /**
     * Convert MIDI velocity (0-127) to amplitude (0.0-1.0)
     */
    fun midiVelocityToAmplitude(velocity: Int): Double = (velocity / WWWGlobals.Midi.MAX_VELOCITY.toDouble()).coerceIn(0.0, 1.0)
}
