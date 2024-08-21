package com.worldwidewaves.shared

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

import android.content.Context
import android.os.Build
import android.util.Log
import com.worldwidewaves.shared.WWWGlobals.Companion.FS_MAPS_FOLDER
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.e_community_europe
import com.worldwidewaves.shared.generated.resources.e_community_usa
import com.worldwidewaves.shared.generated.resources.e_country_brazil
import com.worldwidewaves.shared.generated.resources.e_country_france
import com.worldwidewaves.shared.generated.resources.e_location_paris_france
import com.worldwidewaves.shared.generated.resources.e_location_riodejaneiro_brazil
import com.worldwidewaves.shared.generated.resources.e_location_unitedstates
import com.worldwidewaves.shared.generated.resources.e_location_world
import com.worldwidewaves.shared.generated.resources.not_found
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException
import java.io.File
import java.lang.ref.WeakReference

// --- Platform-specific implementation of the WWWPlatform interface ---

object AndroidPlatform : WWWPlatform  {
    private var _contextRef: WeakReference<Context>? = null

    // private var events : Lazy<WWWEvents> = lazy { WWWEvents() }

    private val context: Context
        get() = _contextRef?.get()
            ?: throw UninitializedPropertyAccessException(
                "AndroidPlatform must be initialized with a context before use.")

    override val name: String
        get() = "Android ${Build.VERSION.SDK_INT}"

    override fun getContext(): Any = context

    fun initialize(context: Context): AndroidPlatform {
        _contextRef = WeakReference(context.applicationContext)
        return this
    }

}

// ---------------------------

actual fun getPlatform(): WWWPlatform = AndroidPlatform

actual fun getEventImage(type: String, id: String): Any? {
    return when (type) {
        "location" -> when (id) {
            "paris_france" -> Res.drawable.e_location_paris_france
            "unitedstates" -> Res.drawable.e_location_unitedstates
            "riodejaneiro_brazil" -> Res.drawable.e_location_riodejaneiro_brazil
            "world" -> Res.drawable.e_location_world
            else -> Res.drawable.not_found
        }

        "community" -> when (id) {
            "europe" -> Res.drawable.e_community_europe
            "usa" -> Res.drawable.e_community_usa
            else -> Res.drawable.not_found
        }

        "country" -> when (id) {
            "brazil" -> Res.drawable.e_country_brazil
            "france" -> Res.drawable.e_country_france
            else -> Res.drawable.not_found
        }

        else -> Res.drawable.not_found
    }
}

// ---------------------------

@OptIn(ExperimentalResourceApi::class)
actual suspend fun getMapFileAbsolutePath(eventId: String, extension: String): String? {
    val context = AndroidPlatform.getContext() as Context
    val cachedFile = File(context.cacheDir, "$eventId.$extension")

    return try {
        Log.i("getMBTilesAbsoluteFilePath", "Trying to get $eventId.$extension")
        val fileBytes = Res.readBytes("$FS_MAPS_FOLDER/$eventId.$extension")
        if (cachedFile.exists() && cachedFile.length().toInt() == fileBytes.size) {
            cachedFile.absolutePath
        } else {
            Log.i("getMBTilesAbsoluteFilePath", "Caching $eventId.$extension")
            cachedFile.outputStream().use { it.write(fileBytes) }
            cachedFile.absolutePath
        }
    } catch (e: MissingResourceException) {
        Log.e("getMBTilesAbsoluteFilePath", "Resource not found: ${e.message}")
        null
    }

}

// ---------------------------

actual fun cachedFileExists(fileName: String): Boolean {
    val context = AndroidPlatform.getContext() as Context
    val isDevelopmentMode = Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish"
    return if (isDevelopmentMode) {
        Log.i("cachedFileExists", "Development mode (not cached): $fileName")
        false
    } else {
        File(context.cacheDir, fileName).exists()
    }
}

actual fun cachedFilePath(fileName: String): String? {
    val context = AndroidPlatform.getContext() as Context
    return File(context.cacheDir, fileName).takeIf { it.exists() }?.toURI()?.path
}

actual fun cacheStringToFile(fileName: String, content: String) {
    val context = AndroidPlatform.getContext() as Context
    Log.i("cacheStringToFile", "Caching data to $fileName")
    File(context.cacheDir, fileName).writeText(content)
}

// ---------------------------

actual fun getLocalDatetime(): LocalDateTime {
    return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
}