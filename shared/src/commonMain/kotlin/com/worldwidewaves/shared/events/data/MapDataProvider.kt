package com.worldwidewaves.shared.events.data

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

import com.worldwidewaves.shared.WWWGlobals.FileSystem
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi

interface MapDataProvider {
    suspend fun geoMapStyleData(): String
}

class DefaultMapDataProvider : MapDataProvider {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun geoMapStyleData(): String =
        withContext(Dispatchers.IO) {
            Log.i(::geoMapStyleData.name, "Loading map style template from ${FileSystem.MAPS_STYLE}")
            val bytes = Res.readBytes(FileSystem.MAPS_STYLE)
            Log.d(::geoMapStyleData.name, "Read ${bytes.size} bytes from style template")
            val result = bytes.decodeToString()
            Log.i(::geoMapStyleData.name, "Style template decoded: ${result.length} chars")
            result
        }
}
