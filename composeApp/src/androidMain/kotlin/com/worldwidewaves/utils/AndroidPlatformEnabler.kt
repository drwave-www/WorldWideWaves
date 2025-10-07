package com.worldwidewaves.utils

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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.worldwidewaves.activities.event.EventActivity
import com.worldwidewaves.activities.event.EventFullMapActivity
import com.worldwidewaves.activities.event.WaveActivity
import com.worldwidewaves.shared.PlatformEnabler
import com.worldwidewaves.shared.utils.Log
import org.koin.mp.KoinPlatform

class AndroidPlatformEnabler(
    val context: Context? = null,
) : PlatformEnabler {
    override fun openEventActivity(eventId: String) {
        val context: Context = context ?: KoinPlatform.getKoin().get()
        context.startActivity(
            Intent(context, EventActivity::class.java).apply {
                putExtra("eventId", eventId)
            },
        )
    }

    override fun openWaveActivity(eventId: String) {
        val context: Context = context ?: KoinPlatform.getKoin().get()
        context.startActivity(
            Intent(context, WaveActivity::class.java).apply {
                putExtra("eventId", eventId)
            },
        )
    }

    override fun openFullMapActivity(eventId: String) {
        val context: Context = context ?: KoinPlatform.getKoin().get()
        context.startActivity(
            Intent(context, EventFullMapActivity::class.java).apply {
                putExtra("eventId", eventId)
            },
        )
    }

    override fun toast(message: String) {
        val context: Context = context ?: KoinPlatform.getKoin().get()
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @Composable
    override fun OpenUrl(url: String) {
        LaunchedEffect(url) {
            openUrl(url)
        }
    }

    override fun openUrl(url: String) {
        try {
            val context: Context = context ?: KoinPlatform.getKoin().get()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AndroidPlatformEnabler", "Failed to open URL: $url", throwable = e)
        }
    }
}
