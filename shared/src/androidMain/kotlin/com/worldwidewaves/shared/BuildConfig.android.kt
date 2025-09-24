package com.worldwidewaves.shared

/**
 * Android implementation of build configuration.
 * Values are configured in the android.defaultConfig block of build.gradle.kts.
 */
actual object BuildConfig {
    // Reference the generated Android BuildConfig
    private val androidBuildConfig = com.worldwidewaves.shared.BuildConfig

    actual val ENABLE_VERBOSE_LOGGING: Boolean = androidBuildConfig.ENABLE_VERBOSE_LOGGING
    actual val ENABLE_DEBUG_LOGGING: Boolean = androidBuildConfig.ENABLE_DEBUG_LOGGING
    actual val ENABLE_PERFORMANCE_LOGGING: Boolean = androidBuildConfig.ENABLE_PERFORMANCE_LOGGING
}