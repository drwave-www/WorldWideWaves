package com.worldwidewaves.shared.utils

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel

/**
 * iOS Antilog implementation that uses NSLog for Apple's Unified Logging.
 * Logs should be visible via:
 * - xcrun simctl spawn <UDID> log stream --level debug --predicate 'processImagePath CONTAINS[c] "WorldWideWaves"'
 * - xcrun simctl spawn <UDID> log show --style compact --last 2m --debug --info --predicate 'processImagePath CONTAINS[c] "WorldWideWaves"'
 */
internal class NSLogAntilog : Antilog() {
    override fun isEnable(
        priority: LogLevel,
        tag: String?,
    ) = true

    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?,
    ) {
        // Ultra-safe implementation that cannot crash and handles character encoding
        try {
            // Sanitize strings to prevent encoding issues
            val safePriority = sanitizeForLog(priority.name)
            val safeTag = sanitizeForLog(tag ?: "LOG")
            val safeMessage = sanitizeForLog(message ?: "no message")

            // Simple string concatenation without format specifiers
            val simpleLog = "$safePriority $safeTag: $safeMessage"
            platform.Foundation.NSLog(simpleLog)
        } catch (e: Throwable) {
            // Ultimate fallback - use basic NSLog without any variables
            try {
                platform.Foundation.NSLog("LOG_ERROR")
            } catch (fallback: Throwable) {
                // Do absolutely nothing to prevent any crash
            }
        }
    }

    /**
     * Sanitize strings to prevent character encoding issues that cause API errors.
     * Removes problematic characters like \M-p\M^_ that cause JSON parsing errors.
     */
    private fun sanitizeForLog(input: String): String =
        try {
            input
                // Remove escape sequences like \M-p\M^_\M^N\M-( that cause JSON errors
                .replace(Regex("\\\\M-[^\\s]*"), "") // Remove \M- sequences
                .replace(Regex("\\\\[^\\s]*"), "") // Remove other escape sequences
                // Remove control characters and non-printable chars
                .replace(Regex("[\\x00-\\x1F\\x7F-\\x9F]"), "")
                // Remove Unicode surrogates that cause JSON parsing issues
                .replace(Regex("[\\uD800-\\uDFFF]"), "")
                // Remove symbols and emojis that can cause encoding issues
                .replace(Regex("[\\p{So}\\p{Sk}\\p{Sc}\\p{Sm}]"), "")
                // Replace any remaining non-ASCII printable characters
                .replace(Regex("[^\\x20-\\x7E]"), "")
                // Trim whitespace and limit length
                .trim()
                .take(200)
        } catch (e: Exception) {
            "SANITIZE_ERROR"
        }
}
