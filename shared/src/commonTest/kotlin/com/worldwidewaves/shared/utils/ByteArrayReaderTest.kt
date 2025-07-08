package com.worldwidewaves.shared.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ByteArrayReaderTest {

    @Test
    fun `test readUInt8 reads unsigned 8-bit values correctly`() {
        // Test with various values including boundary cases
        val bytes = byteArrayOf(0, 1, 127, -1, -128)
        val reader = ByteArrayReader(bytes)

        assertEquals(0, reader.readUInt8())
        assertEquals(1, reader.readUInt8())
        assertEquals(127, reader.readUInt8())
        assertEquals(255, reader.readUInt8()) // -1 as unsigned is 255
        assertEquals(128, reader.readUInt8()) // -128 as unsigned is 128
    }

    @Test
    fun `test readInt16 reads signed 16-bit values correctly`() {
        // Test with various values including boundary cases
        // Values: 0, 1, 256, 32767 (max positive), -1, -32768 (min negative)
        val bytes = byteArrayOf(
            0, 0,       // 0
            0, 1,       // 1
            1, 0,       // 256
            127, -1,    // 32767
            -1, -1,     // -1
            -128, 0     // -32768
        )
        val reader = ByteArrayReader(bytes)

        assertEquals(0, reader.readInt16())
        assertEquals(1, reader.readInt16())
        assertEquals(256, reader.readInt16())
        assertEquals(32767, reader.readInt16())
        assertEquals(-1, reader.readInt16())
        assertEquals(-32768, reader.readInt16())
    }

    @Test
    fun `test readInt32 reads signed 32-bit values correctly`() {
        // Test with various values including boundary cases
        // Values: 0, 1, 16777216, 2147483647 (max positive), -1, -2147483648 (min negative)
        val bytes = byteArrayOf(
            0, 0, 0, 0,             // 0
            0, 0, 0, 1,             // 1
            1, 0, 0, 0,             // 16777216
            127, -1, -1, -1,        // 2147483647
            -1, -1, -1, -1,         // -1
            -128, 0, 0, 0           // -2147483648
        )
        val reader = ByteArrayReader(bytes)

        assertEquals(0, reader.readInt32())
        assertEquals(1, reader.readInt32())
        assertEquals(16777216, reader.readInt32())
        assertEquals(2147483647, reader.readInt32())
        assertEquals(-1, reader.readInt32())
        assertEquals(-2147483648, reader.readInt32())
    }

    @Test
    fun `test readString reads strings of different lengths correctly`() {
        // Test with empty string, short string, and longer string
        val bytes = "HelloWorld!".encodeToByteArray()
        
        // Test empty string
        val emptyReader = ByteArrayReader(bytes)
        assertEquals("", emptyReader.readString(0))
        
        // Test partial string
        val partialReader = ByteArrayReader(bytes)
        assertEquals("Hello", partialReader.readString(5))
        assertEquals("World", partialReader.readString(5))
        assertEquals("!", partialReader.readString(1))
        
        // Test full string
        val fullReader = ByteArrayReader(bytes)
        assertEquals("HelloWorld!", fullReader.readString(11))
    }

    @Test
    fun `test readString handles non-ASCII characters correctly`() {
        // Test with Unicode characters
        val text = "こんにちは世界" // "Hello World" in Japanese
        val bytes = text.encodeToByteArray()
        
        val reader = ByteArrayReader(bytes)
        val result = reader.readString(bytes.size)
        
        assertEquals(text, result)
    }

    @Test
    fun `test readVariableLengthQuantity reads VLQ values correctly`() {
        // Test cases based on MIDI VLQ format:
        // Single byte (0-127)
        // Two bytes (128-16383)
        // Three bytes (16384-2097151)
        // Four bytes (2097152-268435455)
        
        // 0x00 = 0
        // 0x7F = 127
        // 0x81 0x00 = 128
        // 0xFF 0x7F = 16383
        // 0x81 0x80 0x00 = 16384
        // 0xFF 0xFF 0x7F = 2097151
        // 0x81 0x80 0x80 0x00 = 2097152
        
        val bytes = byteArrayOf(
            0x00,                         // 0
            0x7F,                         // 127
            0x81.toByte(), 0x00,          // 128
            0xFF.toByte(), 0x7F,          // 16383
            0x81.toByte(), 0x80.toByte(), 0x00,  // 16384
            0xFF.toByte(), 0xFF.toByte(), 0x7F,  // 2097151
            0x81.toByte(), 0x80.toByte(), 0x80.toByte(), 0x00  // 2097152
        )
        
        val reader = ByteArrayReader(bytes)
        
        assertEquals(0, reader.readVariableLengthQuantity())
        assertEquals(127, reader.readVariableLengthQuantity())
        assertEquals(128, reader.readVariableLengthQuantity())
        assertEquals(16383, reader.readVariableLengthQuantity())
        assertEquals(16384, reader.readVariableLengthQuantity())
        assertEquals(2097151, reader.readVariableLengthQuantity())
        assertEquals(2097152, reader.readVariableLengthQuantity())
    }

    @Test
    fun `test skip advances position correctly`() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val reader = ByteArrayReader(bytes)
        
        // Read first byte
        assertEquals(1, reader.readUInt8())
        
        // Skip 3 bytes
        reader.skip(3)
        
        // Read next byte (should be 5)
        assertEquals(5, reader.readUInt8())
        
        // Skip to end minus one
        reader.skip(3)
        
        // Read last byte
        assertEquals(9, reader.readUInt8())
    }

    @Test
    fun `test position is tracked correctly across operations`() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val reader = ByteArrayReader(bytes)
        
        // Initial position
        assertEquals(0, reader.position)
        
        // Read UInt8
        reader.readUInt8()
        assertEquals(1, reader.position)
        
        // Read Int16
        reader.readInt16()
        assertEquals(3, reader.position)
        
        // Skip
        reader.skip(2)
        assertEquals(5, reader.position)
        
        // Read string
        reader.readString(3)
        assertEquals(8, reader.position)
    }

    @Test
    fun `test reading beyond array bounds throws exception`() {
        val bytes = byteArrayOf(1, 2, 3)
        val reader = ByteArrayReader(bytes)
        
        // Read all bytes
        reader.readUInt8()
        reader.readUInt8()
        reader.readUInt8()
        
        // Next read should throw exception
        assertFailsWith<IndexOutOfBoundsException> {
            reader.readUInt8()
        }
    }

    @Test
    fun `test reading from empty array`() {
        val reader = ByteArrayReader(byteArrayOf())
        
        assertFailsWith<IndexOutOfBoundsException> {
            reader.readUInt8()
        }
    }

    @Test
    fun `test reading string beyond array bounds throws exception`() {
        val bytes = "Hello".encodeToByteArray()
        val reader = ByteArrayReader(bytes)
        
        assertFailsWith<IndexOutOfBoundsException> {
            reader.readString(10) // Trying to read more characters than available
        }
    }

    @Test
    fun `test skip beyond array bounds`() {
        val bytes = byteArrayOf(1, 2, 3)
        val reader = ByteArrayReader(bytes)
        
        // Skip beyond array length
        reader.skip(10)
        
        // Position should be at the end of the array
        assertEquals(10, reader.position)
        
        // Trying to read should throw exception
        assertFailsWith<IndexOutOfBoundsException> {
            reader.readUInt8()
        }
    }

    @Test
    fun `test endianness in multi-byte reads`() {
        // Test that multi-byte values are read in big-endian format
        // 0x0102 = 258 in big-endian, 513 in little-endian
        // 0x01020304 = 16909060 in big-endian, 67305985 in little-endian
        
        val bytes = byteArrayOf(
            0x01, 0x02,                 // Int16 value
            0x01, 0x02, 0x03, 0x04      // Int32 value
        )
        
        val reader = ByteArrayReader(bytes)
        
        // Check Int16 endianness
        assertEquals(258, reader.readInt16(), "Int16 should be read as big-endian")
        
        // Check Int32 endianness
        assertEquals(16909060, reader.readInt32(), "Int32 should be read as big-endian")
    }
}
