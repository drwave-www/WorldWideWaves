package com.worldwidewaves.shared

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

import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.utils.loadableBundle
import platform.Foundation.NSBundle

/**
 * Bundle initializer for iOS to ensure MokoRes bundle is properly loaded
 * before any string resources are accessed.
 *
 * This solves the FileFailedToInitializeException that occurs when
 * MokoRes tries to access the bundle during static initialization.
 */
object BundleInitializer {
    private const val TAG = "BundleInitializer"

    private var _isInitialized = false
    private var _bundle: NSBundle? = null

    /**
     * Initialize the resource bundle before any MokoRes access.
     * This should be called early in the app lifecycle.
     */
    fun initializeBundle(): Boolean =
        try {
            if (!_isInitialized) {
                _bundle = loadBundleWithFallbacks()
                _isInitialized = _bundle != null
                logInitializationResult()
            }
            _isInitialized
        } catch (e: Exception) {
            Log.e(TAG, "BUNDLE_INIT: Exception during bundle initialization: ${e.message}")
            false
        }

    /**
     * Attempts to load the bundle using various identifiers with fallback mechanism.
     */
    private fun loadBundleWithFallbacks(): NSBundle =
        try {
            NSBundle.loadableBundle("com.worldwidewaves.shared.main")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load bundle with identifier 'com.worldwidewaves.shared.main'", e)
            loadBundleSecondaryFallback()
        }

    /**
     * Secondary fallback for bundle loading.
     */
    private fun loadBundleSecondaryFallback(): NSBundle =
        try {
            NSBundle.loadableBundle("com.worldwidewaves.shared")
        } catch (e2: Exception) {
            Log.w(TAG, "Failed to load bundle with identifier 'com.worldwidewaves.shared', falling back to main bundle", e2)
            NSBundle.mainBundle
        }

    /**
     * Logs the initialization result.
     */
    private fun logInitializationResult() {
        if (_isInitialized) {
            Log.i(TAG, "BUNDLE_INIT: MokoRes bundle initialized successfully")
        } else {
            Log.e(TAG, "BUNDLE_INIT: Failed to load MokoRes bundle")
        }
    }

    /**
     * Get the initialized bundle, ensuring it's loaded first.
     */
    fun getBundle(): NSBundle? {
        if (!_isInitialized) {
            initializeBundle()
        }
        return _bundle
    }

    val isInitialized: Boolean
        get() = _isInitialized
}
