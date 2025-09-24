package com.worldwidewaves.shared.utils

/**
 * iOS implementation of logging configuration.
 * For iOS, we enable all logging in debug builds.
 */
actual object LogConfig {
    actual val ENABLE_VERBOSE_LOGGING: Boolean = true
    actual val ENABLE_DEBUG_LOGGING: Boolean = true
    actual val ENABLE_PERFORMANCE_LOGGING: Boolean = true
}
