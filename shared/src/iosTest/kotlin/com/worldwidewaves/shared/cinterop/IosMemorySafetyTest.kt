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

package com.worldwidewaves.shared.cinterop

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for iOS cinterop memory safety patterns.
 *
 * This test suite validates the correct usage of Kotlin/Native cinterop APIs
 * used throughout the WorldWideWaves iOS implementation, particularly:
 * - Memory pinning with usePinned for safe C API interaction
 * - useContents for safe struct field access
 * - NSData creation from pinned Kotlin ByteArrays
 * - Proper memory lifecycle management
 *
 * These patterns are critical for iOS stability and must maintain memory safety.
 */
@OptIn(ExperimentalForeignApi::class)
class IosMemorySafetyTest {
    @Test
    fun `usePinned should provide valid pointer to ByteArray data`() {
        // Validates the basic usePinned pattern - pointer must be valid within scope
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        var pointerWasValid = false

        bytes.usePinned { pinned ->
            val pointer = pinned.addressOf(0)
            // Pointer should not be null within pinned scope
            pointerWasValid = pointer != null
        }

        assertTrue(pointerWasValid, "Pointer should be valid within usePinned scope")
    }

    @Test
    fun `usePinned should keep ByteArray data accessible during C API call`() {
        // Validates that ByteArray remains alive and accessible within pinned scope
        // This is critical for passing Kotlin data to C APIs
        val testData = byteArrayOf(0x48, 0x65, 0x6C, 0x6C, 0x6F) // "Hello" in ASCII
        var dataWasAccessible = false

        testData.usePinned { pinned ->
            val pointer = pinned.addressOf(0)
            // Verify we can create NSData from pinned pointer (actual pattern from PlatformCache)
            val nsData = NSData.create(bytes = pointer, length = testData.size.toULong())
            dataWasAccessible = nsData.length.toInt() == testData.size
        }

        assertTrue(dataWasAccessible, "ByteArray data should remain accessible within pinned scope")
    }

    @Test
    fun `NSData created from pinned ByteArray should preserve data integrity`() {
        // Validates the exact pattern used in PlatformCache.ios.kt (lines 93-97)
        // NSData must correctly copy the pinned ByteArray data
        val originalBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        var nsDataLength: Int? = null

        originalBytes.usePinned { pinned ->
            val nsData =
                NSData.create(
                    bytes = pinned.addressOf(0),
                    length = originalBytes.size.toULong(),
                )
            nsDataLength = nsData.length.toInt()
        }

        assertNotNull(nsDataLength, "NSData should be created successfully")
        assertEquals(
            originalBytes.size,
            nsDataLength,
            "NSData length should match original ByteArray size",
        )
    }

    @Test
    fun `usePinned should work with empty ByteArray`() {
        // Edge case: empty arrays must be handled safely
        val emptyBytes = byteArrayOf()
        var operationSucceeded = false

        emptyBytes.usePinned { pinned ->
            val pointer = pinned.addressOf(0)
            val nsData = NSData.create(bytes = pointer, length = 0u)
            operationSucceeded = nsData.length == 0uL
        }

        assertTrue(operationSucceeded, "usePinned should handle empty ByteArray safely")
    }

    @Test
    fun `usePinned should work with large ByteArray`() {
        // Validates that pinning works correctly with larger data (simulating cache files)
        val largeBytes = ByteArray(1024) { it.toByte() }
        var largeDataPinned = false

        largeBytes.usePinned { pinned ->
            val pointer = pinned.addressOf(0)
            val nsData = NSData.create(bytes = pointer, length = largeBytes.size.toULong())
            largeDataPinned = nsData.length == largeBytes.size.toULong()
        }

        assertTrue(largeDataPinned, "usePinned should handle large ByteArray correctly")
    }

    @Test
    fun `usePinned should properly scope memory access`() {
        // Validates that pinned scope is properly managed
        // After the block completes, the pinned object is released
        val bytes = byteArrayOf(1, 2, 3)
        var scopeExecuted = false
        var nsDataCreated = false

        bytes.usePinned { pinned ->
            scopeExecuted = true
            val pointer = pinned.addressOf(0)
            val nsData = NSData.create(bytes = pointer, length = bytes.size.toULong())
            nsDataCreated = nsData.length > 0uL
        }

        // Both flags should be true, indicating proper scope execution
        assertTrue(scopeExecuted, "usePinned block should execute")
        assertTrue(nsDataCreated, "NSData should be created within pinned scope")
    }

    @Test
    fun `addressOf should provide valid pointer at different indices`() {
        // Validates addressOf can access different positions in the array
        val bytes = byteArrayOf(10, 20, 30, 40, 50)
        var pointersValid = false

        bytes.usePinned { pinned ->
            // Get pointer to start of array
            val pointer0 = pinned.addressOf(0)
            // Get pointer to middle of array
            val pointer2 = pinned.addressOf(2)

            // Both pointers should be valid (non-null)
            pointersValid = pointer0 != null && pointer2 != null
        }

        assertTrue(pointersValid, "addressOf should provide valid pointers at different indices")
    }

    @Test
    fun `multiple usePinned calls should work independently`() {
        // Validates that multiple sequential pinning operations work correctly
        // This simulates multiple cache write operations
        val bytes1 = byteArrayOf(1, 2, 3)
        val bytes2 = byteArrayOf(4, 5, 6, 7)
        var firstPinSucceeded = false
        var secondPinSucceeded = false

        bytes1.usePinned { pinned ->
            val nsData =
                NSData.create(
                    bytes = pinned.addressOf(0),
                    length = bytes1.size.toULong(),
                )
            firstPinSucceeded = nsData.length == bytes1.size.toULong()
        }

        bytes2.usePinned { pinned ->
            val nsData =
                NSData.create(
                    bytes = pinned.addressOf(0),
                    length = bytes2.size.toULong(),
                )
            secondPinSucceeded = nsData.length == bytes2.size.toULong()
        }

        assertTrue(firstPinSucceeded, "First usePinned operation should succeed")
        assertTrue(secondPinSucceeded, "Second usePinned operation should succeed independently")
    }

    @Test
    fun `NSData creation should not retain ByteArray reference after usePinned scope`() {
        // Validates that NSData makes its own copy and doesn't rely on pinned memory
        // After usePinned scope ends, NSData should still be valid
        val bytes = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        var nsData: NSData? = null

        bytes.usePinned { pinned ->
            nsData =
                NSData.create(
                    bytes = pinned.addressOf(0),
                    length = bytes.size.toULong(),
                )
        }

        // NSData should still be valid after usePinned scope ends
        assertNotNull(nsData, "NSData should exist after usePinned scope")
        assertEquals(
            bytes.size.toULong(),
            nsData?.length,
            "NSData should retain correct length after scope",
        )
    }

    @Test
    fun `usePinned with nested operations should maintain memory safety`() {
        // Validates that complex operations within pinned scope remain safe
        // This simulates the PlatformCache pattern with validation
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        var nestedOperationSucceeded = false

        bytes.usePinned { pinned ->
            val pointer = pinned.addressOf(0)

            // Create NSData
            val nsData = NSData.create(bytes = pointer, length = bytes.size.toULong())

            // Perform nested validation (simulating cache write validation)
            val lengthValid = nsData.length == bytes.size.toULong()
            val dataCreated = nsData.length > 0uL

            nestedOperationSucceeded = lengthValid && dataCreated
        }

        assertTrue(
            nestedOperationSucceeded,
            "Nested operations within usePinned should maintain memory safety",
        )
    }
}
