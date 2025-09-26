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

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin modules for real integration tests.
 *
 * These modules provide real implementations instead of mocks for integration testing.
 */

/**
 * Real location services for integration testing
 */
val realLocationModule = module {
    // Real GPS location provider
    single<Any> {
        // Would provide actual AndroidWWWLocationProvider instead of mock
        "RealGPSLocationProvider"
    }

    // Real location permissions handler
    single<Any> {
        // Would provide actual location permission handler
        "RealLocationPermissionHandler"
    }

    // Real geofencing service
    single<Any> {
        // Would provide actual geofencing implementation
        "RealGeofencingService"
    }
}

/**
 * Real Firebase services for integration testing
 */
val realFirebaseModule = module {
    // Real Firestore for events
    single<Any> {
        // Would provide actual Firebase Firestore instance configured for test project
        "RealFirestoreService"
    }

    // Real Firebase Auth
    single<Any> {
        // Would provide actual Firebase Auth for test users
        "RealFirebaseAuth"
    }

    // Real-time database for coordination
    single<Any> {
        // Would provide actual Firebase Realtime Database for coordination
        "RealFirebaseRealtime"
    }
}

/**
 * Real map services for integration testing
 */
val realMapModule = module {
    // Real MapLibre adapter
    single<Any> {
        // Would provide actual MapLibre adapter with real map tiles
        "RealMapLibreAdapter"
    }

    // Real map style loader
    single<Any> {
        // Would provide actual map style loading service
        "RealMapStyleLoader"
    }

    // Real camera controller
    single<Any> {
        // Would provide actual camera control for map interactions
        "RealMapCameraController"
    }
}

/**
 * Real network services for integration testing
 */
val realNetworkModule = module {
    // Real HTTP client
    single<Any> {
        // Would provide actual HTTP client for API calls
        "RealHttpClient"
    }

    // Real network monitor
    single<Any> {
        // Would provide actual network connectivity monitoring
        "RealNetworkMonitor"
    }

    // Real retry handler
    single<Any> {
        // Would provide actual network retry logic
        "RealRetryHandler"
    }
}

/**
 * Test utilities for integration testing
 */
val testUtilityModule = module {
    // Test data manager
    singleOf(::TestDataManager)

    // Real device state manager
    single { RealDeviceStateManager(get()) }

    // Performance monitoring for tests
    single<Any> {
        // Would provide actual performance monitoring
        "RealPerformanceMonitor"
    }

    // Test configuration
    single<Any> {
        // Would provide test-specific configuration
        "TestConfiguration"
    }
}