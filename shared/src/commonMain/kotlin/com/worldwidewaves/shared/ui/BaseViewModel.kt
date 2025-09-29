package com.worldwidewaves.shared.ui

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

import kotlinx.coroutines.CoroutineScope

/**
 * Cross-platform base ViewModel abstraction for WorldWideWaves.
 *
 * This provides a unified ViewModel interface that works across Android and iOS platforms.
 * On Android, this will delegate to androidx.lifecycle.ViewModel, while on iOS it will
 * provide iOS-compatible lifecycle management.
 *
 * Key Features:
 * • Platform-agnostic coroutine scope management
 * • Lifecycle-aware resource cleanup
 * • Consistent state management patterns across platforms
 * • Memory leak prevention through proper cleanup
 *
 * Usage:
 * ```kotlin
 * class MyViewModel(
 *     private val repository: MyRepository
 * ) : BaseViewModel() {
 *     private val _data = MutableStateFlow<List<Data>>(emptyList())
 *     val data: StateFlow<List<Data>> = _data.asStateFlow()
 *
 *     init {
 *         viewModelScope.launch {
 *             repository.getData().collect { _data.value = it }
 *         }
 *     }
 * }
 * ```
 *
 * Platform Implementation:
 * • Android: Extends androidx.lifecycle.ViewModel with viewModelScope
 * • iOS: Provides custom lifecycle with iOS-compatible coroutine scope
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect abstract class BaseViewModel() {
    /**
     * Coroutine scope tied to the ViewModel lifecycle.
     * Automatically canceled when the ViewModel is cleared.
     */
    protected val scope: CoroutineScope

    /**
     * Called when the ViewModel is being cleared.
     * Override to perform cleanup operations.
     */
    protected open fun onCleared()
}
