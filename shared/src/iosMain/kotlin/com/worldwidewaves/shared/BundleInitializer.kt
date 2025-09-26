package com.worldwidewaves.shared

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
    fun initializeBundle(): Boolean =
        try {
            if (!_isInitialized) {
                // Try to load the bundle with various identifiers using moko-resources utilities
                _bundle =
                    try {
                        NSBundle.loadableBundle("com.worldwidewaves.shared.main")
                    } catch (e: Exception) {
                        try {
                            NSBundle.loadableBundle("com.worldwidewaves.shared")
                        } catch (e2: Exception) {
                            NSBundle.mainBundle
                        }
                    }

                _isInitialized = _bundle != null

                if (_isInitialized) {
                    platform.Foundation.NSLog("BUNDLE_INIT: MokoRes bundle initialized successfully")
                } else {
                    platform.Foundation.NSLog("BUNDLE_INIT: Failed to load MokoRes bundle")
                }
            }
            _isInitialized
        } catch (e: Exception) {
            platform.Foundation.NSLog("BUNDLE_INIT: Exception during bundle initialization: ${e.message}")
            false
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
