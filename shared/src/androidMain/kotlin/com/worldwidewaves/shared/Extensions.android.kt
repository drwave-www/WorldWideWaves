package com.worldwidewaves.shared

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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

import com.worldwidewaves.shared.events.utils.BoundingBox
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds

/**
 * Converts a `BoundingBox` to a `LatLngBounds`.
 *
 * This function creates a `LatLngBounds` object from the southwest and northeast corners
 * of the `BoundingBox`. The resulting `LatLngBounds` can be used with MapLibre to define
 * the geographical bounds of a map view.
 *
 * @receiver The `BoundingBox` to convert.
 * @return A `LatLngBounds` object representing the same geographical area as the `BoundingBox`.
 */
fun BoundingBox.toLatLngBounds(): LatLngBounds {
    return LatLngBounds.Builder()
        .include(LatLng(this.sw.lat, this.sw.lng)) // Southwest corner
        .include(LatLng(this.ne.lat, this.ne.lng)) // Northeast corner
        .build()
}