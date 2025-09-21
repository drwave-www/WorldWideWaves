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

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for AudioBuffer interface and factory functionality
 */
class AudioBufferTest {

    @Test
    fun `test AudioBuffer interface contract`() {
        // GIVEN: Mock AudioBuffer implementation
        val mockBuffer = mockk<AudioBuffer>()
        val testData = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

        every { mockBuffer.getRawBuffer() } returns testData
        every { mockBuffer.sampleCount } returns 1024
        every { mockBuffer.sampleRate } returns 44100

        // WHEN: Access buffer properties
        val rawBuffer = mockBuffer.getRawBuffer()
        val sampleCount = mockBuffer.sampleCount
        val sampleRate = mockBuffer.sampleRate

        // THEN: Should return expected values
        assertEquals(testData, rawBuffer)
        assertEquals(1024, sampleCount)
        assertEquals(44100, sampleRate)
    }

    @Test
    fun `test AudioBufferFactory creates buffer from samples`() {
        // GIVEN: Sample data
        val samples = doubleArrayOf(0.0, 0.5, 1.0, -0.5, -1.0)
        val sampleRate = 44100
        val bitsPerSample = 16
        val channels = 1

        // WHEN: Create buffer from samples
        val buffer = AudioBufferFactory.createFromSamples(samples, sampleRate, bitsPerSample, channels)

        // THEN: Buffer should be created with correct properties
        assertNotNull(buffer)
        assertEquals(sampleRate, buffer.sampleRate)
        assertEquals(samples.size, buffer.sampleCount)

        // Raw buffer should contain data
        val rawBuffer = buffer.getRawBuffer()
        assertTrue(rawBuffer.isNotEmpty())
    }

    @Test
    fun `test AudioBufferFactory with different sample rates`() {
        val testSampleRates = listOf(8000, 22050, 44100, 48000, 96000)
        val samples = doubleArrayOf(0.0, 0.5, -0.5)

        for (sampleRate in testSampleRates) {
            // WHEN: Create buffer with specific sample rate
            val buffer = AudioBufferFactory.createFromSamples(samples, sampleRate)

            // THEN: Should preserve sample rate
            assertEquals(sampleRate, buffer.sampleRate)
            assertEquals(samples.size, buffer.sampleCount)
        }
    }

    @Test
    fun `test AudioBufferFactory with different bit depths`() {
        val samples = doubleArrayOf(0.0, 0.25, 0.5, 0.75, 1.0, -0.25, -0.5, -0.75, -1.0)
        val testBitDepths = listOf(8, 16, 24, 32)

        for (bitDepth in testBitDepths) {
            // WHEN: Create buffer with specific bit depth
            val buffer = AudioBufferFactory.createFromSamples(
                samples = samples,
                sampleRate = 44100,
                bitsPerSample = bitDepth
            )

            // THEN: Should create valid buffer
            assertNotNull(buffer)
            assertEquals(samples.size, buffer.sampleCount)
            assertTrue(buffer.getRawBuffer().isNotEmpty())
        }
    }

    @Test
    fun `test AudioBufferFactory with stereo channels`() {
        val samples = doubleArrayOf(0.0, 0.5, 1.0, -0.5)

        // WHEN: Create stereo buffer
        val stereoBuffer = AudioBufferFactory.createFromSamples(
            samples = samples,
            sampleRate = 44100,
            channels = 2
        )

        // THEN: Should create valid stereo buffer
        assertNotNull(stereoBuffer)
        assertEquals(samples.size, stereoBuffer.sampleCount)

        // Raw buffer should be larger for stereo (2 channels)
        val rawBuffer = stereoBuffer.getRawBuffer()
        assertTrue(rawBuffer.isNotEmpty())
    }

    @Test
    fun `test AudioBufferFactory with empty samples`() {
        val emptySamples = doubleArrayOf()

        // WHEN: Create buffer with empty samples
        val buffer = AudioBufferFactory.createFromSamples(emptySamples, 44100)

        // THEN: Should handle empty samples gracefully
        assertNotNull(buffer)
        assertEquals(0, buffer.sampleCount)
    }

    @Test
    fun `test AudioBufferFactory with extreme amplitude values`() {
        val extremeSamples = doubleArrayOf(-2.0, -1.0, 0.0, 1.0, 2.0) // Beyond [-1, 1] range

        // WHEN: Create buffer with extreme values
        val buffer = AudioBufferFactory.createFromSamples(extremeSamples, 44100)

        // THEN: Should handle extreme values (implementation may clamp)
        assertNotNull(buffer)
        assertEquals(extremeSamples.size, buffer.sampleCount)
        assertTrue(buffer.getRawBuffer().isNotEmpty())
    }

    @Test
    fun `test AudioBuffer raw buffer consistency`() {
        val samples = doubleArrayOf(0.1, 0.2, 0.3, 0.4, 0.5)
        val buffer = AudioBufferFactory.createFromSamples(samples, 44100)

        // WHEN: Get raw buffer multiple times
        val rawBuffer1 = buffer.getRawBuffer()
        val rawBuffer2 = buffer.getRawBuffer()

        // THEN: Should return consistent data
        assertEquals(rawBuffer1.size, rawBuffer2.size)
        assertTrue(rawBuffer1.contentEquals(rawBuffer2))
    }

    @Test
    fun `test AudioBuffer properties remain constant`() {
        val samples = doubleArrayOf(0.0, 1.0, -1.0)
        val sampleRate = 48000
        val buffer = AudioBufferFactory.createFromSamples(samples, sampleRate)

        // WHEN: Access properties multiple times
        val sampleCount1 = buffer.sampleCount
        val sampleCount2 = buffer.sampleCount
        val sampleRate1 = buffer.sampleRate
        val sampleRate2 = buffer.sampleRate

        // THEN: Properties should remain constant
        assertEquals(sampleCount1, sampleCount2)
        assertEquals(sampleRate1, sampleRate2)
        assertEquals(samples.size, sampleCount1)
        assertEquals(sampleRate, sampleRate1)
    }
}