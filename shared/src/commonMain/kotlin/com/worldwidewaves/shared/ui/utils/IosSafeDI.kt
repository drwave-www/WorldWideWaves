package com.worldwidewaves.shared.ui.utils

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.utils.IClock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * iOS-Safe dependency injection for Compose components.
 *
 * ⚠️ CRITICAL: This object resolves dependencies ONCE and caches them,
 * preventing the dangerous pattern of calling inject() during Compose composition.
 */
object IosSafeDI : KoinComponent {
    // Pre-resolved dependencies to avoid inject() during composition
    val platform: WWWPlatform by inject()
    val clock: IClock by inject()
}

/**
 * iOS-Safe dependency resolution functions for Composables.
 * These resolve dependencies outside of the composition phase.
 */
fun getIosSafePlatform(): WWWPlatform = IosSafeDI.platform

fun getIosSafeClock(): IClock = IosSafeDI.clock
