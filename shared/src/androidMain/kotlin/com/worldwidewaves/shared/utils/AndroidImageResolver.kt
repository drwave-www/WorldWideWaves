package com.worldwidewaves.shared.utils

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

import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource

object DrawableResources {
    // Mapping of path names to resource IDs
    private val resourceMap = mapOf(
        "wave_warmup_1" to Res.drawable.wave_warmup_1,
        "wave_warmup_2" to Res.drawable.wave_warmup_2,
        "wave_warmup_3" to Res.drawable.wave_warmup_3,
        "wave_warmup_4" to Res.drawable.wave_warmup_4,
        "wave_warmup_5" to Res.drawable.wave_warmup_5,
        "wave_warmup_6" to Res.drawable.wave_warmup_6,
        "wave_warmup_7" to Res.drawable.wave_warmup_7,
        "wave_warmup_8" to Res.drawable.wave_warmup_8,
        "wave_warmup_9" to Res.drawable.wave_warmup_9,
        "wave_waiting_1" to Res.drawable.wave_waiting_1,
        "wave_waiting_2" to Res.drawable.wave_waiting_2,
        "wave_waiting_3" to Res.drawable.wave_waiting_3,
        "wave_hit_1" to Res.drawable.wave_hit_1,
        "wave_hit_2" to Res.drawable.wave_hit_2,
        "wave_hit_3" to Res.drawable.wave_hit_3
    )

    // Get resource ID by path, with fallback to transparent
    fun getResource(path: String): DrawableResource? {
        return resourceMap[path]
    }
}

class AndroidImageResolver : ImageResolver<DrawableResource> {
    override fun resolve(path: String): DrawableResource? {
        return DrawableResources.getResource(path)
    }
}