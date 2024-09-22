package com.worldwidewaves.shared.events.utils

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

/**
 * Represents a bounding box defined by its southwest and northeast corners.
 *
 */
data class BoundingBox(val sw: Position, val ne: Position) {
    constructor(swLat: Double, swLng: Double, neLat: Double, neLng: Double) : this(
        sw = Position(swLat, swLng).init(),
        ne = Position(neLat, neLng).init()
    )

    val minLatitude: Double get() = sw.lat
    val maxLatitude: Double get() = ne.lat
    val minLongitude: Double get() = sw.lng
    val maxLongitude: Double get() = ne.lng

    override fun equals(other: Any?): Boolean =
        this === other || (other is BoundingBox && sw == other.sw && ne == other.ne)

    override fun hashCode(): Int = 31 * sw.hashCode() + ne.hashCode()
}