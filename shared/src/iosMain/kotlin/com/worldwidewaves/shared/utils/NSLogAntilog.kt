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
import platform.Foundation.NSLog

/**
 * iOS Antilog implementation that uses NSLog for output to Apple's Unified Logging.
 * This ensures logs are visible via:
 * - xcrun simctl spawn <UDID> log show --last 10s --predicate 'process == "WorldWideWaves"'
 * - xcrun simctl spawn <UDID> log stream --level debug --predicate 'process == "WorldWideWaves"'
 */
internal class NSLogAntilog : Antilog() {
    override fun isEnable(priority: LogLevel, tag: String?) = true

    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?
    ) {
        try {
            val level = when (priority) {
                LogLevel.VERBOSE -> "V"
                LogLevel.DEBUG -> "D"
                LogLevel.INFO -> "I"
                LogLevel.WARNING -> "W"
                LogLevel.ERROR -> "E"
                LogLevel.ASSERT -> "A"
            }

            val tagStr = tag ?: "WorldWideWaves"
            val messageStr = message ?: ""
            val throwableStr = throwable?.let { " | ${it::class.simpleName}: ${it.message}" } ?: ""

            val fullMessage = "[$level] $tagStr: $messageStr$throwableStr"
            NSLog(fullMessage)
        } catch (e: Exception) {
            // Fallback: if NSLog fails, don't crash the app
            platform.Foundation.NSLog("NSLogAntilog error: ${e.message}")
        }
    }
}