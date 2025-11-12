package com.worldwidewaves.shared.events.utils

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
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

import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.events.utils.Position.Companion.nextId

// ----------------------------------------------------------------------------

/**
 * Represents a geographic position with latitude and longitude coordinates.
 *
 * Coordinates are validated to ensure they fall within valid geographic bounds:
 * - Latitude must be within [-90.0, 90.0]
 * - Longitude must be within [-180.0, 180.0]
 * - Neither coordinate can be NaN or Infinity
 *
 * @throws IllegalArgumentException if coordinates are invalid
 */
open class Position(
    val lat: Double,
    val lng: Double, // Element of the double LL Polygon
    internal var prev: Position? = null,
    internal var next: Position? = null,
) {
    init {
        // Validate latitude is within valid range [-90, 90]
        require(lat in WWWGlobals.Geodetic.MIN_LATITUDE..WWWGlobals.Geodetic.MAX_LATITUDE) {
            "Invalid latitude: $lat (must be between ${WWWGlobals.Geodetic.MIN_LATITUDE} and ${WWWGlobals.Geodetic.MAX_LATITUDE})"
        }

        // Validate longitude is within valid range [-180, 180]
        require(lng in WWWGlobals.Geodetic.MIN_LONGITUDE..WWWGlobals.Geodetic.MAX_LONGITUDE) {
            "Invalid longitude: $lng (must be between ${WWWGlobals.Geodetic.MIN_LONGITUDE} and ${WWWGlobals.Geodetic.MAX_LONGITUDE})"
        }

        // Validate coordinates are not NaN
        require(!lat.isNaN() && !lng.isNaN()) {
            "Invalid coordinates: lat=$lat, lng=$lng (coordinates cannot be NaN)"
        }

        // Validate coordinates are finite (not Infinity)
        require(lat.isFinite() && lng.isFinite()) {
            "Invalid coordinates: lat=$lat, lng=$lng (coordinates must be finite)"
        }
    }

    companion object {
        internal var nextId = 42
    }

    var id: Int = -1
        internal set // Cannot be set outside of the module
        get() { // Cannot be read before being initialized (added to a Polygon)
            check(field != -1) { "ID has not been initialized" }
            return field
        }

    val latitude: Double get() = lat
    val longitude: Double get() = lng

    open operator fun component1() = lat

    open operator fun component2() = lng

    // ------------------------

    fun toCutPosition(
        cutId: Int,
        cutLeft: Position,
        cutRight: Position,
    ) = CutPosition(lat, lng, cutId, cutLeft, cutRight).init()

    fun copy(
        lat: Double? = null,
        lng: Double? = null,
    ): Position = Position(lat ?: this.lat, lng ?: this.lng)

    // ------------------------

    internal open fun xfer() = Position(lat, lng).init() // Polygon detach / reattach

    internal open fun detached() = Position(lat, lng)

    // ------------------------

    override fun equals(other: Any?): Boolean = this === other || (other is Position && lat == other.lat && lng == other.lng)

    override fun toString(): String = "($lat, $lng)"

    override fun hashCode(): Int = 31 * lat.hashCode() + lng.hashCode()
}

// Can only be initialized from internal context
internal fun <T : Position> T.init(): T =
    apply {
        id = nextId++
        require(id != Int.MAX_VALUE) { "Reached maximum capacity for Polygon positions" }
    }

// ------------------

class CutPosition( // A position that has been cut
    lat: Double,
    lng: Double,
    val cutId: Int,
    val cutLeft: Position,
    val cutRight: Position,
) : Position(lat, lng) {
    // Id which is shared/can be compared between the two cut positions
    val pairId: Double by lazy {
        (
            cutId +
                listOf(cutLeft.id, cutRight.id).sorted().let { (first, second) ->
                    ((first shl 4) + (second shr 5)).toDouble()
                }
        )
    }

    override fun xfer() = CutPosition(lat, lng, cutId, cutLeft, cutRight).init()

    override fun equals(other: Any?): Boolean =
        this === other || (other is Position && super.equals(other) && if (other is CutPosition) cutId == other.cutId else true)

    override fun hashCode(): Int = 31 * super.hashCode() + cutId.hashCode()
}
