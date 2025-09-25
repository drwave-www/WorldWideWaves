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
 * iOS Antilog implementation that uses NSLog for Apple's Unified Logging.
 * Logs should be visible via:
 * - xcrun simctl spawn <UDID> log stream --level debug --predicate 'processImagePath CONTAINS[c] "WorldWideWaves"'
 * - xcrun simctl spawn <UDID> log show --style compact --last 2m --debug --info --predicate 'processImagePath CONTAINS[c] "WorldWideWaves"'
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
            val lvl = priority.name
            val tagOr = tag ?: "LOG"
            val err = throwable?.let { " | ${it::class.simpleName}: ${it.message}" } ?: ""
            platform.Foundation.NSLog("%@ %@: %@%@", lvl, tagOr, message ?: "no message", err)
        } catch (e: Throwable) {
            // Fallback - don't crash the app
        }
    }
}