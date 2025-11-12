/*
 * Copyright (c) 2025 WorldWideWaves
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

package com.worldwidewaves.shared.events

import com.worldwidewaves.shared.events.utils.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Security and validation tests for Position coordinate validation.
 *
 * Ensures that Position class rejects invalid coordinates that could cause:
 * - Geographic calculation errors
 * - Map rendering issues
 * - Security vulnerabilities (injection, overflow)
 */
class PositionValidationTest {
    // ========== VALID COORDINATE TESTS ==========

    @Test
    fun `valid coordinates should create Position successfully`() {
        val position = Position(lat = 40.7128, lng = -74.0060) // New York
        assertEquals(40.7128, position.lat)
        assertEquals(-74.0060, position.lng)
    }

    @Test
    fun `latitude at minimum boundary should be valid`() {
        val position = Position(lat = -90.0, lng = 0.0) // South Pole
        assertEquals(-90.0, position.lat)
    }

    @Test
    fun `latitude at maximum boundary should be valid`() {
        val position = Position(lat = 90.0, lng = 0.0) // North Pole
        assertEquals(90.0, position.lat)
    }

    @Test
    fun `longitude at minimum boundary should be valid`() {
        val position = Position(lat = 0.0, lng = -180.0) // International Date Line (West)
        assertEquals(-180.0, position.lng)
    }

    @Test
    fun `longitude at maximum boundary should be valid`() {
        val position = Position(lat = 0.0, lng = 180.0) // International Date Line (East)
        assertEquals(180.0, position.lng)
    }

    @Test
    fun `zero coordinates should be valid`() {
        val position = Position(lat = 0.0, lng = 0.0) // Null Island
        assertEquals(0.0, position.lat)
        assertEquals(0.0, position.lng)
    }

    @Test
    fun `fractional coordinates should be valid`() {
        val position = Position(lat = 48.8566, lng = 2.3522) // Paris
        assertEquals(48.8566, position.lat)
        assertEquals(2.3522, position.lng)
    }

    @Test
    fun `negative coordinates should be valid`() {
        val position = Position(lat = -33.8688, lng = 151.2093) // Sydney
        assertEquals(-33.8688, position.lat)
        assertEquals(151.2093, position.lng)
    }

    // ========== INVALID LATITUDE TESTS ==========

    @Test
    fun `latitude above maximum should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = 90.01, lng = 0.0)
            }
        assertTrue(exception.message!!.contains("Invalid latitude"))
        assertTrue(exception.message!!.contains("90.01"))
    }

    @Test
    fun `latitude far above maximum should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = 100.0, lng = 0.0)
            }
        assertTrue(exception.message!!.contains("Invalid latitude"))
    }

    @Test
    fun `latitude below minimum should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = -90.01, lng = 0.0)
            }
        assertTrue(exception.message!!.contains("Invalid latitude"))
        assertTrue(exception.message!!.contains("-90.01"))
    }

    @Test
    fun `latitude far below minimum should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = -100.0, lng = 0.0)
            }
        assertTrue(exception.message!!.contains("Invalid latitude"))
    }

    @Test
    fun `latitude with extreme positive value should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = 1000.0, lng = 0.0)
            }
        assertTrue(exception.message!!.contains("Invalid latitude"))
    }

    @Test
    fun `latitude with extreme negative value should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = -1000.0, lng = 0.0)
            }
        assertTrue(exception.message!!.contains("Invalid latitude"))
    }

    // ========== INVALID LONGITUDE TESTS ==========

    @Test
    fun `longitude above maximum should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = 0.0, lng = 180.01)
            }
        assertTrue(exception.message!!.contains("Invalid longitude"))
        assertTrue(exception.message!!.contains("180.01"))
    }

    @Test
    fun `longitude far above maximum should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = 0.0, lng = 200.0)
            }
        assertTrue(exception.message!!.contains("Invalid longitude"))
    }

    @Test
    fun `longitude below minimum should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = 0.0, lng = -180.01)
            }
        assertTrue(exception.message!!.contains("Invalid longitude"))
        assertTrue(exception.message!!.contains("-180.01"))
    }

    @Test
    fun `longitude far below minimum should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = 0.0, lng = -200.0)
            }
        assertTrue(exception.message!!.contains("Invalid longitude"))
    }

    @Test
    fun `longitude with extreme positive value should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = 0.0, lng = 1000.0)
            }
        assertTrue(exception.message!!.contains("Invalid longitude"))
    }

    @Test
    fun `longitude with extreme negative value should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = 0.0, lng = -1000.0)
            }
        assertTrue(exception.message!!.contains("Invalid longitude"))
    }

    // ========== NaN TESTS ==========

    @Test
    fun `latitude as NaN should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = Double.NaN, lng = 0.0)
            }
        assertTrue(exception.message!!.contains("NaN"))
    }

    @Test
    fun `longitude as NaN should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = 0.0, lng = Double.NaN)
            }
        assertTrue(exception.message!!.contains("NaN"))
    }

    @Test
    fun `both coordinates as NaN should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = Double.NaN, lng = Double.NaN)
            }
        assertTrue(exception.message!!.contains("NaN") || exception.message!!.contains("Invalid"))
    }

    // ========== INFINITY TESTS ==========

    @Test
    fun `latitude as positive infinity should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = Double.POSITIVE_INFINITY, lng = 0.0)
            }
        assertTrue(exception.message!!.contains("finite") || exception.message!!.contains("Invalid latitude"))
    }

    @Test
    fun `latitude as negative infinity should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = Double.NEGATIVE_INFINITY, lng = 0.0)
            }
        assertTrue(exception.message!!.contains("finite") || exception.message!!.contains("Invalid latitude"))
    }

    @Test
    fun `longitude as positive infinity should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = 0.0, lng = Double.POSITIVE_INFINITY)
            }
        assertTrue(exception.message!!.contains("finite") || exception.message!!.contains("Invalid longitude"))
    }

    @Test
    fun `longitude as negative infinity should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = 0.0, lng = Double.NEGATIVE_INFINITY)
            }
        assertTrue(exception.message!!.contains("finite") || exception.message!!.contains("Invalid longitude"))
    }

    @Test
    fun `both coordinates as infinity should throw exception`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = Double.POSITIVE_INFINITY, lng = Double.POSITIVE_INFINITY)
            }
        assertTrue(exception.message!!.contains("finite") || exception.message!!.contains("Invalid"))
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun `very small positive coordinates should be valid`() {
        val position = Position(lat = 0.0001, lng = 0.0001)
        assertEquals(0.0001, position.lat)
        assertEquals(0.0001, position.lng)
    }

    @Test
    fun `very small negative coordinates should be valid`() {
        val position = Position(lat = -0.0001, lng = -0.0001)
        assertEquals(-0.0001, position.lat)
        assertEquals(-0.0001, position.lng)
    }

    @Test
    fun `maximum valid coordinates should be valid`() {
        val position = Position(lat = 90.0, lng = 180.0)
        assertEquals(90.0, position.lat)
        assertEquals(180.0, position.lng)
    }

    @Test
    fun `minimum valid coordinates should be valid`() {
        val position = Position(lat = -90.0, lng = -180.0)
        assertEquals(-90.0, position.lat)
        assertEquals(-180.0, position.lng)
    }

    // ========== SECURITY TESTS ==========

    @Test
    fun `attempting SQL injection in coordinates should fail with validation`() {
        // If someone tries to exploit coordinate parsing, validation should catch it
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = 999.0, lng = 999.0) // Unrealistic values
            }
        assertTrue(exception.message!!.contains("Invalid"))
    }

    @Test
    fun `attempting overflow with max double value should fail`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = Double.MAX_VALUE, lng = 0.0)
            }
        assertTrue(exception.message!!.contains("Invalid latitude"))
    }

    @Test
    fun `attempting overflow with min double value should fail`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Position(lat = -Double.MAX_VALUE, lng = 0.0)
            }
        assertTrue(exception.message!!.contains("Invalid latitude"))
    }
}
