package com.worldwidewaves.shared

/**
 * Cross-platform build configuration interface.
 * Platform-specific implementations provide build-time configuration values.
 */
expect object BuildConfig {
    val ENABLE_VERBOSE_LOGGING: Boolean
    val ENABLE_DEBUG_LOGGING: Boolean
    val ENABLE_PERFORMANCE_LOGGING: Boolean
}