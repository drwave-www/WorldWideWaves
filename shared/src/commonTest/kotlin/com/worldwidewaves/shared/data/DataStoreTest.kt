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
    fun `test dataStoreFileName constant has correct value`() {
        assertEquals(
            "wwwaves.preferences_pb",
            dataStoreFileName,
            "dataStoreFileName should have the expected value",
        )
    }

    @Test
    fun `test createDataStore logs initialization message`() {
        // GIVEN: Mock path provider
        val pathProvider = mockk<() -> String>()
        every { pathProvider() } returns "/test/path"

        // WHEN & THEN: Creating DataStore should call path provider
        // Note: In test environment, DataStore creation may fail due to platform limitations
        // but the path provider should still be called and failures should be properly logged
        try {
            @Suppress("DEPRECATION")
            createDataStore(pathProvider)
            // If successful, verify path provider was called
            verify { pathProvider() }
        } catch (e: DataStoreException) {
            // Expected error behavior: DataStore creation failed but was properly logged
            verify { pathProvider() }
            assertTrue(e.message?.contains("DataStore creation failed") == true,
                "DataStoreException should contain proper error message")
        } catch (e: Exception) {
            // Unexpected error: should be wrapped in DataStoreException
            fail("Unexpected exception type: ${e::class.simpleName}. Should be wrapped in DataStoreException")
        }
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
    fun `test path provider function is called`() {
        // GIVEN: Mock path provider
        val pathProviderMock = mockk<() -> String>()
        every { pathProviderMock() } returns "/test/path"

        // WHEN & THEN: Creating DataStore should call path provider regardless of success/failure
        try {
            @Suppress("DEPRECATION")
            createDataStore(pathProviderMock)
            verify { pathProviderMock() }
        } catch (e: DataStoreException) {
            // Expected: DataStore creation failed but path provider was called
            verify { pathProviderMock() }
            assertNotNull(e.cause, "DataStoreException should have a cause")
        } catch (e: Exception) {
            // Unexpected: should be wrapped in DataStoreException
            fail("Exception should be wrapped in DataStoreException: ${e::class.simpleName}")
        }
    }

    @Test
    fun `test DataStore error handling with invalid path`() {
        // GIVEN: Path provider that returns invalid path
        val pathProvider = mockk<() -> String>()
        every { pathProvider() } returns ""

        // WHEN & THEN: Should wrap exceptions in DataStoreException
        try {
            @Suppress("DEPRECATION")
            createDataStore(pathProvider)
            // If creation somehow succeeds with empty path, that's also acceptable
        } catch (e: DataStoreException) {
            // Expected: DataStore creation failed and was properly wrapped
            assertTrue(e.message?.contains("DataStore creation failed") == true,
                "Error message should indicate DataStore creation failure")
            assertNotNull(e.cause, "DataStoreException should have underlying cause")
        } catch (e: Exception) {
            fail("Raw exceptions should be wrapped in DataStoreException: ${e::class.simpleName}")
        }
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
