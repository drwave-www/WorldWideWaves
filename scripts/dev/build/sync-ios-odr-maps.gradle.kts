/* * Copyright 2025 DrWave
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
 * limitations under the License. */

/**
 * iOS ODR Maps Synchronization Task
 *
 * Copies map resources from Android modules to iOS ODR bundle structure.
 * Handles both geojson and mbtiles files with MD5 validation for efficiency.
 * Integrates with iOS build process for automated map packaging.
 */

tasks.register("syncIOSODRMaps") {
    group = "iOS ODR"
    description = "Sync map resources from Android modules to iOS ODR bundle"

    val iosResourcesDir = File(projectDir, "iosApp/worldwidewaves/Resources/Maps")
    val androidMapsDir = File(projectDir, "maps/android")

    inputs.dir(androidMapsDir)
    outputs.dir(iosResourcesDir)

    doLast {
        logger.lifecycle("🗺️ Syncing maps from Android modules to iOS ODR bundle...")

        if (!androidMapsDir.exists()) {
            logger.warn("Android maps directory not found: ${androidMapsDir.absolutePath}")
            return@doLast
        }

        // Ensure iOS resources directory exists
        iosResourcesDir.mkdirs()

        var mapsCopied = 0
        var mapsSkipped = 0

        androidMapsDir.listFiles()?.filter { it.isDirectory }?.forEach { mapDir ->
            val mapId = mapDir.name
            logger.info("Processing map: $mapId")

            // Find source files in Android module
            val geojsonSource = File(mapDir, "src/main/assets/${mapId}.geojson")
            val mbtilesSource = File(mapDir, "src/main/assets/${mapId}.mbtiles")

            if (!geojsonSource.exists() || !mbtilesSource.exists()) {
                logger.warn("Skipping $mapId - missing source files (geojson: ${geojsonSource.exists()}, mbtiles: ${mbtilesSource.exists()})")
                mapsSkipped++
                return@forEach
            }

            // Create iOS destination directory
            val iosMapDir = File(iosResourcesDir, mapId)
            iosMapDir.mkdirs()

            val geojsonDest = File(iosMapDir, "${mapId}.geojson")
            val mbtilesDest = File(iosMapDir, "${mapId}.mbtiles")

            // Copy geojson with MD5 check
            if (shouldCopyFile(geojsonSource, geojsonDest)) {
                geojsonSource.copyTo(geojsonDest, overwrite = true)
                logger.info("Copied ${mapId}.geojson to iOS bundle")
            }

            // Copy mbtiles with MD5 check
            if (shouldCopyFile(mbtilesSource, mbtilesDest)) {
                mbtilesSource.copyTo(mbtilesDest, overwrite = true)
                logger.info("Copied ${mapId}.mbtiles to iOS bundle")
            }

            mapsCopied++
        }

        logger.lifecycle("✅ iOS ODR sync completed: $mapsCopied maps processed, $mapsSkipped skipped")
    }
}

/**
 * Helper function to determine if file should be copied based on MD5 comparison
 */
fun shouldCopyFile(source: File, destination: File): Boolean {
    if (!destination.exists()) return true

    val sourceMD5 = source.inputStream().use {
        java.security.MessageDigest.getInstance("MD5").digest(it.readBytes()).joinToString("") {
            "%02x".format(it)
        }
    }

    val destMD5 = destination.inputStream().use {
        java.security.MessageDigest.getInstance("MD5").digest(it.readBytes()).joinToString("") {
            "%02x".format(it)
        }
    }

    return sourceMD5 != destMD5
}

// Integrate with iOS build process
tasks.named("embedAndSignAppleFrameworkForXcode") {
    dependsOn("syncIOSODRMaps")
}