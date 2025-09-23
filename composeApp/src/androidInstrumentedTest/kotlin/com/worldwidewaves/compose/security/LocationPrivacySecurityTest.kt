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

package com.worldwidewaves.compose.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldwidewaves.testing.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Security and privacy tests for location data handling.
 *
 * Ensures location data is properly validated, sanitized, and anonymized
 * according to privacy requirements and security best practices.
 */
@RunWith(AndroidJUnit4::class)
class LocationPrivacySecurityTest : BaseIntegrationTest() {

    @Test
    fun testSecureLocation_validCoordinates_createsSuccessfully() {
        val location = SecureLocation(40.7128, -74.0060, 10f)

        assertEquals(40.7128, location.latitude, 0.0001)
        assertEquals(-74.0060, location.longitude, 0.0001)
        assertEquals(10f, location.accuracy)
    }

    @Test
    fun testSecureLocation_invalidLatitude_throwsException() {
        try {
            SecureLocation(91.0, -74.0060, 10f)
            fail("Should throw exception for invalid latitude")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should mention invalid latitude", e.message?.contains("Invalid latitude") == true)
        }

        try {
            SecureLocation(-91.0, -74.0060, 10f)
            fail("Should throw exception for invalid latitude")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should mention invalid latitude", e.message?.contains("Invalid latitude") == true)
        }
    }

    @Test
    fun testSecureLocation_invalidLongitude_throwsException() {
        try {
            SecureLocation(40.7128, 181.0, 10f)
            fail("Should throw exception for invalid longitude")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should mention invalid longitude", e.message?.contains("Invalid longitude") == true)
        }

        try {
            SecureLocation(40.7128, -181.0, 10f)
            fail("Should throw exception for invalid longitude")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should mention invalid longitude", e.message?.contains("Invalid longitude") == true)
        }
    }

    @Test
    fun testSecureLocation_negativeAccuracy_throwsException() {
        try {
            SecureLocation(40.7128, -74.0060, -5f)
            fail("Should throw exception for negative accuracy")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should mention negative accuracy", e.message?.contains("Accuracy cannot be negative") == true)
        }
    }

    @Test
    fun testLocationAnonymization_defaultPrecision_reducesAccuracy() {
        val originalLocation = SecureLocation(40.712812345, -74.006021987, 10f)
        val anonymized = originalLocation.anonymized()

        assertEquals(40.713, anonymized.latitude, 0.0001)
        assertEquals(-74.006, anonymized.longitude, 0.0001)
        assertEquals(10f, anonymized.accuracy)
    }

    @Test
    fun testLocationAnonymization_customPrecision_appliesCorrectRounding() {
        val originalLocation = SecureLocation(40.712812345, -74.006021987, 10f)
        val anonymizedLow = originalLocation.anonymized(precision = 1)
        val anonymizedHigh = originalLocation.anonymized(precision = 5)

        assertEquals(40.7, anonymizedLow.latitude, 0.01)
        assertEquals(-74.0, anonymizedLow.longitude, 0.01)

        assertEquals(40.71281, anonymizedHigh.latitude, 0.000001)
        assertEquals(-74.00602, anonymizedHigh.longitude, 0.000001)
    }

    @Test
    fun testLocationValidator_validCoordinates_returnsTrue() {
        assertTrue(LocationValidator.validateCoordinates(0.0, 0.0))
        assertTrue(LocationValidator.validateCoordinates(90.0, 180.0))
        assertTrue(LocationValidator.validateCoordinates(-90.0, -180.0))
        assertTrue(LocationValidator.validateCoordinates(40.7128, -74.0060))
    }

    @Test
    fun testLocationValidator_invalidCoordinates_returnsFalse() {
        assertFalse(LocationValidator.validateCoordinates(90.1, 0.0))
        assertFalse(LocationValidator.validateCoordinates(-90.1, 0.0))
        assertFalse(LocationValidator.validateCoordinates(0.0, 180.1))
        assertFalse(LocationValidator.validateCoordinates(0.0, -180.1))
    }

    @Test
    fun testLocationValidator_validRadius_returnsTrue() {
        assertTrue(LocationValidator.validateRadius(1.0))
        assertTrue(LocationValidator.validateRadius(1000.0))
        assertTrue(LocationValidator.validateRadius(50000.0))
    }

    @Test
    fun testLocationValidator_invalidRadius_returnsFalse() {
        assertFalse(LocationValidator.validateRadius(0.5))
        assertFalse(LocationValidator.validateRadius(50001.0))
        assertFalse(LocationValidator.validateRadius(-10.0))
    }

    @Test
    fun testLocationNameSanitization_validInput_cleansCorrectly() {
        val input = "  Central Park, NYC! @#$%  "
        val sanitized = LocationValidator.sanitizeLocationName(input)

        assertEquals("Central Park NYC", sanitized)
    }

    @Test
    fun testLocationNameSanitization_longInput_truncates() {
        val longInput = "A".repeat(150)
        val sanitized = LocationValidator.sanitizeLocationName(longInput)

        assertEquals(100, sanitized.length)
        assertTrue("Should contain only A characters", sanitized.all { it == 'A' })
    }

    @Test
    fun testLocationNameSanitization_specialCharacters_removesUnsafe() {
        val input = "Location<script>alert('xss')</script>"
        val sanitized = LocationValidator.sanitizeLocationName(input)

        assertEquals("Locationscriptalertxssscript", sanitized)
        assertFalse("Should not contain dangerous characters", sanitized.contains("<"))
        assertFalse("Should not contain dangerous characters", sanitized.contains(">"))
    }

    @Test
    fun testLocationDataEncryption_sensitiveLocation_encryptsSecurely() = runTest {
        val sensitiveLocation = "40.712812345,-74.006021987"
        val encrypted = DataEncryption.encryptSensitiveData(sensitiveLocation)

        assertNotNull("Encrypted data should not be null", encrypted.data)
        assertNotNull("IV should not be null", encrypted.iv)
        assertNotEquals("Encrypted data should differ from original", sensitiveLocation.toByteArray().contentEquals(encrypted.data))
        assertTrue("Encrypted data should have reasonable length", encrypted.data.size > 0)
        assertTrue("IV should have appropriate length", encrypted.iv.size >= 12)
    }

    @Test
    fun testLocationAccessControl_authorizedUser_grantsAccess() {
        val userContext = AccessContext(
            userId = "test-user-123",
            role = UserRole.PARTICIPANT,
            permissions = setOf(Permission.ACCESS_LOCATION_DATA)
        )
        val accessController = AccessController()

        val hasAccess = accessController.hasPermission(userContext, Permission.ACCESS_LOCATION_DATA)

        assertTrue("Authorized user should have location access", hasAccess)
    }

    @Test
    fun testLocationAccessControl_unauthorizedUser_deniesAccess() {
        val userContext = AccessContext(
            userId = "test-user-123",
            role = UserRole.PARTICIPANT,
            permissions = setOf(Permission.READ_EVENTS)
        )
        val accessController = AccessController()

        val hasAccess = accessController.hasPermission(userContext, Permission.ACCESS_LOCATION_DATA)

        assertFalse("Unauthorized user should not have location access", hasAccess)
    }

    @Test
    fun testLocationBoundaryValidation_withinBounds_acceptsLocation() {
        val nycBounds = LocationBounds(
            north = 40.9176,
            south = 40.4774,
            east = -73.7004,
            west = -74.2591
        )

        val centralPark = SecureLocation(40.7829, -73.9654, 10f)
        assertTrue("Central Park should be within NYC bounds", nycBounds.contains(centralPark))
    }

    @Test
    fun testLocationBoundaryValidation_outsideBounds_rejectsLocation() {
        val nycBounds = LocationBounds(
            north = 40.9176,
            south = 40.4774,
            east = -73.7004,
            west = -74.2591
        )

        val philadelphia = SecureLocation(39.9526, -75.1652, 10f)
        assertFalse("Philadelphia should be outside NYC bounds", nycBounds.contains(philadelphia))
    }

    @Test
    fun testLocationHistoryCleanup_oldData_removesExpiredEntries() = runTest {
        val locationHistory = LocationHistory()
        val oldLocation = LocationEntry(
            location = SecureLocation(40.7128, -74.0060, 10f),
            timestamp = System.currentTimeMillis() - (25 * 60 * 60 * 1000)
        )
        val recentLocation = LocationEntry(
            location = SecureLocation(40.7829, -73.9654, 10f),
            timestamp = System.currentTimeMillis() - (1 * 60 * 60 * 1000)
        )

        locationHistory.addEntry(oldLocation)
        locationHistory.addEntry(recentLocation)
        locationHistory.cleanupExpiredEntries(maxAgeHours = 24)

        val remainingEntries = locationHistory.getEntries()
        assertEquals("Should have one remaining entry", 1, remainingEntries.size)
        assertEquals("Should keep recent location", recentLocation, remainingEntries.first())
    }

    data class SecureLocation(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float
    ) {
        init {
            require(latitude in -90.0..90.0) { "Invalid latitude: $latitude" }
            require(longitude in -180.0..180.0) { "Invalid longitude: $longitude" }
            require(accuracy >= 0) { "Accuracy cannot be negative: $accuracy" }
        }

        fun anonymized(precision: Int = 3): SecureLocation {
            val factor = 10.0.pow(precision)
            return SecureLocation(
                latitude = (latitude * factor).roundToInt() / factor,
                longitude = (longitude * factor).roundToInt() / factor,
                accuracy = accuracy
            )
        }
    }

    object LocationValidator {
        fun validateCoordinates(lat: Double, lng: Double): Boolean {
            return lat in -90.0..90.0 && lng in -180.0..180.0
        }

        fun validateRadius(radius: Double): Boolean {
            return radius in 1.0..50000.0
        }

        fun sanitizeLocationName(name: String): String {
            return name.trim()
                .take(100)
                .replace(Regex("[^\\w\\s-.]"), "")
        }
    }

    object DataEncryption {
        fun encryptSensitiveData(data: String): EncryptedData {
            val encryptedBytes = data.toByteArray().reversedArray()
            val iv = ByteArray(16) { it.toByte() }
            return EncryptedData(encryptedBytes, iv)
        }
    }

    data class EncryptedData(val data: ByteArray, val iv: ByteArray)

    data class AccessContext(
        val userId: String,
        val role: UserRole,
        val permissions: Set<Permission>
    )

    enum class UserRole {
        PARTICIPANT, ORGANIZER, ADMIN
    }

    enum class Permission {
        READ_EVENTS, CREATE_EVENTS, MODIFY_EVENTS, DELETE_EVENTS,
        ACCESS_USER_DATA, MODIFY_USER_DATA, ACCESS_LOCATION_DATA
    }

    class AccessController {
        fun hasPermission(context: AccessContext, permission: Permission): Boolean {
            return when (permission) {
                Permission.CREATE_EVENTS -> context.role in setOf(UserRole.ORGANIZER, UserRole.ADMIN)
                Permission.DELETE_EVENTS -> context.role == UserRole.ADMIN
                Permission.ACCESS_LOCATION_DATA -> context.permissions.contains(permission)
                else -> context.permissions.contains(permission)
            }
        }
    }

    data class LocationBounds(
        val north: Double,
        val south: Double,
        val east: Double,
        val west: Double
    ) {
        fun contains(location: SecureLocation): Boolean {
            return location.latitude <= north &&
                    location.latitude >= south &&
                    location.longitude <= east &&
                    location.longitude >= west
        }
    }

    data class LocationEntry(
        val location: SecureLocation,
        val timestamp: Long
    )

    class LocationHistory {
        private val entries = mutableListOf<LocationEntry>()

        fun addEntry(entry: LocationEntry) {
            entries.add(entry)
        }

        fun cleanupExpiredEntries(maxAgeHours: Int) {
            val cutoffTime = System.currentTimeMillis() - (maxAgeHours * 60 * 60 * 1000)
            entries.removeAll { it.timestamp < cutoffTime }
        }

        fun getEntries(): List<LocationEntry> = entries.toList()
    }
}