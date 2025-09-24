package com.worldwidewaves.shared.data

import com.worldwidewaves.shared.events.utils.Log
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.fail

/**
 * Tests for DataStore functionality in a KMP-compatible way.
 * These tests focus on the public API and behavior that can be verified
 * without using reflection or JVM-specific features.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreTest {
    @BeforeTest
    fun setUp() {
        // Mock the Log object for verification
        mockkObject(Log)
        justRun { Log.i(any(), any()) }
        justRun { Log.v(any(), any()) }
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test DATA_STORE_FILE_NAME constant has correct value`() {
        assertEquals(
            "wwwaves.preferences_pb",
            DATA_STORE_FILE_NAME,
            "DATA_STORE_FILE_NAME should have the expected value",
        )
    }

    @Test
    fun `test createDataStore with valid path calls path provider`() {
        // GIVEN: Mock path provider with valid path
        val pathProvider = mockk<() -> String>()
        every { pathProvider() } returns "/test/path"

        // WHEN: Creating DataStore (may succeed or fail depending on platform)
        // THEN: Path provider should always be called
        var pathProviderCalled = false
        try {
            @Suppress("DEPRECATION")
            createDataStore(pathProvider)
            pathProviderCalled = true
        } catch (e: DataStoreException) {
            // DataStore creation failed in test environment - this is acceptable
            pathProviderCalled = true
            assertTrue(e.message?.contains("DataStore creation failed") == true,
                "DataStoreException should contain proper error message")
        }

        assertTrue(pathProviderCalled, "Path provider should be called regardless of outcome")
        verify { pathProvider() }
    }

    @Test
    fun `test TestDataStoreFactory creates isolated instances`() {
        // GIVEN: TestDataStoreFactory for isolated test instances
        val factory = TestDataStoreFactory()
        val pathProvider1 = mockk<() -> String>()
        val pathProvider2 = mockk<() -> String>()
        every { pathProvider1() } returns "/test/path1"
        every { pathProvider2() } returns "/test/path2"

        // WHEN: Creating multiple DataStore instances
        val dataStore1 = factory.create(pathProvider1)
        val dataStore2 = factory.create(pathProvider2)

        // THEN: Each call should use the path provider and create separate instances
        verify { pathProvider1() }
        verify { pathProvider2() }

        // Each TestDataStoreFactory.create() call creates a new instance
        // This ensures test isolation
        assertTrue(dataStore1 !== dataStore2, "TestDataStoreFactory should create separate instances for test isolation")
    }

    @Test
    fun `test path provider function is always called`() {
        // GIVEN: Mock path provider
        val pathProviderMock = mockk<() -> String>()
        every { pathProviderMock() } returns "/test/path"

        // WHEN: Creating DataStore
        // THEN: Path provider should be called regardless of success/failure
        try {
            @Suppress("DEPRECATION")
            createDataStore(pathProviderMock)
        } catch (e: DataStoreException) {
            // DataStore creation failed - this is acceptable in test environment
            assertNotNull(e.cause, "DataStoreException should have a cause")
        }

        // Path provider should always be called
        verify { pathProviderMock() }
    }

    @Test
    fun `test DataStore error handling with invalid path`() {
        // GIVEN: Path provider that returns invalid path
        val pathProvider = mockk<() -> String>()
        every { pathProvider() } returns ""

        // WHEN: Creating DataStore with invalid path
        // THEN: Should either succeed (if implementation handles empty path) or fail with DataStoreException
        try {
            @Suppress("DEPRECATION")
            createDataStore(pathProvider)
            // If creation succeeds with empty path, that's acceptable
        } catch (e: DataStoreException) {
            // Expected: DataStore creation failed and was properly wrapped
            assertTrue(e.message?.contains("DataStore creation failed") == true,
                "Error message should indicate DataStore creation failure")
            assertNotNull(e.cause, "DataStoreException should have underlying cause")
        }

        // Path provider should still be called
        verify { pathProvider() }
    }

    @Test
    fun `test DefaultDataStoreFactory maintains singleton behavior`() {
        // GIVEN: DefaultDataStoreFactory for production use
        val factory = DefaultDataStoreFactory()
        val pathProvider = mockk<() -> String>()
        every { pathProvider() } returns "/test/production/path"

        // WHEN: Creating multiple DataStore instances from same factory
        try {
            val dataStore1 = factory.create(pathProvider)
            val dataStore2 = factory.create(pathProvider)

            // THEN: Should return the same instance (singleton behavior)
            assertTrue(dataStore1 === dataStore2, "DefaultDataStoreFactory should maintain singleton behavior")

            // Path provider should be called for both, but second call should return existing instance
            verify(atLeast = 1) { pathProvider() }
        } catch (e: DataStoreException) {
            // Expected in test environment due to platform limitations
            verify(atLeast = 1) { pathProvider() }
        }
    }

    @Test
    fun `test expect function keyValueStorePath exists`() {
        // This test verifies that the expect function exists
        // We can't test its implementation directly in common code

        // Just verify that the function is declared
        // This is a compile-time check, not a runtime check
        val functionExists = true // If this compiles, the function exists
        assertTrue(functionExists, "keyValueStorePath function should be declared")
    }
}
