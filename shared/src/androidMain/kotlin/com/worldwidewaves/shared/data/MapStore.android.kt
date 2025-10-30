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
import com.google.android.play.core.splitcompat.SplitCompat
import com.worldwidewaves.shared.events.data.GeoJsonDataProvider
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "MapStore"

// File I/O Constants
private const val BUFFER_SIZE_BYTES = 64 * 1024 // 64KB buffer for efficient file operations
private const val MAX_FILE_COPY_RETRIES = 3
private const val RETRY_DELAY_MS = 100L
private const val FETCH_RETRY_DELAY_MS = 120L
private const val MIN_ANDROID_VERSION_FOR_SPLIT_CONTEXT = 26 // Android 8.0 Oreo

/**
 * Cache for split contexts to avoid repeated createContextForSplit() and SplitCompat.install() calls.
 * Key: eventId, Value: split context
 */
private val splitContextCache = mutableMapOf<String, Context>()

private fun ctx(): Context = inject<Context>(Context::class.java).value

actual suspend fun platformTryCopyInitialTagToCache(
    eventId: String,
    extension: String,
    destAbsolutePath: String,
): Boolean {
    val base = ctx()
    val assetName = "$eventId.$extension"

    var finalResult: CopyResult? = null

    repeat(MAX_FILE_COPY_RETRIES) { attempt ->
        val result = attemptFileCopy(base, eventId, assetName, destAbsolutePath)
        finalResult = result
        when (result) {
            CopyResult.Success, CopyResult.FatalError -> return@repeat
            CopyResult.Retry ->
                if (attempt < MAX_FILE_COPY_RETRIES - 1) {
                    delay(RETRY_DELAY_MS.milliseconds)
                }
        }
    }

    if (finalResult != CopyResult.Success) {
        Log.d(TAG, "platformTryCopyInitialTagToCache: not found for $assetName")
    }
    return finalResult == CopyResult.Success
}

/**
 * Result type for file copy operations.
 */
private enum class CopyResult {
    Success,
    Retry,
    FatalError,
}

/**
 * Attempts a single file copy operation from assets to cache.
 */
private fun attemptFileCopy(
    baseContext: Context,
    eventId: String,
    assetName: String,
    destAbsolutePath: String,
): CopyResult =
    try {
        // Use cached split context (SplitCompat.install already called in cache)
        val splitCtx = createSplitContext(baseContext, eventId)

        copyAssetToFile(splitCtx, assetName, destAbsolutePath)

        Log.d(TAG, "platformTryCopyInitialTagToCache: copied $assetName → $destAbsolutePath")
        CopyResult.Success
    } catch (e: FileNotFoundException) {
        Log.d(TAG, "platformTryCopyInitialTagToCache: file not found for $assetName, will retry: ${e.message}")
        CopyResult.Retry
    } catch (e: Exception) {
        Log.d(TAG, "platformTryCopyInitialTagToCache: error for $assetName: ${e.message}")
        CopyResult.FatalError
    }

/**
 * Creates or retrieves a cached split context for the given event ID (Android 8.0+).
 * Caches the result to avoid repeated createContextForSplit() and SplitCompat.install() overhead.
 */
private fun createSplitContext(
    baseContext: Context,
    eventId: String,
): Context =
    if (Build.VERSION.SDK_INT >= MIN_ANDROID_VERSION_FOR_SPLIT_CONTEXT) {
        splitContextCache.getOrPut(eventId) {
            runCatching {
                val splitCtx = baseContext.createContextForSplit(eventId)
                // Install split compatibility immediately when creating context
                SplitCompat.install(splitCtx)
                Log.d(TAG, "Created and cached split context for $eventId")
                splitCtx
            }.getOrElse {
                Log.w(TAG, "Failed to create split context for $eventId, using base context")
                baseContext
            }
        }
    } else {
        baseContext
    }

/**
 * Copies an asset file to the specified destination path with buffering.
 */
private fun copyAssetToFile(
    context: Context,
    assetName: String,
    destAbsolutePath: String,
) {
    val dest = File(destAbsolutePath)
    dest.parentFile?.mkdirs()

    context.assets.open(assetName).use { input ->
        BufferedInputStream(input, BUFFER_SIZE_BYTES).use { bufferedInput ->
            dest.outputStream().use { output ->
                copyBufferedStream(bufferedInput, output)
            }
        }
    }
}

/**
 * Copies data from a buffered input stream to an output stream.
 */
private fun copyBufferedStream(
    input: BufferedInputStream,
    output: FileOutputStream,
) {
    BufferedOutputStream(output, BUFFER_SIZE_BYTES).use { bufferedOutput ->
        val buffer = ByteArray(BUFFER_SIZE_BYTES)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            bufferedOutput.write(buffer, 0, bytesRead)
        }
        bufferedOutput.flush()
    }
}

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

        // Note: Removed isMapAvailable() check to support dynamic feature modules
        // loaded from Android Studio (bundled but not in installedModules).
        // The function handles FileNotFoundException gracefully via retry logic.

        val context: Context by inject(Context::class.java)
        val assetName = "$eventId.$extension"

        val result = attemptFetchWithRetries(context, eventId, assetName, destAbsolutePath)

        if (!result.success) {
            val errorMsg =
                "platformFetchToFile: Failed to fetch $eventId.$extension " +
                    "after $MAX_FILE_COPY_RETRIES attempts: ${result.lastException?.message}"
            Log.e(TAG, errorMsg)
        }
        result.success
    }

/**
 * Result of fetch operation with retries.
 */
private data class FetchResult(
    val success: Boolean,
    val lastException: Exception? = null,
)

/**
 * Attempts to fetch a file with retries.
 */
private suspend fun attemptFetchWithRetries(
    context: Context,
    eventId: String,
    assetName: String,
    destAbsolutePath: String,
): FetchResult {
    var lastException: Exception? = null
    var fetchSucceeded = false

    repeat(MAX_FILE_COPY_RETRIES) { attempt ->
        val result = attemptSingleFetch(context, eventId, assetName, destAbsolutePath, attempt)
        when {
            result.success -> {
                Log.i(TAG, "platformFetchToFile: Successfully fetched $assetName")
                fetchSucceeded = true
                return@repeat
            }
            result.shouldRetry && attempt < MAX_FILE_COPY_RETRIES - 1 -> {
                lastException = result.exception
                delay(FETCH_RETRY_DELAY_MS)
            }
            else -> {
                lastException = result.exception
                return@repeat
            }
        }
    }

    return FetchResult(fetchSucceeded, lastException)
}

/**
 * Result of a single fetch attempt.
 */
private data class SingleFetchResult(
    val success: Boolean,
    val shouldRetry: Boolean = false,
    val exception: Exception? = null,
)

/**
 * Attempts a single file fetch operation.
 */
private fun attemptSingleFetch(
    context: Context,
    eventId: String,
    assetName: String,
    destAbsolutePath: String,
    attempt: Int,
): SingleFetchResult =
    try {
        Log.d(TAG, "platformFetchToFile: Attempt ${attempt + 1}/$MAX_FILE_COPY_RETRIES for $assetName")

        val splitCtx = createSplitContext(context, eventId)
        SplitCompat.install(splitCtx)

        copyAssetToFile(splitCtx, assetName, destAbsolutePath)

        SingleFetchResult(success = true)
    } catch (e: FileNotFoundException) {
        Log.w(TAG, "platformFetchToFile: Asset $assetName not found (attempt ${attempt + 1}/$MAX_FILE_COPY_RETRIES)")
        SingleFetchResult(success = false, shouldRetry = true, exception = e)
    } catch (e: Exception) {
        SingleFetchResult(success = false, shouldRetry = false, exception = e)
    }

actual fun cacheStringToFile(
    fileName: String,
    content: String,
): String? {
    val root = platformCacheRoot()
    val f = File(root, fileName)
    f.parentFile?.mkdirs()
    Log.v(TAG, "cacheStringToFile: Caching data to $fileName")
    return try {
        f.writeText(content)
        val absolutePath = f.toURI().path
        Log.d(TAG, "cacheStringToFile: Successfully cached to: $absolutePath")
        absolutePath
    } catch (e: Exception) {
        Log.e(TAG, "cacheStringToFile: Failed to cache $fileName: ${e.message}", e)
        null
    }
}
