package com.worldwidewaves.shared.data

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
import android.os.Build
import android.util.Log
import com.google.android.play.core.splitcompat.SplitCompat
import com.worldwidewaves.shared.domain.usecases.MapAvailabilityChecker
import com.worldwidewaves.shared.events.utils.GeoJsonDataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException

private const val TAG = "MapStore"

private fun ctx(): Context = inject<Context>(Context::class.java).value

// ---- platform shims ----
actual fun platformCacheRoot(): String = ctx().cacheDir.absolutePath

actual fun platformFileExists(path: String) = File(path).exists()

actual fun platformReadText(path: String) = File(path).readText()

actual fun platformWriteText(
    path: String,
    content: String,
) {
    File(path).writeText(content)
}

actual fun platformDeleteFile(path: String) {
    runCatching { File(path).delete() }
}

actual fun platformEnsureDir(path: String) {
    File(path).mkdirs()
}

actual fun platformAppVersionStamp(): String =
    // changes on app update AND on dynamic feature split updates
    runCatching {
        ctx()
            .packageManager
            .getPackageInfo(ctx().packageName, 0)
            .lastUpdateTime
            .toString()
    }.getOrElse { System.currentTimeMillis().toString() }

actual fun platformInvalidateGeoJson(eventId: String) {
    runCatching { inject<GeoJsonDataProvider>(GeoJsonDataProvider::class.java).value.invalidateCache(eventId) }
        .onFailure { Log.w(TAG, "GeoJSON invalidate failed for $eventId: ${it.message}") }
}

actual suspend fun platformFetchToFile(
    eventId: String,
    extension: String,
    destAbsolutePath: String,
): Boolean =
    withContext(Dispatchers.IO) {
        Log.d(TAG, "platformFetchToFile: Fetching $eventId.$extension to $destAbsolutePath")

        val mapChecker: MapAvailabilityChecker by inject(MapAvailabilityChecker::class.java)
        if (!mapChecker.isMapDownloaded(eventId)) {
            Log.w(TAG, "platformFetchToFile: Map $eventId not downloaded, aborting")
            return@withContext false
        }

        val context: Context by inject(Context::class.java)
        val assetName = "$eventId.$extension"

        var last: Exception? = null
        var success = false

        for (attempt in 0 until 3) {
            try {
                Log.d(TAG, "platformFetchToFile: Attempt ${attempt + 1}/3 for $assetName")

                val base = context
                val splitCtx =
                    if (Build.VERSION.SDK_INT >= 26) {
                        runCatching { base.createContextForSplit(eventId) }.getOrElse { base }
                    } else {
                        base
                    }

                SplitCompat.install(splitCtx)

                val destFile = File(destAbsolutePath)
                destFile.parentFile?.mkdirs()

                splitCtx.assets.open(assetName).use { input ->
                    BufferedInputStream(input, 64 * 1024).use { bin ->
                        destFile.outputStream().use { out ->
                            BufferedOutputStream(out, 64 * 1024).use { bout ->
                                val buf = ByteArray(64 * 1024)
                                var n: Int
                                while (bin.read(buf).also { n = it } != -1) {
                                    bout.write(buf, 0, n)
                                }
                                bout.flush()
                            }
                        }
                    }
                }

                success = true
                Log.i(TAG, "platformFetchToFile: Successfully fetched $assetName")
                break
            } catch (e: FileNotFoundException) {
                last = e
                Log.w(TAG, "platformFetchToFile: Asset $assetName not found (attempt ${attempt + 1}/3)")
                if (attempt < 2) delay(120)
            } catch (e: Exception) {
                last = e
                break
            }
        }

        if (!success) {
            Log.e(TAG, "platformFetchToFile: Failed to fetch $eventId.$extension after 3 attempts: ${last?.message}")
        }
        success
    }

actual fun cacheStringToFile(
    fileName: String,
    content: String,
): String {
    val root = platformCacheRoot()
    val f = File(root, fileName)
    f.parentFile?.mkdirs()
    Log.v("cacheStringToFile", "Caching data to $fileName")
    f.writeText(content)
    return fileName
}
