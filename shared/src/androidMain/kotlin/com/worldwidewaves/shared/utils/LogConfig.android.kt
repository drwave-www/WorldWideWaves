package com.worldwidewaves.shared.utils

/**
 * Android implementation of logging configuration.
 * For Android, we enable debug/verbose logging in debug builds.
 */
actual object LogConfig {
    actual val ENABLE_VERBOSE_LOGGING: Boolean = true // Enable for development
    actual val ENABLE_DEBUG_LOGGING: Boolean = true // Enable for development
    actual val ENABLE_PERFORMANCE_LOGGING: Boolean = true // Enable for development
}