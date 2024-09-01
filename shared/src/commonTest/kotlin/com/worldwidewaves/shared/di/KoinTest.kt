package com.worldwidewaves.shared.di

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

import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.data.createDataStore
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import kotlin.test.Test

class KoinTest : KoinTest {

    class MockWWWPlatform : WWWPlatform() {
        override val name: String = "MockPlatform"
        override fun getContext(): Any = "MockContext"
    }

    // ---------------------------

    @Test
    fun `check MVP hierarchy`() {
        val testPlatformModule = module {
            single<WWWPlatform> { MockWWWPlatform() }
            single { createDataStore { "/fake/path" } }
        }

        checkModules {
            modules(sharedModule() + testPlatformModule)
        }
    }

}