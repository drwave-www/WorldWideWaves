package com.worldwidewaves.shared.utils

/**
 * Cross-platform logging configuration interface.
 * Platform-specific implementations provide build-time logging configuration values.
 */
expect object LogConfig {
    val ENABLE_VERBOSE_LOGGING: Boolean
    val ENABLE_DEBUG_LOGGING: Boolean
    val ENABLE_PERFORMANCE_LOGGING: Boolean
}