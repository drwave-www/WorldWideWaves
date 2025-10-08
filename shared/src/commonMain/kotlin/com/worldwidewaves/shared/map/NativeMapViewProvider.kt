package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.events.IWWWEvent

/**
 * Platform-specific provider for native map views.
 * The iOS app provides the implementation via Koin DI.
 */
interface NativeMapViewProvider {
    /**
     * Creates a platform-native map view controller/view.
     *
     * @param event The event to display
     * @param styleURL The map style URL (mbtiles:// or https://)
     * @param enableGestures Whether to enable zoom/scroll gestures (matches Android activateMapGestures)
     * @return Platform-specific view (UIViewController on iOS, View on Android)
     */
    fun createMapView(
        event: IWWWEvent,
        styleURL: String,
        enableGestures: Boolean = true,
    ): Any

    /**
     * Gets the map wrapper instance for an event (iOS only).
     * On Android, this returns null.
     *
     * @param eventId The event ID
     * @return The map wrapper (MapLibreViewWrapper on iOS) or null
     */
    fun getMapWrapper(eventId: String): Any? = null
}
