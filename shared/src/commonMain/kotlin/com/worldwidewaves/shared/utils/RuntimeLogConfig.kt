package com.worldwidewaves.shared.utils

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

/**
 * Runtime log configuration manager using Firebase Remote Config.
 *
 * This enables dynamic log level control without app updates:
 * - Enable verbose logging for specific tags in production
 * - Disable noisy logs without redeploying
 * - Debug production issues remotely
 *
 * ## Remote Config Keys
 *
 * The following keys are supported in Firebase Remote Config:
 *
 * **Global Log Level Control**:
 * - `log_level_global`: Minimum log level for all tags (default: "INFO")
 *   - Values: "VERBOSE", "DEBUG", "INFO", "WARNING", "ERROR"
 *   - Affects all logs unless overridden by tag-specific config
 *
 * **Tag-Specific Log Level Control**:
 * - `log_level_<tag>`: Minimum log level for specific tag
 *   - Example: `log_level_PositionManager` = "VERBOSE"
 *   - Example: `log_level_WWW.Position.Manager` = "DEBUG"
 *   - Overrides global log level for that tag
 *
 * **Feature-Specific Control** (overrides build config):
 * - `enable_verbose_logging`: Enable verbose logs (default: BuildKonfig.ENABLE_VERBOSE_LOGGING)
 * - `enable_debug_logging`: Enable debug logs (default: BuildKonfig.ENABLE_DEBUG_LOGGING)
 * - `enable_performance_logging`: Enable performance logs (default: BuildKonfig.ENABLE_PERFORMANCE_LOGGING)
 * - `enable_position_tracking_logging`: Enable position tracking logs (default: false)
 *
 * ## Usage
 *
 * ```kotlin
 * // Initialize during app startup
 * suspend fun initializeApp() {
 *     RuntimeLogConfig.initialize()
 * }
 *
 * // Check before logging (automatic in Log wrapper)
 * if (RuntimeLogConfig.shouldLog("PositionManager", LogLevel.VERBOSE)) {
 *     Log.v("PositionManager", "Position update: $position")
 * }
 *
 * // Get minimum log level for a tag
 * val minLevel = RuntimeLogConfig.getMinLogLevel("EventObserver")
 * ```
 *
 * ## Fallback Behavior
 *
 * If Firebase Remote Config is unavailable or initialization fails:
 * - Falls back to build configuration (BuildKonfig flags)
 * - Logs continue to work normally
 * - No app crashes due to config unavailability
 *
 * ## Performance
 *
 * - Remote Config is cached locally after first fetch
 * - Log level checks are fast (in-memory lookups)
 * - No network calls during normal logging operations
 * - Fetch interval: 12 hours (configurable)
 */
expect object RuntimeLogConfig {
    /**
     * Initialize Firebase Remote Config and fetch latest values.
     *
     * This should be called during app initialization (after Koin setup).
     * It's safe to call multiple times (idempotent).
     *
     * ## Implementation Notes
     * - Fetches remote config values from Firebase
     * - Sets default values based on build configuration
     * - Activates fetched config for immediate use
     * - Handles network errors gracefully (uses defaults)
     *
     * @throws Exception if Firebase initialization fails catastrophically
     *   (normal network errors are handled silently)
     */
    suspend fun initialize()

    /**
     * Check if a specific log level should be enabled for a tag.
     *
     * This checks both:
     * 1. Tag-specific remote config (`log_level_<tag>`)
     * 2. Global log level config (`log_level_global`)
     * 3. Build configuration (fallback)
     *
     * ## Examples
     * ```kotlin
     * // Check if verbose logging enabled for PositionManager
     * if (RuntimeLogConfig.shouldLog("PositionManager", LogLevel.VERBOSE)) {
     *     Log.v("PositionManager", "Detailed position info")
     * }
     *
     * // Check if error logging enabled (usually always true)
     * if (RuntimeLogConfig.shouldLog("EventObserver", LogLevel.ERROR)) {
     *     Log.e("EventObserver", "Critical error", throwable)
     * }
     * ```
     *
     * @param tag Log tag to check (e.g., "PositionManager", "WWW.Position.Manager")
     * @param level Log level to check (VERBOSE, DEBUG, INFO, WARNING, ERROR)
     * @return true if logging at this level is enabled for this tag
     */
    fun shouldLog(
        tag: String,
        level: LogLevel,
    ): Boolean

    /**
     * Get minimum log level for a specific tag from remote config.
     *
     * Returns the effective minimum log level considering:
     * 1. Tag-specific remote config
     * 2. Global remote config
     * 3. Build configuration (fallback)
     *
     * ## Examples
     * ```kotlin
     * val minLevel = RuntimeLogConfig.getMinLogLevel("EventObserver")
     * // Returns: LogLevel.VERBOSE, DEBUG, INFO, WARNING, or ERROR
     * ```
     *
     * @param tag Log tag to check
     * @return Minimum log level for this tag
     */
    fun getMinLogLevel(tag: String): LogLevel
}

/**
 * Log levels with priority values for comparison.
 *
 * Higher priority = more severe = less verbose.
 *
 * ## Priority Values
 * - VERBOSE: 1 (most verbose, lowest priority)
 * - DEBUG: 2
 * - INFO: 3
 * - WARNING: 4
 * - ERROR: 5 (least verbose, highest priority)
 *
 * ## Usage
 * ```kotlin
 * if (LogLevel.DEBUG >= LogLevel.VERBOSE) {
 *     // True - DEBUG has higher priority than VERBOSE
 * }
 * ```
 */
enum class LogLevel(
    val priority: Int,
) {
    VERBOSE(1),
    DEBUG(2),
    INFO(3),
    WARNING(4),
    ERROR(5),
    ;

    companion object {
        /**
         * Parse log level from string (case-insensitive).
         *
         * @param value String value ("VERBOSE", "DEBUG", "INFO", "WARNING", "ERROR")
         * @return Corresponding LogLevel or null if invalid
         */
        fun fromString(value: String): LogLevel? =
            try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
    }
}
