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

import io.github.aakira.napier.Napier

/**
 * iOS implementation of Napier initialization.
 * Uses OSLogAntilog which outputs to Apple's Unified Logging system,
 * making logs visible via xcrun simctl log commands.
 */
actual fun initNapier() {
    if (LogConfig.ENABLE_DEBUG_LOGGING) {
        Napier.base(OSLogAntilog())
    } else {
        Napier.takeLogarithm()
    }
}
