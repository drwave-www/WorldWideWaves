package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.events.IWWWEvent

/**
 * Android implementation - not used since AndroidEventMap directly creates MapView
 */
actual fun createNativeMapViewController(
    event: IWWWEvent,
    styleURL: String,
    enableGestures: Boolean,
): Any = throw UnsupportedOperationException("Android uses AndroidEventMap with MapView directly")
