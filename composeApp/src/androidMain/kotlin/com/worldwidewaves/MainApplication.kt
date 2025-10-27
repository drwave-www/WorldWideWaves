package com.worldwidewaves

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

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import com.google.android.play.core.splitcompat.SplitCompat
import com.worldwidewaves.di.applicationModule
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.WWWShutdownHandler
import com.worldwidewaves.shared.di.androidModule
import com.worldwidewaves.shared.di.initializeSimulationMode
import com.worldwidewaves.shared.di.sharedModule
import com.worldwidewaves.shared.utils.CloseableCoroutineScope
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.utils.RuntimeLogConfig
import com.worldwidewaves.shared.utils.initNapier
import com.worldwidewaves.shared.utils.setupDebugSimulation
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import kotlin.time.ExperimentalTime

open class MainApplication :
    Application(),
    Configuration.Provider {
    private val wwwShutdownHandler: WWWShutdownHandler by inject()

    // ---------------------------------------------------------------------
    // Install SplitCompat as early as possible so that assets from newly
    // downloaded dynamic-feature modules (maps) are immediately visible.
    // ---------------------------------------------------------------------
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        SplitCompat.install(this)
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setMinimumLoggingLevel(
                    if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR,
                ).build()

    @OptIn(ExperimentalTime::class)
    override fun onCreate() {
        super.onCreate()

        // Initialize Napier logging for Android
        initNapier()

        // Ensure split compat is installed
        SplitCompat.install(this)

        startKoin {
            androidContext(this@MainApplication)
            androidLogger()
            modules(sharedModule + androidModule + applicationModule)
        }

        // -------------------------------------------------------------------- //
        //  Simulation mode initialization for Firebase Test Lab UI testing
        // -------------------------------------------------------------------- //
        val platform = get<WWWPlatform>()
        initializeSimulationMode(platform, BuildConfig.ENABLE_SIMULATION_MODE)

        // -------------------------------------------------------------------- //
        //  Default simulation initialization (runs after Koin properties are ready)
        // -------------------------------------------------------------------- //
        setupDebugSimulation()

        // -------------------------------------------------------------------- //
        //  Initialize runtime log configuration (async, non-blocking)
        // -------------------------------------------------------------------- //
        MainScope().launch {
            try {
                RuntimeLogConfig.initialize()
            } catch (e: Exception) {
                Log.w(
                    "MainApplication",
                    "Failed to initialize RuntimeLogConfig, using defaults",
                    e,
                )
            }
        }
    }

    override fun onTerminate() {
        wwwShutdownHandler.onAppShutdown()
        get<CloseableCoroutineScope>().close()
        super.onTerminate()
    }
}
