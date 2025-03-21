package com.worldwidewaves.shared.choreographies

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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

import com.worldwidewaves.shared.sound.AudioBuffer
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.sound.VolumeController
import platform.AVFAudio.AVAudioEngine
import kotlin.time.Duration
import platform.AVFoundation.*
import platform.Foundation.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.outputVolume
import platform.AVFAudio.setActive

/**
 * iOS implementation of AudioBuffer
 */
actual object AudioBufferFactory {
    actual fun createFromSamples(
        samples: DoubleArray,
        sampleRate: Int,
        bitsPerSample: Int,
        channels: Int
    ): AudioBuffer {
        return IOSAudioBuffer(samples, sampleRate, bitsPerSample, channels)
    }
}

/**
 * iOS-specific audio buffer implementation
 */
class IOSAudioBuffer(
    samples: DoubleArray,
    override val sampleRate: Int,
    bitsPerSample: Int,
    channels: Int
) : AudioBuffer {

    private val buffer: ByteArray = when (bitsPerSample) {
        8 ->  convert8Bit(samples)
        16 -> convert16Bit(samples)
        else -> throw IllegalArgumentException("Unsupported bits per sample: $bitsPerSample")
    }
    override val sampleCount: Int = samples.size

    // Conversion methods same as Android implementation

    override fun getRawBuffer(): ByteArray = buffer
}

/**
 * iOS implementation of SoundPlayer using AVAudioEngine
 */
class IOSSoundPlayer : SoundPlayer, VolumeController {
    private val audioSession = AVAudioSession.sharedInstance()
    private val audioEngine = AVAudioEngine()

    init {
        // Initialize audio session
        audioSession.setCategory(
            AVAudioSessionCategoryPlayback,
            withOptions = setOf(AVAudioSessionCategoryOptionMixWithOthers),
            error = null
        )
        audioSession.setActive(true, null)

        // Initialize audio engine
        audioEngine.prepare()
        audioEngine.startAndReturnError(null)
    }

    override fun getCurrentVolume(): Float {
        return audioSession.outputVolume()
    }

    override fun setVolume(level: Float) {
        // Note: iOS doesn't allow direct volume control from apps
        // This requires special entitlements or using MPVolumeView
        println("Volume control on iOS requires user interaction")

        // For volume control, you should present a volume view to the user:
        // MPVolumeView solution would go here in Swift/Objective-C interop
    }

    override suspend fun playTone(
        frequency: Double,
        amplitude: Double,
        duration: Duration,
        waveform: SoundPlayer.Waveform
    ) {
        // Save current volume (though we can't directly change it on iOS)
        val originalVolume = getCurrentVolume()

        try {
            // iOS requires user interaction for volume changes, so we'll just
            // play at maximum amplitude instead

            // Play tone at full amplitude
            playTone(frequency, 1.0, duration, waveform)

            // Wait for completion
            delay(duration + 100.milliseconds)
        } finally {
            // Volume restoration not needed on iOS as we can't change it directly
        }
    }

    override fun release() {
        audioEngine.stop()
        audioSession.setActive(false, null)
    }
}