package com.worldwidewaves.shared

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
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

import android.location.Location
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.close
import kotlin.time.Instant
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.geojson.Point
import kotlin.time.ExperimentalTime

/**
 * Converts a `BoundingBox` to a `LatLngBounds`.
 *
 * This function creates a `LatLngBounds` object from the southwest and northeast corners
 * of the `BoundingBox`. The resulting `LatLngBounds` can be used with MapLibre to define
 * the geographical bounds of a map view.
 *
 */
fun BoundingBox.toLatLngBounds(): LatLngBounds = LatLngBounds.Builder()
        .include(LatLng(this.sw.lat, this.sw.lng)) // Southwest corner
        .include(LatLng(this.ne.lat, this.ne.lng)) // Northeast corner
        .build()

@OptIn(ExperimentalTime::class)
fun Position.toLocation(now: Instant): Location {
        val location = Location("custom_provider")
        location.latitude = this.lat
        location.longitude = this.lng
        location.altitude = 0.0
        location.time = now.toEpochMilliseconds()
        return location
}

fun LatLng.toPosition(): Position = Position(this.latitude, this.longitude)

fun Polygon.toMapLibrePolygon() : org.maplibre.geojson.Polygon {
        close()
        return org.maplibre.geojson.Polygon.fromLngLats(
                listOf (map { Point.fromLngLat(it.lng, it.lat) })
        )
}