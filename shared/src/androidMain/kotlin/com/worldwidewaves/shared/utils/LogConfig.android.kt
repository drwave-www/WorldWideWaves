package com.worldwidewaves.shared.utils

import com.worldwidewaves.shared.BuildConfig

/**
 * Android implementation of logging configuration.
 * Values are retrieved from the generated Android BuildConfig.
 */
actual object LogConfig {
    actual val ENABLE_VERBOSE_LOGGING: Boolean = BuildConfig.ENABLE_VERBOSE_LOGGING
    actual val ENABLE_DEBUG_LOGGING: Boolean = BuildConfig.ENABLE_DEBUG_LOGGING
    actual val ENABLE_PERFORMANCE_LOGGING: Boolean = BuildConfig.ENABLE_PERFORMANCE_LOGGING
}