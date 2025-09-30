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

package com.worldwidewaves.shared.sound

import com.worldwidewaves.shared.utils.Log
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioMixerNode
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.outputVolume
import platform.AVFAudio.setActive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * iOS implementation of SoundPlayer using AVAudioEngine
 *
 * This is a working implementation that provides functional audio capabilities
 * while using iOS-safe APIs that compile correctly with Kotlin/Native.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class IOSSoundPlayer :
    SoundPlayer,
    VolumeController {
    companion object {
        private const val TAG = "IOSSoundPlayer"
    }

    private val audioSession = AVAudioSession.sharedInstance()
    private val audioEngine = AVAudioEngine()
    private val playerNode = AVAudioPlayerNode()
    private val mixerNode = AVAudioMixerNode()
    private val playbackMutex = Mutex()
    private var isEngineStarted = false
    private var isEngineSetupAttempted = false
    private var originalMixerVolume: Float = 1.0f

    init {
        setupAudioSession()
        // Defer engine setup to first playback attempt
        // This prevents crashes on simulators without audio I/O
    }

    private fun setupAudioSession() {
        try {
            audioSession.setCategory(
                AVAudioSessionCategoryPlayback,
                AVAudioSessionCategoryOptionMixWithOthers,
                null,
            )
            audioSession.setActive(true, null)
            Log.v(TAG, "Audio session setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup audio session", e)
        }
    }

    private fun setupAudioEngine() {
        if (isEngineSetupAttempted) return
        isEngineSetupAttempted = true

        try {
            val outputNode = audioEngine.outputNode
            val format = outputNode.outputFormatForBus(0u)

            // Attach nodes: playerNode -> mixerNode -> outputNode
            audioEngine.attachNode(playerNode)
            audioEngine.attachNode(mixerNode)
            audioEngine.connect(playerNode, mixerNode, format)
            audioEngine.connect(mixerNode, outputNode, format)

            audioEngine.prepare()
            audioEngine.startAndReturnError(null)
            isEngineStarted = true
            Log.v(TAG, "Audio engine setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup audio engine (simulator or hardware issue)", e)
            isEngineStarted = false
        }
    }

    override fun getCurrentVolume(): Float {
        // Return system output volume (read-only on iOS)
        return audioSession.outputVolume()
    }

    override fun setVolume(level: Float) {
        // iOS does not allow programmatic system volume changes
        // We control mixer node volume instead
        if (isEngineStarted) {
            mixerNode.setVolume(level)
            Log.v(TAG, "Set mixer volume to $level")
        }
    }

    override suspend fun playTone(
        frequency: Double,
        amplitude: Double,
        duration: Duration,
        waveform: SoundPlayer.Waveform,
    ) {
        playbackMutex.withLock {
            // Lazy engine initialization on first playback
            if (!isEngineSetupAttempted) {
                setupAudioEngine()
            }

            if (!isEngineStarted) {
                Log.w(TAG, "Audio engine not available (simulator mode), skipping playback")
                delay(duration) // Maintain timing even without audio
                return@withLock
            }

            try {
                // Save current mixer volume
                originalMixerVolume = mixerNode.volume

                // Set mixer to maximum volume
                mixerNode.setVolume(1.0f)
                Log.v(TAG, "Set volume to max (1.0) from $originalMixerVolume")

                // Wait for volume change to take effect
                delay(50.milliseconds)

                Log.v(TAG, "Playing tone: freq=$frequency, amp=$amplitude, dur=$duration, wave=$waveform")

                // Generate waveform samples using shared generator
                val sampleRate = 44100
                val samples =
                    WaveformGenerator.generateWaveform(
                        sampleRate = sampleRate,
                        frequency = frequency,
                        amplitude = amplitude,
                        duration = duration,
                        waveform = waveform,
                    )

                if (samples.isNotEmpty()) {
                    // Create PCM buffer and schedule for playback
                    val format =
                        AVAudioFormat(
                            standardFormatWithSampleRate = sampleRate.toDouble(),
                            channels = 1u,
                        )

                    val frameCapacity = samples.size.toUInt()
                    val buffer =
                        AVAudioPCMBuffer(
                            pCMFormat = format,
                            frameCapacity = frameCapacity,
                        )

                    if (buffer != null) {
                        buffer.frameLength = frameCapacity

                        // Copy samples to buffer
                        val floatChannelData = buffer.floatChannelData
                        if (floatChannelData != null) {
                            val channel0 = floatChannelData[0]
                            if (channel0 != null) {
                                samples.forEachIndexed { index, sample ->
                                    channel0[index] = sample.toFloat()
                                }

                                // Schedule and play buffer
                                playerNode.scheduleBuffer(buffer, null)
                                playerNode.play()

                                // Wait for playback to complete
                                delay(duration + 50.milliseconds)
                                Log.v(TAG, "iOS audio playback completed (${samples.size} samples)")
                            } else {
                                Log.w(TAG, "Failed to get channel 0 pointer")
                            }
                        } else {
                            Log.w(TAG, "Failed to get channel data from buffer")
                        }
                    } else {
                        Log.w(TAG, "Failed to create PCM buffer")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing tone: freq=$frequency, dur=$duration", e)
            } finally {
                // Always restore original mixer volume
                mixerNode.setVolume(originalMixerVolume)
                Log.v(TAG, "Restored volume to $originalMixerVolume")
            }
        }
    }

    override fun release() {
        try {
            if (playerNode.isPlaying()) {
                playerNode.stop()
            }

            if (isEngineStarted) {
                audioEngine.stop()
                isEngineStarted = false
            }

            audioSession.setActive(false, null)
            Log.v(TAG, "iOS sound player released")
        } catch (e: Exception) {
            Log.e(TAG, "Error during sound player release", e)
        }
    }
}
