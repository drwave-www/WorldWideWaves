package com.worldwidewaves.shared.utils

/*
 * Copyright 2025 DrWave
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

/**
 * Helper class for reading byte arrays
 */
class ByteArrayReader(private val bytes: ByteArray) {
    var position: Int = 0

    fun readUInt8(): Int {
        return bytes[position++].toInt() and 0xFF
    }

    fun readInt16(): Int {
        val msb = readUInt8()
        val lsb = readUInt8()
        return (msb shl 8) or lsb
    }

    fun readInt32(): Int {
        val b1 = readUInt8()
        val b2 = readUInt8()
        val b3 = readUInt8()
        val b4 = readUInt8()
        return (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
    }

    fun readString(length: Int): String {
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
            result = (result shl 7) or (currentByte and 0x7F).toLong()
        } while ((currentByte and 0x80) != 0)

        return result
    }

    fun skip(count: Int) {
        position += count
    }
}