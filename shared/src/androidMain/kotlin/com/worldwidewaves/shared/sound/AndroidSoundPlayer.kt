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

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Android implementation of AudioBuffer
 */
actual object AudioBufferFactory {
    actual fun createFromSamples(
        samples: DoubleArray,
        sampleRate: Int,
        bitsPerSample: Int,
        channels: Int,
    ): AudioBuffer = AndroidAudioBuffer(samples, sampleRate, bitsPerSample, channels)
}

// ----------------------------------------------------------------------------

/**
 * Android-specific audio buffer implementation
 */
class AndroidAudioBuffer(
    samples: DoubleArray,
    override val sampleRate: Int,
    bitsPerSample: Int,
    channels: Int,
) : AudioBuffer {
    private val buffer: ByteArray
    override val sampleCount: Int = samples.size

    init {
        buffer =
            when (bitsPerSample) {
                8 -> convert8Bit(samples)
                16 -> convert16Bit(samples)
                else -> throw IllegalArgumentException("Unsupported bits per sample: $bitsPerSample")
            }
    }

    private fun convert16Bit(samples: DoubleArray): ByteArray {
        val result = ByteArray(samples.size * 2)
        var idx = 0
        for (sample in samples) {
            // Convert to 16-bit PCM
            val value = (sample * 32767).toInt().coerceIn(-32768, 32767).toShort()
            // Write as little-endian
            result[idx++] = (value.toInt() and 0xFF).toByte()
            result[idx++] = ((value.toInt() shr 8) and 0xFF).toByte()
        }
        return result
    }

    private fun convert8Bit(samples: DoubleArray): ByteArray {
        val result = ByteArray(samples.size)
        for (i in samples.indices) {
            // Convert to 8-bit unsigned PCM (0-255)
            result[i] = ((samples[i] * 0.5 + 0.5) * 255).toInt().coerceIn(0, 255).toByte()
        }
        return result
    }

    override fun getRawBuffer(): ByteArray = buffer
}

// ----------------------------------------------------------------------------

/**
 * Android implementation of SoundPlayer using AudioTrack
 */
class AndroidSoundPlayer(
    private val context: Context,
) : SoundPlayer,
    VolumeController {
    private val sampleRate = 44100 // Hz
    private val activeTracks = mutableListOf<AudioTrack>()

    // Audio manager for volume control
    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun getCurrentVolume(): Float {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        return currentVolume / maxVolume
    }

    override fun setVolume(level: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volumeIndex = (level * maxVolume).toInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volumeIndex,
            0, // No flags, silent operation
        )
    }

    override suspend fun playTone(
        frequency: Double,
        amplitude: Double,
        duration: Duration,
        waveform: SoundPlayer.Waveform,
    ) = withContext(Dispatchers.Main) {
        // Save current volume (Main dispatcher needed for AudioManager access)
        val originalVolume = getCurrentVolume()

        try {
            // Set to maximum volume
            setVolume(1.0f)

            // Wait a moment for volume change to take effect
            delay(50.milliseconds)

            // Generate waveform on Default dispatcher (CPU-bound mathematical operations)
            withContext(Dispatchers.Default) {
                // Generate and play
                val samples =
                    WaveformGenerator.generateWaveform(
                        sampleRate = sampleRate,
                        frequency = frequency,
                        amplitude = amplitude,
                        duration = duration,
                        waveform = waveform,
                    )

                val buffer =
                    AudioBufferFactory.createFromSamples(
                        samples = samples,
                        sampleRate = sampleRate,
                        bitsPerSample = 16,
                        channels = 1,
                    )

                playBuffer(buffer, duration)
            }

            // Wait for playback to complete
            delay(duration + 100.milliseconds)
        } finally {
            // Always restore original volume
            setVolume(originalVolume)
        }
    }

    private suspend fun playBuffer(
        buffer: AudioBuffer,
        duration: Duration,
    ) = withContext(Dispatchers.Default) {
        // Audio processing and playback, not I/O
        val bufferSizeInBytes = buffer.getRawBuffer().size

        val audioTrack =
            AudioTrack
                .Builder()
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                ).setAudioFormat(
                    AudioFormat
                        .Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(buffer.sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                ).setBufferSizeInBytes(bufferSizeInBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

        synchronized(activeTracks) {
            activeTracks.add(audioTrack)
        }

        try {
            audioTrack.write(buffer.getRawBuffer(), 0, bufferSizeInBytes)
            audioTrack.play()

            // Wait for completion
            delay(duration + 100.milliseconds)
        } finally {
            audioTrack.release()
            synchronized(activeTracks) {
                activeTracks.remove(audioTrack)
            }
        }
    }

    override fun release() {
        synchronized(activeTracks) {
            activeTracks.forEach { it.release() }
            activeTracks.clear()
        }
    }
}
