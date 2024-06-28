package com.worldwidewaves.shared

import kotlinx.serialization.*

@Serializable
data class WWWEvent(
    val id: String,
    val type: String,
    val location: String,
    val community: String? = null,
    val date: String,
    val startHour: String
)
