package com.worldwidewaves.shared.utils.extensions

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

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Updates the value of this MutableStateFlow only if the new value is different from the current value.
 * This helps prevent unnecessary emissions and observers from being triggered when the value hasn't changed.
 *
 * @param newValue The new value to set if it's different from the current value
 */
fun <T> MutableStateFlow<T>.updateIfChanged(newValue: T) {
    if (value != newValue) {
        value = newValue
    }
}
