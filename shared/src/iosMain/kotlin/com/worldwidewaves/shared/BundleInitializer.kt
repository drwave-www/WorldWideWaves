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
    private var _isInitialized = false
    private var _bundle: NSBundle? = null

    /**
     * Initialize the resource bundle before any MokoRes access.
     * This should be called early in the app lifecycle.
     */
    fun initializeBundle(): Boolean {
        return try {
            if (!_isInitialized) {
                _bundle = loadBundleWithFallbacks()
                _isInitialized = _bundle != null
                logInitializationResult()
            }
            _isInitialized
        } catch (e: Exception) {
            platform.Foundation.NSLog("BUNDLE_INIT: Exception during bundle initialization: ${e.message}")
            false
        }
    }

    private fun loadBundleWithFallbacks(): NSBundle {
        return tryLoadBundle("com.worldwidewaves.shared.main")
            ?: tryLoadBundle("com.worldwidewaves.shared")
            ?: NSBundle.mainBundle
    }

    private fun tryLoadBundle(identifier: String): NSBundle? {
        return try {
            NSBundle.loadableBundle(identifier)
        } catch (e: Exception) {
            null
        }
    }

    private fun logInitializationResult() {
        if (_isInitialized) {
            platform.Foundation.NSLog("BUNDLE_INIT: MokoRes bundle initialized successfully")
        } else {
            platform.Foundation.NSLog("BUNDLE_INIT: Failed to load MokoRes bundle")
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