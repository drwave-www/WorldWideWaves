package com.worldwidewaves.shared.data

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

import android.content.Context
import com.worldwidewaves.shared.WWWGlobals.Companion.FS_DATASTORE_FOLDER
import org.koin.java.KoinJavaComponent.inject

/**
 * Retrieves the file path for the key-value store.
 *
 * This function constructs the file path for the key-value store by accessing the application's
 * files directory and appending the specified folder and file name for the data store.
 *
 */
actual fun keyValueStorePath(): String {
        val context: Context by inject(Context::class.java)
        return context
                .filesDir
                .resolve("$FS_DATASTORE_FOLDER/$dataStoreFileName")
                .absolutePath
}