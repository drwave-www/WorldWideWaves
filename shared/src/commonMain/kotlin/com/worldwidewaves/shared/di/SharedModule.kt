package com.worldwidewaves.shared.di

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

/**
 * Master module list aggregating all DI modules for the WorldWideWaves shared codebase.
 *
 * ## Module Purpose
 * Provides the complete dependency graph for Kotlin Multiplatform shared code by
 * aggregating all feature-specific modules in the correct initialization order.
 *
 * ## Initialization Order
 * Modules are loaded in this specific order to respect dependencies:
 * 1. **commonModule** - Core event and sound infrastructure (no dependencies)
 * 2. **helpersModule** - Utility services and domain logic (depends on commonModule)
 * 3. **datastoreModule** - Data persistence layer (depends on helpersModule)
 * 4. **uiModule** - UI components and ViewModels (depends on all above)
 *
 * ## Platform Integration
 * This module list is used by both Android and iOS platforms:
 * - **Android**: Loaded in Application.onCreate() via startKoin(modules = sharedModule)
 * - **iOS**: Loaded in SceneDelegate via initKoin(modules = sharedModule)
 *
 * Platform-specific modules are loaded separately:
 * - Android: androidModule (added via modules(...) after startKoin)
 * - iOS: iosModule (added via modules(...) after initKoin)
 *
 * ## Thread Safety
 * All modules must be loaded on the main thread before any DI access.
 * iOS has strict threading requirements - violating this causes deadlocks.
 *
 * ## Dependencies
 * - **commonModule**: Event management, sound choreography
 * - **helpersModule**: Position tracking, wave progression, scheduling
 * - **datastoreModule**: Event favorites, persistent storage
 * - **uiModule**: Repositories, use cases, shared UI components
 *
 * @see commonModule for core event and sound dependencies
 * @see helpersModule for utility and domain services
 * @see datastoreModule for data persistence
 * @see uiModule for UI-related dependencies
 */
val sharedModule =
    listOf(
        commonModule,
        helpersModule,
        datastoreModule,
        uiModule,
    )
