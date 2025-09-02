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

import kotlin.time.Duration

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
    suspend fun playTone(
        frequency: Double,
        amplitude: Double,
        duration: Duration,
        waveform: Waveform = Waveform.SINE,
    )

    fun release()

    /**
     * Available waveform types for synthesis
     */
    enum class Waveform {
        SINE, // Smooth, pure tone
        SQUARE, // Rich in harmonics, sounds "buzzy"
        TRIANGLE, // Smoother than square, but with some harmonics
        SAWTOOTH, // Very rich in harmonics, sounds "brassy"
    }
}

/**
 * Interface for platform-specific volume control
 */
interface VolumeController {
    fun getCurrentVolume(): Float

    fun setVolume(level: Float)
}

/**
 * Base audio buffer interface for platform-specific implementations
 */
interface AudioBuffer {
    fun getRawBuffer(): ByteArray

    val sampleCount: Int
    val sampleRate: Int
}

/**
 * Factory for creating platform-specific audio buffers
 */
expect object AudioBufferFactory {
    fun createFromSamples(
        samples: DoubleArray,
        sampleRate: Int,
        bitsPerSample: Int = 16,
        channels: Int = 1,
    ): AudioBuffer
}
