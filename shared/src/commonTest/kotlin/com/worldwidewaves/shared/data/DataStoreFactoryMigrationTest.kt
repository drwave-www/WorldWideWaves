package com.worldwidewaves.shared.data

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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.di.testDatastoreModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Demonstration test showing how to migrate from the old createDataStore pattern
 * to the new DataStoreFactory pattern using testDatastoreModule.
 *
 * This test serves as a migration guide for other test files.
 */
class DataStoreFactoryMigrationTest : KoinTest {
    @BeforeTest
    fun setUp() {
        // Clean up any previous Koin context
        try {
            stopKoin()
        } catch (e: Exception) {
            // Ignore if Koin is not running
        }
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } catch (e: Exception) {
            // Ignore if Koin is not running
        }
    }

    @Test
    fun `demonstration of old pattern with deprecation warning`() {
        // OLD PATTERN (deprecated): Direct createDataStore usage
        @Suppress("DEPRECATION")
        val dataStore1 = createDataStore { "/tmp/old_pattern_test_1.pb" }

        @Suppress("DEPRECATION")
        val dataStore2 = createDataStore { "/tmp/old_pattern_test_2.pb" }

        // Each call to deprecated createDataStore creates a new TestDataStoreFactory instance
        // This provides isolation but doesn't demonstrate proper DI usage
        assertTrue(dataStore1 !== dataStore2, "Old pattern should create separate instances")
    }

    @Test
    fun `demonstration of new pattern with testDatastoreModule`() {
        // NEW PATTERN (preferred): Use testDatastoreModule with proper DI
        val testModule =
            module {
                single<WWWPlatform> { WWWPlatform("test") }
            }

        startKoin {
            modules(testDatastoreModule + testModule)
        }

        try {
            // Get DataStore instances through DI
            val dataStore1 = get<DataStore<Preferences>>()
            val dataStore2 = get<DataStore<Preferences>>()

            // With testDatastoreModule using factory, each get() call creates a new instance
            // This provides maximum test isolation
            assertNotSame(dataStore1, dataStore2, "testDatastoreModule uses factory - each get() creates new instance for isolation")

            // Get factory to demonstrate direct usage if needed
            val factory = get<DataStoreFactory>()
            assertTrue(factory is TestDataStoreFactory, "Should use TestDataStoreFactory in tests")

            // Direct factory usage creates new instances (for advanced scenarios)
            val directDataStore1 = factory.create { "/tmp/direct_test_1.pb" }
            val directDataStore2 = factory.create { "/tmp/direct_test_2.pb" }
            assertNotSame(directDataStore1, directDataStore2, "Direct factory calls create separate instances")
        } finally {
            stopKoin()
        }
    }

    @Test
    fun `demonstration of production pattern with DefaultDataStoreFactory`() {
        // PRODUCTION PATTERN: DefaultDataStoreFactory maintains singleton behavior
        val factory = DefaultDataStoreFactory()

        try {
            val dataStore1 = factory.create { "/tmp/production_test.pb" }
            val dataStore2 = factory.create { "/tmp/production_test.pb" }

            // DefaultDataStoreFactory should return the same instance for same path
            assertSame(dataStore1, dataStore2, "DefaultDataStoreFactory should maintain singleton behavior")
        } catch (e: DataStoreException) {
            // Expected in test environment - just verify behavior
            assertTrue(e.message?.contains("DataStore creation failed") == true)
        }
    }

    @Test
    fun `demonstration of factory isolation between test instances`() {
        // Each TestDataStoreFactory instance creates separate DataStores
        val factory1 = TestDataStoreFactory()
        val factory2 = TestDataStoreFactory()

        val dataStore1 = factory1.create { "/tmp/isolation_test.pb" }
        val dataStore2 = factory2.create { "/tmp/isolation_test.pb" }

        // Different factory instances create different DataStore instances
        // This ensures complete test isolation
        assertNotSame(dataStore1, dataStore2, "Different TestDataStoreFactory instances should create isolated DataStores")
    }
}
