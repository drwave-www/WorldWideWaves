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
package com.worldwidewaves.shared

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.UIKit.UIDevice

class IOSPlatform: WWWPlatform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override fun getContext(): Any {
        TODO("Not yet implemented")
    }
}

actual fun getPlatform(): WWWPlatform = IOSPlatform()

actual fun getEventImage(type: String, id: String): Any? {
    TODO("Not yet implemented")
}

actual suspend fun getMBTilesAbsoluteFilePath(eventId: String): String? {
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
actual fun cacheStringToFile(fileName: String, content: String) {
    val cacheDir = getCacheDir()
    val filePath = "$cacheDir/$fileName"
    val nsString = NSString.create(string = content)
    nsString.writeToFile(filePath, true, NSUTF8StringEncoding, null)
}

private fun getCacheDir(): String {
    return NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String
}