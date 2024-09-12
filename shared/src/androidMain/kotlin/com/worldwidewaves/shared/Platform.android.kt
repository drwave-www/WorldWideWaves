package com.worldwidewaves.shared

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

object AndroidPlatform : WWWPlatform  { // TODO: manage with the cache in production on app update
    private var _contextRef: WeakReference<Context>? = null

    private val context: Context
        get() = _contextRef?.get()
            ?: throw UninitializedPropertyAccessException(
                "$(::AndroidPlatform.name) must be initialized with a context before use.")

    override val name: String
        get() = "Android ${Build.VERSION.SDK_INT}"

    override fun getContext(): Any = context

    fun initialize(context: Context): AndroidPlatform {
        debugBuild()
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

/**
 * Retrieves the absolute path of a map file for a given event.
 *
 * This function attempts to get the absolute path of a map file (e.g., MBTiles, GeoJSON) associated with the event.
 * It first checks if the file is already cached in the device's cache directory. If the file is not cached or the
 * cached file size does not match the expected size, it reads the file from the resources and caches it.
 *
 */
@OptIn(ExperimentalResourceApi::class)
actual suspend fun getMapFileAbsolutePath(eventId: String, extension: String): String? {
    val context = AndroidPlatform.getContext() as Context
    val cachedFile = File(context.cacheDir, "$eventId.$extension")

    return try {
        Log.i(::getMapFileAbsolutePath.name, "Trying to get $eventId.$extension")
        val fileBytes = Res.readBytes("$FS_MAPS_FOLDER/$eventId.$extension")
        if (!cachedFile.exists() || cachedFile.length().toInt() != fileBytes.size) {
            Log.i(::getMapFileAbsolutePath.name, "Caching $eventId.$extension")
            cachedFile.outputStream().use { it.write(fileBytes) }
        }
        cachedFile.absolutePath
    } catch (e: MissingResourceException) {
        Log.e(::getMapFileAbsolutePath.name, "Resource not found: ${e.message}")
        null
    }
}

// ---------------------------

/**
 * Checks if a cached file exists in the application's cache directory.
 *
 * This function determines whether a file with the specified name exists in the cache directory.
 * It also considers whether the application is running in development mode, in which case it always
 * returns `false` to simulate the absence of cached files.
 *
 */
actual fun cachedFileExists(fileName: String): Boolean {
    val context = AndroidPlatform.getContext() as Context
    val isDevelopmentMode = Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish"

    return if (isDevelopmentMode) {
        Log.i(::cachedFileExists.name, "Development mode (not cached): $fileName")
        false
    } else {
        File(context.cacheDir, fileName).exists()
    }
}

/**
 * Retrieves the absolute path of a cached file if it exists.
 *
 * This function checks if a file with the given name exists in the cache directory of the Android context.
 * If the file exists, it returns the absolute path of the file as a string. If the file does not exist,
 * it returns `null`.
 *
 */
actual fun cachedFilePath(fileName: String): String? {
    val context = AndroidPlatform.getContext() as Context
    return File(context.cacheDir, fileName).takeIf { it.exists() }?.toURI()?.path
}

/**
 * Caches a string content to a file in the application's cache directory.
 *
 * This function writes the provided string content to a file with the specified name
 * in the cache directory of the application. It logs the file name to which the data
 * is being cached.
 *
 */
actual fun cacheStringToFile(fileName: String, content: String) : String {
    val context = AndroidPlatform.getContext() as Context
    Log.i(::cacheStringToFile.name, "Caching data to $fileName")
    File(context.cacheDir, fileName).writeText(content)
    return fileName
}

/**
 * Caches a file from the application's resources to the device's cache directory.
 *
 * This function reads the bytes of a file from the application's resources and writes them to a file
 * in the device's cache directory. If the cache directory does not exist, it is created. If an error
 * occurs during the process, it is logged.
 *
 */
@OptIn(ExperimentalResourceApi::class)
actual suspend fun cacheDeepFile(fileName: String) {
    try {
        val context = AndroidPlatform.getContext() as Context
        val fileBytes = Res.readBytes(fileName)
        val cacheFile = File(context.cacheDir, fileName)

        Log.i(::cacheDeepFile.name, "Caching data to $cacheFile")

        cacheFile.parentFile?.mkdirs()
        cacheFile.outputStream().use { it.write(fileBytes) }
    } catch (e: Exception) {
        Log.e(::cacheDeepFile.name, "Error caching file: $fileName", e)
    }
}

/**
 * Retrieves the absolute path to the cache directory on the Android platform.
 *
 * This function uses the Android context to access the cache directory and returns its absolute path.
 * The cache directory is a location where the application can store temporary files.
 *
 */
actual fun getCacheDir(): String {
    val context = AndroidPlatform.getContext() as Context
    return context.cacheDir.absolutePath
}

// ---------------------------

/**
 * Retrieves the current local date and time.
 *
 * This function uses the system clock to get the current instant in time and converts it to a
 * `LocalDateTime` object using the system's default time zone.
 *
 */
actual fun getLocalDatetime(): LocalDateTime =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
