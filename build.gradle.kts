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

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.dynamic.feature) apply false
    alias(libs.plugins.licenseReport) apply false
    alias(libs.plugins.icerock.moko.multiplatform) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

subprojects {
    apply(plugin = "com.github.jk1.dependency-license-report")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    // Configure license report output
    extensions.configure<com.github.jk1.license.LicenseReportExtension>("licenseReport") {
        renderers = arrayOf(
            com.github.jk1.license.render.JsonReportRenderer("licenses-gradle.json")
        )
        filters = arrayOf(
            com.github.jk1.license.filter.LicenseBundleNormalizer()
        )
        outputDir = "${rootProject.layout.buildDirectory.get()}/reports/licenses"
    }

    // Configure ktlint
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension>("ktlint") {
        version.set("1.4.1")
        debug.set(false)
        verbose.set(false)
        android.set(true)
        outputToConsole.set(true)
        outputColorName.set("RED")
        ignoreFailures.set(true)
        enableExperimentalRules.set(false)
        filter {
            exclude("**/generated/**")
            exclude("**/build/**")
            exclude("**/MokoRes.kt")
        }
    }

    // Configure detekt
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        config.setFrom("$rootDir/config/detekt/detekt.yml")
        buildUponDefaultConfig = true
        parallel = true
        ignoreFailures = false
        autoCorrect = false
        setSource(files(
            "src/main/kotlin",
            "src/androidMain/kotlin",
            "src/commonMain/kotlin",
            "src/iosMain/kotlin"
        ))
        include("**/*.kt")
        exclude("**/build/**")
    }
}

// iOS Build Temporary Files Cleanup Task
tasks.register("cleanupIOSTempFiles") {
    group = "iOS Build"
    description = "Clean up temporary files from iOS builds older than 2 days"

    doLast {
        // Skip if in CI environment
        if (System.getenv("CI") != null) {
            logger.info("ℹ️ Skipping iOS temp cleanup in CI environment")
            return@doLast
        }

        // Skip if explicitly disabled
        if (project.hasProperty("skipTempCleanup") && project.property("skipTempCleanup") == "true") {
            logger.info("ℹ️ iOS temp cleanup disabled via skipTempCleanup property")
            return@doLast
        }

        logger.lifecycle("🧹 Cleaning up iOS build temporary files (older than 2 days)...")

        val tempBaseDir = File("/private/var/folders")
        if (!tempBaseDir.exists()) {
            logger.warn("Temp directory not found: ${tempBaseDir.absolutePath}")
            return@doLast
        }

        var filesDeleted = 0
        var bytesFreed = 0L
        val twoDaysAgo = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000L)

        try {
            // Clean Kotlin daemon logs
            tempBaseDir.walkTopDown()
                .filter { it.isFile }
                .filter { it.name.startsWith("kotlin-daemon") && it.name.endsWith(".log") }
                .filter { it.lastModified() < twoDaysAgo }
                .forEach { file ->
                    try {
                        val size = file.length()
                        if (file.delete()) {
                            filesDeleted++
                            bytesFreed += size
                            logger.info("Deleted: ${file.absolutePath}")
                        }
                    } catch (e: Exception) {
                        logger.debug("Could not delete ${file.absolutePath}: ${e.message}")
                    }
                }

            // Clean Kotlin compiler temp directories
            tempBaseDir.walkTopDown()
                .maxDepth(3)
                .filter { it.isDirectory }
                .filter { it.name == "org.jetbrains.kotlin" }
                .filter { it.lastModified() < twoDaysAgo }
                .forEach { dir ->
                    try {
                        val size = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                        if (dir.deleteRecursively()) {
                            filesDeleted++
                            bytesFreed += size
                            logger.info("Deleted directory: ${dir.absolutePath}")
                        }
                    } catch (e: Exception) {
                        logger.debug("Could not delete ${dir.absolutePath}: ${e.message}")
                    }
                }

            val mbFreed = bytesFreed / (1024.0 * 1024.0)
            if (filesDeleted > 0) {
                logger.lifecycle("✅ iOS temp cleanup completed: $filesDeleted items deleted, %.2f MB freed".format(mbFreed))
            } else {
                logger.lifecycle("✅ iOS temp cleanup completed: No old temp files found")
            }
        } catch (e: Exception) {
            logger.warn("iOS temp cleanup encountered an error: ${e.message}")
        }
    }
}

// Hook cleanup into iOS framework build process
afterEvaluate {
    project(":shared").afterEvaluate {
        tasks.findByName("embedAndSignAppleFrameworkForXcode")?.let { embedTask ->
            val cleanupTask = rootProject.tasks.findByName("cleanupIOSTempFiles")
            if (cleanupTask != null) {
                embedTask.finalizedBy(cleanupTask)
            }
        }
    }
}