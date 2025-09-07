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