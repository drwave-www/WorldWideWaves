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

package com.worldwidewaves.testing.real

import android.app.Application
import com.worldwidewaves.shared.monitoring.PerformanceMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Test application for real integration tests.
 *
 * This application:
 * - Uses real implementations instead of mocks
 * - Configures test-specific Firebase project
 * - Sets up performance monitoring for test analysis
 * - Configures test-friendly logging
 */
class RealIntegrationTestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        println("🚀 RealIntegrationTestApplication: Initializing for real integration tests")

        // Initialize performance monitoring in test mode
        PerformanceMonitor.initialize(isTestMode = true)

        // Start Koin with test configuration
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@RealIntegrationTestApplication)
            modules(getRealIntegrationTestModules())
        }

        println("✅ RealIntegrationTestApplication: Initialization complete")
    }

    private fun getRealIntegrationTestModules() = listOf(
        // Real implementations for integration testing
        realLocationModule,
        realFirebaseModule,
        realMapModule,
        realNetworkModule,
        testUtilityModule
    )
}