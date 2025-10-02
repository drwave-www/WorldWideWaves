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

// Audio conversion constants
private const val SAMPLE_CONVERSION_FACTOR = 32767.0
private const val SAMPLE_MIN_VALUE = -32768
private const val SAMPLE_MAX_VALUE = 32767
private const val LOW_BYTE_MASK = 0xFF
private const val BYTE_SHIFT = 8

/**
 * iOS-specific audio buffer implementation
 */
class IosAudioBuffer(
    samples: DoubleArray,
    override val sampleRate: Int,
    bitsPerSample: Int,
    channels: Int,
) : AudioBuffer {
    private val buffer: ByteArray =
        when (bitsPerSample) {
            8 -> samples.toByteArray()
            16 -> samples.toShortByteArray()
            else -> throw IllegalArgumentException("Unsupported bits per sample: $bitsPerSample")
        }

    override val sampleCount: Int = samples.size

    override fun getRawBuffer(): ByteArray = buffer

    // Convert double array to byte array (8-bit PCM)
    private fun DoubleArray.toByteArray(): ByteArray {
        val result = ByteArray(this.size)
        for (i in this.indices) {
            // Scale to -128..127 range
            val sample = (this[i] * 127.0).toInt().coerceIn(-128, 127)
            result[i] = sample.toByte()
        }
        return result
    }

    // Convert double array to byte array (16-bit PCM)
    private fun DoubleArray.toShortByteArray(): ByteArray {
        val result = ByteArray(this.size * 2)
        for (i in this.indices) {
            // Scale to -32768..32767 range
            val sample = (this[i] * SAMPLE_CONVERSION_FACTOR).toInt().coerceIn(SAMPLE_MIN_VALUE, SAMPLE_MAX_VALUE)
            // Little endian
            result[i * 2] = (sample and LOW_BYTE_MASK).toByte()
            result[i * 2 + 1] = ((sample shr BYTE_SHIFT) and LOW_BYTE_MASK).toByte()
        }
        return result
    }
}

/**
 * iOS implementation of AudioBufferFactory
 */
actual object AudioBufferFactory {
    /**
     * Creates an AudioBuffer from sample data
     *
     * @param samples Array of audio samples in the range -1.0 to 1.0
     * @param sampleRate Sample rate in Hz (e.g., 44100)
     * @param bitsPerSample Bit depth (8 or 16)
     * @param channels Number of audio channels (1 for mono, 2 for stereo)
     * @return Platform-specific AudioBuffer implementation
     */
    actual fun createFromSamples(
        samples: DoubleArray,
        sampleRate: Int,
        bitsPerSample: Int,
        channels: Int,
    ): AudioBuffer = IosAudioBuffer(samples, sampleRate, bitsPerSample, channels)
}
