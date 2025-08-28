/*
 * Copyright 2025 DrWave
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
package com.worldwidewaves.shared

import com.worldwidewaves.shared.di.IOSModule
import com.worldwidewaves.shared.doInitKoin
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.LocalDateTime
import org.koin.core.context.loadKoinModules
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIDevice

fun initKoinIOS() {
    // Initialise Koin only once (see Helper.doInitKoin).
    doInitKoin()
    loadKoinModules(IOSModule)
}

/**
 * Platform descriptor for iOS.  
 * Simply instantiate the common `WWWPlatform` with the device name/version.
 */


actual fun getEventImage(type: String, id: String): Any? {
    TODO("Not yet implemented")
}

actual suspend fun readGeoJson(eventId: String): String? {
    TODO("Not yet implemented")
}

actual suspend fun getMapFileAbsolutePath(eventId: String, extension: String): String? {
    TODO("Not yet implemented")
}

actual fun cachedFileExists(fileName: String): Boolean {
    val cacheDir = getCacheDir()
    val filePath = "$cacheDir/$fileName"
    return NSFileManager.defaultManager.fileExistsAtPath(filePath)
}

actual fun cachedFilePath(fileName: String): String? {
    val cacheDir = getCacheDir()
    val filePath = "$cacheDir/$fileName"
    return NSURL.fileURLWithPath(filePath).absoluteString
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun cacheStringToFile(fileName: String, content: String) : String {
    val cacheDir = getCacheDir()
    val filePath = "$cacheDir/$fileName"
    val nsString = NSString.create(string = content)
    nsString.writeToFile(filePath, true, NSUTF8StringEncoding, null)
    return fileName
}

actual fun getCacheDir(): String {
    return NSSearchPathForDirectoriesInDomains(
        NSCachesDirectory,
        NSUserDomainMask,
        true
    ).first() as String
}

actual suspend fun cacheDeepFile(fileName: String) {
    TODO("Not yet implemented")
}
