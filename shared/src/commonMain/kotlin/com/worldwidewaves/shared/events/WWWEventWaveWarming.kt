package com.worldwidewaves.shared.events

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

import com.worldwidewaves.shared.events.utils.DataValidator
import kotlinx.serialization.Serializable

@Serializable
data class WWWEventWaveWarming(
    val type: String,
    val longitude: Double? = null,
) : DataValidator {

    override fun validationErrors(): List<String>? = mutableListOf<String>().apply {
        when {
            type == "longitude-cut" && longitude == null ->
                add("Longitude must not be null for type 'longitude-cut'")

            type == "longitude-cut" && (longitude!! < -180 || longitude > 180) ->
                add("Longitude must be between -180 and 180")

            else -> {}
        }
    }.takeIf { it.isNotEmpty() }?.map { "warming: $it" }

}