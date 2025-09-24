package com.worldwidewaves.shared.utils

import com.worldwidewaves.shared.WWWGlobals

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

/**
 * Helper class for reading byte arrays
 */
class ByteArrayReader(
    private val bytes: ByteArray,
) {
    var position: Int = 0

    private fun checkBounds(bytesToRead: Int) {
        if (position + bytesToRead > bytes.size) {
            val remaining = bytes.size - position
            throw IndexOutOfBoundsException(
                "Attempting to read $bytesToRead bytes at position $position, but only $remaining bytes remaining",
            )
        }
    }

    fun readUInt8(): Int {
        checkBounds(1)
        return bytes[position++].toInt() and WWWGlobals.ByteProcessing.BYTE_MASK
    }

    fun readInt16(): Int {
        val msb = readUInt8()
        val lsb = readUInt8()
        return (msb shl WWWGlobals.ByteProcessing.BIT_SHIFT_8) or lsb
    }

    fun readInt32(): Int {
        val b1 = readUInt8()
        val b2 = readUInt8()
        val b3 = readUInt8()
        val b4 = readUInt8()
        return (b1 shl WWWGlobals.ByteProcessing.BIT_SHIFT_24) or (b2 shl WWWGlobals.ByteProcessing.BIT_SHIFT_16) or
            (b3 shl WWWGlobals.ByteProcessing.BIT_SHIFT_8) or
            b4
    }

    fun readString(length: Int): String {
        checkBounds(length)
        val chars = CharArray(length)
        for (i in 0 until length) {
            chars[i] = bytes[position + i].toInt().toChar()
        }
        position += length
        return chars.concatToString()
    }

    fun readVariableLengthQuantity(): Long {
        var result: Long = 0
        var currentByte: Int

        do {
            currentByte = readUInt8()
            result =
                (result shl WWWGlobals.ByteProcessing.VLQ_BIT_SHIFT) or (currentByte and WWWGlobals.ByteProcessing.VLQ_DATA_MASK).toLong()
        } while ((currentByte and WWWGlobals.ByteProcessing.VLQ_CONTINUATION_MASK) != 0)

        return result
    }

    fun skip(count: Int) {
        // Allow skipping beyond bounds for flexibility, but reads will fail
        position += count
    }
}
