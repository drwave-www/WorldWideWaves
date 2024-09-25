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

import androidx.annotation.VisibleForTesting
import com.worldwidewaves.shared.events.utils.GeoUtils.normalizeLongitude
import com.worldwidewaves.shared.events.utils.Position.Companion.nextId

// ----------------------------------------------------------------------------

/**
 * Represents a geographic position with latitude and longitude coordinates.
 *
 */
open class Position(val lat: Double, val lng: Double, // Element of the double LL Polygon
                    internal var prev: Position? = null,
                    internal var next: Position? = null) {

    var id: Int = -1
        internal set // Cannot be set outside of the module
        get() { // Cannot be read before being initialized (added to a Polygon)
            if (field == -1)
                throw IllegalStateException("ID has not been initialized")
            return field
        }

    // ------------------------

    companion object { internal var nextId = 42 }

    // ------------------------

    open operator fun component1() = lat
    open operator fun component2() = lng

    // ------------------------

    fun toCutPosition(cutId: Int, cutLeft: Position, cutRight: Position) =
        CutPosition(lat, lng, cutId, cutLeft, cutRight).init()

    fun toPointCut(cutId: Int) =
        CutPosition(lat, lng, cutId, this, this).init()

    fun copy(lat: Double? = null, lng: Double? = null): Position {
        return Position(lat ?: this.lat, lng ?: this.lng)
    }

    // ------------------------

    internal open fun xfer() = Position(lat, lng).init() // Polygon detach / reattach
    internal open fun detached() = Position(lat, lng)
    fun normalized() = Position(lat,normalizeLongitude(lng))

    // ------------------------

    override fun equals(other: Any?): Boolean =
        this === other || (other is Position && lat == other.lat && lng == other.lng)
    override fun toString(): String = "($lat, $lng)"
    override fun hashCode(): Int = 31 * lat.hashCode() + lng.hashCode()
}

// Can only be initialized from internal context
@VisibleForTesting
internal fun <T : Position> T.init(): T = apply {
    id = nextId++
    require(id != Int.MAX_VALUE) { "Reached maximum capacity for Polygon positions" }
}

// ------------------

class CutPosition( // A position that has been cut
    lat: Double, lng: Double,
    val cutId: Int, val cutLeft: Position, val cutRight: Position
) : Position(lat, lng) {

    // Id which is shared/can be compared between the two cut positions
    val pairId: Double by lazy { (cutId + listOf(cutLeft.id, cutRight.id).sorted().let { (first, second) ->
        ((first shl 4) + (second shr 5)).toDouble()
    }) }

    val isPointCut by lazy { cutLeft == cutRight }
    val isPointOnLine by lazy { cutLeft == this || cutRight == this}

    override fun xfer() = CutPosition(lat, lng, cutId, cutLeft, cutRight).init()

    override fun equals(other: Any?): Boolean =
        this === other || (other is Position && super.equals(other) && if (other is CutPosition) cutId == other.cutId else true)
    override fun hashCode(): Int = 31 * super.hashCode() + cutId.hashCode()
}