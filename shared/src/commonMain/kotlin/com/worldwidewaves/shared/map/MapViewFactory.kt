package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.events.IWWWEvent

/**
 * Platform-specific factory for creating native map view controllers.
 * This allows Compose UI to embed platform-native map views.
 */
expect fun createNativeMapViewController(
    event: IWWWEvent,
    styleURL: String,
    enableGestures: Boolean = true,
): Any // Returns UIViewController on iOS, View on Android
