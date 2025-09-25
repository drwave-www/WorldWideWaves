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

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

/**
 * Android implementation of Napier initialization.
 * Uses DebugAntilog which outputs to Android Logcat.
 */
actual fun initNapier() {
    if (LogConfig.ENABLE_DEBUG_LOGGING) {
        Napier.base(DebugAntilog())
    } else {
        Napier.takeLogarithm()
    }
}