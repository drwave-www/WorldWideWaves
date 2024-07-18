package com.worldwidewaves.shared.events

import kotlinx.serialization.*

@Serializable
data class WWWEvent(
    val id: String,
    val type: String,
    val location: String,
    val country: String? = null,
    val community: String? = null,
    val date: String,
    val startHour: String,
    val speed: Int
)

fun WWWEvent.isDone(): Boolean {
    return this.id == "paris_france" // test
}

fun WWWEvent.isSoon(): Boolean {
    return this.id == "unitedstates" // test
}

fun WWWEvent.isRunning(): Boolean {
    return this.id == "riodejaneiro_brazil" // test
}
