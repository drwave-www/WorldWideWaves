package com.worldwidewaves.shared.utils

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

import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.e_choreography_hit
import com.worldwidewaves.shared.generated.resources.e_choreography_waiting
import com.worldwidewaves.shared.generated.resources.e_choreography_warming_seq_1
import com.worldwidewaves.shared.generated.resources.e_choreography_warming_seq_2
import com.worldwidewaves.shared.generated.resources.e_choreography_warming_seq_3
import com.worldwidewaves.shared.generated.resources.e_choreography_warming_seq_4
import com.worldwidewaves.shared.generated.resources.e_choreography_warming_seq_5
import com.worldwidewaves.shared.generated.resources.e_choreography_warming_seq_6
import com.worldwidewaves.shared.generated.resources.transparent
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.DrawableResource

object IOSDrawableResources {
    // Mapping of path names to resource IDs - same as Android for cross-platform compatibility
    private val resourceMap =
        mapOf(
            "e_choreography_warming_seq_1" to Res.drawable.e_choreography_warming_seq_1,
            "e_choreography_warming_seq_2" to Res.drawable.e_choreography_warming_seq_2,
            "e_choreography_warming_seq_3" to Res.drawable.e_choreography_warming_seq_3,
            "e_choreography_warming_seq_4" to Res.drawable.e_choreography_warming_seq_4,
            "e_choreography_warming_seq_5" to Res.drawable.e_choreography_warming_seq_5,
            "e_choreography_warming_seq_6" to Res.drawable.e_choreography_warming_seq_6,
            "e_choreography_waiting" to Res.drawable.e_choreography_waiting,
            "e_choreography_hit" to Res.drawable.e_choreography_hit,
        )

    // Get resource ID by path, with fallback to transparent
    fun getResource(path: String): DrawableResource? = resourceMap[path] ?: Res.drawable.transparent
}

/**
 * iOS implementation of ImageResolver using DrawableResource for cross-platform compatibility.
 * This matches the Android implementation to ensure choreography works consistently.
 */
class IOSImageResolver : ImageResolver<DrawableResource> {
    /**
     * Resolves a resource path to a DrawableResource for cross-platform compatibility.
     *
     * @param path The resource path to resolve.
     * @return The DrawableResource for the resource, or null if not found.
     */
    override fun resolve(path: String): DrawableResource? {
        try {
            val resource = IOSDrawableResources.getResource(path)
            if (resource == null) {
                Napier.e("Failed to resolve choreography image: $path")
            } else {
                Napier.d("Successfully resolved choreography image: $path")
            }
            return resource
        } catch (e: Exception) {
            Napier.e("Error resolving choreography image $path: ${e.message}")
            return null
        }
    }
}
